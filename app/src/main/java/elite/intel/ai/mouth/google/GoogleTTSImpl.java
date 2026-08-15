package elite.intel.ai.mouth.google;

import com.google.cloud.texttospeech.v1.*;
import com.google.common.eventbus.Subscribe;
import elite.intel.ai.ears.AudioDeviceEnumerator;
import elite.intel.ai.mouth.*;
import elite.intel.ai.mouth.subscribers.events.*;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
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

import javax.sound.sampled.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of {@code MouthInterface} for Google Text-to-Speech (TTS).
 * Provides functionality for text-to-speech synthesis using Google Cloud TTS APIs.
 * This class is responsible for managing the lifecycle of TTS operations,
 * queuing requests, and handling event-driven vocalization.
 */
public class GoogleTTSImpl implements MouthInterface {
    private static final Logger log = LogManager.getLogger(GoogleTTSImpl.class);
    private static final GoogleTTSImpl INSTANCE = new GoogleTTSImpl();
    private final BlockingQueue<VoiceRequest> ttsQueue;
    private final BlockingQueue<VocalizationRequest> vocalizationQueue;
    private final GoogleVoiceProvider googleVoiceProvider;
    private final AtomicBoolean interruptRequested = new AtomicBoolean(false);
    private final AtomicLong interruptGeneration = new AtomicLong(0);
    private final AtomicReference<SourceDataLine> currentLine = new AtomicReference<>();
    private final AtomicReference<VoiceRequest> currentSynthesis = new AtomicReference<>();
    private final AtomicReference<VocalizationRequest> currentPlayback = new AtomicReference<>();
    private TextToSpeechClient textToSpeechClient;
    private Thread ttsProcessingThread;
    private Thread vocalizationProcessingThread;
    private volatile boolean running;
    private SourceDataLine persistentLine; // Add persistent line
    /** Voices Google actually offers per BCP-47 language code, cached from listVoices; cleared on stop. */
    private final Map<String, List<GoogleVoiceProvider.AvailableVoice>> voiceCache = new ConcurrentHashMap<>();
    private final SystemSession systemSession = SystemSession.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    private GoogleTTSImpl() {
        this.ttsQueue = new LinkedBlockingQueue<>();
        this.vocalizationQueue = new LinkedBlockingQueue<>();
        googleVoiceProvider = GoogleVoiceProvider.getInstance();
    }

    public static GoogleTTSImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized void start() {
        if (ttsProcessingThread != null && ttsProcessingThread.isAlive()) {
            log.warn("VoiceGenerator is already running");
            return;
        }

        try {
            String apiKey = systemSession.getTtsApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                log.error("TTS API key is not provided");
                return;
            }
            TextToSpeechSettings settings = TextToSpeechSettings.newBuilder().setApiKey(apiKey).build();
            textToSpeechClient = TextToSpeechClient.create(settings);
            // Let the voice provider validate a localized voice against what this language actually offers, so it
            // never requests a Chirp3-HD character that does not exist in the selected locale.
            googleVoiceProvider.setAvailableVoices(this::availableVoices);
            log.info("TextToSpeechClient initialized successfully with API key");
        } catch (Exception e) {
            log.error("Failed to initialize TextToSpeechClient: {}", e.getMessage(), e);
            UiBus.publish(new AppLogEvent("Google TTS failed to start: " + e.getMessage()));
            return;
        }

        completeQueuedSpeech();
        interruptRequested.set(false);
        running = true;
        GameEventBus.register(this);
        ttsProcessingThread = new Thread(this::processTTSQueue, "TTSThread");
        ttsProcessingThread.start();

        vocalizationProcessingThread = new Thread(this::processVocalizationQueue, "VocalizationThread");
        vocalizationProcessingThread.start();


