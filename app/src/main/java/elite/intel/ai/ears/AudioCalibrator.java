package elite.intel.ai.ears;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Provides audio calibration functionality to determine noise and speech
 * levels for setting thresholds in voice-activated systems. This class
 * performs multi-phase calibration to measure the ambient noise level
 * (noise floor) and average speech RMS (Root Mean Square) to compute
 * thresholds for voice activity detection.
 */
public class AudioCalibrator {
    private static final Logger log = LogManager.getLogger(AudioCalibrator.class);
    private static final int NOISE_CALIBRATION_DURATION_MS = 5000;
    private static final int SPEECH_CALIBRATION_DURATION_MS = 5000;
    // Upper bound on how long we wait for a TTS prompt to finish playing before
    // recording. Guards against a TTS pipeline that never signals completion.
    private static final int TTS_COMPLETION_TIMEOUT_MS = 15000;
    // Short settle delay after the prompt finishes so the speaker tail/click is
    // not captured at the start of the recording window.
    private static final int POST_PROMPT_SETTLE_MS = 300;
    private static final double DEFAULT_RMS_THRESHOLD_HIGH = 0;
    private static final double DEFAULT_RMS_THRESHOLD_LOW = 0;
    // Percentile used to estimate the noise floor from the collected samples.
    // 75th percentile captures typical ambient level while ignoring the top 25%
    // (transient peaks, brief louder music passages, etc.).
    private static final double NOISE_PERCENTILE = 0.75;
    // Fraction of the noise-to-speech gap used to set the VAD trigger.
    // 0.5 = midpoint: triggers halfway between ambient and average speech.
    private static final double GATE_MIDPOINT_FACTOR = 0.5;
    // Minimum acceptable separation between the gate-open level and the noise floor,
    // expressed as a true RATIO (gateOpen >= noiseFloor * ratio) so it scales with the
    // environment instead of a fixed additive margin. 2.0x is ~6 dB - below this the
    // top quartile of ambient noise reliably crosses the gate and false-triggers.
    private static final double MIN_GATE_TO_NOISE_RATIO = 2.0;
    // Absolute lower bound on the gate-open margin, for near-silent rooms where the
    // noise floor is ~0-30 and a pure ratio would set the gate down in mic self-noise.
    private static final double MIN_GATE_OPEN_ABS = 120.0;
    private static final double MAX_NOISE_AVG = 800.0;
    // Bounded retry for opening the capture line. On Windows the mic is frequently
    // grabbed for a moment by another process (Discord, a browser, the game's own
    // voice chat), which surfaces - confusingly - as an IllegalArgumentException
    // "...not supported" rather than a clean LineUnavailableException. A short retry
    // rides out that transient contention so a one-off race no longer aborts an
    // otherwise valid calibration.
    private static final int OPEN_MAX_ATTEMPTS = 4;
    private static final int OPEN_RETRY_BASE_DELAY_MS = 250;


    public static RmsTupple<Double, Double> calibrateRMS(AudioFormatDetector.Format format) {
        return calibrateRMS(format, null);
    }

    public static RmsTupple<Double, Double> calibrateRMS(AudioFormatDetector.Format format, Mixer.Info mixerInfo) {
        log.info("Starting RMS calibration: noise for {}ms, speech for {}ms",
                NOISE_CALIBRATION_DURATION_MS, SPEECH_CALIBRATION_DURATION_MS);

        AudioFormat captureFormat = format.getCaptureFormat();
        int bufferSize = format.getBufferSize();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, captureFormat);
        byte[] buffer = new byte[bufferSize];

        // Phase 1: noise floor
        speakPromptAndWait("speech.audioCalibrationRemainSilent");
        double noiseFloor = calibrateNoiseFloor(captureFormat, bufferSize, buffer, info, mixerInfo);

        // Phase 2: speech
        speakPromptAndWait("speech.audioCalibrationCountTo12");
        double avgSpeechRMS = calibrateSpeech(captureFormat, bufferSize, buffer, info, noiseFloor, mixerInfo);

