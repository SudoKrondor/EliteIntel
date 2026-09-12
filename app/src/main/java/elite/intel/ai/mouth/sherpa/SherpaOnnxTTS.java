package elite.intel.ai.mouth.sherpa;

import com.google.common.eventbus.Subscribe;
import com.k2fsa.sherpa.onnx.GeneratedAudio;
import com.k2fsa.sherpa.onnx.OfflineTts;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import elite.intel.ai.ears.AudioDeviceEnumerator;
import elite.intel.ai.ears.Resampler;
import elite.intel.ai.mouth.*;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.RadioTransmissionEvent;
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
import elite.intel.util.AudioPlayer;
import elite.intel.util.PlayBeepEvent;
import elite.intel.util.SherpaOnnxNatives;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The offline TTS pipeline every sherpa-onnx engine runs on: sentence splitting, a synthesis queue, a
 * playback queue, interrupts, the persistent audio line, the MAIN / RADIO roles and the ducking under
 * {@link MainVoicePlaybackGate}. Synthesis of sentence N+1 overlaps with playback of sentence N.
 * <p>
 * An engine ({@code KokoroTTS}, {@code SupertonicTTS}) supplies only what differs between models: how the
 * {@link OfflineTts} is built and whether it has to be rebuilt on a language switch, how one sentence is
 * generated, where a sentence ends, and its own voice cast. Everything about queues, threads, handles and
 * the audio line lives here exactly once, so the two engines cannot drift apart on interrupt semantics -
 * they had, when each carried its own copy.
 * <p>
 * Each concrete engine is a singleton holding exactly one {@code OfflineTts} per process lifetime; see
 * {@link #stop()} for why that handle is never released.
 */
public abstract class SherpaOnnxTTS implements MouthInterface {

    /**
     * The rate the rest of the mouth pipeline is fixed at (declicker, radio filter, playback line). A model
     * that generates at another rate is resampled to this one, so nothing downstream sees a second rate.
     */
    protected static final int SAMPLE_RATE = 24000;

    /**
     * MAIN: the primary voice engine (handles all narration, and radio too when it is also the radio engine,
     * through one queue).
     * RADIO: a radio-only engine that runs alongside a main mouth of another engine (Google, or the other
     * local engine), handling only radio transmissions and ducking behind the main voice via
     * {@link MainVoicePlaybackGate}. It is built for the language the transmissions are written in - the game
     * client's - not the commander's (see {@link RadioVoicing#transmissionLanguage()}).
     */
    public enum Role {MAIN, RADIO}

    protected final Logger log = LogManager.getLogger(getClass());
    private final String engineName = getClass().getSimpleName();

    private volatile Role role = Role.MAIN;
    /**
     * Whether this engine owns the radio channel, decided at {@link #start()} - see {@link RadioVoicing}.
     */
    private volatile boolean voicesRadio;

    private final AtomicBoolean interruptRequested = new AtomicBoolean(false);
    private final AtomicLong interruptGeneration = new AtomicLong(0);
    private final AtomicReference<SourceDataLine> currentLine = new AtomicReference<>();
    private final AtomicReference<SynthesisTask> currentSynthesis = new AtomicReference<>();
    private final AtomicReference<PlaybackTask> currentPlayback = new AtomicReference<>();

    /**
     * One sentence waiting for synthesis. {@code language} is the one the text is written in: the commander's
     * for everything we say ourselves, the game client's for a radio transmission (see
     * {@link #languageOf(VocalisationRequestEvent)}).
     */
    private record SynthesisTask(String text, String voiceName, boolean isRadio, Language language,
                                 long generation, boolean lastSentence, VocalisationHandle handle) {
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

    protected final SystemSession systemSession = SystemSession.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private SourceDataLine persistentLine;
    private volatile boolean running = false;
    private Thread synthesisThread;
    private Thread playbackThread;
    private OfflineTts tts;
    private Language builtFor;

    protected SherpaOnnxTTS() {
    }

    // -- What an engine supplies -----------------------------------------------

    /**
     * The provider this engine is, for the radio-engine decision at start.
     */
    protected abstract TtsProvider provider();

    /**
     * Builds the native engine for {@code language}. Called once per process for a model that reads the
     * language per call, and again on every language switch for one that bakes it in (see
     * {@link #rebuildsOnLanguageSwitch()}).
     */
    protected abstract OfflineTts buildOfflineTts(Language language);

    /**
     * Whether the language is fixed at build time, so a switch needs a new {@link OfflineTts}. A model that
     * takes the language on every generate call answers false and is built exactly once.
     */
    protected abstract boolean rebuildsOnLanguageSwitch();

    /**
     * One sentence of speech from the model.
     *
     * @param speed 1.0 is the model's own pace; {@code 1 + speechSpeed} as the commander set it
     */
    protected abstract GeneratedAudio generate(OfflineTts tts, String text, int sid, float speed, Language language);

    /**
     * The regex a transmission is split on, one queue entry per piece.
     */
    protected abstract String sentenceBoundary();

    /**
     * The voice a ship gets from this engine until the commander picks one, and the voice a stored name
     * this engine no longer carries collapses to.
     */
    protected abstract String defaultVoiceName();

    protected abstract boolean isInTheCast(String voiceName);

    /**
     * The speaker index for a voice name, or for the active ship's own voice when {@code voiceName} is null.
     */
    protected abstract int sidOf(String voiceName);

    /**
     * The voice for a radio transmission from {@code speakerKey}: the same speaker draws the same voice, a
     * transmission with no speaker draws at random, and neither is ever the ship's own voice or one reserved
     * for a carrier.
     */
    protected abstract String radioVoiceNameFor(String speakerKey, Set<String> reserved);

    // -- Lifecycle -------------------------------------------------------------

    /**
     * Sets whether this engine acts as the main mouth or the radio-only engine. Must be set before
     * {@link #start()}; a running engine keeps its role until the next stop/start cycle.
     */
    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public synchronized void start() {
        if (running) return;
        log.info("{}.start() called from thread: {}", engineName, Thread.currentThread().getName());
        try {
            SherpaOnnxNatives.load();
        } catch (Exception e) {
            log.error("{}: native lib load failed - TTS unavailable", engineName, e);
            return;
        }

        // A radio-only engine speaks nothing but the game client's prose, so it is built for that language.
        Language builtLanguage = role == Role.RADIO ? RadioVoicing.transmissionLanguage() : systemSession.getLanguage();
        if (!ensureEngineBuilt(builtLanguage)) return;

        voicesRadio = RadioVoicing.isRadioEngine(provider());
        running = true;
        completeQueuedSpeech();
        interruptRequested.set(false); // ← reset after stop() left it true

        synthesisThread = new Thread(this::processSynthesisQueue, engineName + "-Synthesis");
        synthesisThread.setDaemon(true);
        synthesisThread.start();

        playbackThread = new Thread(this::processPlaybackQueue, engineName + "-Playback");
        playbackThread.setDaemon(true);
        playbackThread.start();

        GameEventBus.register(this);
        log.info("{} started ({}, radio: {}) - default voice: {}", engineName, role, voicesRadio, defaultVoiceName());
        // Only the main voice greets on start; the radio-only engine stays silent (its greeting would
        // otherwise be voiced by the main mouth as a normal narration).
        if (role == Role.MAIN) {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.greeting(playerSession.getConfiguredPlayerName())));
        }
    }

    /**
     * The native engine, built for {@code language} when it is not already. A model with the language baked
     * in is released and rebuilt on a switch, here, at the one point where nothing is synthesising - the
     * safe place to release a handle that {@link #stop()} deliberately leaves alone.
     *
     * @return false when the engine could not be built, which leaves this mouth stopped
     */
    private boolean ensureEngineBuilt(Language language) {
        boolean stale = tts != null && rebuildsOnLanguageSwitch() && builtFor != language;
        if (stale) {
            try {
                tts.release();
            } catch (Exception e) {
                log.warn("{}: tts.release on language switch failed", engineName, e);
            }
            tts = null;
        }
        if (tts != null) return true;
        try {
            tts = buildOfflineTts(language);
            builtFor = language;
            return true;
        } catch (Exception e) {
            log.error("{}: engine init failed", engineName, e);
            return false;
        }
    }

    @Override
    public synchronized void stop() {
        running = false;
        try {
            GameEventBus.unregister(this);
        } catch (IllegalArgumentException ignored) {
            log.warn("{} is not registered on event bus, ignore", engineName);
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

        // WHY tts is NOT released here: tts.release() crashed in Kokoro's lexicon destructor (SIGSEGV) due
        // to shared native state with ONNX Runtime. Every engine is a singleton with exactly one OfflineTts
        // per process lifetime, and native memory is reclaimed when the process exits. The one release that
        // is safe - a language switch, in start(), with nothing synthesising - is in ensureEngineBuilt().
    }

    // -- MouthInterface --------------------------------------------------------

    @Override
    public void interruptAndClear() {
        interruptGeneration.incrementAndGet();
        interruptRequests(null);

        log.info("{} interrupted and queues cleared", engineName);
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
        // In RADIO role this engine runs alongside a non-local main mouth and voices radio only;
        // normal narration belongs to the main mouth. In MAIN role it handles everything, radio included,
        // when it is also the radio engine (see RadioVoicing).
        if (role == Role.RADIO && !event.isRadio()) return;
        if (event.isRadio() && !voicesRadio) return;
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

            String[] allSentences = sanitizedText.split(sentenceBoundary());
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
            Language language = languageOf(event);
            for (int i = 0; i < sentences.size(); i++) {
                boolean isLast = (i == sentences.size() - 1);
                boolean isRadio = event.isRadio();
                if (!Status.getInstance().isInMainShip()) isRadio = true;
                if (!synthesisQueue.offer(new SynthesisTask(
                        sentences.get(i), voiceName, isRadio, language, generation, isLast, handle))) {
                    handle.fail(new IllegalStateException(engineName + " synthesis queue rejected vocalisation"));
                    return;
                }
            }
        } catch (RuntimeException failure) {
            handle.fail(failure);
            log.warn("Failed to enqueue {} request", engineName, failure);
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
     * A name this engine no longer carries collapses to {@link #defaultVoiceName()}. A cast may shrink - a
     * voice that breaks immersion is removed from it - but a carrier that was given that voice still has the
     * name stored in the database. Resolving it strictly would throw on the synthesis thread and drop the
     * line, so that carrier would fall silent for good with only a warning in the log. Drawing a stranger
     * instead would be worse in its own way: the draw happens once per transmission, so the commander's own
     * carrier would answer in a different voice every message, and being recognisable is the whole reason it
     * was given a voice. One fixed default keeps it one speaker until the commander picks again.
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
                    ? radioVoiceNameFor(event.getSpeakerKey(), event.getReservedVoices())
                    : null;
        }
        if (!isInTheCast(named)) {
            log.warn("{} no longer carries the voice '{}'; speaking as {} until it is picked again",
                    engineName, named, defaultVoiceName());
            return defaultVoiceName();
        }
        return named;
    }

    /**
     * The language the request's text is written in. A radio transmission is the game client's own prose,
     * in the client's language; everything else - narration, a carrier voice audition, a system callout -
     * we wrote ourselves in the commander's. The distinction is the origin, not the radio flag: an audition
     * is flagged radio so it gets the transmission filter, but its words are ours.
     * <p>
     * Only a model that takes the language per call (Supertonic) can honour a different one per task; Kokoro
     * has it baked in at build time and reads every task with the language it was built for.
     */
    private Language languageOf(VocalisationRequestEvent event) {
        return event.getOriginType() == RadioTransmissionEvent.class
                ? RadioVoicing.transmissionLanguage()
                : systemSession.getLanguage();
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

                resetNumericLocale();
                GeneratedAudio audio = generate(
                        tts,
                        //Remove dots, TTS say "dot" all the time.
                        task.text().replace(".", " "),
                        sidOf(task.voiceName()),
                        1f + systemSession.getSpeechSpeed(),
                        task.language()
                );

                if (audio == null || audio.getSamples() == null || audio.getSamples().length == 0) {
                    log.warn("{}: empty audio for: {}", engineName, task.text());
                    task.handle().fail(new IllegalStateException(engineName + " produced empty audio"));
                    continue;
                }
                if (isObsolete(task.handle(), task.generation())) {
                    continue;
                }

                byte[] pcm = floatToPcm16(audio.getSamples());
                // A model generating at its own rate (Supertonic 3 at 44100 Hz) is brought to the pipeline's
                // rate here. Playing a higher-rate clip through a lower-rate line stretches it out and drops
                // the pitch, sounding slow.
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
                log.warn("{} synthesis error: {}", engineName, e.getMessage(), e);
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
            IllegalStateException failure = new IllegalStateException(engineName + " audio output is unavailable");
            failAllSpeech(failure);
            running = false;
            try {
                GameEventBus.unregister(this);
            } catch (IllegalArgumentException ignored) {
                log.debug("{} was already unregistered after audio failure", engineName);
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
                log.warn("{} playback error: {}", engineName, e.getMessage(), e);
            } finally {
                if (task != null) {
                    currentPlayback.compareAndSet(task, null);
                }
                UiBus.publish(new AppLogEvent(""));
            }
        }
        closePersistentLine();
    }

    /**
     * Plays one sentence, stopping early when its request has been interrupted, completed elsewhere, or
     * superseded by a newer generation.
     *
     * @return whether the whole clip was played out, which is what lets the last sentence complete its handle;
     * an interrupted clip's handle was completed by whoever interrupted it
     */
    private boolean playPcm(PlaybackTask task) {
        byte[] audioData = task.pcm();
        if (persistentLine == null || !persistentLine.isOpen()) {
            if (!openPersistentLine()) {
                throw new IllegalStateException(engineName + " audio output is unavailable");
            }
        }

        // Radio ducks behind the main voice: wait out any ongoing main-voice sentence, then play.
        // The main voice (Google, or a local engine as MAIN) brackets its own playback so radio can see it.
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
                if (!stillWanted(task)) {
                    break;
                }
                int remaining = audioData.length - offset;
                int thisChunk = (Math.min(CHUNK, remaining) / frameSize) * frameSize;
                if (thisChunk == 0) break;
                VoiceLevelTap.observe(audioData, offset, thisChunk, fmt);
                persistentLine.write(audioData, offset, thisChunk);
            }

            boolean completed = stillWanted(task);
            if (completed) persistentLine.drain();
            else persistentLine.flush();
            return completed;
        } finally {
            currentLine.set(null);
            interruptRequested.set(false);
            if (role != Role.RADIO) MainVoicePlaybackGate.end();
        }
    }

    private boolean stillWanted(PlaybackTask task) {
        return !interruptRequested.get()
                && !task.handle().isDone()
                && (!task.handle().interruptible() || task.generation() == interruptGeneration.get());
    }

    private boolean openPersistentLine() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            Mixer.Info outputMixer = AudioDeviceEnumerator.resolveOutputDevice(systemSession.getAudioOutputDevice());
            persistentLine = AudioDeviceEnumerator.openOutputLine(info, outputMixer);
            persistentLine.open(format, (int) (format.getFrameSize() * format.getSampleRate() / 10));
            persistentLine.start();
            log.info("{} audio line: {}Hz 16-bit mono", engineName, SAMPLE_RATE);
            return true;
        } catch (Exception e) {
            log.error("{}: failed to open audio line", engineName, e);
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
                log.warn("{}: error closing audio line", engineName, e);
            } finally {
                persistentLine = null;
            }
        }
    }

    // -- Locale fix ------------------------------------------------------------

    /**
     * ONNX Runtime (initialized by OfflineRecognizer / Parakeet STT) calls setlocale()
     * which can change LC_NUMERIC to the system locale (e.g. de_DE uses "," as decimal).
     * Kokoro's espeak-ng phonemizer calls stof() inside Generate(), which is locale-sensitive and
     * crashes with std::invalid_argument if LC_NUMERIC is not "C". Reset before every generate() call so
     * Parakeet's init can't corrupt synthesis. Applied for every engine rather than only Kokoro: it costs
     * one libc call, and which of Supertonic's native front-end parsers are locale-sensitive is not known.
     */
    private interface CLib extends Library {
        String setlocale(int category, String locale);
    }

    private void resetNumericLocale() {
        try {
            // LC_NUMERIC: Linux=1, macOS=4, Windows=2
            int LC_NUMERIC = Platform.isLinux() ? 1 : Platform.isMac() ? 4 : 2;
            String libName = Platform.isWindows() ? "msvcrt" : "c";
            Native.load(libName, CLib.class).setlocale(LC_NUMERIC, "C");
        } catch (Throwable e) {
            log.warn("{}: could not reset LC_NUMERIC locale: {}", engineName, e.getMessage());
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
