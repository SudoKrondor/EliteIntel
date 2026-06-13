package elite.intel.ai.mouth.google;

import com.google.cloud.texttospeech.v1.*;
import com.google.common.eventbus.Subscribe;
import elite.intel.ai.ears.AudioDeviceEnumerator;
import elite.intel.ai.mouth.AudioDeClicker;
import elite.intel.ai.mouth.MouthInterface;
import elite.intel.ai.mouth.RadioFilter;
import elite.intel.ai.mouth.subscribers.events.*;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AiResponseLogEvent;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import elite.intel.util.AudioPlayer;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.sound.sampled.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final AtomicReference<SourceDataLine> currentLine = new AtomicReference<>();
    private TextToSpeechClient textToSpeechClient;
    private Thread ttsProcessingThread;
    private Thread vocalizationProcessingThread;
    private volatile boolean running;
    private SourceDataLine persistentLine; // Add persistent line
    private final SystemSession systemSession = SystemSession.getInstance();
    private final AtomicBoolean canBeInterrupted = new AtomicBoolean(true);

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
            log.info("TextToSpeechClient initialized successfully with API key");
        } catch (Exception e) {
            log.error("Failed to initialize TextToSpeechClient: {}", e.getMessage(), e);
            EventBusManager.publish(new AppLogEvent("Google TTS failed to start: " + e.getMessage()));
            return;
        }

        running = true;
        EventBusManager.register(this);
        ttsProcessingThread = new Thread(this::processTTSQueue, "TTSThread");
        ttsProcessingThread.start();

        vocalizationProcessingThread = new Thread(this::processVocalizationQueue, "VocalizationThread");
        vocalizationProcessingThread.start();


        log.info("VoiceGenerator started");
        if (systemSession.getRmsThresholdHigh() != null) {
            EventBusManager.publish(new AiResponseLogEvent(MultiLingualTextProvider.getText("speech.enabled")));
        }
        EventBusManager.publish(new AiVoxResponseEvent(StringUtls.greeting(PlayerSession.getInstance().getConfiguredPlayerName())));
    }

    @Override
    public synchronized void stop() {
        if (running) EventBusManager.unregister(this);
        if (ttsProcessingThread == null || !ttsProcessingThread.isAlive()) {
            log.warn("VoiceGenerator is not running");
            return;
        }
        running = false;
        interruptAndClear();
        ttsProcessingThread.interrupt();
        vocalizationProcessingThread.interrupt();
        try {
            ttsProcessingThread.join(5000);
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
    }

    /**
     * Interrupts any ongoing voice synthesis operation, clears the voice request queue,
     * and resets the state of the text-to-speech (TTS) system to allow for future operations.
     * <p>
     * This method ensures the following:
     * - Any pending requests in the voice queue are cleared.
     * - The `interruptRequested` flag is set to prevent further processing of requests momentarily.
     * - The currently active audio playback `SourceDataLine` (if any) is stopped, closed, and removed.
     * - The `interruptRequested` flag is reset to indicate that the system is ready for new voice requests.
     * - Logs the interruption and state of the system.
     * - Verifies if the processing thread is running; if it has stopped unexpectedly, the method restarts the thread.
     * <p>
     * This method is synchronized to ensure thread-safe modifications to shared resources.
     */
    @Override
    public synchronized void interruptAndClear() {
        if(!canBeInterrupted.get()) return;

        // Drain queues and complete any pending customCommand completion futures to avoid 30s timeout
        List<VoiceRequest> drainedTts = new ArrayList<>();
        ttsQueue.drainTo(drainedTts);
        drainedTts.stream().map(VoiceRequest::completionFuture).filter(Objects::nonNull).forEach(f -> f.complete(null));

        List<VocalizationRequest> drainedVox = new ArrayList<>();
        vocalizationQueue.drainTo(drainedVox);
        drainedVox.stream().map(VocalizationRequest::completionFuture).filter(Objects::nonNull).forEach(f -> f.complete(null));

        interruptRequested.set(true);
        SourceDataLine line = currentLine.get();
        if (line != null) {
            line.stop();
            line.flush(); // Flush instead of close
            line.start();
            currentLine.set(line);
        }
        interruptRequested.set(false);
        log.info("TTS interrupted and queue cleared, thread alive={}, interruptRequested={}", ttsProcessingThread != null && ttsProcessingThread.isAlive(), interruptRequested.get());
        if (ttsProcessingThread == null || !ttsProcessingThread.isAlive()) {
            log.warn("Processing thread stopped unexpectedly, restarting");
            start();
        }
    }

    @Subscribe public void shutUp(TTSInterruptEvent event) {
        interruptAndClear();
    }

    @Subscribe @Override public void onVoiceProcessEvent(VocalisationRequestEvent event) {
        canBeInterrupted.set(event.canBeInterrupted());
        log.debug("Received VoiceProcessEvent: text='{}', useRandom={}", event.getText(), event.useRandomVoice());
        String text = StringUtls.sanitizeTts(event.getText());
        if (text.isEmpty()) {
            return;
        }
        try {
            String voiceName = event.useRandomVoice()
                    ? googleVoiceProvider.getRandomVoice().getName()
                    : event.getVoiceName();
            if (voiceName == null) {
                voiceName = systemSession.getGoogleVoice().name();
            }
            ;

            String[] sentences = text.split("(?<=[.!?])\\s+(?=\\S)");
            CompletableFuture<Void> completionFuture = event.getCompletionFuture();
            for (int i = 0; i < sentences.length; i++) {
                // Only the last sentence carries the future; earlier sentences pass null
                boolean isLast = (i == sentences.length - 1);
                ttsQueue.put(new VoiceRequest(sentences[i], voiceName, (1f + systemSession.getSpeechSpeed()), event.getOriginType(), event.isRadio(), isLast ? completionFuture : null));
            }

            AudioPlayer.getInstance().playBeep(AudioPlayer.BEEP_2);
            EventBusManager.publish(new AiResponseLogEvent(event.getText()));
            log.debug("Added VoiceRequest to queue: text='{}', voice='{}'", event.getText(), voiceName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while adding voice request to queue");
        }
    }

    private void processTTSQueue() {
        // Open persistent line at thread start
        if (!openPersistentLine()) {
            log.error("Failed to open persistent audio line, cannot process voice queue");
            return;
        }
        while (running) {
            try {
                log.trace("Polling voice queue, size={}, interruptRequested={}",
                        ttsQueue.size(), interruptRequested.get());
                VoiceRequest request = ttsQueue.poll(1, TimeUnit.SECONDS);
                if (request == null) {
                    if (Thread.currentThread().isInterrupted() || !running) {
                        log.info("Shutting down VoiceGenerator due to interruption or stop signal");
                        closePersistentLine();
                        return;
                    }
                    continue;
                }
                processVoiceRequest(
                        request.text().replace("present", "detected").replace("_", " ").replace("*", ""),
                        request.voiceName(),
                        request.speechRate(),
                        request.originType(),
                        request.isRadio(),
                        request.completionFuture()
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("VoiceGenerator interrupted, shutting down");
                closePersistentLine();
                return;
            } catch (Exception e) {
                log.error("Unexpected error in VoiceGenerator", e);
            }  finally {
                EventBusManager.publish(new AppLogEvent(""));
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

    private void processVoiceRequest(String text, String voiceName, double speechRate, Class<? extends BaseVoxEvent> originType, boolean isRadio, @Nullable CompletableFuture<Void> completionFuture) {
        if (text == null || text.isEmpty()) {
            return;
        }
        long startTime = System.currentTimeMillis();
        Thread currentThread = Thread.currentThread();
        log.debug("Processing VoiceRequest: text='{}', voice='{}', threadName='{}', threadState={}", text, voiceName, currentThread.getName(), currentThread.getState());

        try {
            if (textToSpeechClient == null) {
                log.error("TextToSpeechClient is null, restarting service");
                synchronized (this) {
                    start();
                }
                if (textToSpeechClient == null) {
                    log.error("Failed to reinitialize TextToSpeechClient, skipping request");
                    return;
                }
            }
///
            VoiceSelectionParams voice = googleVoiceProvider.getVoiceParams(voiceName);
            if (voice == null) {
                log.warn("No voice found for name: {}, using default", voiceName);
                voice = googleVoiceProvider.getVoiceParams(GoogleVoices.JENNIFER.getName());
            }

            if (interruptRequested.get()) {
                log.debug("Request interrupted before synthesis, skipping: {}", text);
                return;
            }

            log.debug("Calling Google TTS API");
            long apiStartTime = System.currentTimeMillis();
            SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();
            AudioConfig config = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.LINEAR16)
                    .setSpeakingRate(speechRate)
                    .build();
            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, config);
            long apiEndTime = System.currentTimeMillis();
            log.debug("Google TTS API call completed in {}ms", apiEndTime - apiStartTime);

            byte[] audioData = response.getAudioContent().toByteArray();
            AudioDeClicker.sanitize(audioData, 6);
            AudioDeClicker.applyVolume(audioData, systemSession.getVoiceVolume() / 100f);
            if (isRadio) RadioFilter.apply(audioData);
            // Use persistent line instead of opening/closing
            if (persistentLine == null || !persistentLine.isOpen()) {
                log.warn("Persistent line not available, attempting to reopen");
                if (!openPersistentLine()) {
                    log.error("Cannot play audio, line unavailable");
                    return;
                }
            }
            currentLine.set(persistentLine);
            ///NOTE: Can be null if the user stops the service
            if (vocalizationQueue != null)
                vocalizationQueue.put(new VocalizationRequest(text, voiceName, originType, audioData, completionFuture));

        } catch (Exception e) {
            log.error("Text-to-speech error: {}", e.getMessage(), e);
        } finally {
            currentLine.set(null);
            interruptRequested.set(false);
        }
        log.debug("VoiceRequest processing completed in {}ms", System.currentTimeMillis() - startTime);
    }

    private void processVocalizationQueue(){
        while(running){
            try {
                VocalizationRequest request = vocalizationQueue.poll(1, TimeUnit.SECONDS);
                if (request == null) continue;
                vocalize(request.text(), request.voiceName(), request.originType(), request.audioData(), request.completionFuture());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void vocalize(String text, String voiceName, Class<? extends BaseVoxEvent> originType, byte[] audioData, @Nullable CompletableFuture<Void> completionFuture) {
        log.debug("Starting playback on persistent line");
        AudioFormat format = persistentLine.getFormat();
        int bufferBytes = (int) (format.getFrameSize() * format.getSampleRate() / 10);
        int silenceFrames = (int) (format.getSampleRate() / 50);
        byte[] silenceBuffer = new byte[silenceFrames * format.getFrameSize()];

        persistentLine.write(silenceBuffer, 0, silenceBuffer.length);
        log.info("Spoke with voice {}: {}", voiceName, text);
        long writeStartTime = System.currentTimeMillis();
        for (int i = 0; i < audioData.length; i += bufferBytes) {
            if (interruptRequested.get()) {
                log.debug("Playback interrupted mid-stream: {}", text);
                break;
            }
            int len = Math.min(bufferBytes, audioData.length - i);
            persistentLine.write(audioData, i, len);
        }
        if (interruptRequested.get()) {
            persistentLine.flush();
            log.debug("Playback interrupted");
        } else {
            persistentLine.drain();
        }
        publishCompletionEvent(originType);
        if (completionFuture != null) {
            completionFuture.complete(null);
        }
        log.debug("Audio playback completed in {}ms", System.currentTimeMillis() - writeStartTime);
    }

    private void publishCompletionEvent(Class<? extends BaseVoxEvent> originType) {
        try {
            EventBusManager.publish(
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
                                @Nullable CompletableFuture<Void> completionFuture) {
    }

    private record VocalizationRequest(String text, String voiceName, Class<? extends BaseVoxEvent> originType,
                                       byte[] audioData, @Nullable CompletableFuture<Void> completionFuture) {
    }
}
