package elite.intel.ai.mouth.edge;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.*;
import elite.intel.ai.mouth.subscribers.events.*;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AiResponseLogEvent;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import elite.intel.util.AudioPlayer;
import elite.intel.util.PlayBeepEvent;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Microsoft Edge consumer Read Aloud mouth. A single synthesis worker downloads and decodes sentences ahead
 * of a single playback worker, preserving request order while overlapping network work with speech output.
 */
public final class EdgeTTSImpl implements MouthInterface {
    private static final Logger log = LogManager.getLogger(EdgeTTSImpl.class);
    private static final EdgeTTSImpl INSTANCE = productionInstance();

    /**
     * MAIN: the primary voice engine, handling all narration (including radio when Edge is also the radio
     * engine) through one queue. RADIO: a radio-only engine running alongside a non-Edge main mouth in the
     * Cyrillic locales Kokoro cannot pronounce, ducking behind the main voice via
     * {@link MainVoicePlaybackGate}.
     */
    public enum Role {MAIN, RADIO}

    private final BlockingQueue<SynthesisTask> synthesisQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<PlaybackTask> playbackQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean interruptRequested = new AtomicBoolean();
    private final AtomicBoolean voiceEnumerationAttempted = new AtomicBoolean();
    private final AtomicLong interruptGeneration = new AtomicLong();
    private final AtomicReference<SynthesisTask> currentSynthesis = new AtomicReference<>();
    private final AtomicReference<SynthesisTask> currentNetworkSynthesis = new AtomicReference<>();
    private final AtomicReference<PlaybackTask> currentPlayback = new AtomicReference<>();
    private final EdgeSynthesisClient client;
    private final EdgeAudioDecoder decoder;
    private final EdgeVoiceProvider voiceProvider;
    private final EdgeAudioOutput audioOutput;
    private final EdgeTtsSettings settings;
    private final boolean publishStartupEvents;

    private volatile Role role = Role.MAIN;
    private volatile boolean running;
    private Thread synthesisThread;
    private Thread playbackThread;

    private record SynthesisTask(
            String text,
            String selectedVoiceName,
            Language language,
            String rate,
            float gain,
            boolean radio,
            Class<? extends BaseVoxEvent> originType,
            long generation,
            boolean lastSentence,
            VocalisationHandle handle
    ) {
    }

    private record PlaybackTask(
            String text,
            String voiceName,
            byte[] pcm,
            Class<? extends BaseVoxEvent> originType,
            long generation,
            boolean lastSentence,
            VocalisationHandle handle
    ) {
    }

    private EdgeTTSImpl(
            EdgeSynthesisClient client,
            EdgeAudioDecoder decoder,
            EdgeVoiceProvider voiceProvider,
            EdgeAudioOutput audioOutput,
            EdgeTtsSettings settings,
            boolean publishStartupEvents
    ) {
        this.client = client;
        this.decoder = decoder;
        this.voiceProvider = voiceProvider;
        this.audioOutput = audioOutput;
        this.settings = settings;
        this.publishStartupEvents = publishStartupEvents;
    }

    EdgeTTSImpl(
            EdgeSynthesisClient client,
            EdgeAudioDecoder decoder,
            EdgeVoiceProvider voiceProvider,
            EdgeAudioOutput audioOutput,
            EdgeTtsSettings settings
    ) {
        this(client, decoder, voiceProvider, audioOutput, settings, false);
    }

    public static EdgeTTSImpl getInstance() {
        return INSTANCE;
    }

    /**
     * Sets whether this engine acts as the main mouth or the radio-only engine. Must be set before
     * {@link #start()}; a running engine keeps its role until the next stop/start cycle.
     */
    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        if (!workersStopped()) {
            log.error("Edge TTS cannot restart while a previous worker is still stopping");
            return;
        }
        try {
            audioOutput.open();
        } catch (Exception e) {
            log.error("Edge TTS failed to open the configured audio output", e);
            UiBus.publish(new AppLogEvent("Edge TTS failed to start: " + e.getMessage()));
            return;
        }

