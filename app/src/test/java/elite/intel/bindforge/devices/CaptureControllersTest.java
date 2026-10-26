package elite.intel.bindforge.devices;

import elite.intel.devices.model.DeviceDescription;
import elite.intel.devices.model.GameAxis;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private static final List<GameAxis> GAMEPAD_AXES =
            List.of(GameAxis.X, GameAxis.Y, GameAxis.Z, GameAxis.RX, GameAxis.RY, GameAxis.RZ);

    @Test
    void anUnmatchedGamePadUsesSdlGamePadAxisOrder() {
        assertEquals(GAMEPAD_AXES, CaptureControllers.axesFor(true, 6, DeviceDescription.axesUnknown(0)));
    }

    @Test
    void gamePadOrderWinsOverACompatibleGenericDescription() {
        DeviceDescription generic = new DeviceDescription(true,
                List.of(GameAxis.X, GameAxis.Y, GameAxis.Z, GameAxis.RX, GameAxis.RY, GameAxis.RZ), 0,
                List.of(GameAxis.RZ, GameAxis.RY, GameAxis.RX, GameAxis.Z, GameAxis.Y, GameAxis.X), 0);
        assertEquals(GAMEPAD_AXES, CaptureControllers.axesFor(true, 6, generic));
    }
}
