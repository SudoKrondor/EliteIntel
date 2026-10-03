package elite.intel.ai.mouth.supertonic;

/**
 * The commander's extra output gain for Supertonic 3 (audio settings), on 24 kHz mono PCM-16 LE. Peaks above
 * 85% of full scale are rounded off by a soft ceiling rather than clipped flat.
 */
public final class SupertonicBoost {

    private static final double SOFT_CEILING = 0.85;

    private SupertonicBoost() {
    }

    /**
     * @param percent 0 to 100: how much louder, 100 doubling ordinary samples
     */
    public static void apply(byte[] pcm, int percent) {
        if (percent < 0 || percent > 100) throw new IllegalArgumentException("Supertonic boost must be 0-100%");
        double gain = 1 + percent / 100.0;
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            short raw = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xff));
            double raised = raw / 32768.0 * gain;
            double magnitude = Math.abs(raised);
            if (magnitude > SOFT_CEILING) {
                double headroom = 1 - SOFT_CEILING;
                raised = Math.copySign(SOFT_CEILING + headroom * (1 - Math.exp(-(magnitude - SOFT_CEILING) / headroom)), raised);
            }
            int sample = (int) Math.round(raised * 32767);
            pcm[i] = (byte) sample;
            pcm[i + 1] = (byte) (sample >>> 8);
        }
    }
}
