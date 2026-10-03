package elite.intel.ai.ears;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the one decision the normalizer makes - whether a capture holds anything worth lifting to
 * -3 dBFS - and that the decision is taken against the room the microphone is in, not against a
 * fixed amplitude. Levels are linear amplitudes in 16-bit sample units.
 */
class AmplifierTest {

    /**
     * -3 dBFS, the normalizer's peak target.
     */
    private static final int TARGET_PEAK = 23197;

    /**
     * A buffer of silence carrying one sample at {@code peak}, which is what the normalizer measures.
     */
    private static byte[] withPeak(int peak) {
        byte[] pcm = new byte[400];
        pcm[200] = (byte) (peak & 0xFF);
        pcm[201] = (byte) ((peak >> 8) & 0xFF);
        return pcm;
    }

    private static int peakOf(byte[] pcm) {
        int peak = 0;
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            int sample = Math.abs((int) (short) (((pcm[i + 1] & 0xFF) << 8) | (pcm[i] & 0xFF)));
            if (sample > peak) peak = sample;
        }
        return peak;
    }

    @Nested
    @DisplayName("The floor is a ratio over the room, so a quiet microphone is still lifted")
    class RelativeFloor {

        @ParameterizedTest(name = "floor {0}, peak {1}")
        @CsvSource({
                // A quiet microphone in a treated room: real speech well under the old absolute 100.
                "5,    40",
                "10,   80",
                // Ordinary rooms.
                "30,   500",
                "100,  1000",
                // A noisy room whose speech is loud in proportion.
                "800,  6000",
        })
        void speechClearingTheFloorIsNormalized(double floor, int peak) {
            byte[] out = Amplifier.amplify(withPeak(peak), floor);
            assertEquals(TARGET_PEAK, peakOf(out), 1);
        }

        @ParameterizedTest(name = "floor {0}, peak {1}")
        @CsvSource({
                // Noise-only captures: the peak sits at the room's own crest factor (~10-12 dB), under 15.
                "30,   90",
                "100,  330",
                "800,  2500",
                // A hot microphone's hiss: loud in absolute terms, still just the room.
                "2000, 8000",
        })
        void theRoomAloneIsLeftAsItIs(double floor, int peak) {
            byte[] in = withPeak(peak);
            assertSame(in, Amplifier.amplify(in, floor));
        }

        @Test
        void theMarginIsFifteenDecibelsOverTheFloor() {
            double floor = 100;
            int justUnder = (int) Math.floor(floor * Amplifier.MIN_PEAK_ABOVE_NOISE) - 1;
            int justOver = (int) Math.ceil(floor * Amplifier.MIN_PEAK_ABOVE_NOISE) + 1;
            byte[] under = withPeak(justUnder);
            assertSame(under, Amplifier.amplify(under, floor));
            assertEquals(TARGET_PEAK, peakOf(Amplifier.amplify(withPeak(justOver), floor)), 1);
        }
    }

    @Nested
    @DisplayName("Before calibration the old absolute floor stands, so an uncalibrated install is unchanged")
    class UncalibratedFloor {

        @Test
        void underTheAbsoluteFloorIsRefused() {
            byte[] in = withPeak(90);
            assertSame(in, Amplifier.amplify(in, 0));
        }

        @Test
        void overTheAbsoluteFloorIsNormalized() {
            assertEquals(TARGET_PEAK, peakOf(Amplifier.amplify(withPeak(150), 0)), 1);
        }

        @Test
        void aNegativeFloorCountsAsNone() {
            assertEquals(Amplifier.UNCALIBRATED_MIN_PEAK, Amplifier.minPeakToNormalize(-1));
        }
    }

    @Nested
    @DisplayName("Loud input is never boosted or clipped")
    class Ceiling {

        @Test
        void atOrAboveTheTargetIsUntouched() {
            byte[] in = withPeak(30000);
            assertSame(in, Amplifier.amplify(in, 100));
        }

        @Test
        void degenerateBuffersPassThrough() {
            assertNull(Amplifier.amplify(null, 100));
            byte[] one = new byte[1];
            assertSame(one, Amplifier.amplify(one, 100));
        }
    }
}
