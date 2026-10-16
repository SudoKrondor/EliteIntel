package elite.intel.bindforge.devices;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * An unplugged controller is offered up to the highest button and hat its saved names or its bindings mention.
 */
class CaptureControllersTest {

    @Test
    void countsReachTheHighestNumberedButtonAndHat() {
        assertArrayEquals(new int[]{79, 2},
                CaptureControllers.counts(List.of("Joy_1", "Joy_79", "Joy_POV1Up", "Joy_POV2Left", "Joy_XAxis")));
    }

    @Test
    void axesAndKeysCountForNothing() {
        assertArrayEquals(new int[]{0, 0}, CaptureControllers.counts(List.of("Joy_RZAxis", "Neg_Joy_YAxis", "Key_A")));
    }
}
