package elite.intel.ai.ears.parakeet;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
    void leavesALeadInOfExactlyTheMarginAlone() {
        byte[] capture = capture(ParakeetSTTImpl.LEAD_IN_MS, 800);

        assertSame(capture, ParakeetSTTImpl.trimLeadingLowEnergy(capture, NOISE_FLOOR));
    }

    @Test
    void topsUpALeadInShorterThanTheMarginWithSilence() {
        byte[] capture = capture(100, 800);

        byte[] conditioned = ParakeetSTTImpl.trimLeadingLowEnergy(capture, NOISE_FLOOR);

        assertEquals((ParakeetSTTImpl.LEAD_IN_MS + 800) * BYTES_PER_MS, conditioned.length);
        assertQuietThenUnchanged(capture, conditioned, ParakeetSTTImpl.LEAD_IN_MS - 100);
    }

    /**
     * Push-to-talk has no pre-roll, so a commander who speaks as the button goes down hands over a capture
     * that is loud from its first frame. Five of those in a row decoded to nothing in one support bundle.
     */
    @Test
    void givesACaptureThatStartsLoudAFullLeadIn() {
        byte[] capture = capture(0, 800);

        byte[] conditioned = ParakeetSTTImpl.trimLeadingLowEnergy(capture, NOISE_FLOOR);

        assertEquals((ParakeetSTTImpl.LEAD_IN_MS + 800) * BYTES_PER_MS, conditioned.length);
        assertQuietThenUnchanged(capture, conditioned, ParakeetSTTImpl.LEAD_IN_MS);
    }

    /**
     * {@code conditioned} is {@code addedMs} of digital silence followed by every byte of {@code capture}.
     */
    private static void assertQuietThenUnchanged(byte[] capture, byte[] conditioned, int addedMs) {
        int added = addedMs * BYTES_PER_MS;
        for (int i = 0; i < added; i++) assertEquals(0, conditioned[i], "added silence at byte " + i);
        assertArrayEquals(capture, Arrays.copyOfRange(conditioned, added, conditioned.length));
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