        // VAD gate-open = midpoint between noise and speech (always above ambient,
        // always below speech), but never closer to the floor than the minimum
        // ratio/absolute margin. We reject only when the *speech* itself fails to
        // clear that margin - i.e. the environment is genuinely unusable - rather
        // than silently accepting a gate sitting in the noise.
        double gap = avgSpeechRMS - noiseFloor;
        double minOpen = Math.max(noiseFloor * MIN_GATE_TO_NOISE_RATIO, noiseFloor + MIN_GATE_OPEN_ABS);
        double midpointOpen = noiseFloor + gap * GATE_MIDPOINT_FACTOR;
        double highThreshold;
        if (avgSpeechRMS <= noiseFloor || midpointOpen < minOpen) {
            log.warn("Insufficient speech/noise separation (noiseFloor={}, speechAvg={}, gap={}). " +
                            "Environment too loud or mic gain too low; gate clamped to minimum ratio above floor, speech may be missed.",
                    (int) noiseFloor, (int) avgSpeechRMS, (int) gap);
            UiBus.publish(new AppLogEvent(StringUtls.localizedLlm("log.audioCalibrationLowGap", String.valueOf((int) gap))));
            highThreshold = minOpen;
        } else {
            highThreshold = midpointOpen;
        }

        // noiseFloor is stored as-is (raw measured ambient level). The runtime VAD
        // derives the gate-CLOSE level from (noiseFloor, highThreshold) as a
        // Schmitt trigger, so no separate close value is persisted.
        double lowThreshold = noiseFloor;

        highThreshold = Math.round(highThreshold * 100.0) / 100.0;
        lowThreshold = Math.round(lowThreshold * 100.0) / 100.0;

        SystemSession systemSession = SystemSession.getInstance();
        systemSession.setRmsThresholdHigh(highThreshold);
        systemSession.setRmsThresholdLow(lowThreshold);

