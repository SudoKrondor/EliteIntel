package elite.intel.ai.ears.parakeet;

import com.google.common.eventbus.Subscribe;
import com.k2fsa.sherpa.onnx.*;
import elite.intel.ai.brain.actions.handlers.commands.builtin.InterruptCommand;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ai.brain.i18n.InputNormalizerLocalizations;
import elite.intel.ai.brain.vega.input.BargeInEvent;
import elite.intel.ai.ears.*;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.PttButtonStateEvent;
import elite.intel.util.AppPaths;
import elite.intel.util.SherpaOnnxNatives;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static elite.intel.eventbus.AudioMonitorBus.publish;
import static java.util.Arrays.copyOf;

public class ParakeetSTTImpl implements EarsInterface {

    private static final Logger log = LogManager.getLogger(ParakeetSTTImpl.class);
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNELS = 1;
    private static final int ENTER_VOICE_FRAMES = 1;
    private static final int EXIT_SILENCE_FRAMES = 6;
    private static final int PRE_ROLL_FRAMES = 2;
    private static final long BASE_BACKOFF_MS = 2000;
    private static final long MAX_BACKOFF_MS = 60000;
    private static final long INFERENCE_TIMEOUT_SEC = 4;
    private static final int MIN_AUDIO_MS = 1500;
    private static final int MIN_AUDIO_BYTES = SAMPLE_RATE * 2 * MIN_AUDIO_MS / 1000;
    private static final double LEADING_TRIM_THRESHOLD_FACTOR = 3.0; // trim leading frames below NOISE_FLOOR * this
    private static final int MAX_UTTERANCE_MS = 8000;
    private static final int MAX_UTTERANCE_BYTES = SAMPLE_RATE * 2 * MAX_UTTERANCE_MS / 1000;

    private final AtomicBoolean isStopping = new AtomicBoolean(false);
    private final AtomicBoolean isListening = new AtomicBoolean(false);
    private final AtomicBoolean isSpeaking = new AtomicBoolean(false);
    /**
     * The mapped controller button's current level: under push-to-talk this alone is the capture window.
     */
    private final AtomicBoolean pttHeld = new AtomicBoolean(false);
    /**
     * Set on every press and consumed by the capture loop, so a press shorter than one frame is not missed.
     */
    private final AtomicBoolean pttPressed = new AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicInteger pendingTranscriptions = new java.util.concurrent.atomic.AtomicInteger(0);
    private final SystemSession systemSession = SystemSession.getInstance();
    private final ByteArrayOutputStream audioCollector = new ByteArrayOutputStream();
    /**
     * The push-to-talk capture window: while it is armed, this and not the VAD says what a frame is for.
     */
    private final PushToTalkCaptureWindow pttWindow = new PushToTalkCaptureWindow(MAX_UTTERANCE_BYTES);
    private final ArrayDeque<byte[]> preRoll = new ArrayDeque<>();

    private ExecutorService transcriptionExecutor;
    private OfflineRecognizer recognizer;
    private Resampler resampler;
    private AntiAliasingFilter antiAliasingFilter;
    private int sampleRateHertz;
    private int bufferSize;
    private AudioFormat captureFormat;
    public double RMS_THRESHOLD_HIGH;   // gate-OPEN level
    public double RMS_THRESHOLD_LOW;    // gate-CLOSE level (Schmitt-trigger hysteresis)
    public double NOISE_FLOOR;          // raw ambient floor (leading-trim + HUD)
    // Gate-close sits midway between the raw noise floor and the open level, so a
    // dip to just below HIGH does not close the gate - only a drop back toward the
    // ambient floor does. Provides amplitude hysteresis on top of the time-based
    // EXIT_SILENCE_FRAMES guard.
    private static final double GATE_CLOSE_FRACTION = 0.5;
    // Whether a persisted gate is usable is decided by AudioCalibrator.gateClearsNoiseFloor, so the
    // startup warning cannot drift from the calibration that produced the value. Copies of the
    // thresholds used to live here and had already drifted (an absolute floor of 250 against the
    // calibrator's 120), which warned about perfectly good gates from quiet rooms.
    private Thread processingThread;
    private Mixer.Info inputMixerInfo;

