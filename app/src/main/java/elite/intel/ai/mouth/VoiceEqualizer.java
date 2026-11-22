package elite.intel.ai.mouth;

/**
 * A six-band EQ for one voice, on mono PCM-16 LE: a low shelf, four peaking bands and a high shelf (RBJ
 * Audio EQ Cookbook biquads). Applied to the clean voice, before any radio processing, so the radio effect
 * sits on top of a voice that is already right.
 * <p>
 * The filter state starts fresh on each call. Every call is one sentence that the declicker fades in, so
 * there is no running tail to carry across.
 */
public final class VoiceEqualizer {

    /**
     * Band centres in Hz, low to high: the first is a low shelf's corner, the last a high shelf's, and the
     * four between them are peaking bands. 9 kHz stays below the 12 kHz Nyquist limit of the 24 kHz pipeline.
     */
    public static final int[] BAND_HZ = {100, 300, 800, 2000, 4500, 9000};
    public static final int BAND_COUNT = BAND_HZ.length;

    /**
     * About one and a half octaves wide: broad enough to lift a voice's colour without ringing.
     */
    private static final double PEAK_Q = 1.0;

    private VoiceEqualizer() {
    }

    /**
     * @param pcm        mono PCM-16 LE, changed in place
     * @param sampleRate its rate
     * @param gainsDb    gain per band in dB; a band at 0 is skipped
     */
    public static void apply(byte[] pcm, int sampleRate, int[] gainsDb) {
        if (gainsDb.length != BAND_COUNT) {
            throw new IllegalArgumentException("Expected " + BAND_COUNT + " EQ bands, got " + gainsDb.length);
        }
        int samples = pcm.length / 2;
        double[] signal = null;
        for (int band = 0; band < BAND_COUNT; band++) {
            if (gainsDb[band] == 0) continue;
            if (signal == null) signal = toDoubles(pcm, samples);
            filter(signal, coefficients(band, gainsDb[band], sampleRate));
        }
        if (signal != null) {
            fromDoubles(signal, pcm);
        }
    }

    /**
     * Normalised {b0, b1, b2, a1, a2} for one band.
     */
    static double[] coefficients(int band, int gainDb, int sampleRate) {
        double a = Math.pow(10, gainDb / 40.0);
        double w0 = 2 * Math.PI * BAND_HZ[band] / sampleRate;
        double cos = Math.cos(w0);
        double sin = Math.sin(w0);
        double b0, b1, b2, a0, a1, a2;
        if (band == 0 || band == BAND_COUNT - 1) {
            // Shelf slope 1, the steepest without a bump at the corner.
            double twoRootAAlpha = 2 * Math.sqrt(a) * (sin / 2 * Math.sqrt(2));
            if (band == 0) {
                b0 = a * ((a + 1) - (a - 1) * cos + twoRootAAlpha);
                b1 = 2 * a * ((a - 1) - (a + 1) * cos);
                b2 = a * ((a + 1) - (a - 1) * cos - twoRootAAlpha);
                a0 = (a + 1) + (a - 1) * cos + twoRootAAlpha;
                a1 = -2 * ((a - 1) + (a + 1) * cos);
                a2 = (a + 1) + (a - 1) * cos - twoRootAAlpha;
            } else {
                b0 = a * ((a + 1) + (a - 1) * cos + twoRootAAlpha);
                b1 = -2 * a * ((a - 1) + (a + 1) * cos);
                b2 = a * ((a + 1) + (a - 1) * cos - twoRootAAlpha);
                a0 = (a + 1) - (a - 1) * cos + twoRootAAlpha;
                a1 = 2 * ((a - 1) - (a + 1) * cos);
                a2 = (a + 1) - (a - 1) * cos - twoRootAAlpha;
            }
        } else {
            double alpha = sin / (2 * PEAK_Q);
            b0 = 1 + alpha * a;
            b1 = -2 * cos;
            b2 = 1 - alpha * a;
            a0 = 1 + alpha / a;
            a1 = -2 * cos;
            a2 = 1 - alpha / a;
        }
        return new double[]{b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0};
    }

    private static void filter(double[] signal, double[] c) {
        double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        for (int i = 0; i < signal.length; i++) {
            double x = signal[i];
            double y = c[0] * x + c[1] * x1 + c[2] * x2 - c[3] * y1 - c[4] * y2;
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
            signal[i] = y;
        }
    }

    private static double[] toDoubles(byte[] pcm, int samples) {
        double[] signal = new double[samples];
        for (int i = 0; i < samples; i++) {
            signal[i] = (short) ((pcm[2 * i + 1] << 8) | (pcm[2 * i] & 0xFF));
        }
        return signal;
    }

    private static void fromDoubles(double[] signal, byte[] pcm) {
        for (int i = 0; i < signal.length; i++) {
            long sample = Math.round(signal[i]);
            if (sample > Short.MAX_VALUE) sample = Short.MAX_VALUE;
            else if (sample < Short.MIN_VALUE) sample = Short.MIN_VALUE;
            pcm[2 * i] = (byte) sample;
            pcm[2 * i + 1] = (byte) (sample >>> 8);
        }
    }
}
