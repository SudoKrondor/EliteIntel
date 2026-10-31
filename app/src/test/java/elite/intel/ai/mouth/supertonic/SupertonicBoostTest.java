package elite.intel.ai.mouth.supertonic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupertonicBoostTest {

    @Test
    void raisesNormalSamplesBySixtyPercentAndLimitsPeaks() {
        byte[] pcm = {0x10, 0x27, (byte) 0xff, 0x7f}; // 10,000 and 32,767
        SupertonicBoost.apply(pcm);
        assertEquals(16000, sample(pcm, 0), 2);
        assertTrue(sample(pcm, 1) > 29000);
        assertTrue(sample(pcm, 1) < 32767);
    }

    @Test
    void negativePeaksAreLimitedToo() {
        byte[] pcm = {0x01, (byte) 0x80}; // -32,767
        SupertonicBoost.apply(pcm);
        assertTrue(sample(pcm, 0) < -29000);
        assertTrue(sample(pcm, 0) > -32768);
    }

    private static int sample(byte[] pcm, int index) {
        return (short) ((pcm[index * 2 + 1] << 8) | (pcm[index * 2] & 0xff));
    }
}