    public ParakeetSTTImpl() {
        GameEventBus.register(this);
        UiBus.register(this);
    }

    @Override
    public void start() {
        if (processingThread != null && processingThread.isAlive()) {
            log.warn("Parakeet STT already running");
            return;
        }

        inputMixerInfo = AudioDeviceEnumerator.resolveInputDevice(systemSession.getAudioInputDevice());
        AudioFormatDetector.Format format = AudioFormatDetector.detectSupportedFormat(inputMixerInfo);
        this.sampleRateHertz = format.getSampleRate();
        this.bufferSize = format.getBufferSize();
        this.captureFormat = format.getCaptureFormat();

        Double high = systemSession.getRmsThresholdHigh();
        Double low = systemSession.getRmsThresholdLow();
        if (high == 0 || low == 0) {
            RmsTupple<Double, Double> cal = AudioCalibrator.calibrateRMS(format, inputMixerInfo);
            this.RMS_THRESHOLD_HIGH = cal.getRmsHigh();
            this.NOISE_FLOOR = cal.getRmsLow();
        } else {
            this.RMS_THRESHOLD_HIGH = high;
            this.NOISE_FLOOR = low;
        }
        // Derive the gate-close level from the persisted (floor, open) pair.
        this.RMS_THRESHOLD_LOW = NOISE_FLOOR + (RMS_THRESHOLD_HIGH - NOISE_FLOOR) * GATE_CLOSE_FRACTION;

        recognizer = buildRecognizer();
        log.info("Parakeet recognizer loaded from {}", AppPaths.getParakeetModelDir());

        transcriptionExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Parakeet-Transcription");
            t.setDaemon(true);
            return t;
        });
        isListening.set(true);
        processingThread = new Thread(this::captureLoop);
        processingThread.start();

        if (RMS_THRESHOLD_HIGH == 0 || NOISE_FLOOR == 0) {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedResponse("speech.audioCalibrationRequired")));
        } else if (!AudioCalibrator.gateClearsNoiseFloor(NOISE_FLOOR, RMS_THRESHOLD_HIGH)) {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedSpeech("speech.voiceInputEnabledWarning")));
        } else {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedSpeech("speech.voiceInputEnabled")));
        }
    }

    @Override
    public void stop() {
        isStopping.set(true);
        isListening.set(false);
        if (processingThread != null) processingThread.interrupt();

        if (transcriptionExecutor != null) {
            transcriptionExecutor.shutdown();
            try {
                transcriptionExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (recognizer != null) {
            recognizer.release();
            recognizer = null;
        }
        isStopping.set(false);
        GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedSpeech("speech.voiceInputDisabled")));
    }

    private OfflineRecognizer buildRecognizer() {
        try {
            SherpaOnnxNatives.load();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load sherpa-onnx native libraries", e);
        }

        // Windows DLL hell: another app (e.g. LM Studio) may have installed a different
        // onnxruntime.dll into System32. System32 is searched before our native dir for
        // transitive DLL dependencies, so we preload the bundled copy first to win the race.
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            Path onnxRuntime = AppPaths.getNativeLibDir().resolve("sherpa-onnx/onnxruntime.dll");
            if (Files.exists(onnxRuntime)) {
                try {
                    System.load(onnxRuntime.toAbsolutePath().toString());
                    log.debug("Preloaded bundled onnxruntime.dll from {}", onnxRuntime);
                } catch (UnsatisfiedLinkError e) {
                    log.warn("Could not preload bundled onnxruntime.dll: {}", e.getMessage());
                }
            }
        }

        Path modelDir = AppPaths.getParakeetModelDir();
        Path encoderFile = modelDir.resolve("encoder.int8.onnx");
        Path decoderFile = modelDir.resolve("decoder.int8.onnx");
        Path joinerFile = modelDir.resolve("joiner.int8.onnx");
        Path tokensFile = modelDir.resolve("tokens.txt");

        if (!Files.exists(encoderFile)) throw new IllegalStateException("Parakeet encoder missing at: " + encoderFile);
        if (!Files.exists(decoderFile)) throw new IllegalStateException("Parakeet decoder missing at: " + decoderFile);
        if (!Files.exists(joinerFile)) throw new IllegalStateException("Parakeet joiner missing at: " + joinerFile);
        if (!Files.exists(tokensFile)) throw new IllegalStateException("Parakeet tokens missing at: " + tokensFile);

        OfflineTransducerModelConfig transducer = OfflineTransducerModelConfig.builder()
                .setEncoder(AppPaths.toNativePath(encoderFile))
                .setDecoder(AppPaths.toNativePath(decoderFile))
                .setJoiner(AppPaths.toNativePath(joinerFile))
                .build();

        OfflineModelConfig modelConfig = OfflineModelConfig.builder()
                .setTransducer(transducer)
                .setTokens(AppPaths.toNativePath(tokensFile))
                .setNumThreads(Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), systemSession.getSttThreads())))
                .setDebug(false)
                .setProvider("cpu")
                .build();

        FeatureConfig featureConfig = FeatureConfig.builder()
                .setSampleRate(SAMPLE_RATE)
                .setFeatureDim(128)
                .build();


        OfflineRecognizerConfig.Builder configBuilder = OfflineRecognizerConfig.builder()
                .setFeatureConfig(featureConfig)
                .setOfflineModelConfig(modelConfig)
                .setDecodingMethod("greedy_search")
                .setMaxActivePaths(50)  /// slightly slower, but more accurate
                .setBlankPenalty(-2.0f); /// low value prevents trash in transcriptions for short utterances
        return new OfflineRecognizer(configBuilder.build());
    }

    private void captureLoop() {
        SpectralNoiseReducer.getInstance().reset();
        if (sampleRateHertz != SAMPLE_RATE) {
            antiAliasingFilter = new AntiAliasingFilter(sampleRateHertz, SAMPLE_RATE);
            resampler = new Resampler(sampleRateHertz, SAMPLE_RATE, CHANNELS);
            log.info("Resampling {} → {} Hz with anti-aliasing filter", sampleRateHertz, SAMPLE_RATE);
        }

        int retryCount = 0;
        while (isListening.get()) {
            try {
                runVadAndTranscribe();
                retryCount = 0;
            } catch (LineUnavailableException e) {
                log.error("Audio line unavailable: {}", e.getMessage());
                retryWithBackoff(retryCount++);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Capture loop error: {}", e.getMessage(), e);
                retryWithBackoff(retryCount++);
            }
        }
    }

    private void runVadAndTranscribe() throws LineUnavailableException, InterruptedException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, captureFormat);

        try (TargetDataLine line = AudioDeviceEnumerator.openInputLine(info, inputMixerInfo)) {
            line.open(captureFormat, bufferSize);
            line.start();
            byte[] buffer = new byte[bufferSize];

            boolean isActive = false;
            boolean capturedWithPttHeld = false;
            int consecutiveVoice = 0;
            int consecutiveSilence = 0;
            audioCollector.reset();
            preRoll.clear();
            pttWindow.reset();

            while (isListening.get() && line.isOpen()) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead <= 0) continue;

                // Convert capture format (e.g. 24-bit stereo) to 16-bit mono before all downstream processing.
                byte[] mono16 = AudioFormatDetector.toPCM16Mono(buffer, bytesRead, captureFormat);

                byte[] preResample = (antiAliasingFilter != null)
                        ? antiAliasingFilter.filter(mono16, mono16.length)
                        : mono16;
                int preResampleLen = (antiAliasingFilter != null) ? preResample.length : mono16.length;
                byte[] audio = (resampler != null)
                        ? resampler.resample(preResample, preResampleLen)
                        : preResample;
                int audioLen = (resampler != null) ? audio.length : preResampleLen;

                double rms = calculateRMS(audio, audioLen);

                publish(new AudioMonitorEvent(
                        copyOf(audio, audioLen), audioLen, rms, NOISE_FLOOR, RMS_THRESHOLD_HIGH)
                );

                boolean pushToTalk = systemSession.isPushToTalkEnabled();
                boolean wasActive;

                if (pushToTalk) {
                    // The button IS the capture window. Nothing heard with it up is kept - not in the
                    // collector, not in the pre-roll - so a remark made before the press cannot be swept into
                    // the next capture, and the VAD never opens a window of its own.
                    preRoll.clear();
                    consecutiveVoice = 0;
                    consecutiveSilence = 0;

                    // A press seen since the last frame counts as held even if the button is already back up,
                    // so a tap shorter than one 100ms frame still captures the frame it happened in.
                    boolean held = pttHeld.get() | pttPressed.compareAndSet(true, false);
                    PushToTalkCaptureWindow.Frame frame = pttWindow.onFrame(held, audioLen);
                    if (frame != PushToTalkCaptureWindow.Frame.DISCARD && !isActive) {
                        capturedWithPttHeld = true;
                        audioCollector.reset();
                        log.info("PTT: button held, capture window open");
                    }
                    wasActive = frame != PushToTalkCaptureWindow.Frame.DISCARD;
                    if (wasActive) {
                        // The frame the release lands in is written before the window shuts, so the last word
                        // is not clipped by a commander who lets go the instant they finish saying it.
                        audioCollector.write(audio, 0, audioLen);
                    }
                    isActive = frame == PushToTalkCaptureWindow.Frame.COLLECT;
                    if (frame == PushToTalkCaptureWindow.Frame.CLOSE_ON_RELEASE) {
                        log.info("PTT: button released, capture window closed ({}ms captured)",
                                audioCollector.size() * 1000 / (SAMPLE_RATE * 2));
                    } else if (frame == PushToTalkCaptureWindow.Frame.CLOSE_ON_MAX_LENGTH) {
                        log.warn("PTT: max utterance length ({}ms) reached with the button still held", MAX_UTTERANCE_MS);
                    }
                } else {
                    // Nothing hands-free may act on a press, and neither a stale latch nor a window left open
                    // by the mode change may open a phantom capture the next time push-to-talk is armed.
                    pttPressed.set(false);
                    pttWindow.reset();

                    preRoll.addLast(copyOf(audio, audioLen));
                    if (preRoll.size() > PRE_ROLL_FRAMES) preRoll.removeFirst();

                    // Schmitt-trigger hysteresis: the gate opens only above the HIGH
                    // level, but stays open until rms falls below the lower CLOSE level
                    // for EXIT_SILENCE_FRAMES. Trailing quiet speech (between LOW and
                    // HIGH) keeps the gate open instead of clipping the tail of an
                    // utterance, while opening still requires a clear voice onset.
                    if (rms > RMS_THRESHOLD_HIGH) {
                        consecutiveVoice++;
                    } else {
                        consecutiveVoice = 0;
                    }
                    if (rms > RMS_THRESHOLD_LOW) {
                        consecutiveSilence = 0;
                    } else {
                        consecutiveSilence++;
                    }

                    boolean justActivated = false;
                    if (!isActive && consecutiveVoice >= ENTER_VOICE_FRAMES) {
                        isActive = true;
                        justActivated = true;
                        capturedWithPttHeld = false;
                        audioCollector.reset();
                        for (byte[] frame : preRoll) audioCollector.write(frame, 0, frame.length);
                        preRoll.clear();
                        log.info("VAD: speech started (rms={}, threshold={})", (int) rms, (int) RMS_THRESHOLD_HIGH);
                    }

                    wasActive = isActive;
                    if (isActive && consecutiveSilence >= EXIT_SILENCE_FRAMES) {
                        isActive = false;
                        log.debug("VAD: speech ended");
                    }
                    if (isActive && !justActivated) {
                        audioCollector.write(audio, 0, audioLen);
                        if (audioCollector.size() >= MAX_UTTERANCE_BYTES) {
                            isActive = false;
                            consecutiveSilence = 0;
                            log.warn("VAD: max utterance length ({}ms) reached, forcing gate close", MAX_UTTERANCE_MS);
                        }
                    }
                }

                // Under push-to-talk a closed window is not silence - it is everything said with the button
                // up - so only frames at ambient level may teach the noise profile what the room sounds like.
                boolean quietEnoughToProfileNoise = !isActive && (!pushToTalk || rms <= RMS_THRESHOLD_LOW);
                if (quietEnoughToProfileNoise && systemSession.isNoiseReductionEnabled()) {
                    SpectralNoiseReducer.getInstance().accumulateNoise(audio, audioLen);
                }

                if (wasActive && !isActive && audioCollector.size() > 0) {
                    final byte[] utterance = audioCollector.toByteArray();
                    final boolean pttHeldDuringCapture = capturedWithPttHeld;
                    DumpAudioForTesting.getInstance().dumpAudioAsWav(utterance, SAMPLE_RATE);
                    audioCollector.reset();
                    int pending = pendingTranscriptions.get();
                    if (pending > 0) log.warn("Transcription queue backed up: {} utterances waiting", pending);
                    pendingTranscriptions.incrementAndGet();
                    submitWithTimeout(utterance, pttHeldDuringCapture);
                }
            }
        }
    }

    private void submitWithTimeout(byte[] utterance, boolean capturedWithPttHeld) {
        Future<?> future = transcriptionExecutor.submit(() -> transcribeAndDispatch(utterance, capturedWithPttHeld));
        Thread watchdog = new Thread(() -> {
            try {
                future.get(INFERENCE_TIMEOUT_SEC, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                log.error("Speech To Text hung after {}s - replacing executor", INFERENCE_TIMEOUT_SEC);
                GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedSpeech("speech.sttHungRestarting")));
                transcriptionExecutor.shutdownNow();
                transcriptionExecutor = Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "Parakeet-Transcription");
                    t.setDaemon(true);
                    return t;
                });
            } catch (Exception e) {
                // task completed with exception - already logged in transcribeAndDispatch
            }
        }, "Parakeet-Watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private void transcribeAndDispatch(byte[] pcmBytes, boolean capturedWithPttHeld) {
        pendingTranscriptions.decrementAndGet();
        try {
            if (systemSession.isNoiseReductionEnabled()) {
                pcmBytes = SpectralNoiseReducer.getInstance().denoise(pcmBytes, systemSession.getNoiseReductionStrength());
            }
            // Everything a drop path below might need to explain itself. A phrase that vanishes leaves no
            // other trace anywhere - no UI line, no event - so the reason has to travel with the transcript.
            // The peak is here because Amplifier normalizes to a peak: one loud sample anywhere in the
            // capture (a beep, a knock) sets the gain for the whole utterance and leaves the voice quiet.
            byte[] conditioned = padAudio(trimLeadingLowEnergy(pcmBytes));
            byte[] forDecoder = Amplifier.amplify(conditioned);
            // Peak alone cannot tell speech from silence: one button click in an otherwise empty buffer
            // reads the same as a spoken phrase. RMS is the sustained level, so the pair separates them -
            // a high peak over a low RMS is a transient, not a voice.
            String capture = String.format("captured %dms, %dms to decoder, peak %d, rms %d",
                    durationMs(pcmBytes.length), durationMs(conditioned.length),
                    peakOf(conditioned), (int) calculateRMS(conditioned, conditioned.length));
            float[] samples = pcm16ToFloat(forDecoder);

            long timeStart = System.currentTimeMillis();
            OfflineStream stream = recognizer.createStream();
            try {
                if (stream.hasOption("language")) {
                    stream.setOption("language", toLangCode(systemSession.getLanguage()));
                }
                stream.acceptWaveform(samples, SAMPLE_RATE);
                recognizer.decode(stream);
                OfflineRecognizerResult result = recognizer.getResult(stream);
                String transcript = result.getText().toLowerCase().trim();
                log.debug("Parakeet transcription took {} ms", System.currentTimeMillis() - timeStart);

                if (transcript.isBlank() || transcript.length() < 3) {
                    log.info("STT dropped (nothing a phrase could be made of): [{}] - {}", transcript, capture);
                    // Keep exactly what the decoder was given. This is the one failure the numbers above
                    // cannot explain on their own, and listening to it answers in seconds what another
                    // session of logs only narrows down.
                    DumpAudioForTesting.getInstance().dumpFailedCapture(forDecoder, SAMPLE_RATE, "empty");
                    return;
                }

                // Case 1: pure trash → nothing left after stripping → block
                // Case 2: trash prefix + real content → strip prefix, pass remainder
                String finalTranscript = stripTrashPrefix(transcript);
                if (finalTranscript.isBlank()) {
                    log.info("STT dropped (all trash after prefix strip): [{}] - {}", transcript, capture);
                    return;
                }

                // Laughter is what the engine returns for noise it cannot map to words, so it is
                // never something the commander said - drop it before it costs an AI round trip.
                if (LaughterFilter.isLaughter(finalTranscript)) {
                    log.info("STT dropped (laughter): [{}] - {}", finalTranscript, capture);
                    return;
                }

                log.info("STT accepted: [{}] - {}", finalTranscript, capture);
                UiBus.publish(new AppLogEvent("STT: [" + finalTranscript + "]"));

                switch (MicrophoneGate.decide(capturedWithPttHeld,
                        systemSession.isPushToTalkEnabled(), systemSession.isSleeping())) {
                    case OPEN_PUSH_TO_TALK -> sendToAi(finalTranscript, true);
                    case OPEN_HANDS_FREE -> sendToAi(finalTranscript, false);
                    case CLOSED_PUSH_TO_TALK ->
                            log.info("STT dropped (captured with push-to-talk released): [{}]", finalTranscript);
                    case CLOSED_ASLEEP -> routePastSleepGate(finalTranscript);
                }
            } finally {
                stream.release();
            }
        } catch (Exception e) {
            log.error("Parakeet transcription failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Strips leading trash tokens Parakeet prepends to real utterances, e.g.
     * "mm-hmm. fire lasers" → "fire lasers".
     * Returns empty string if the entire transcript is trash (Case 1 block).
     * Matching is punctuation-tolerant: "okay," and "okay." both match "okay".
     */
    private @NonNull String stripTrashPrefix(String transcript) {
        String[] tokens = transcript.split("\\s+");
        int start = 0;
        outer:
        while (start < tokens.length) {
            for (String trash : InputNormalizerLocalizations.trashPhrases()) {
                String[] trashTokens = trash.split("\\s+");
                if (start + trashTokens.length > tokens.length) continue;
                boolean matches = true;
                for (int i = 0; i < trashTokens.length; i++) {
                    String tok = tokens[start + i].replaceAll("[?.!;:,]+$", "");
                    String tr = trashTokens[i].replaceAll("[?.!;:,]+$", "");
                    if (!tok.equalsIgnoreCase(tr)) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    start += trashTokens.length;
                    continue outer;
                }
            }
            break;
        }
        if (start >= tokens.length) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < tokens.length; i++) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(tokens[i]);
        }
        return sb.toString().replace("?", "").replace("!", "").replace(";", "").replace(":", "").replace(",", "").replace(".", "");
    }

    /**
     * The only way a spoken word reaches the companion while she is asleep: {@link WakeBypass} decides, and
     * everything it does not admit is dropped here rather than costing an AI round trip.
     */
    private void routePastSleepGate(String transcript) {
        String admitted = WakeBypass.forCurrentLanguage().admit(transcript);
        if (admitted == null) {
            log.info("STT dropped (asleep, not a wake phrase): [{}]", transcript);
            return;
        }
        sendToAi(admitted, false);
    }

    private void sendToAi(String transcript, boolean pttCapture) {
        if (isSpeaking.get()) {
            // A held button is an explicit order, never a bare interrupt: only a hands-free utterance may stop
            // at the interrupt phrase, or pressing to talk over her would silently discard what was said.
            if (!pttCapture && isInterruptPhrase(transcript)) {
                log.info("Interrupt phrase detected during TTS playback: {}", transcript);
                GameEventBus.publish(new BargeInEvent());
                return;
            }
            // BargeInController is the sole fan-out owner: it emits one TTS interrupt and interrupts thoughts.
            // Recognition stays active during playback; every non-control transcript continues as normal input.
            GameEventBus.publish(new BargeInEvent());
        } else {
            GameEventBus.publish(new TTSInterruptEvent());
        }
        //AudioPlayer.getInstance().playBeep(AudioPlayer.BEEP_1);
        GameEventBus.publish(new UserInputEvent(transcript));
    }

    /**
     * Returns true if the transcript exactly matches one of the localized interrupt phrases
     * for the current language. Used to allow TTS interruption while the app is speaking.
     */
    private boolean isInterruptPhrase(String transcript) {
        String lower = transcript.trim().toLowerCase(Locale.ROOT);
        for (String phrase : AiActionLocalizations.phrasesForAction(InterruptCommand.ID)) {
            if (lower.equals(phrase.trim().toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    /** Tracks TTS lifecycle only to identify barge-in; recognition and normal command dispatch stay active. */
    @Subscribe
    public void onIsSpeakingEvent(IsSpeakingEvent event) {
        isSpeaking.set(event.isSpeaking());
    }

    /**
     * The push-to-talk button reported from {@link elite.intel.ai.ears.PushToTalkService}, recorded as a level
     * the capture loop samples rather than acted on here: the gate must be timed by the thread that owns the
     * capture window, never by the thread that reads the button.
     */
    @Subscribe
    public void onPttButtonState(PttButtonStateEvent event) {
        pttHeld.set(event.isHeld());
        if (event.isHeld()) pttPressed.set(true);
    }

    private byte[] padAudio(byte[] pcm) {
        if (pcm.length >= MIN_AUDIO_BYTES) return pcm;
        byte[] padded = new byte[MIN_AUDIO_BYTES];
        System.arraycopy(pcm, 0, padded, 0, pcm.length);
        return padded;
    }

    private float[] pcm16ToFloat(byte[] pcm) {
        float[] samples = new float[pcm.length / 2];
        for (int i = 0; i < samples.length; i++) {
            short s = (short) ((pcm[i * 2 + 1] << 8) | (pcm[i * 2] & 0xFF));
            samples[i] = s / 32768.0f;
        }
        return samples;
    }

    /**
     * Strips leading 10ms frames whose RMS is below NOISE_FLOOR * LEADING_TRIM_THRESHOLD_FACTOR.
     * Stops at the first frame that crosses the threshold, so speech onsets are preserved.
     */
    private byte[] trimLeadingLowEnergy(byte[] pcm) {
        final int FRAME_BYTES = 320; // 160 samples = 10ms at 16kHz, 16-bit mono
        final double threshold = NOISE_FLOOR * LEADING_TRIM_THRESHOLD_FACTOR;
        int offset = 0;
        while (offset + FRAME_BYTES <= pcm.length) {
            if (calculateRMS(pcm, offset, FRAME_BYTES) >= threshold) break;
            offset += FRAME_BYTES;
        }
        if (offset == 0) return pcm;
        byte[] trimmed = new byte[pcm.length - offset];
        System.arraycopy(pcm, offset, trimmed, 0, trimmed.length);
        log.debug("Leading trim: removed {}ms of low-energy audio", offset * 1000 / (SAMPLE_RATE * 2));
        return trimmed;
    }

    /**
     * Duration of a 16 kHz PCM-16 mono buffer of {@code byteCount} bytes, in milliseconds.
     */
    private static int durationMs(int byteCount) {
        return byteCount * 1000 / (SAMPLE_RATE * 2);
    }

    /**
     * Largest sample magnitude in a PCM-16 LE buffer: what {@link Amplifier} will normalize against.
     */
    private static int peakOf(byte[] pcm) {
        int peak = 0;
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            int abs = Math.abs((short) ((pcm[i + 1] << 8) | (pcm[i] & 0xFF)));
            if (abs > peak) peak = abs;
        }
        return peak;
    }

    private double calculateRMS(byte[] buffer, int length) {
        return calculateRMS(buffer, 0, length);
    }

    private double calculateRMS(byte[] buffer, int offset, int length) {
        if (length < 2) return 0.0;
        double sum = 0.0;
        int samples = length / 2;
        for (int i = offset; i < offset + length; i += 2) {
            int val = (buffer[i + 1] << 8) | (buffer[i] & 0xFF);
            if (val > 32767) val -= 65536;
            sum += (double) val * val;
        }
        return Math.sqrt(sum / samples);
    }

    private static String toLangCode(Language lang) {
        return switch (lang) {
            case EN -> "en";
            case FR -> "fr";
            case DE -> "de";
            case ES -> "es";
            case RU -> "ru";
            case UK -> "uk";
            case IT -> "it";
            // Parakeet takes ISO 639-1 only: both Portuguese variants transcribe as "pt".
            case PT, PTBZ -> "pt";
        };
    }

    private void retryWithBackoff(int retryCount) {
        long backoff = Math.min(BASE_BACKOFF_MS * (long) Math.pow(2, retryCount), MAX_BACKOFF_MS);
        log.info("Retrying after {}ms (attempt {})", backoff, retryCount + 1);
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