        log.info("Final calibrated RMS thresholds: HIGH={}, LOW={} (noise floor={}, speech avg={}, gap={})",
                highThreshold, lowThreshold, (int) noiseFloor, (int) avgSpeechRMS, (int) gap);
        UiBus.publish(new AppLogEvent(StringUtls.localizedLlm("log.audioCalibrationComplete",
                String.valueOf(highThreshold), String.valueOf(lowThreshold))));
        return new RmsTupple<>(highThreshold, lowThreshold);
    }

    /**
     * Speaks a calibration prompt and blocks until the TTS pipeline finishes playing it
     * (or until {@link #TTS_COMPLETION_TIMEOUT_MS} elapses), then waits a short settle
     * delay. This replaces a fixed sleep so a prompt longer than the old delay does not
     * bleed its audio tail into the recording window - important for localized prompts
     * whose spoken length varies by language.
     *
     * @param speechKey the llm-bundle key of the prompt to speak.
     */
    private static void speakPromptAndWait(String speechKey) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedLlm(speechKey), done));
        log.info("Prompted '{}', waiting up to {}ms for TTS to finish", speechKey, TTS_COMPLETION_TIMEOUT_MS);
        try {
            done.get(TTS_COMPLETION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            Thread.sleep(POST_PROMPT_SETTLE_MS);
        } catch (TimeoutException e) {
            log.warn("TTS prompt '{}' did not finish within {}ms; proceeding with calibration", speechKey, TTS_COMPLETION_TIMEOUT_MS);
        } catch (ExecutionException e) {
            log.warn("TTS prompt '{}' completed exceptionally: {}", speechKey,
                    e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        } catch (InterruptedException e) {
            log.warn("TTS prompt wait interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Calibrates the noise floor by analyzing ambient noise levels over a fixed duration.
     * The method records audio samples, calculates their Root Mean Square (RMS) values,
     * and determines the noise floor based on a specified percentile of the collected data.
     *
     * @param format     the audio format to be used for capturing audio input.
     * @param bufferSize the size of the audio buffer used for capturing data.
     * @param buffer     a byte array to store the audio data read from the input line.
     * @param info       the audio line information specifying the data line type.
     * @param mixerInfo  the audio mixer information for selecting the input device.
     * @return the calculated noise floor value as a double, representing the typical RMS
     * level of ambient noise based on collected data.
     */
    private static double calibrateNoiseFloor(AudioFormat format, int bufferSize, byte[] buffer, DataLine.Info info, Mixer.Info mixerInfo) {
        List<Double> samples = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        try (TargetDataLine line = openAndStartWithRetry(format, bufferSize, info, mixerInfo)) {
            while (System.currentTimeMillis() - startTime < NOISE_CALIBRATION_DURATION_MS) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    byte[] mono16 = AudioFormatDetector.toPCM16Mono(buffer, bytesRead, format);
                    samples.add(calculateRMS(mono16, mono16.length));
                }
            }
        }

        if (samples.isEmpty()) {
            log.warn("No noise samples collected");
            UiBus.publish(new AppLogEvent(StringUtls.localizedLlm("log.audioCalibrationNoAudio")));
            throw new AudioCalibrationException("No noise samples collected");
        }

        Collections.sort(samples);
        // 75th percentile: robust estimate of typical ambient level.
        // Works correctly in both quiet environments (returns ~30) and loud
        // environments like music (returns ~1400) because it uses actual
        // measured values rather than filtering against a hardcoded ceiling.
        int idx = Math.min((int) (samples.size() * NOISE_PERCENTILE), samples.size() - 1);
        double noiseFloor = samples.get(idx);

        log.info("Noise calibration: {} samples, min={}, median={}, 75th%={}, max={}",
                samples.size(),
                (int) samples.getFirst().doubleValue(),
                (int) samples.get(samples.size() / 2).doubleValue(),
                (int) noiseFloor,
                (int) samples.getLast().doubleValue());

        if (noiseFloor > MAX_NOISE_AVG) {
            log.warn("High noise floor detected ({}); consider quieter environment", (int) noiseFloor);
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("speech.audioCalibrationNoisy")));
            UiBus.publish(new AppLogEvent(StringUtls.localizedLlm("log.audioCalibrationHighNoise", String.valueOf((int) noiseFloor))));
        }
        return noiseFloor;
    }

    /**
     * Calibrates the average speech Root Mean Square (RMS) value by analyzing audio input
     * and comparing it to a predefined noise floor. This method identifies speech levels
     * based on RMS values greater than the noise floor and calculates the average RMS
     * level for speech detection.
     *
     * @param format     the audio format to be used for capturing audio input.
     * @param bufferSize the size of the audio buffer used for capturing data.
     * @param buffer     a byte array to store the audio data read from the input line.
     * @param info       the audio line information specifying the data line type.
     * @param noiseFloor the noise floor threshold as a double for distinguishing speech from noise.
     * @param mixerInfo  the audio mixer information for selecting the input device.
     * @return the calculated average RMS value for speech as a double, or a default threshold
     * if calibration fails or insufficient speech samples are detected.
     */
    private static double calibrateSpeech(AudioFormat format, int bufferSize, byte[] buffer, DataLine.Info info, double noiseFloor, Mixer.Info mixerInfo) {
        double sumSpeechRMS = 0.0;
        double peakSpeechRMS = 0.0;
        int speechSampleCount = 0;
        int totalSampleCount = 0;
        long startTime = System.currentTimeMillis();

        try (TargetDataLine line = openAndStartWithRetry(format, bufferSize, info, mixerInfo)) {
            while (System.currentTimeMillis() - startTime < SPEECH_CALIBRATION_DURATION_MS) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    byte[] mono16 = AudioFormatDetector.toPCM16Mono(buffer, bytesRead, format);
                    double rms = calculateRMS(mono16, mono16.length);
                    totalSampleCount++;
                    if (rms > noiseFloor * 1.3) {
                        sumSpeechRMS += rms;
                        speechSampleCount++;
                        if (rms > peakSpeechRMS) peakSpeechRMS = rms;
                    }
                }
            }
        } finally {
            log.info("Speech calibration: {} total samples, {} speech samples, avg={}, peak={}",
                    totalSampleCount, speechSampleCount,
                    speechSampleCount > 0 ? (int) (sumSpeechRMS / speechSampleCount) : 0,
                    (int) peakSpeechRMS);
        }

        if (speechSampleCount < totalSampleCount / 4) {
            log.warn("Insufficient speech detected ({} speech / {} total). Using noise-based fallback.", speechSampleCount, totalSampleCount);
            UiBus.publish(new AppLogEvent(StringUtls.localizedLlm("log.audioCalibrationInsufficientSpeech")));
            return DEFAULT_RMS_THRESHOLD_HIGH;
        }

        return sumSpeechRMS / speechSampleCount;
    }

    /**
     * Opens and starts the capture line, retrying transient open failures with a short
     * backoff before giving up. This rides out the common Windows case where the mic is
     * momentarily held by another process and the JVM reports it as a confusing
     * "format not supported" error rather than a clean unavailable-line error.
     * <p>
     * Because production logs are pinned to ERROR level, the user cannot see WARN/INFO
     * diagnostics; each transient retry and the final give-up are therefore surfaced to
     * the UI as localized {@link AppLogEvent} notices so the user gets a readable account
     * of what happened (and what to do about it).
     *
     * @return an opened, started {@link TargetDataLine}; the caller owns closing it.
     * @throws AudioCalibrationException if the line cannot be opened after all attempts.
     */
    private static TargetDataLine openAndStartWithRetry(AudioFormat format, int bufferSize, DataLine.Info info, Mixer.Info mixerInfo) {
        String deviceName = mixerInfo != null ? mixerInfo.getName() : "default";
        Exception last = null;
        for (int attempt = 1; attempt <= OPEN_MAX_ATTEMPTS; attempt++) {
            TargetDataLine line = null;
            try {
                line = AudioDeviceEnumerator.openInputLine(info, mixerInfo);
                line.open(format, bufferSize);
                line.start();
                if (attempt > 1) {
                    log.info("Audio input '{}' opened on attempt {}/{}", deviceName, attempt, OPEN_MAX_ATTEMPTS);
                }
                return line;
            } catch (LineUnavailableException | IllegalArgumentException e) {
                last = e;
                if (line != null) line.close();
                log.warn("Audio input '{}' open attempt {}/{} failed: {}", deviceName, attempt, OPEN_MAX_ATTEMPTS, e.getMessage());
                UiBus.publish(new AppLogEvent(StringUtls.localizedLlm("log.audioCalibrationDeviceBusyRetry",
                        deviceName, String.valueOf(attempt), String.valueOf(OPEN_MAX_ATTEMPTS))));
                if (attempt < OPEN_MAX_ATTEMPTS) {
                    try {
                        Thread.sleep((long) OPEN_RETRY_BASE_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AudioCalibrationException("Interrupted while retrying audio input open", ie);
                    }
                }
            }
        }
        log.error("Audio input '{}' unavailable after {} attempts: {}", deviceName, OPEN_MAX_ATTEMPTS,
                last != null ? last.getMessage() : "unknown");
        UiBus.publish(new AppLogEvent(StringUtls.localizedLlm("log.audioCalibrationDeviceUnavailable",
                deviceName, String.valueOf(OPEN_MAX_ATTEMPTS))));
        throw new AudioCalibrationException(
                "Audio input '" + deviceName + "' unavailable after " + OPEN_MAX_ATTEMPTS + " attempts", last);
    }

    private static double calculateRMS(byte[] buffer, int length) {
        if (length < 2) return 0.0;
        double sum = 0.0;
        int samples = length / 2;
        for (int i = 0; i < length; i += 2) {
            int val = (buffer[i + 1] << 8) | (buffer[i] & 0xFF);
            if (val > 32767) val -= 65536;
            sum += (double) val * val;
        }
        return Math.sqrt(sum / samples);
    }
}
