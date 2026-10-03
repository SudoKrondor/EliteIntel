package elite.intel.ai.mouth.supertonic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SupertonicBoostTest {

    @Test
    void raisesNormalSamplesAndLimitsPeaks() {
        byte[] pcm = {0x10, 0x27, (byte) 0xff, 0x7f}; // 10,000 and 32,767
        SupertonicBoost.apply(pcm, 20);
        assertEquals(12000, sample(pcm, 0), 2);
        assertTrue(sample(pcm, 1) > 29000);
        assertTrue(sample(pcm, 1) < 32767);
    }

    @Test
    void canReachOneHundredPercentAndZeroLeavesOrdinarySamplesAlone() {
        byte[] quiet = {0x10, 0x27}; // 10,000
        SupertonicBoost.apply(quiet, 100);
        assertEquals(20000, sample(quiet, 0), 2);
        byte[] zero = {0x10, 0x27};
        SupertonicBoost.apply(zero, 0);
        assertEquals(10000, sample(zero, 0), 2);
        assertThrows(IllegalArgumentException.class, () -> SupertonicBoost.apply(zero, 101));
    }

    private static int sample(byte[] pcm, int index) {
        return (short) ((pcm[index * 2 + 1] << 8) | (pcm[index * 2] & 0xff));
    }
}
