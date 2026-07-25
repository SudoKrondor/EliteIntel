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
    // The gate is anchored a fixed number of decibels BELOW average speech. Speech is the only
    // stable reference here: the commander's quieter syllables sit some way under the average, and
    // 12 dB clears them without reaching down toward breath and key clicks. Anchoring to a midpoint
    // between floor and speech instead makes the gate a hostage to the noise floor - it lands ~6 dB
    // under speech in a noisy room (dropping half of all speech frames) and 20+ dB under speech in a
    // treated one (opening on anything).
    private static final double GATE_BELOW_SPEECH_DB = 12.0;
    // ...but never closer to the ambient floor than this, which is what keeps the top quartile of
    // room noise from crossing the gate and false-triggering. Binds only in a noisy room.
    private static final double MIN_GATE_ABOVE_NOISE_DB = 6.0;
    // A room is usable when both bounds above can be honoured at once. Derived, never tuned apart
    // from them: if speech does not clear noise by their sum, the two bounds cross and the gate has
    // nowhere legal to sit.
    private static final double MIN_SPEECH_TO_NOISE_DB = GATE_BELOW_SPEECH_DB + MIN_GATE_ABOVE_NOISE_DB;
    // Absolute lower bound applied ONLY to a degenerate measurement (muted mic, or speech that never
    // rose above ambient), where the noise bound alone can leave the gate down in mic self-noise.
    // Never applied to a valid calibration: an absolute amplitude says nothing about whether a gate
    // is well placed, and a quiet room with good gear legitimately gates far below it.
    private static final double DEGENERATE_GATE_FALLBACK = 120.0;
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

        // A room is judged solely on how many DECIBELS speech clears ambient by. The absolute levels
        // are irrelevant: a quiet room with a good microphone yields a low noise floor and a low
        // gate, and that is a healthy calibration, not a degraded one.
        double separation = separationDb(noiseFloor, avgSpeechRMS);
        double gateOpen = gateOpenLevel(noiseFloor, avgSpeechRMS);
        double highThreshold;
        if (separation < MIN_SPEECH_TO_NOISE_DB) {
            log.warn("Insufficient speech/noise separation (noiseFloor={}, speechAvg={}, separation={} dB, minimum={} dB). " +
                            "Environment too loud or mic gain too low; gate pinned just above the floor, speech may be missed.",
                    (int) noiseFloor, (int) avgSpeechRMS, String.format("%.1f", separation), MIN_SPEECH_TO_NOISE_DB);
            UiBus.publish(new AppLogEvent(StringUtls.localizedSpeech("log.audioCalibrationLowGap",
                    String.format("%.1f dB", separation))));
            highThreshold = Math.max(gateOpen, DEGENERATE_GATE_FALLBACK);
        } else {
            highThreshold = gateOpen;
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

        log.info("Final calibrated RMS thresholds: HIGH={} ({}), LOW={} ({}) (speech avg={} ({}), separation={} dB)",
                highThreshold, formatDbfs(highThreshold), lowThreshold, formatDbfs(lowThreshold),
                (int) avgSpeechRMS, formatDbfs(avgSpeechRMS), String.format("%.1f", separation));
        UiBus.publish(new AppLogEvent(StringUtls.localizedSpeech("log.audioCalibrationComplete",
                formatDbfs(highThreshold), formatDbfs(lowThreshold))));
        return new RmsTupple<>(highThreshold, lowThreshold);
    }

    /**
     * @return the linear amplitude ratio equivalent to a gain of {@code db} decibels.
     */
    private static double dbToRatio(double db) {
        return Math.pow(10.0, db / 20.0);
    }

    /**
     * @return {@code amplitude} as a dBFS readout, matching what the mic meter displays.
     */
    private static String formatDbfs(double amplitude) {
        if (amplitude <= 0) return "-inf dBFS";
        return String.format("%.1f dBFS", 20.0 * Math.log10(amplitude / 32768.0));
    }

    /**
     * @return how many decibels {@code avgSpeechRMS} clears {@code noiseFloor} by. Infinite for a
     * digitally silent floor, negative infinity when no speech was captured at all.
     */
    static double separationDb(double noiseFloor, double avgSpeechRMS) {
        if (avgSpeechRMS <= 0) return Double.NEGATIVE_INFINITY;
        if (noiseFloor <= 0) return Double.POSITIVE_INFINITY;
        return 20.0 * Math.log10(avgSpeechRMS / noiseFloor);
    }

    /**
     * Places the VAD gate-open level {@link #GATE_BELOW_SPEECH_DB} below average speech, but no
     * closer than {@link #MIN_GATE_ABOVE_NOISE_DB} to the ambient noise floor.
     * <p>
     * Both bounds are ratios, so the gate tracks the room instead of a hardcoded amplitude. In a
     * quiet room the speech anchor governs and the gate sits far above the floor; in a noisy one the
     * floor bound takes over. When {@link #separationDb} is at least {@link #MIN_SPEECH_TO_NOISE_DB}
     * the two cannot conflict. Below that the caller warns, and the floor bound wins - the safer
     * failure, since a gate in the noise false-triggers continuously.
     * <p>
     * The noise bound therefore also carries a failed speech measurement, where {@code avgSpeechRMS}
     * arrives as zero: the gate still lands {@link #MIN_GATE_ABOVE_NOISE_DB} clear of the ambient
     * floor rather than collapsing to it. Only a mic that captured nothing at all yields zero.
     * <p>
     * Arguments and result are linear RMS amplitudes in 16-bit sample units; the full-scale
     * reference cancels out of every ratio here and is therefore never needed.
     *
     * @return the gate-open amplitude, or {@code 0} when neither speech nor ambient noise registered.
     */
    static double gateOpenLevel(double noiseFloor, double avgSpeechRMS) {
        double belowSpeech = Math.max(avgSpeechRMS, 0) * dbToRatio(-GATE_BELOW_SPEECH_DB);
        double aboveNoise = Math.max(noiseFloor, 0) * dbToRatio(MIN_GATE_ABOVE_NOISE_DB);
        return Math.max(belowSpeech, aboveNoise);
    }

    /**
     * @return whether a persisted {@code gateOpen} still clears {@code noiseFloor} by the minimum
     * margin. The single criterion for a usable gate, shared with the STT startup check so the two
     * cannot disagree. Deliberately not a test against any absolute amplitude: a low gate is exactly
     * what a quiet room and a good microphone produce.
     */
    public static boolean gateClearsNoiseFloor(double noiseFloor, double gateOpen) {
        if (gateOpen <= 0) return false;
        if (noiseFloor <= 0) return true;
        return separationDb(noiseFloor, gateOpen) >= MIN_GATE_ABOVE_NOISE_DB - 1e-9;
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
        GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedResponse(speechKey), done));
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
            UiBus.publish(new AppLogEvent(StringUtls.localizedSpeech("log.audioCalibrationNoAudio")));
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
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedResponse("speech.audioCalibrationNoisy")));
            UiBus.publish(new AppLogEvent(StringUtls.localizedSpeech("log.audioCalibrationHighNoise", String.valueOf((int) noiseFloor))));
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
            UiBus.publish(new AppLogEvent(StringUtls.localizedSpeech("log.audioCalibrationInsufficientSpeech")));
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
                UiBus.publish(new AppLogEvent(StringUtls.localizedSpeech("log.audioCalibrationDeviceBusyRetry",
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
        UiBus.publish(new AppLogEvent(StringUtls.localizedSpeech("log.audioCalibrationDeviceUnavailable",
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
