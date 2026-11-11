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

    /** E2: an unplugged controller offers the axes its labels or bindings mention, whole or by direction. */
    @Test
    void anUnpluggedControllerOffersTheAxesItsNamesMention() {
        assertEquals(List.of(GameAxis.Y, GameAxis.RZ),
                CaptureControllers.axesOffered(List.of("Joy_RZAxis", "Neg_Joy_YAxis", "pos_joy_yaxis", "Joy_3")));
    }

    /** With none mentioned, all eight the game names, as an axis row offers. */
    @Test
    void anUnpluggedControllerMentioningNoAxisOffersAllEight() {
        assertEquals(List.of(GameAxis.values()), CaptureControllers.axesOffered(List.of("Joy_1", "Joy_POV1Up")));
        assertEquals(List.of(), CaptureControllers.axesNamed(List.of("Joy_1", "Key_A")));
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