        log.info("VoiceGenerator started");
        if (systemSession.getRmsThresholdHigh() != null) {
            UiBus.publish(new AiResponseLogEvent(MultiLingualTextProvider.getText("speech.enabled")));
        }
        GameEventBus.publish(new AiVoxResponseEvent(StringUtls.greeting(playerSession.getConfiguredPlayerName())));
    }

    @Override
    public synchronized void stop() {
        try {
            GameEventBus.unregister(this);
        } catch (IllegalArgumentException ignored) {
            log.debug("Google TTS was already unregistered");
        }
        running = false;
        interruptGeneration.incrementAndGet();
        interruptRequested.set(true);
        completeAllSpeech();
        SourceDataLine line = currentLine.get();
        if (line != null && line.isOpen()) {
            line.stop();
            line.flush();
        }
        if (ttsProcessingThread != null) {
            ttsProcessingThread.interrupt();
        }
        if (vocalizationProcessingThread != null) {
            vocalizationProcessingThread.interrupt();
        }
        try {
            if (ttsProcessingThread != null) {
                ttsProcessingThread.join(5000);
            }
            if (vocalizationProcessingThread != null) {
                vocalizationProcessingThread.join(5000);
            }
            log.info("VoiceGenerator stopped");
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for VoiceGenerator to stop. System shutdown?", e);
            Thread.currentThread().interrupt();
        }
        // Close persistent line
        closePersistentLine();
        try {
            if (textToSpeechClient != null) {
                textToSpeechClient.close();
                log.info("TextToSpeechClient closed");
            }
        } catch (Exception e) {
            log.error("Failed to close TextToSpeechClient", e);
        }
        ttsProcessingThread = null;
        vocalizationProcessingThread = null;
        textToSpeechClient = null;
        voiceCache.clear(); // client is gone; re-enumerate voices on next start
    }

    /**
     * Interrupts any ongoing voice synthesis and playback, and clears both queues.
     * <p>
     * Fencing is done by bumping {@code interruptGeneration}: every queued request carries the generation
     * captured at enqueue time, so after the bump all in-flight and queued requests are stale and get dropped
     * at each pipeline stage (their handles are completed so callers do not wait out the timeout).
     * A fresh request enqueued after this call captures the new generation and proceeds normally.
     * <p>
     * The {@code interruptRequested} flag is set separately to break the currently-playing write loop
     * mid-stream; it is reset by the playback thread when it next handles a live request, not here.
     * The active {@code SourceDataLine} (if any) is stopped and flushed to discard buffered audio at once.
     * This method is synchronized to ensure thread-safe modifications to shared resources.
     */
    @Override
    public synchronized void interruptAndClear() {
        interruptGeneration.incrementAndGet();
        interruptRequests(null);
        log.info("TTS interrupted and queue cleared, thread alive={}, interruptRequested={}", ttsProcessingThread != null && ttsProcessingThread.isAlive(), interruptRequested.get());
    }

    @Subscribe public void shutUp(TTSInterruptEvent event) {
        if (event.requestId() == null) {
            interruptAndClear();
        } else {
            interruptRequests(event.requestId());
        }
    }

    @Subscribe @Override public void onVoiceProcessEvent(VocalisationRequestEvent event) {
        // Radio transmissions are always voiced by the Kokoro radio engine, even when Google is the
        // main mouth; ignore them here so radio is not double-spoken.
        if (event.isRadio()) return;
        if (!running) {
            return;
        }
        VocalisationHandle handle = event.handle();
        if (!handle.claimForPlayback()) {
            return;
        }
        log.debug("Received VoiceProcessEvent: text='{}', useRandom={}", event.getText(), event.useRandomVoice());
        try {
            // Google's neural voices use punctuation for intonation, so keep it (no espeak-ng hardening);
            // GoogleSsml turns it into explicit SSML pauses at synthesis time (and normalizes "!" to ".").
            String text = StringUtls.sanitizeTts(event.getText(), false);
            if (text == null || text.isBlank()) {
                handle.fail(new IllegalArgumentException("Vocalisation text is blank after TTS sanitization"));
                return;
            }
            String voiceName = event.useRandomVoice()
                    ? googleVoiceProvider.getRandomVoice().getName()
                    : event.getVoiceName();
            if (voiceName == null) {
                voiceName = systemSession.getGoogleVoice().name();
            }

            String[] sentences = text.split("(?<=[.!?])\\s+(?=\\S)");
            long generation = interruptGeneration.get();
            for (int i = 0; i < sentences.length; i++) {
                boolean isLast = (i == sentences.length - 1);
                ttsQueue.put(new VoiceRequest(sentences[i], voiceName, (1f + systemSession.getSpeechSpeed()),
                        event.getOriginType(), event.isRadio(), generation, isLast, handle));
            }

            GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_2));
            UiBus.publish(new AiResponseLogEvent(event.getText(), event.getSpeaker()));
            log.debug("Added VoiceRequest to queue: text='{}', voice='{}'", event.getText(), voiceName);
        } catch (InterruptedException e) {
            handle.complete();
            Thread.currentThread().interrupt();
            log.warn("Interrupted while adding voice request to queue");
        } catch (RuntimeException e) {
            handle.fail(e);
            log.error("Failed to enqueue Google TTS request", e);
        }
    }

    private void interruptRequests(String requestId) {
        long liveGeneration = interruptGeneration.get();
        for (VoiceRequest request : new ArrayList<>(ttsQueue)) {
            if (shouldInterrupt(request.handle(), request.generation(), requestId, liveGeneration)
                    && ttsQueue.remove(request)) {
                request.handle().complete();
            }
        }
        for (VocalizationRequest request : new ArrayList<>(vocalizationQueue)) {
            if (shouldInterrupt(request.handle(), request.generation(), requestId, liveGeneration)
                    && vocalizationQueue.remove(request)) {
                request.handle().complete();
            }
        }

        VoiceRequest synthesis = currentSynthesis.get();
        if (synthesis != null
                && shouldInterrupt(synthesis.handle(), synthesis.generation(), requestId, liveGeneration)) {
            synthesis.handle().complete();
        }
        VocalizationRequest playback = currentPlayback.get();
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
            long requestGeneration,
            String requestId,
            long liveGeneration
    ) {
        if (!handle.interruptible()) {
            return false;
        }
        return requestId == null
                ? requestGeneration != liveGeneration
                : requestId.equals(handle.requestId());
    }

    private boolean isObsolete(VocalisationHandle handle, long requestGeneration) {
        if (handle.isDone()) {
            return true;
        }
        if (handle.interruptible() && requestGeneration != interruptGeneration.get()) {
            handle.complete();
            return true;
        }
        return false;
    }

    private void completeQueuedSpeech() {
        List<VoiceRequest> synthesis = new ArrayList<>();
        ttsQueue.drainTo(synthesis);
        synthesis.forEach(request -> request.handle().complete());
        List<VocalizationRequest> playback = new ArrayList<>();
        vocalizationQueue.drainTo(playback);
        playback.forEach(request -> request.handle().complete());
    }

    private void completeAllSpeech() {
        completeQueuedSpeech();
        VoiceRequest synthesis = currentSynthesis.get();
        if (synthesis != null) {
            synthesis.handle().complete();
        }
        VocalizationRequest playback = currentPlayback.get();
        if (playback != null) {
            playback.handle().complete();
        }
    }

    private void failAllSpeech(Throwable failure) {
        List<VoiceRequest> synthesis = new ArrayList<>();
        ttsQueue.drainTo(synthesis);
        synthesis.forEach(request -> request.handle().fail(failure));
        List<VocalizationRequest> playback = new ArrayList<>();
        vocalizationQueue.drainTo(playback);
        playback.forEach(request -> request.handle().fail(failure));
        VoiceRequest activeSynthesis = currentSynthesis.get();
        if (activeSynthesis != null) {
            activeSynthesis.handle().fail(failure);
        }
        VocalizationRequest activePlayback = currentPlayback.get();
        if (activePlayback != null) {
            activePlayback.handle().fail(failure);
        }
    }

    private void processTTSQueue() {
        // Open persistent line at thread start
        if (!openPersistentLine()) {
            log.error("Failed to open persistent audio line, cannot process voice queue");
            IllegalStateException failure = new IllegalStateException("Google TTS audio output is unavailable");
            failAllSpeech(failure);
            running = false;
            try {
                GameEventBus.unregister(this);
            } catch (IllegalArgumentException ignored) {
                log.debug("Google TTS was already unregistered after audio failure");
            }
            if (vocalizationProcessingThread != null) {
                vocalizationProcessingThread.interrupt();
            }
            return;
        }
        while (running) {
            VoiceRequest request = null;
            try {
                log.trace("Polling voice queue, size={}, interruptRequested={}",
                        ttsQueue.size(), interruptRequested.get());
                request = ttsQueue.poll(1, TimeUnit.SECONDS);
                if (request == null) {
                    if (Thread.currentThread().isInterrupted() || !running) {
                        log.info("Shutting down VoiceGenerator due to interruption or stop signal");
                        closePersistentLine();
                        return;
                    }
                    continue;
                }
                currentSynthesis.set(request);
                if (isObsolete(request.handle(), request.generation())) {
                    continue;
                }
                processVoiceRequest(request);
            } catch (InterruptedException e) {
                if (request != null) {
                    request.handle().complete();
                }
                Thread.currentThread().interrupt();
                log.info("VoiceGenerator interrupted, shutting down");
                closePersistentLine();
                return;
            } catch (Exception e) {
                if (request != null) {
                    request.handle().fail(e);
                }
                log.error("Unexpected error in VoiceGenerator", e);
            }  finally {
                if (request != null) {
                    currentSynthesis.compareAndSet(request, null);
                }
                UiBus.publish(new AppLogEvent(""));
            }
        }
        closePersistentLine();
    }




    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean openPersistentLine() {
        try {
            AudioFormat format = new AudioFormat(24000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            int bufferBytes = (int) (format.getFrameSize() * format.getSampleRate() / 10);
            Mixer.Info outputMixer = AudioDeviceEnumerator.resolveOutputDevice(systemSession.getAudioOutputDevice());
            persistentLine = AudioDeviceEnumerator.openOutputLine(info, outputMixer);
            persistentLine.open(format, bufferBytes);
            persistentLine.start();
            log.info("Persistent audio line opened successfully");
            return true;
        } catch (LineUnavailableException e) {
            log.error("Failed to open persistent audio line: {}", e.getMessage(), e);
            return false;
        }
    }

    private void closePersistentLine() {
        if (persistentLine != null) {
            try {
                if (!running) {
                    persistentLine.flush(); // forced stop - discard buffered audio immediately
                } else {
                    persistentLine.drain(); // normal end - play out remaining audio
                }
                persistentLine.stop();
                persistentLine.close();
                log.info("Persistent audio line closed");
            } catch (Exception e) {
                log.error("Error closing persistent audio line", e);
            } finally {
                persistentLine = null;
            }
        }
    }

    /**
     * Voices Google offers for a BCP-47 language code (e.g. "ru-RU"), fetched once via listVoices and cached.
     * Returns an empty list on lookup failure (the provider then falls back to a known-good voice) and does not
     * cache the failure, so a transient error is retried next time.
     */
    private List<GoogleVoiceProvider.AvailableVoice> availableVoices(String languageCode) {
        List<GoogleVoiceProvider.AvailableVoice> cached = voiceCache.get(languageCode);
        if (cached != null) {
            return cached;
        }
        TextToSpeechClient client = textToSpeechClient;
        if (client == null) {
            return List.of();
        }
        try {
            List<GoogleVoiceProvider.AvailableVoice> voices = client.listVoices(languageCode).getVoicesList().stream()
                    .map(v -> new GoogleVoiceProvider.AvailableVoice(
                            v.getName(), v.getSsmlGender() == SsmlVoiceGender.MALE))
                    .toList();
            voiceCache.put(languageCode, voices);
            return voices;
        } catch (Exception e) {
            log.warn("Could not list Google voices for {}: {}", languageCode, e.getMessage());
            return List.of();
        }
    }

    private void processVoiceRequest(VoiceRequest request) throws InterruptedException {
        String text = request.text().replace("present", "detected").replace("_", " ").replace("*", "");
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Google TTS request contains no speakable text");
        }
        long startTime = System.currentTimeMillis();
        Thread currentThread = Thread.currentThread();
        log.debug("Processing VoiceRequest: text='{}', voice='{}', threadName='{}', threadState={}",
                text, request.voiceName(), currentThread.getName(), currentThread.getState());

        try {
            if (textToSpeechClient == null) {
                throw new IllegalStateException("Google TextToSpeechClient is unavailable");
            }
            VoiceSelectionParams voice = googleVoiceProvider.getVoiceParams(request.voiceName());
            if (voice == null) {
                log.warn("No voice found for name: {}, using default", request.voiceName());
                voice = googleVoiceProvider.getVoiceParams(GoogleVoices.JENNIFER.getName());
            }
            if (voice == null) {
                throw new IllegalStateException("Google TTS has no usable voice");
            }

            if (isObsolete(request.handle(), request.generation())) {
                log.debug("Request interrupted before synthesis, skipping: {}", text);
                return;
            }

            log.debug("Calling Google TTS API");
            long apiStartTime = System.currentTimeMillis();
            // Legacy Chirp-HD rejects SSML; compatible voices retain punctuation-aware SSML pauses.
            SynthesisInput input = GoogleSynthesisInputFactory.create(text, voice);
            AudioConfig config = createAudioConfig(
                    voice, request.speechRate(), systemSession.getGoogleWaveNetPitch());
            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, config);
            long apiEndTime = System.currentTimeMillis();
            log.debug("Google TTS API call completed in {}ms", apiEndTime - apiStartTime);
            if (isObsolete(request.handle(), request.generation())) {
                return;
            }

            byte[] audioData = response.getAudioContent().toByteArray();
            if (audioData.length == 0) {
                throw new IllegalStateException("Google TTS produced empty audio");
            }
            AudioDeClicker.sanitize(audioData, 6);
            AudioDeClicker.applyVolume(audioData, systemSession.getVoiceVolume() / 100f);
            if (request.isRadio()) RadioFilter.apply(audioData);
            // Use persistent line instead of opening/closing
            if (persistentLine == null || !persistentLine.isOpen()) {
                log.warn("Persistent line not available, attempting to reopen");
                if (!openPersistentLine()) {
                    throw new IllegalStateException("Google TTS audio output is unavailable");
                }
            }
            vocalizationQueue.put(new VocalizationRequest(
                    text, request.voiceName(), request.originType(), audioData, request.generation(),
                    request.lastSentence(), request.handle()));
        } finally {
            log.debug("VoiceRequest processing completed in {}ms", System.currentTimeMillis() - startTime);
        }
    }

    private void processVocalizationQueue(){
        while(running){
            VocalizationRequest request = null;
            try {
                request = vocalizationQueue.poll(1, TimeUnit.SECONDS);
                if (request == null) continue;
                currentPlayback.set(request);
                if (isObsolete(request.handle(), request.generation())) {
                    continue;
                }
                interruptRequested.set(false);
                boolean completed = vocalize(request);
                if (completed && request.lastSentence()) {
                    request.handle().complete();
                }
            } catch (InterruptedException e) {
                if (request != null) {
                    request.handle().complete();
                }
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                if (request != null) {
                    request.handle().fail(e);
                }
                log.error("Google TTS playback failed", e);
            } finally {
                if (request != null) {
                    currentPlayback.compareAndSet(request, null);
                }
            }
        }
    }

    static AudioConfig createAudioConfig(VoiceSelectionParams voice, double speechRate, int waveNetPitch) {
        AudioConfig.Builder config = AudioConfig.newBuilder()
                .setAudioEncoding(AudioEncoding.LINEAR16)
                .setSpeakingRate(speechRate);
        if (voice.getName().contains("-Wavenet-") && waveNetPitch != 0) {
            config.setPitch(waveNetPitch);
        }
        return config.build();
    }

    private boolean vocalize(VocalizationRequest request) {
        log.debug("Starting playback on persistent line");
        if (persistentLine == null || !persistentLine.isOpen()) {
            throw new IllegalStateException("Google TTS audio output is unavailable");
        }
        AudioFormat format = persistentLine.getFormat();
        int bufferBytes = (int) (format.getFrameSize() * format.getSampleRate() / 10);
        int silenceFrames = (int) (format.getSampleRate() / 50);
        byte[] silenceBuffer = new byte[silenceFrames * format.getFrameSize()];
        boolean interrupted = false;

        // Bracket main-voice playback so the always-on Kokoro radio engine can duck behind it.
        MainVoicePlaybackGate.begin();
        try {
            currentLine.set(persistentLine);
            persistentLine.write(silenceBuffer, 0, silenceBuffer.length);
            log.info("Spoke with voice {}: {}", request.voiceName(), request.text());
            long writeStartTime = System.currentTimeMillis();
            for (int i = 0; i < request.audioData().length; i += bufferBytes) {
                if (interruptRequested.get()) {
                    interrupted = true;
                    log.debug("Playback interrupted mid-stream: {}", request.text());
                    break;
                }
                int len = Math.min(bufferBytes, request.audioData().length - i);
                persistentLine.write(request.audioData(), i, len);
            }
            if (interruptRequested.get()) {
                interrupted = true;
                persistentLine.flush();
                log.debug("Playback interrupted");
            } else {
                persistentLine.drain();
            }
            log.debug("Audio playback completed in {}ms", System.currentTimeMillis() - writeStartTime);
        } finally {
            currentLine.set(null);
            interruptRequested.set(false);
            MainVoicePlaybackGate.end();
        }
        if (!interrupted) {
            publishCompletionEvent(request.originType());
        }
        return !interrupted;
    }

    private void publishCompletionEvent(Class<? extends BaseVoxEvent> originType) {
        try {
            GameEventBus.publish(
                    new VocalisationSuccessfulEvent<>(
                            originType.getConstructor(String.class).newInstance("")
                    )
            );
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("Failed to publish VocalisationSuccessfulEvent {}", e.getMessage(), e);
        }
    }

    private record VoiceRequest(String text, String voiceName, double speechRate,
                                Class<? extends BaseVoxEvent> originType, boolean isRadio,
                                long generation, boolean lastSentence, VocalisationHandle handle) {
    }

    private record VocalizationRequest(String text, String voiceName, Class<? extends BaseVoxEvent> originType,
                                       byte[] audioData, long generation, boolean lastSentence,
                                       VocalisationHandle handle) {
    }
}
