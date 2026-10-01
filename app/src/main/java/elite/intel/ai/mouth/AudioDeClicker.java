package elite.intel.ai.mouth;

public class AudioDeClicker {

    /**
     * Scales every PCM-16 LE sample in {@code pcm} by {@code gain}.
     * {@code gain} should be in the range [0.0, 1.0] where 1.0 is full volume.
     * Samples are clamped to the signed 16-bit range after scaling.
     */
    public static void applyVolume(byte[] pcm, float gain) {
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            short sample = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xFF));
            int scaled = Math.round(sample * gain);
            if (scaled > 32767) scaled = 32767;
            else if (scaled < -32768) scaled = -32768;
            pcm[i] = (byte) (scaled & 0xFF);
            pcm[i + 1] = (byte) ((scaled >>> 8) & 0xFF);
        }
    }

    public static void sanitize(byte[] audioData, int fadeMs) {
        //removeClicks(audioData);
        applyFade(audioData, fadeMs, true);
    }

    private static void removeClicks(byte[] audioData) {
        if ((audioData.length & 1) != 0) {
            byte[] even = new byte[audioData.length - 1];
            System.arraycopy(audioData, 0, even, 0, even.length);
        }
    }

    private static void applyFade(byte[] audioData, int fadeMs, boolean isFadeIn) {
        int samplesToFade = (24000 * fadeMs) / 1000;
        int startIndex = isFadeIn ? 0 : Math.max(0, audioData.length / 2 - samplesToFade);
        for (int i = startIndex; i < startIndex + samplesToFade && (i * 2 + 1) < audioData.length; i++) {
            int lo = audioData[2 * i] & 0xFF;
            int hi = audioData[2 * i + 1] & 0xFF;
            short sample = (short) ((hi << 8) | lo);
            float gain = isFadeIn ? (float) i / samplesToFade : (float) (startIndex + samplesToFade - i) / samplesToFade;
            int scaled = Math.round(sample * gain);
            audioData[2 * i] = (byte) (scaled & 0xFF);
            audioData[2 * i + 1] = (byte) ((scaled >>> 8) & 0xFF);
        }
    }
}