        completeQueuedSpeech();
        interruptRequested.set(false);
        voiceEnumerationAttempted.set(false);
        running = true;
        startWorkers();
        GameEventBus.register(this);
        log.info("Edge Read Aloud TTS started");
        // Only the main voice greets on start; the radio-only engine stays silent (its greeting would
        // otherwise arrive over a comms channel as if a station had said it).
        if (publishStartupEvents && role == Role.MAIN) {
            publishStartupEvents();
        }
    }

    @Override
    public synchronized void stop() {
        unregister();
        running = false;
        interruptGeneration.incrementAndGet();
        interruptRequested.set(true);
        client.cancelAll();
        completeAllSpeech();
        audioOutput.interrupt();
        interruptWorkers();
        joinWorkers();
        audioOutput.close();
        voiceProvider.clear();
        synthesisThread = stoppedReference(synthesisThread);
        playbackThread = stoppedReference(playbackThread);
        log.info("Edge Read Aloud TTS stopped");
    }

    @Override
    public void interruptAndClear() {
        interruptGeneration.incrementAndGet();
        interruptRequests(null);
        log.info("Edge TTS interrupted and queues cleared");
    }

    @Subscribe
    public void shutUp(TTSInterruptEvent event) {
        if (event.requestId() == null) {
            interruptAndClear();
        } else {
            interruptRequests(event.requestId());
        }
    }

    @Subscribe
    @Override
    public synchronized void onVoiceProcessEvent(VocalisationRequestEvent event) {
        if (!running) {
            return;
        }
        // Radio is Kokoro's everywhere it can pronounce the language; Edge takes it only in the Cyrillic
        // locales (see RadioVoicing), where it may be the main mouth or a dedicated radio engine.
        if (role == Role.RADIO && !event.isRadio()) {
            return;
        }
        if (event.isRadio() && RadioVoicing.engineFor(settings.language()) != TtsProvider.EDGE) {
            return;
        }
        VocalisationHandle handle = event.handle();
        if (!handle.claimForPlayback()) {
            return;
        }
        try {
            enqueue(event, handle);
        } catch (RuntimeException e) {
            handle.fail(e);
            log.error("Failed to enqueue Edge TTS request", e);
        }
    }

    private void enqueue(VocalisationRequestEvent event, VocalisationHandle handle) {
        String text = StringUtls.sanitizeCloudSpeech(StringUtls.sanitizeTts(event.getText(), false));
        List<String> sentences = EdgeSentenceSplitter.split(text);
        if (sentences.isEmpty()) {
            throw new IllegalArgumentException("Vocalisation text is blank after TTS sanitization");
        }

        Language language = settings.language();
        // One voice for the whole transmission: the draw happens here, not per sentence, or a station would
        // change speaker mid-message.
        String selected;
        if (event.isRadio()) {
            // A radio request that names a voice comes from a speaker the commander has assigned one to -
            // their own carrier's traffic control. Everyone else on the channel stays a stranger.
            selected = event.getVoiceName() == null
                    // Keyed on the individual behind it, so one pirate keeps one voice across the lines they
                    // send. A station or a police wing carries no key and stays a stranger.
                    ? voiceProvider.radioVoiceNameFor(language, event.getSpeakerKey(),
                    reservedShortNames(event.getReservedVoices()))
                    : EdgeVoices.shortNameOrDefault(event.getVoiceName());
        } else if (event.getVoiceName() == null) {
            selected = settings.selectedVoiceName();
        } else {
            selected = EdgeVoices.shortNameOrDefault(event.getVoiceName());
        }
        String rate = EdgeSsml.rate(settings.speechSpeed());
        float gain = Math.max(0, Math.min(100, settings.voiceVolume())) / 100f;
        long generation = interruptGeneration.get();
        for (int i = 0; i < sentences.size(); i++) {
            synthesisQueue.add(new SynthesisTask(
                    sentences.get(i), selected, language, rate, gain, event.isRadio(),
                    event.getOriginType(), generation, i == sentences.size() - 1, handle));
        }
        publishAccepted(event);
    }

    /**
     * The reserved voices as Edge ShortNames, which is what the draw works in. A name this engine does not
     * know is dropped rather than defaulted: {@code shortNameOrDefault} would answer with the default voice,
     * and reserving that would quietly exclude the one voice every locale is guaranteed to have.
     */
    private static Set<String> reservedShortNames(Set<String> voiceNames) {
        Set<String> shortNames = new HashSet<>();
        for (String name : voiceNames) {
            EdgeVoices voice = EdgeVoices.find(name);
            if (voice != null) shortNames.add(voice.defaultShortName());
        }
        return shortNames;
    }

    private void publishAccepted(VocalisationRequestEvent event) {
        GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_2));
        UiBus.publish(new AiResponseLogEvent(event.getText(), event.getSpeaker()));
    }

    private void processSynthesisQueue() {
        while (running) {
            SynthesisTask task = null;
            try {
                task = synthesisQueue.take();
                currentSynthesis.set(task);
                if (isObsolete(task.handle(), task.generation())) {
                    continue;
                }
                synthesize(task);
            } catch (InterruptedException e) {
                if (task != null) {
                    task.handle().complete();
                }
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // WHY: this is the worker boundary; one provider/decoder failure must settle its handle without
                // orphaning the service threads or preventing later independent requests from being processed.
                if (task != null && !task.handle().isDone()) {
                    failRequest(task, e);
                    log.error("Edge TTS synthesis failed", e);
                } else {
                    log.debug("Edge TTS synthesis ended after cancellation", e);
                }
            } finally {
                if (task != null) {
                    currentSynthesis.compareAndSet(task, null);
                }
            }
        }
    }

    private void synthesize(SynthesisTask task) throws Exception {
        refreshVoicesIfNeeded();
        if (isObsolete(task.handle(), task.generation())) {
            return;
        }
        EdgeVoice voice = task.radio()
                ? voiceProvider.resolveRadio(task.selectedVoiceName(), task.language())
                : voiceProvider.resolve(task.selectedVoiceName(), task.language());
        EdgeSynthesisRequest request = new EdgeSynthesisRequest(
                task.handle().requestId(), task.text(), voice, task.rate());
        byte[] compressed;
        currentNetworkSynthesis.set(task);
        try {
            if (isObsolete(task.handle(), task.generation())) {
                return;
            }
            compressed = client.synthesize(request);
        } finally {
            currentNetworkSynthesis.compareAndSet(task, null);
        }
        if (isObsolete(task.handle(), task.generation())) {
            return;
        }
        byte[] pcm = decoder.decode(compressed);
        validatePcm(pcm);
        if (isObsolete(task.handle(), task.generation())) {
            return;
        }
        AudioDeClicker.sanitize(pcm, 6);
        AudioDeClicker.applyVolume(pcm, task.gain());
        if (task.radio()) {
            // Edge decodes to the 24 kHz mono PCM-16 the filter expects, the same shape Kokoro produces.
            RadioFilter.apply(pcm);
        }
        if (isObsolete(task.handle(), task.generation())) {
            return;
        }
        playbackQueue.put(new PlaybackTask(
                task.text(), voice.shortName(), pcm, task.originType(), task.generation(),
                task.lastSentence(), task.handle()));
    }

    private void refreshVoicesIfNeeded() throws InterruptedException {
        if (voiceProvider.hasAvailableVoices() || !voiceEnumerationAttempted.compareAndSet(false, true)) {
            return;
        }
        try {
            voiceProvider.setAvailableVoices(client.listVoices());
        } catch (java.io.IOException e) {
            // WHY: voice enumeration is an optional enhancement. Known stable fallback voices keep TTS usable
            // during a transient list-endpoint failure, while synthesis failures still fail the request.
            log.warn("Could not enumerate Edge Read Aloud voices; using deterministic fallback: {}", e.getMessage());
        }
    }

    private void processPlaybackQueue() {
        while (running) {
            PlaybackTask task = null;
            try {
                task = playbackQueue.take();
                currentPlayback.set(task);
                if (isObsolete(task.handle(), task.generation())) {
                    continue;
                }
                boolean completed = play(task);
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
                // WHY: this is the playback worker boundary; a device failure settles the affected request and
                // keeps the worker available for a later request if the device recovers.
                if (task != null && !task.handle().isDone()) {
                    task.handle().fail(e);
                }
                log.error("Edge TTS playback failed", e);
            } finally {
                if (task != null) {
                    currentPlayback.compareAndSet(task, null);
                }
                UiBus.publish(new AppLogEvent(""));
            }
        }
    }

    private boolean play(PlaybackTask task) throws Exception {
        // Radio ducks behind the main voice: wait out any ongoing main-voice sentence, then play. As the main
        // mouth this engine instead brackets its own playback so a radio engine can see it.
        boolean radioEngine = role == Role.RADIO;
        if (radioEngine) {
            MainVoicePlaybackGate.awaitIdleForRadio();
        } else {
            MainVoicePlaybackGate.begin();
        }
        try {
            boolean completed = audioOutput.play(task.pcm(), () -> interruptRequested.get()
                    || task.handle().isDone()
                    || (task.handle().interruptible() && task.generation() != interruptGeneration.get()));
            if (completed) {
                log.info("Spoke with Edge voice {}: {}", task.voiceName(), task.text());
                publishCompletionEvent(task.originType());
            }
            return completed;
        } finally {
            interruptRequested.set(false);
            if (!radioEngine) {
                MainVoicePlaybackGate.end();
            }
        }
    }

    private void interruptRequests(String requestId) {
        long liveGeneration = interruptGeneration.get();
        removeInterruptedSynthesis(requestId, liveGeneration);
        removeInterruptedPlayback(requestId, liveGeneration);

        SynthesisTask synthesis = currentSynthesis.get();
        if (synthesis != null && shouldInterrupt(
                synthesis.handle(), synthesis.generation(), requestId, liveGeneration)) {
            synthesis.handle().complete();
            SynthesisTask network = currentNetworkSynthesis.get();
            if (network == synthesis) {
                client.cancel(synthesis.handle().requestId());
            }
        }
        PlaybackTask playback = currentPlayback.get();
        if (playback == null || !shouldInterrupt(
                playback.handle(), playback.generation(), requestId, liveGeneration)) {
            return;
        }
        playback.handle().complete();
        interruptRequested.set(true);
        audioOutput.interrupt();
    }

    private void removeInterruptedSynthesis(String requestId, long liveGeneration) {
        for (SynthesisTask task : new ArrayList<>(synthesisQueue)) {
            if (shouldInterrupt(task.handle(), task.generation(), requestId, liveGeneration)
                    && synthesisQueue.remove(task)) {
                task.handle().complete();
            }
        }
    }

    private void removeInterruptedPlayback(String requestId, long liveGeneration) {
        for (PlaybackTask task : new ArrayList<>(playbackQueue)) {
            if (shouldInterrupt(task.handle(), task.generation(), requestId, liveGeneration)
                    && playbackQueue.remove(task)) {
                task.handle().complete();
            }
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

    private void startWorkers() {
        synthesisThread = new Thread(this::processSynthesisQueue, "EdgeTTS-Synthesis");
        synthesisThread.setDaemon(true);
        synthesisThread.start();
        playbackThread = new Thread(this::processPlaybackQueue, "EdgeTTS-Playback");
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    private void interruptWorkers() {
        if (synthesisThread != null) {
            synthesisThread.interrupt();
        }
        if (playbackThread != null) {
            playbackThread.interrupt();
        }
    }

    private void joinWorkers() {
        joinWorker(synthesisThread);
        joinWorker(playbackThread);
    }

    private static void joinWorker(Thread worker) {
        if (worker == null) {
            return;
        }
        try {
            worker.join(5_000);
            if (worker.isAlive()) {
                log.error("Edge TTS worker did not stop deterministically: {}", worker.getName());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while stopping Edge TTS worker {}", worker.getName(), e);
        }
    }

    private static Thread stoppedReference(Thread worker) {
        return worker == null || !worker.isAlive() ? null : worker;
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

    private void failRequest(SynthesisTask failed, Exception failure) {
        failed.handle().fail(failure);
        synthesisQueue.removeIf(task -> task.handle() == failed.handle());
        playbackQueue.removeIf(task -> task.handle() == failed.handle());
        PlaybackTask playback = currentPlayback.get();
        if (playback != null && playback.handle() == failed.handle()) {
            interruptRequested.set(true);
            audioOutput.interrupt();
        }
    }

    private void unregister() {
        try {
            GameEventBus.unregister(this);
        } catch (IllegalArgumentException ignored) {
            log.debug("Edge TTS was already unregistered");
        }
    }

    private void publishStartupEvents() {
        SystemSession systemSession = SystemSession.getInstance();
        if (systemSession.getRmsThresholdHigh() != null) {
            UiBus.publish(new AiResponseLogEvent(MultiLingualTextProvider.getText("speech.enabled")));
        }
        String playerName = PlayerSession.getInstance().getConfiguredPlayerName();
        GameEventBus.publish(new AiVoxResponseEvent(StringUtls.greeting(playerName)));
    }

    private void publishCompletionEvent(Class<? extends BaseVoxEvent> originType) {
        try {
            GameEventBus.publish(new VocalisationSuccessfulEvent<>(
                    originType.getConstructor(String.class).newInstance("")));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                 | NoSuchMethodException e) {
            log.error("Failed to publish Edge VocalisationSuccessfulEvent", e);
        }
    }

    private static void validatePcm(byte[] pcm) {
        if (pcm == null || pcm.length == 0 || (pcm.length & 1) != 0) {
            throw new IllegalStateException("Edge MP3 decoder returned invalid PCM-16 audio");
        }
    }

    boolean workersStopped() {
        return (synthesisThread == null || !synthesisThread.isAlive())
                && (playbackThread == null || !playbackThread.isAlive());
    }

    private static EdgeTTSImpl productionInstance() {
        SystemSession session = SystemSession.getInstance();
        return new EdgeTTSImpl(
                new EdgeReadAloudClient(),
                new EdgeMp3Decoder(),
                new EdgeVoiceProvider(),
                new JavaSoundEdgeAudioOutput(session::getAudioOutputDevice),
                EdgeTtsSettings.system(),
                true);
    }
}
