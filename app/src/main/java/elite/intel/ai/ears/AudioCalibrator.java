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
    private static final double MIN_SPEECH_NOISE_GAP = 150.0;
    private static final double MAX_NOISE_AVG = 800.0;


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

        // VAD trigger = midpoint between noise and speech.
        // Always above ambient, always below speech, regardless of absolute levels.
        double gap = avgSpeechRMS - noiseFloor;
        double highThreshold;
        if (avgSpeechRMS <= noiseFloor || gap < MIN_SPEECH_NOISE_GAP) {
            log.warn("Insufficient speech/noise separation (gap={}). Speech may not have been detected or environment is too loud.", gap);
            UiBus.publish(new AppLogEvent(StringUtls.localizedLlm("log.audioCalibrationLowGap", String.valueOf((int) gap))));
            // Fallback: 30% above noise floor
            highThreshold = noiseFloor * 1.3 + 50;
        } else {
            highThreshold = noiseFloor + gap * GATE_MIDPOINT_FACTOR;
        }

        // noiseFloor is stored as-is (raw measured ambient level)
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

        try (TargetDataLine line = AudioDeviceEnumerator.openInputLine(info, mixerInfo)) {
            line.open(format, bufferSize);
            line.start();
            while (System.currentTimeMillis() - startTime < NOISE_CALIBRATION_DURATION_MS) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    byte[] mono16 = AudioFormatDetector.toPCM16Mono(buffer, bytesRead, format);
                    samples.add(calculateRMS(mono16, mono16.length));
                }
            }
        } catch (LineUnavailableException | IllegalArgumentException e) {
            log.error("Noise calibration failed: {}", e.getMessage());
            UiBus.publish(new AppLogEvent(StringUtls.localizedLlm("log.audioCalibrationNoiseFailed", String.valueOf(e.getMessage()))));
            throw new AudioCalibrationException("Noise calibration failed: " + e.getMessage(), e);
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

        try (TargetDataLine line = AudioDeviceEnumerator.openInputLine(info, mixerInfo)) {
            line.open(format, bufferSize);
            line.start();
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
        } catch (LineUnavailableException | IllegalArgumentException e) {
            log.error("Speech calibration failed: {}", e.getMessage());
            UiBus.publish(new AppLogEvent(StringUtls.localizedLlm("log.audioCalibrationSpeechFailed", String.valueOf(e.getMessage()))));
            throw new AudioCalibrationException("Speech calibration failed: " + e.getMessage(), e);
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
