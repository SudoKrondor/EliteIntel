package elite.intel.ai.ears.parakeet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The leading trim must never hand the recogniser a waveform that starts on the first syllable. Parakeet
 * decodes such a capture to nothing about a quarter of the time - the push-to-talk "half my orders vanish"
 * report - while the same audio with a fifth of a second of room before the word decodes normally.
 */
class ParakeetLeadingTrimTest {

    private static final double NOISE_FLOOR = 150;
    private static final int BYTES_PER_MS = 32; // 16 kHz, 16-bit mono

    @Test
    void keepsTheLeadInBeforeTheFirstWord() {
        byte[] capture = capture(1000, 800);

        byte[] trimmed = ParakeetSTTImpl.trimLeadingLowEnergy(capture, NOISE_FLOOR);

        assertEquals((ParakeetSTTImpl.LEAD_IN_MS + 800) * BYTES_PER_MS, trimmed.length);
    }

    @Test
    void leavesALeadInShorterThanTheMarginAlone() {
        byte[] capture = capture(100, 800);

        assertSame(capture, ParakeetSTTImpl.trimLeadingLowEnergy(capture, NOISE_FLOOR));
    }

    @Test
    void leavesACaptureThatStartsLoudAlone() {
        byte[] capture = capture(0, 800);

        assertSame(capture, ParakeetSTTImpl.trimLeadingLowEnergy(capture, NOISE_FLOOR));
    }

    /**
     * {@code quietMs} of silence, then {@code loudMs} of a level far over the trim threshold.
     */
    private static byte[] capture(int quietMs, int loudMs) {
        byte[] pcm = new byte[(quietMs + loudMs) * BYTES_PER_MS];
        for (int i = quietMs * BYTES_PER_MS; i + 1 < pcm.length; i += 2) {
            short sample = (short) ((i / 2) % 2 == 0 ? 8000 : -8000);
            pcm[i] = (byte) sample;
            pcm[i + 1] = (byte) (sample >> 8);
        }
        return pcm;
    }
}
