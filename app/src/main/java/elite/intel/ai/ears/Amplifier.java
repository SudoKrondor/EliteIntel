package elite.intel.ai.ears;

/**
 * Audio normalizer for 16-bit little-endian PCM.
 * <p>
 * Replaces blind gain amplification with peak normalization to a target
 * dBFS headroom, so transcription input is consistently loud without clipping.
 * <p>
 * Target: -3 dBFS  →  peak target = 32767 * 10^(-3/20) ≈ 23197
 */
public class Amplifier {

    /**
     * Target peak level in linear scale: -3 dBFS
     */
    private static final double TARGET_DBFS = -3.0;
    private static final double TARGET_PEAK = 32767.0 * Math.pow(10.0, TARGET_DBFS / 20.0); // ≈ 23197

    /**
     * A capture is only normalized when its peak clears the calibrated noise floor by this many decibels;
     * anything under it is taken to hold no voice and is left alone, so ambient noise is never lifted to
     * -3 dBFS and handed to the decoder as if it were speech.
     * <p>
     * A ratio, never an absolute amplitude, because the floor is whatever the commander's microphone
     * makes of their room: a quiet mic in a treated room can put real speech under any fixed number
     * that would keep a hot mic's hiss out. 15 dB is the discriminator: room noise peaks 10-12 dB above
     * its own RMS (its crest factor), while a voice sits some way above the floor to begin with and then
     * peaks 12-18 dB above <i>that</i>, so a capture with any speech in it clears the margin however
     * weak the microphone, and a capture with none does not.
     */
    static final double MIN_PEAK_ABOVE_NOISE_DB = 15.0;
    static final double MIN_PEAK_ABOVE_NOISE = Math.pow(10.0, MIN_PEAK_ABOVE_NOISE_DB / 20.0); // ≈ 5.62

    /**
     * The floor in force before calibration has measured one (the noise floor arrives as zero). Held at
     * the old absolute value so an uncalibrated install behaves exactly as it did; only a measured floor
     * moves the gate.
     */
    static final double UNCALIBRATED_MIN_PEAK = 100.0;

    /**
     * Normalizes PCM audio to -3 dBFS peak.
     *
     * @param audioData  16-bit little-endian PCM bytes
     * @param noiseFloor the calibrated ambient RMS in 16-bit sample units, or {@code 0} when there is none
     * @return normalized audio at -3 dBFS peak, or the original when its peak does not clear the floor
     * by {@link #MIN_PEAK_ABOVE_NOISE_DB} (nothing in it to normalize) or already sits at the target
     */
    public static byte[] amplify(byte[] audioData, double noiseFloor) {
        if (audioData == null || audioData.length < 2) return audioData;

        int len = audioData.length & ~1; // ensure even

        // --- Pass 1: find peak sample magnitude ---
        int peak = 0;
        for (int i = 0; i < len; i += 2) {
            int sample = (audioData[i] & 0xFF) | (audioData[i + 1] << 8); // LE, sign-extended
            int abs = Math.abs(sample);
            if (abs > peak) peak = abs;
        }

        // Nothing but the room in the buffer - return as-is
        if (peak < minPeakToNormalize(noiseFloor)) return audioData;

        // --- Derive exact gain to hit target peak ---
        double normalizeGain = TARGET_PEAK / peak;

        // Already at or above target - no amplification needed (avoid boosting loud audio)
        if (normalizeGain <= 1.0) return audioData;

        // --- Pass 2: apply gain with hard clip safety net ---
        byte[] output = new byte[audioData.length];
        for (int i = 0; i < len; i += 2) {
            int sample = (audioData[i] & 0xFF) | (audioData[i + 1] << 8);
            long amplified = Math.round(sample * normalizeGain);
            if (amplified > 32767)  amplified = 32767;
            else if (amplified < -32768) amplified = -32768;
            output[i]     = (byte) (amplified & 0xFF);
            output[i + 1] = (byte) ((amplified >> 8) & 0xFF);
        }

        // Preserve any trailing odd byte (shouldn't happen with 16-bit PCM)
        if (len < audioData.length) output[len] = audioData[len];

        return output;
    }

    /**
     * @return the smallest peak worth normalizing over {@code noiseFloor}: the floor raised by
     * {@link #MIN_PEAK_ABOVE_NOISE_DB}, or {@link #UNCALIBRATED_MIN_PEAK} when no floor has been measured.
     */
    static double minPeakToNormalize(double noiseFloor) {
        return noiseFloor > 0 ? noiseFloor * MIN_PEAK_ABOVE_NOISE : UNCALIBRATED_MIN_PEAK;
    }
}
