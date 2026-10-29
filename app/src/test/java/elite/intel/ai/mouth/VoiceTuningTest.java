package elite.intel.ai.mouth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VoiceTuningTest {

    @Test
    void theDefaultIsSpeedTwentyFullVolumeAndAFlatEq() {
        assertEquals(20, VoiceTuning.DEFAULT.speed());
        assertEquals(100, VoiceTuning.DEFAULT.volume());
        assertArrayEquals(new int[VoiceEqualizer.BAND_COUNT], VoiceTuning.DEFAULT.eq());
        assertEquals(1.2f, VoiceTuning.DEFAULT.pace(), 1e-6);
        assertEquals(1.0f, VoiceTuning.DEFAULT.volumeGain(), 1e-6);
    }

    @Test
    void valuesOutsideTheSlidersAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> VoiceTuning.DEFAULT.withSpeed(101));
        assertThrows(IllegalArgumentException.class, () -> VoiceTuning.DEFAULT.withVolume(151));
        assertThrows(IllegalArgumentException.class, () -> VoiceTuning.DEFAULT.withEqBand(0, 13));
        assertThrows(IllegalArgumentException.class, () -> new VoiceTuning(20, 100, new int[5]));
    }

    @Test
    void theEqCannotBeChangedFromOutside() {
        int[] bands = new int[VoiceEqualizer.BAND_COUNT];
        VoiceTuning tuning = new VoiceTuning(20, 100, bands);
        bands[0] = 6;
        tuning.eq()[1] = 6;

        assertEquals(VoiceTuning.DEFAULT, tuning);
    }

    @Test
    void editingOneBandLeavesTheRestAndTheOriginal() {
        VoiceTuning edited = VoiceTuning.DEFAULT.withEqBand(3, -4);

        assertArrayEquals(new int[]{0, 0, 0, -4, 0, 0}, edited.eq());
        assertNotEquals(VoiceTuning.DEFAULT, edited);
        assertArrayEquals(new int[VoiceEqualizer.BAND_COUNT], VoiceTuning.DEFAULT.eq());
    }
}
