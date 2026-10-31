package elite.intel.ai.mouth.supertonic;

/**
 * Supertonic 3's extra output gain, on 24 kHz mono PCM-16 LE. The engine renders quieter than the others, so its
 * speech is always raised by 60%. Peaks above 85% of full scale are rounded off by a soft ceiling rather than
 * clipped flat.
 */
public final class SupertonicBoost {

    /**
     * The level the commander settled on while it was still a slider (60%), fixed once tuned.
     */
    private static final double GAIN = 1.6;
    private static final double SOFT_CEILING = 0.85;

    private SupertonicBoost() {
    }

    public static void apply(byte[] pcm) {
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            short raw = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xff));
            double raised = raw / 32768.0 * GAIN;
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
