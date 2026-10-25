package elite.intel.ai.mouth.supertonic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupertonicTTSSpeedTest {

    @Test
    void appliesSpeedAdjustmentAtFortyAndFiftyPercentSliderPositions() {
        assertEquals(1.05f, SupertonicTTS.effectivePace(1.4f), 0.0001f);
        assertEquals(1.125f, SupertonicTTS.effectivePace(1.5f), 0.0001f);
    }

    @Test
    void clampsEffectiveSpeedToSupportedRange() {
        assertEquals(0.8f, SupertonicTTS.effectivePace(0.5f), 0.0001f);
        assertEquals(1.5f, SupertonicTTS.effectivePace(2.0f), 0.0001f);
    }

    @Test
    void rejectsNonFiniteSpeed() {
        assertThrows(IllegalArgumentException.class, () -> SupertonicTTS.effectivePace(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> SupertonicTTS.effectivePace(Float.POSITIVE_INFINITY));
    }
}
