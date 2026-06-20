package elite.intel.ai.ears.parakeet;

import com.google.common.eventbus.Subscribe;
import com.k2fsa.sherpa.onnx.*;
import elite.intel.ai.brain.actions.command.builtin.InterruptCommand;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static elite.intel.ai.brain.Reducer.trashSttWords;
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
    private final AtomicBoolean pttHeld = new AtomicBoolean(false);
    private final AtomicBoolean pttForceClose = new AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicInteger pendingTranscriptions = new java.util.concurrent.atomic.AtomicInteger(0);
    private final SystemSession systemSession = SystemSession.getInstance();
    private final ByteArrayOutputStream audioCollector = new ByteArrayOutputStream();
    private final ArrayDeque<byte[]> preRoll = new ArrayDeque<>();

    private ExecutorService transcriptionExecutor;
    private OfflineRecognizer recognizer;
    private Resampler resampler;
    private AntiAliasingFilter antiAliasingFilter;
    private int sampleRateHertz;
    private int bufferSize;
    private AudioFormat captureFormat;
    public double RMS_THRESHOLD_HIGH;
    public double NOISE_FLOOR;
    private final double MINIMUM_NOISE_FLOOR_TO_RMS_RATIO = 300;
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
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedSpeech("speech.audioCalibrationRequired")));
        } else if (RMS_THRESHOLD_HIGH < 250 || RMS_THRESHOLD_HIGH - NOISE_FLOOR < MINIMUM_NOISE_FLOOR_TO_RMS_RATIO) {
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
            boolean capturedWhileAwake = false;
            int consecutiveVoice = 0;
            int consecutiveSilence = 0;
            audioCollector.reset();
            preRoll.clear();

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

                preRoll.addLast(copyOf(audio, audioLen));
                if (preRoll.size() > PRE_ROLL_FRAMES) preRoll.removeFirst();

                if (rms > RMS_THRESHOLD_HIGH) {
                    consecutiveVoice++;
                    consecutiveSilence = 0;
                } else {
                    consecutiveVoice = 0;
                    consecutiveSilence++;
                }

                boolean justActivated = false;
                if (!isActive && consecutiveVoice >= ENTER_VOICE_FRAMES) {
                    isActive = true;
                    justActivated = true;
                    capturedWhileAwake = pttHeld.get();
                    audioCollector.reset();
                    for (byte[] frame : preRoll) audioCollector.write(frame, 0, frame.length);
                    preRoll.clear();
                    log.info("VAD: speech started (rms={}, threshold={})", (int) rms, (int) RMS_THRESHOLD_HIGH);
                }

                boolean wasActive = isActive;
                if (isActive && consecutiveSilence >= EXIT_SILENCE_FRAMES) {
                    isActive = false;
                    log.debug("VAD: speech ended");
                }
                boolean forceClose = pttForceClose.compareAndSet(true, false);
                if (forceClose && isActive) {
                    isActive = false;
                    log.info("PTT: button released, closing VAD");
                }
                if (isActive && !justActivated) {
                    if (pttHeld.get()) capturedWhileAwake = true; // latch: button pressed mid-utterance
                    audioCollector.write(audio, 0, audioLen);
                    if (audioCollector.size() >= MAX_UTTERANCE_BYTES) {
                        isActive = false;
                        consecutiveSilence = 0;
                        log.warn("VAD: max utterance length ({}ms) reached, forcing gate close", MAX_UTTERANCE_MS);
                    }
                }
                if (!isActive && systemSession.isNoiseReductionEnabled()) {
                    SpectralNoiseReducer.getInstance().accumulateNoise(audio, audioLen);
                }

                if (wasActive && !isActive && audioCollector.size() > 0) {
                    final byte[] utterance = audioCollector.toByteArray();
                    final boolean awake = capturedWhileAwake;
                    DumpAudioForTesting.getInstance().dumpAudioAsWav(utterance, SAMPLE_RATE);
                    audioCollector.reset();
                    int pending = pendingTranscriptions.get();
                    if (pending > 0) log.warn("Transcription queue backed up: {} utterances waiting", pending);
                    pendingTranscriptions.incrementAndGet();
                    submitWithTimeout(utterance, awake);
                }
            }
        }
    }

    private void submitWithTimeout(byte[] utterance, boolean capturedWhileAwake) {
        Future<?> future = transcriptionExecutor.submit(() -> transcribeAndDispatch(utterance, capturedWhileAwake));
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

    private void transcribeAndDispatch(byte[] pcmBytes, boolean capturedWhileAwake) {
        pendingTranscriptions.decrementAndGet();
        try {
            if (systemSession.isNoiseReductionEnabled()) {
                pcmBytes = SpectralNoiseReducer.getInstance().denoise(pcmBytes, systemSession.getNoiseReductionStrength());
            }
            float[] samples = pcm16ToFloat(Amplifier.amplify(padAudio(trimLeadingLowEnergy(pcmBytes))));

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

                if (transcript.isBlank() || transcript.length() < 3) return;

                // Case 1: pure trash → nothing left after stripping → block
                // Case 2: trash prefix + real content → strip prefix, pass remainder
                String finalTranscript = stripTrashPrefix(transcript);
                if (finalTranscript.isBlank()) return;

                UiBus.publish(new AppLogEvent("STT: [" + finalTranscript + "]"));

                if (capturedWhileAwake) {
                    String stripped = stripListenBypassPrefix(finalTranscript);
                    sendToAi(stripped != null ? stripped : finalTranscript, true);
                } else if (systemSession.isSleepingModeOn()) {
                    if (passThrough(finalTranscript)) {
                        String stripped = stripListenBypassPrefix(finalTranscript);
                        sendToAi(stripped != null ? stripped : finalTranscript, false);
                    }
                } else {
                    sendToAi(finalTranscript, false);
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
            for (String trash : trashSttWords) {
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

    private boolean passThrough(String transcript) {
        // Sleep mode only accepts exact wake phrases or listen-prefix commands at the start.
        // This prevents phrases like "do not listen open map" from bypassing the gate.
        return isPureWakePhrase(transcript) || stripListenBypassPrefix(transcript) != null;
    }

    private boolean isPureWakePhrase(String transcript) {
        String lower = transcript.trim().toLowerCase(Locale.ROOT);
        for (String phrase : AiActionLocalizations.wakeBypassPhrases()) {
            if (lower.equals(phrase.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    /**
     * If the transcript starts with a localized listen-type prefix followed by
     * meaningful content, returns the content with the prefix stripped.
     * Returns null if the transcript is a pure wake phrase or has no listen prefix
     * caller sends the original unchanged so the AI can route it via WAKEUP.
     */
    private String stripListenBypassPrefix(String transcript) {
        return stripPrefixAtStart(transcript, AiActionLocalizations.listenBypassPrefixes());
    }

    private String stripPrefixAtStart(String transcript, Collection<String> prefixes) {
        String lower = transcript.toLowerCase(Locale.ROOT);
        List<String> sortedPrefixes = new ArrayList<>(prefixes);
        sortedPrefixes.sort(Comparator.comparingInt(String::length).reversed());
        for (String prefix : sortedPrefixes) {
            String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
            if (lower.startsWith(lowerPrefix)
                    && lower.length() > lowerPrefix.length()
                    && Character.isWhitespace(lower.charAt(lowerPrefix.length()))) {
                String remainder = transcript.substring(prefix.length()).trim();
                if (!remainder.isBlank()) return remainder;
            }
        }
        return null;
    }

    private void sendToAi(String transcript, boolean pttCapture) {
        if (isSpeaking.get()) {
            if (pttCapture) {
                log.info("PTT: interrupting TTS to dispatch transcript: {}", transcript.replace("computer", ""));
                GameEventBus.publish(new TTSInterruptEvent());
            } else if (isInterruptPhrase(transcript)) {
                log.info("Interrupt phrase detected during TTS playback: {}", transcript);
                GameEventBus.publish(new TTSInterruptEvent());
                return;
            } else {
                log.debug("Ignoring transcript while TTS is speaking: {}", transcript);
                return;
            }
        } else {
            GameEventBus.publish(new TTSInterruptEvent());
        }
        //AudioPlayer.getInstance().playBeep(AudioPlayer.BEEP_1);
        log.info("Dispatching transcript: {}", transcript.replace("computer", ""));
        GameEventBus.publish(new UserInputEvent(transcript.replace("computer", "")));
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

    /** Tracks TTS playback state to gate non-interrupt transcripts while the app is speaking. */
    @Subscribe
    public void onIsSpeakingEvent(IsSpeakingEvent event) {
        isSpeaking.set(event.isSpeaking());
    }

    @Subscribe
    public void onPttButtonState(PttButtonStateEvent event) {
        pttHeld.set(event.isHeld());
        if (!event.isHeld()) pttForceClose.set(true);
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
            case PT -> "pt";
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
