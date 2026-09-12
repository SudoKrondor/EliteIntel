package elite.intel.ai.mouth.supertonic;

import com.google.common.eventbus.Subscribe;
import com.k2fsa.sherpa.onnx.*;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import elite.intel.ai.ears.AudioDeviceEnumerator;
import elite.intel.ai.ears.Resampler;
import elite.intel.ai.mouth.*;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AiResponseLogEvent;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Offline TTS using Supertonic 3 via sherpa-onnx JNI.
 * <p>
 * Two-queue pipeline: sentence splitting → synthesis queue → playback queue.
 * Synthesis of sentence N+1 overlaps with playback of sentence N.
 * <p>
 * The alternative local engine, beside Kokoro: the one the Cyrillic locales get, because Kokoro cannot read
 * them, and a choice for everyone else (see {@link TtsProvider}). Unlike Kokoro, Supertonic is a single
 * multilingual model: the language is not baked into the engine at build time, it is passed on every
 * {@code generate} call (see {@link #supertonicLangCode(Language)}). The engine is therefore built exactly
 * once and never rebuilt on a language switch.
 */
public class SupertonicTTS implements MouthInterface {

    private static final Logger log = LogManager.getLogger(SupertonicTTS.class);

    private static final int SAMPLE_RATE = 24000;
    private static final int DEFAULT_SID = SupertonicVoices.DEFAULT_VOICE.getSid();
    private static final int NUM_STEPS = 8;

    /**
     * MAIN: the primary voice engine (handles all narration, including radio, through one queue).
     * RADIO: a radio-only engine that runs alongside a non-Supertonic main mouth (e.g. Google), handling
     * only radio transmissions and ducking behind the main voice via {@link MainVoicePlaybackGate}.
     */
    public enum Role {MAIN, RADIO}

    private static volatile SupertonicTTS instance;
    private volatile Role role = Role.MAIN;

    private final AtomicBoolean interruptRequested = new AtomicBoolean(false);
    private final AtomicLong interruptGeneration = new AtomicLong(0);
    private final AtomicReference<SourceDataLine> currentLine = new AtomicReference<>();
    private final AtomicReference<SynthesisTask> currentSynthesis = new AtomicReference<>();
    private final AtomicReference<PlaybackTask> currentPlayback = new AtomicReference<>();

    private record SynthesisTask(String text, String voiceName, boolean isRadio, long generation,
                                 boolean lastSentence, VocalisationHandle handle) {
    }

    /**
     * Synthesized PCM paired with an optional completion future from the originating request.
     */
    private record PlaybackTask(byte[] pcm, long generation, boolean lastSentence, VocalisationHandle handle) {
    }

    // Stage 1: raw sentence strings waiting for synthesis
    private final BlockingQueue<SynthesisTask> synthesisQueue = new LinkedBlockingQueue<>();
    // Stage 2: synthesized PCM waiting for playback
    private final BlockingQueue<PlaybackTask> playbackQueue = new LinkedBlockingQueue<>();

    private final SystemSession systemSession = SystemSession.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private SourceDataLine persistentLine;
    private volatile boolean running = false;
    private Thread synthesisThread;
    private Thread playbackThread;
    private OfflineTts tts;

    private SupertonicTTS() {
    }

    public static SupertonicTTS getInstance() {
        if (instance == null) {
            synchronized (SupertonicTTS.class) {
                if (instance == null) instance = new SupertonicTTS();
            }
        }
        return instance;
    }

    /**
     * Sets whether this engine acts as the main mouth or the radio-only engine. Must be set before
     * {@link #start()}; a running engine keeps its role until the next stop/start cycle.
     */
    public void setRole(Role role) {
        this.role = role;
    }

    // -- Lifecycle -------------------------------------------------------------

    @Override
    public synchronized void start() {
        if (running) return;
        log.info("SupertonicTTS.start() called from thread: {}", Thread.currentThread().getName());
        try {
            SherpaOnnxNatives.load();
        } catch (Exception e) {
            log.error("SupertonicTTS: native lib load failed - TTS unavailable", e);
            return;
        }

        if (tts == null) {
            try {
                tts = buildOfflineTts();
            } catch (Exception e) {
                log.error("SupertonicTTS: engine init failed", e);
                return;
            }
        }

        running = true;
        completeQueuedSpeech();
        interruptRequested.set(false); // ← reset after stop() left it true

        synthesisThread = new Thread(this::processSynthesisQueue, "SupertonicTTS-Synthesis");
        synthesisThread.setDaemon(true);
        synthesisThread.start();

        playbackThread = new Thread(this::processPlaybackQueue, "SupertonicTTS-Playback");
        playbackThread.setDaemon(true);
        playbackThread.start();

        GameEventBus.register(this);
        log.info("SupertonicTTS started ({}) - voice: {} sid={}", role, SupertonicVoices.DEFAULT_VOICE.getDisplayName(), DEFAULT_SID);
        // Only the main voice greets on start; the radio-only engine stays silent (its greeting would
        // otherwise be voiced by the main mouth as a normal narration).
        if (role == Role.MAIN) {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.greeting(playerSession.getConfiguredPlayerName())));
        }
    }

    @Override
    public synchronized void stop() {
        running = false;
        try {
            GameEventBus.unregister(this);
        } catch (IllegalArgumentException ignored) {
            log.warn("Supertonic is not registered on event bus, ignore");
        }
        interruptGeneration.incrementAndGet();
        interruptRequested.set(true);
        completeAllSpeech();

        if (persistentLine != null && persistentLine.isOpen()) {
            try {
                persistentLine.stop();
                persistentLine.flush();
            } catch (Exception ignored) {
            }
        }

        if (synthesisThread != null) {
            synthesisThread.interrupt();
            try {
                synthesisThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                synthesisThread = null;
            }
        }
        if (playbackThread != null) {
            playbackThread.interrupt();
            try {
                playbackThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                playbackThread = null;
            }
        }

        closePersistentLine();

        // NOTE:
        // tts is intentionally NOT released here. tts.release() crashed in Kokoro's lexicon destructor
        // (SIGSEGV) due to shared native state with ONNX Runtime, and SupertonicTTS is a singleton - there is
        // exactly one OfflineTts per process lifetime. Native memory is reclaimed when the process exits.
        // Supertonic never needs a rebuild for a language switch, so this is purely the "never release a
        // singleton's native handle mid-process" precaution rather than a language seam.
    }

    // -- MouthInterface --------------------------------------------------------

    @Override
    public void interruptAndClear() {
        interruptGeneration.incrementAndGet();
        interruptRequests(null);

        log.info("SupertonicTTS interrupted and queues cleared");
    }

    @Subscribe
    public void shutUp(TTSInterruptEvent event) {
        if (event.requestId() == null) {
            interruptAndClear();
        } else {
            interruptRequests(event.requestId());
        }
    }

    @Override
    @Subscribe
    public void onVoiceProcessEvent(VocalisationRequestEvent event) {
        // In RADIO role this engine runs alongside a non-Supertonic main mouth and voices radio only;
        // normal narration belongs to the main mouth.
        if (role == Role.RADIO && !event.isRadio()) return;
        if (event.isRadio() && !RadioVoicing.isRadioEngine(TtsProvider.SUPERTONIC)) return;
        if (!running) {
            return;
        }
        VocalisationHandle handle = event.handle();
        if (!handle.claimForPlayback()) {
            return;
        }

        try {
            String sanitizedText = StringUtls.sanitizeTts(event.getText());
            if (sanitizedText == null || sanitizedText.isBlank()) {
                handle.fail(new IllegalArgumentException("Vocalisation text is blank after TTS sanitization"));
                return;
            }

            GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_2));
            UiBus.publish(new AiResponseLogEvent(sanitizedText, event.getSpeaker()));

            // Split only on sentence-ending punctuation. Keeping comma clauses together lets Supertonic
            // control the comma pause through its own prosody instead of adding a queue boundary and gap.
            String[] allSentences = sanitizedText.split("(?<=[.!?])\\s+(?=\\S)");
            List<String> sentences = new ArrayList<>();
            for (String sentence : allSentences) {
                if (!sentence.isBlank()) sentences.add(sentence);
            }
            if (sentences.isEmpty()) {
                handle.fail(new IllegalArgumentException("Vocalisation contains no speakable sentences"));
                return;
            }
            long generation = interruptGeneration.get();
            // One voice for the whole transmission: the draw happens here, not per sentence, or a station
            // would change speaker mid-message.
            String voiceName = resolveVoiceName(event);
            for (int i = 0; i < sentences.size(); i++) {
                boolean isLast = (i == sentences.size() - 1);
                boolean isRadio = event.isRadio();
                if (!Status.getInstance().isInMainShip()) isRadio = true;
                if (!synthesisQueue.offer(new SynthesisTask(
                        sentences.get(i), voiceName, isRadio, generation, isLast, handle))) {
                    handle.fail(new IllegalStateException("Supertonic synthesis queue rejected vocalisation"));
                    return;
                }
            }
        } catch (RuntimeException failure) {
            handle.fail(failure);
            log.warn("Failed to enqueue Supertonic TTS request", failure);
        }
    }

    private void interruptRequests(String requestId) {
        long liveGeneration = interruptGeneration.get();
        for (SynthesisTask task : new ArrayList<>(synthesisQueue)) {
            if (shouldInterrupt(task.handle(), task.generation(), requestId, liveGeneration)
                    && synthesisQueue.remove(task)) {
                task.handle().complete();
            }
        }
        for (PlaybackTask task : new ArrayList<>(playbackQueue)) {
            if (shouldInterrupt(task.handle(), task.generation(), requestId, liveGeneration)
                    && playbackQueue.remove(task)) {
                task.handle().complete();
            }
        }

        SynthesisTask synthesis = currentSynthesis.get();
        if (synthesis != null
                && shouldInterrupt(synthesis.handle(), synthesis.generation(), requestId, liveGeneration)) {
            synthesis.handle().complete();
        }
        PlaybackTask playback = currentPlayback.get();
        if (playback == null
                || !shouldInterrupt(playback.handle(), playback.generation(), requestId, liveGeneration)) {
            return;
        }
        playback.handle().complete();
        interruptRequested.set(true);
        SourceDataLine line = currentLine.get();
        if (line != null && line.isOpen()) {
            line.stop();
            line.flush();
            line.start();
        }
    }

    private static boolean shouldInterrupt(
            VocalisationHandle handle,
            long taskGeneration,
            String requestId,
            long liveGeneration
    ) {
        if (!handle.interruptible()) {
            return false;
        }
        return requestId == null
                ? taskGeneration != liveGeneration
                : requestId.equals(handle.requestId());
    }

    private boolean isObsolete(VocalisationHandle handle, long taskGeneration) {
        if (handle.isDone()) {
            return true;
        }
        if (handle.interruptible() && taskGeneration != interruptGeneration.get()) {
            handle.complete();
            return true;
        }
        return false;
    }

    private void completeQueuedSpeech() {
        List<SynthesisTask> synthesis = new ArrayList<>();
        synthesisQueue.drainTo(synthesis);
        synthesis.forEach(task -> task.handle().complete());
        List<PlaybackTask> playback = new ArrayList<>();
        playbackQueue.drainTo(playback);
        playback.forEach(task -> task.handle().complete());
    }

    private void completeAllSpeech() {
        completeQueuedSpeech();
        SynthesisTask synthesis = currentSynthesis.get();
        if (synthesis != null) {
            synthesis.handle().complete();
        }
        PlaybackTask playback = currentPlayback.get();
        if (playback != null) {
            playback.handle().complete();
        }
    }

    private void failAllSpeech(Throwable failure) {
        List<SynthesisTask> synthesis = new ArrayList<>();
        synthesisQueue.drainTo(synthesis);
        synthesis.forEach(task -> task.handle().fail(failure));
        List<PlaybackTask> playback = new ArrayList<>();
        playbackQueue.drainTo(playback);
        playback.forEach(task -> task.handle().fail(failure));
        SynthesisTask activeSynthesis = currentSynthesis.get();
        if (activeSynthesis != null) {
            activeSynthesis.handle().fail(failure);
        }
        PlaybackTask activePlayback = currentPlayback.get();
        if (activePlayback != null) {
            activePlayback.handle().fail(failure);
        }
    }

    /**
     * The voice for one whole transmission, or {@code null} to use the ship's own.
     * <p>
     * A name this engine no longer carries collapses to {@link SupertonicVoices#DEFAULT_VOICE}. The cast in
     * {@link SupertonicVoices} may shrink in a future release - a voice that breaks immersion would be removed
     * from it - but a carrier that was given that voice still has the name stored in the database. Resolving
     * it strictly would throw on the synthesis thread and drop the line, so that carrier would fall silent
     * for good with only a warning in the log. Drawing a stranger instead would be worse in its own way: the
     * draw happens once per transmission, so the commander's own carrier would answer in a different voice
     * every message, and being recognisable is the whole reason it was given a voice. One fixed default keeps
     * it one speaker until the commander picks again.
     * <p>
     * A transmission that names no voice is voiced by whoever the speaker's name maps to, so one NPC keeps one
     * voice; a transmission with no speaker either is a stranger, and draws at random.
     */
    private String resolveVoiceName(VocalisationRequestEvent event) {
        String named = event.getVoiceName();
        if (named == null) {
            // Keyed on the individual behind the transmission, so the same pirate keeps one voice for the
            // whole encounter instead of sounding like a fresh attacker on every line. Only an NPC pilot
            // carries a key; a station or a police wing has none and stays a stranger.
            return event.isRadio()
                    ? SupertonicVoices.radioVoiceFor(event.getSpeakerKey(), systemSession.getSupertonicVoice().name(),
                    event.getReservedVoices()).name()
                    : null;
        }
        if (!isInTheCast(named)) {
            log.warn("Supertonic no longer carries the voice '{}'; speaking as {} until it is picked again",
                    named, SupertonicVoices.DEFAULT_VOICE.name());
            return SupertonicVoices.DEFAULT_VOICE.name();
        }
        return named;
    }

    private static boolean isInTheCast(String voiceName) {
        return Arrays.stream(SupertonicVoices.values()).anyMatch(voice -> voice.name().equals(voiceName));
    }

    // -- Stage 1: Synthesis thread ---------------------------------------------

    private void processSynthesisQueue() {
        while (running) {
            SynthesisTask task = null;
            try {
                task = synthesisQueue.take();
                currentSynthesis.set(task);
                if (isObsolete(task.handle(), task.generation())) {
                    continue;
                }

                SupertonicVoices voice = task.voiceName() != null
                        ? SupertonicVoices.voiceOrDefault(task.voiceName())
                        : systemSession.getSupertonicVoice();
                int sid = voice.getSid();

                resetNumericLocale();

                GenerationConfig genConfig = new GenerationConfig();
                genConfig.setSid(sid);
                genConfig.setSpeed(1f + systemSession.getSpeechSpeed());
                genConfig.setNumSteps(NUM_STEPS);
                genConfig.setExtra(Map.of("lang", supertonicLangCode(SystemSession.getInstance().getLanguage())));

                GeneratedAudio audio = tts.generateWithConfigAndCallback(
                        //Remove dots, TTS say "dot" all the time.
                        task.text().replace(".", " "),
                        genConfig,
                        (java.util.function.Consumer<float[]>) samples -> { /* no streaming consumer needed */ });

                if (audio == null || audio.getSamples() == null || audio.getSamples().length == 0) {
                    log.warn("SupertonicTTS: empty audio for: {}", task.text());
                    task.handle().fail(new IllegalStateException("Supertonic produced empty audio"));
                    continue;
                }
                if (isObsolete(task.handle(), task.generation())) {
                    continue;
                }

                byte[] pcm = floatToPcm16(audio.getSamples());
                // Supertonic 3 generates at its own native rate (44100 Hz); the rest of the mouth pipeline
                // (declicker, radio filter, playback line) is fixed at SAMPLE_RATE. Playing a higher-rate
                // clip through a lower-rate line stretches it out and drops the pitch, sounding slow.
                if (audio.getSampleRate() != SAMPLE_RATE) {
                    pcm = new Resampler(audio.getSampleRate(), SAMPLE_RATE, 1).resample(pcm, pcm.length);
                }

                AudioDeClicker.sanitize(pcm, 5);
                AudioDeClicker.applyVolume(pcm, systemSession.getVoiceVolume() / 100f);
                if (task.isRadio()) {
                    RadioFilter.apply(pcm);
                }
                playbackQueue.put(new PlaybackTask(
                        pcm, task.generation(), task.lastSentence(), task.handle()));

            } catch (InterruptedException e) {
                if (task != null) {
                    task.handle().complete();
                }
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                if (task != null) {
                    task.handle().fail(e);
                }
                log.warn("SupertonicTTS synthesis error: {}", e.getMessage(), e);
            } finally {
                if (task != null) {
                    currentSynthesis.compareAndSet(task, null);
                }
            }
        }
    }

    // -- Stage 2: Playback thread ----------------------------------------------

    private void processPlaybackQueue() {
        if (!openPersistentLine()) {
            IllegalStateException failure = new IllegalStateException("Supertonic audio output is unavailable");
            failAllSpeech(failure);
            running = false;
            try {
                GameEventBus.unregister(this);
            } catch (IllegalArgumentException ignored) {
                log.debug("Supertonic TTS was already unregistered after audio failure");
            }
            if (synthesisThread != null) {
                synthesisThread.interrupt();
            }
            return;
        }

        while (running) {
            PlaybackTask task = null;
            try {
                task = playbackQueue.poll(200, TimeUnit.MILLISECONDS);
                if (task == null) continue;
                currentPlayback.set(task);
                if (isObsolete(task.handle(), task.generation())) {
                    continue;
                }

                interruptRequested.set(false);
                boolean completed = playPcm(task);
                if (completed && task.lastSentence()) {
                    task.handle().complete();
                }

            } catch (InterruptedException e) {
                if (task != null) {
                    task.handle().complete();
                }
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                if (task != null) {
                    task.handle().fail(e);
                }
                log.warn("SupertonicTTS playback error: {}", e.getMessage(), e);
            } finally {
                if (task != null) {
                    currentPlayback.compareAndSet(task, null);
                }
                UiBus.publish(new AppLogEvent(""));
            }
        }
        closePersistentLine();
    }

    private boolean playPcm(PlaybackTask task) {
        byte[] audioData = task.pcm();
        if (persistentLine == null || !persistentLine.isOpen()) {
            if (!openPersistentLine()) {
                throw new IllegalStateException("Supertonic audio output is unavailable");
            }
        }

        // Radio ducks behind the main voice: wait out any ongoing main-voice sentence, then play.
        // The main voice (Google, or Supertonic-as-MAIN) brackets its own playback so radio can see it.
        if (role == Role.RADIO) {
            MainVoicePlaybackGate.awaitIdleForRadio();
        } else {
            MainVoicePlaybackGate.begin();
        }

        try {
            currentLine.set(persistentLine);

            AudioFormat fmt = persistentLine.getFormat();
            int frameSize = fmt.getFrameSize();

            // Small silence gap between sentences
            byte[] silence = new byte[(int) (SAMPLE_RATE * 0.03f) * frameSize];
            persistentLine.write(silence, 0, silence.length);

            final int CHUNK = 8192;
            for (int offset = 0; offset < audioData.length; offset += CHUNK) {
                if (interruptRequested.get()
                        || task.handle().isDone()
                        || (task.handle().interruptible() && task.generation() != interruptGeneration.get())) {
                    break;
                }
                int remaining = audioData.length - offset;
                int thisChunk = (Math.min(CHUNK, remaining) / frameSize) * frameSize;
                if (thisChunk == 0) break;
                VoiceLevelTap.observe(audioData, offset, thisChunk, fmt);
                persistentLine.write(audioData, offset, thisChunk);
            }

            boolean completed = !interruptRequested.get()
                    && !task.handle().isDone()
                    && (!task.handle().interruptible() || task.generation() == interruptGeneration.get());
            if (completed) persistentLine.drain();
            else persistentLine.flush();
            return completed;
        } finally {
            currentLine.set(null);
            interruptRequested.set(false);
            if (role != Role.RADIO) MainVoicePlaybackGate.end();
        }
    }

    private boolean openPersistentLine() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            Mixer.Info outputMixer = AudioDeviceEnumerator.resolveOutputDevice(systemSession.getAudioOutputDevice());
            persistentLine = AudioDeviceEnumerator.openOutputLine(info, outputMixer);
            persistentLine.open(format, (int) (format.getFrameSize() * format.getSampleRate() / 10));
            persistentLine.start();
            log.info("SupertonicTTS audio line: {}Hz 16-bit mono", SAMPLE_RATE);
            return true;
        } catch (Exception e) {
            log.error("SupertonicTTS: failed to open audio line", e);
            return false;
        }
    }

    private void closePersistentLine() {
        if (persistentLine != null && persistentLine.isOpen()) {
            try {
                if (interruptRequested.get()) {
                    persistentLine.flush(); // forced stop - discard buffered audio immediately
                } else {
                    persistentLine.drain(); // normal end - play out remaining audio
                }
                persistentLine.stop();
                persistentLine.close();
            } catch (Exception e) {
                log.warn("SupertonicTTS: error closing audio line", e);
            } finally {
                persistentLine = null;
            }
        }
    }

    // -- Engine construction ---------------------------------------------------

    /**
     * The language code Supertonic reads the text with, passed per-call via {@link GenerationConfig#setExtra}
     * rather than baked into the model at build time (see {@link #buildOfflineTts()}). Supertonic ships one
     * multilingual model covering all nine languages this app supports, Cyrillic included, so unlike Kokoro
     * there is no language this falls back to English for - which is why it is the stand-in for Kokoro in
     * the Cyrillic locales (see {@link TtsProvider#forLanguage}).
     */
    private static String supertonicLangCode(Language language) {
        return switch (language) {
            case RU -> "ru";
            case UK -> "uk";
            case FR -> "fr";
            case ES -> "es";
            case IT -> "it";
            case DE -> "de";
            // Supertonic ships one Portuguese code; European Portuguese speaks with whatever accent that
            // single model carries, same trade-off Kokoro made with its Brazilian-only Portuguese voice.
            case PT, PTBZ -> "pt";
            default -> "en";
        };
    }

    private OfflineTts buildOfflineTts() {
        Path modelDir = AppPaths.getTtsModelDir().resolve("sherpa-onnx-supertonic-3-tts-int8-2026-05-11");
        if (!Files.exists(modelDir)) {
            throw new IllegalStateException(
                    "Supertonic model missing at: " + modelDir +
                            " - run the installer to download TTS models.");
        }

        OfflineTtsSupertonicModelConfig supertonic = OfflineTtsSupertonicModelConfig.builder()
                .setDurationPredictor(AppPaths.toNativePath(modelDir.resolve("duration_predictor.int8.onnx")))
                .setTextEncoder(AppPaths.toNativePath(modelDir.resolve("text_encoder.int8.onnx")))
                .setVectorEstimator(AppPaths.toNativePath(modelDir.resolve("vector_estimator.int8.onnx")))
                .setVocoder(AppPaths.toNativePath(modelDir.resolve("vocoder.int8.onnx")))
                .setTtsJson(AppPaths.toNativePath(modelDir.resolve("tts.json")))
                .setUnicodeIndexer(AppPaths.toNativePath(modelDir.resolve("unicode_indexer.bin")))
                .setVoiceStyle(AppPaths.toNativePath(modelDir.resolve("voice.bin")))
                .build();

        OfflineTtsModelConfig modelConfig = OfflineTtsModelConfig.builder()
                .setSupertonic(supertonic)
                .setNumThreads(2)
                .setDebug(false)
                .setProvider("cpu")
                .build();

        OfflineTtsConfig config = OfflineTtsConfig.builder()
                .setModel(modelConfig)
                .setMaxNumSentences(1)
                .build();

        return new OfflineTts(config);
    }

    // -- Locale fix ------------------------------------------------------------

    /**
     * ONNX Runtime (initialized by OfflineRecognizer / Parakeet STT) calls setlocale()
     * which can change LC_NUMERIC to the system locale (e.g. de_DE uses "," as decimal).
     * espeak-ng inside Generate() calls stof() which is locale-sensitive and crashes
     * with std::invalid_argument if LC_NUMERIC is not "C".
     * Reset before every generate() call so Parakeet's init can't corrupt TTS synthesis.
     */
    private interface CLib extends Library {
        String setlocale(int category, String locale);
    }

    private static void resetNumericLocale() {
        try {
            // LC_NUMERIC: Linux=1, macOS=4, Windows=2
            int LC_NUMERIC = Platform.isLinux() ? 1 : Platform.isMac() ? 4 : 2;
            String libName = Platform.isWindows() ? "msvcrt" : "c";
            Native.load(libName, CLib.class).setlocale(LC_NUMERIC, "C");
        } catch (Throwable e) {
            log.warn("SupertonicTTS: could not reset LC_NUMERIC locale: {}", e.getMessage());
        }
    }

    /**
     * Converts float PCM samples in [-1, 1] to signed 16-bit little-endian PCM bytes.
     */
    private static byte[] floatToPcm16(float[] samples) {
        byte[] pcm = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            float clamped = Math.max(-1f, Math.min(1f, samples[i]));
            short s = (short) (clamped * 32767f);
            pcm[i * 2] = (byte) (s & 0xFF);
            pcm[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return pcm;
    }
}
