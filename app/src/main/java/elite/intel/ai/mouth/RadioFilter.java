package elite.intel.ai.mouth;

import java.util.Random;

/**
 * Simulates a shortwave radio transmission effect on PCM-16 LE audio at 24000 Hz.
 * <p>
 * Processing chain:
 * <ol>
 *   <li>Butterworth highpass at 300 Hz - cuts bass rumble and voice fundamental</li>
 *   <li>Butterworth lowpass at 5500 Hz - passes sibilance and upper harmonics for shortwave character</li>
 *   <li>Light static noise - ~1% random amplitude</li>
 *   <li>Gain compensation - recovers level lost to the bandpass attenuation</li>
 * </ol>
 */
public class RadioFilter {

    // Keep the radio band-pass character without adding a constant hiss to every transmission.
    private static final float NOISE_AMPLITUDE = 0f;

    // Compensate for energy loss caused by the narrow bandpass, then reduce by 30%
    private static final float GAIN = 1.4f; // 2.0 (bandpass compensation) × 0.70 (−30% volume)

    // --- Butterworth highpass biquad, fc=300 Hz, fs=24000 Hz, Q=0.707 --------
    // ω0 = 2π·300/24000 = 0.07854  sin=0.07846  cos=0.99692  α=0.05547
    private static final double HP_B0 = 0.94601;
    private static final double HP_B1 = -1.89201;
    private static final double HP_B2 = 0.94601;
    private static final double HP_A1 = -1.88909;
    private static final double HP_A2 = 0.89489;

    // --- Butterworth lowpass biquad, fc=5500 Hz, fs=24000 Hz, Q=0.707 --------
    // ω0 = 2π·5500/24000 = 1.4400  sin=0.99146  cos=0.13042  α=0.70117
    private static final double LP_B0 = 0.25558;
    private static final double LP_B1 = 0.51117;
    private static final double LP_B2 = 0.25558;
    private static final double LP_A1 = -0.15333;
    private static final double LP_A2 = 0.17566;

    private static final Random RNG = new Random();

    private RadioFilter() {
    }

    /**
     * Applies the radio transmission effect to a PCM-16 LE buffer in-place.
     * Expects 24000 Hz, 16-bit, mono, signed, little-endian data.
     */
    public static void apply(byte[] pcm) {
        int numSamples = pcm.length / 2;
        float[] samples = new float[numSamples];

        // Decode PCM-16 LE → float [-1, 1]
        for (int i = 0; i < numSamples; i++) {
            short raw = (short) ((pcm[2 * i + 1] << 8) | (pcm[2 * i] & 0xFF));
            samples[i] = raw / 32767f;
        }

        // Bandpass: highpass then lowpass
        applyBiquad(samples, HP_B0, HP_B1, HP_B2, HP_A1, HP_A2);
        applyBiquad(samples, LP_B0, LP_B1, LP_B2, LP_A1, LP_A2);

        // Add light static and apply gain compensation
        float noiseScale = NOISE_AMPLITUDE / 32767f;
        for (int i = 0; i < numSamples; i++) {
            float noise = (RNG.nextFloat() * 2f - 1f) * noiseScale;
            samples[i] = samples[i] * GAIN + noise;
        }

        // Encode float → PCM-16 LE with clamp
        for (int i = 0; i < numSamples; i++) {
            int s = Math.round(samples[i] * 32767f);
            if (s > 32767) s = 32767;
            else if (s < -32768) s = -32768;
            pcm[2 * i] = (byte) (s & 0xFF);
            pcm[2 * i + 1] = (byte) ((s >> 8) & 0xFF);
        }
    }

    /**
     * Direct-form II transposed biquad, processes {@code x} in-place.
     */
    private static void applyBiquad(float[] x, double b0, double b1, double b2, double a1, double a2) {
        double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        for (int i = 0; i < x.length; i++) {
            double xi = x[i];
            double yi = b0 * xi + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = xi;
            y2 = y1;
            y1 = yi;
            x[i] = (float) yi;
        }
    }
}