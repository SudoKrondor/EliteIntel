package elite.intel.ai.mouth.kokoro;

import com.google.common.eventbus.Subscribe;
import com.k2fsa.sherpa.onnx.*;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import elite.intel.ai.ears.AudioDeviceEnumerator;
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
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Offline TTS using Kokoro via sherpa-onnx JNI.
 * <p>
 * Two-queue pipeline: sentence splitting → synthesis queue → playback queue.
 * Synthesis of sentence N+1 overlaps with playback of sentence N.
 */
public class KokoroTTS implements MouthInterface {

    private static final Logger log = LogManager.getLogger(KokoroTTS.class);

    private static final int SAMPLE_RATE = 24000;
    private static final int DEFAULT_SID = KokoroVoices.GEORGE.getSid();
    /**
     * MAIN: the primary voice engine (handles all narration, including radio, through one queue).
     * RADIO: a radio-only engine that runs alongside a non-Kokoro main mouth (e.g. Google), handling
     * only radio transmissions and ducking behind the main voice via {@link MainVoicePlaybackGate}.
     */
    public enum Role {MAIN, RADIO}

    private static volatile KokoroTTS instance;
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
    private Language lastBuiltLanguage;

    private KokoroTTS() {
    }

    public static KokoroTTS getInstance() {
        if (instance == null) {
            synchronized (KokoroTTS.class) {
                if (instance == null) instance = new KokoroTTS();
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
        log.info("KokoroTTS.start() called from thread: {}", Thread.currentThread().getName());
        try {
            SherpaOnnxNatives.load();
        } catch (Exception e) {
            log.error("KokoroTTS: native lib load failed - TTS unavailable", e);
            return;
        }

        Language currentLanguage = SystemSession.getInstance().getLanguage();
        if (tts == null || lastBuiltLanguage != currentLanguage) {
            if (tts != null) {
                try {
                    tts.release();
                } catch (Exception e) {
                    log.warn("KokoroTTS: tts.release on language switch failed", e);
                }
                tts = null;
            }
            try {
                tts = buildOfflineTts();
                lastBuiltLanguage = currentLanguage;
            } catch (Exception e) {
                log.error("KokoroTTS: engine init failed", e);
                return;
            }
        }

        running = true;
        completeQueuedSpeech();
        interruptRequested.set(false); // ← reset after stop() left it true

        synthesisThread = new Thread(this::processSynthesisQueue, "KokoroTTS-Synthesis");
        synthesisThread.setDaemon(true);
        synthesisThread.start();

        playbackThread = new Thread(this::processPlaybackQueue, "KokoroTTS-Playback");
        playbackThread.setDaemon(true);
        playbackThread.start();

        GameEventBus.register(this);
        log.info("KokoroTTS started ({}) - voice: {} sid={}", role, KokoroVoices.GEORGE.getDisplayName(), DEFAULT_SID);
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
            log.warn("Kokoro is not registered on event bus, ignore");
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
        // tts is intentionally NOT released here. tts.release() crashes in the
        // KokoroMultiLangLexicon destructor (SIGSEGV) due to shared native state with ONNX Runtime.
        // KokoroTTS is a singleton there is exactly one OfflineTts per process lifetime.
        // Native memory is reclaimed when the process exits.
        // Language changes force a rebuild in start(), which releases the old instance at a safe point.
    }

    // -- MouthInterface --------------------------------------------------------

    @Override
    public void interruptAndClear() {
        interruptGeneration.incrementAndGet();
        interruptRequests(null);

        log.info("KokoroTTS interrupted and queues cleared");
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
        // In RADIO role this engine runs alongside a non-Kokoro main mouth and voices radio only;
        // normal narration belongs to the main mouth. In MAIN role it handles everything except radio in a
        // Cyrillic locale, which Kokoro cannot pronounce and Edge voices instead (see RadioVoicing).
        if (role == Role.RADIO && !event.isRadio()) return;
        if (event.isRadio() && !RadioVoicing.isRadioEngine(TtsProvider.KOKORO)) return;
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

            // Split on sentence boundaries and enqueue each piece for synthesis.
            String[] allSentences = sanitizedText.split("(?<=[.,!?])\\s+(?=\\S)");
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
            String voiceName = event.isRadio() && event.getVoiceName() == null
                    ? KokoroVoices.randomRadioVoice(systemSession.getKokoroVoice().name()).name()
                    : event.getVoiceName();
            for (int i = 0; i < sentences.size(); i++) {
                boolean isLast = (i == sentences.size() - 1);
                boolean isRadio = event.isRadio();
                if (!Status.getInstance().isInMainShip()) isRadio = true;
                if (!synthesisQueue.offer(new SynthesisTask(
                        sentences.get(i), voiceName, isRadio, generation, isLast, handle))) {
                    handle.fail(new IllegalStateException("Kokoro synthesis queue rejected vocalisation"));
                    return;
                }
            }
        } catch (RuntimeException failure) {
            handle.fail(failure);
            log.warn("Failed to enqueue Kokoro TTS request", failure);
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

                KokoroVoices voice = task.voiceName() != null
                        ? KokoroVoices.valueOf(task.voiceName())
                        : systemSession.getKokoroVoice();
                int sid = voice.getSid();

                resetNumericLocale();
                GeneratedAudio audio = tts.generate(
                        //Remove dots, TTS say "dot" all the time.
                        task.text().replace(".", " "),
                        sid,
                        1f + systemSession.getSpeechSpeed()
                );

                if (audio == null || audio.getSamples() == null || audio.getSamples().length == 0) {
                    log.warn("KokoroTTS: empty audio for: {}", task.text());
                    task.handle().fail(new IllegalStateException("Kokoro produced empty audio"));
                    continue;
                }
                if (isObsolete(task.handle(), task.generation())) {
                    continue;
                }

                byte[] pcm = floatToPcm16(audio.getSamples());

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
                log.warn("KokoroTTS synthesis error: {}", e.getMessage(), e);
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
            IllegalStateException failure = new IllegalStateException("Kokoro audio output is unavailable");
            failAllSpeech(failure);
            running = false;
            try {
                GameEventBus.unregister(this);
            } catch (IllegalArgumentException ignored) {
                log.debug("Kokoro TTS was already unregistered after audio failure");
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
                playPcm(task.pcm());
                if (task.lastSentence()) {
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
                log.warn("KokoroTTS playback error: {}", e.getMessage(), e);
            } finally {
                if (task != null) {
                    currentPlayback.compareAndSet(task, null);
                }
                UiBus.publish(new AppLogEvent(""));
            }
        }
        closePersistentLine();
    }

    private void playPcm(byte[] audioData) {
        if (persistentLine == null || !persistentLine.isOpen()) {
            if (!openPersistentLine()) {
                throw new IllegalStateException("Kokoro audio output is unavailable");
            }
        }

        // Radio ducks behind the main voice: wait out any ongoing main-voice sentence, then play.
        // The main voice (Google, or Kokoro-as-MAIN) brackets its own playback so radio can see it.
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
                if (interruptRequested.get()) break;
                int remaining = audioData.length - offset;
                int thisChunk = (Math.min(CHUNK, remaining) / frameSize) * frameSize;
                if (thisChunk == 0) break;
                VoiceLevelTap.observe(audioData, offset, thisChunk, fmt);
                persistentLine.write(audioData, offset, thisChunk);
            }

            if (!interruptRequested.get()) persistentLine.drain();
            else persistentLine.flush();
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
            log.info("KokoroTTS audio line: {}Hz 16-bit mono", SAMPLE_RATE);
            return true;
        } catch (Exception e) {
            log.error("KokoroTTS: failed to open audio line", e);
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
                log.warn("KokoroTTS: error closing audio line", e);
            } finally {
                persistentLine = null;
            }
        }
    }

    // -- Engine construction ---------------------------------------------------

    /**
     * The espeak-ng phonemizer language Kokoro reads the text with. This is what decides pronunciation, and
     * it is separate from the voice: a language with no native Kokoro voice (German) is still phonemized
     * correctly here and merely spoken with the accent of whatever voice is selected. Getting this wrong is
     * worse than an accent — German text read with "en-us" rules is mangled, not accented.
     * <p>
     * Cyrillic (RU/UK) has no entry on purpose: it cannot be phonemized at all, so those sessions are
     * answered in English upstream (see {@code AiResponseLanguagePolicy}) and land on the default.
     */
    private static String kokoroLangCode(Language language) {
        return switch (language) {
            case FR -> "fr";
            case ES -> "es";
            case IT -> "it";
            case DE -> "de";
            // Kokoro ships Brazilian Portuguese only, so European Portuguese speaks with a Brazilian accent.
            case PT, PTBZ -> "pt-br";
            default -> "en-us";
        };
    }

    private OfflineTts buildOfflineTts() {
        Path modelDir = AppPaths.getTtsModelDir().resolve("kokoro-multi-lang-v1_0");
        if (!Files.exists(modelDir)) {
            throw new IllegalStateException(
                    "Kokoro model missing at: " + modelDir +
                            " - run the installer to download TTS models.");
        }

        OfflineTtsKokoroModelConfig kokoro = OfflineTtsKokoroModelConfig.builder()
                .setModel(AppPaths.toNativePath(modelDir.resolve("model.onnx")))
                .setVoices(AppPaths.toNativePath(modelDir.resolve("voices.bin")))
                .setTokens(AppPaths.toNativePath(modelDir.resolve("tokens.txt")))
                .setDataDir(AppPaths.toNativePath(modelDir.resolve("espeak-ng-data")))
                .setLang(kokoroLangCode(SystemSession.getInstance().getLanguage()))
                .build();

        OfflineTtsModelConfig modelConfig = OfflineTtsModelConfig.builder()
                .setKokoro(kokoro)
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
            log.warn("KokoroTTS: could not reset LC_NUMERIC locale: {}", e.getMessage());
        }
    }

    // -- Helpers ---------------------------------------------------------------

    private static byte[] floatToPcm16(float[] samples) {
        byte[] out = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            short s = (short) (Math.max(-1f, Math.min(1f, samples[i])) * 32767);
            out[2 * i] = (byte) (s & 0xFF);
            out[2 * i + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return out;
    }


}
