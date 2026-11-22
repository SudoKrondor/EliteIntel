package elite.intel.devices.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static elite.intel.devices.model.GameAxis.RZ;
import static elite.intel.devices.model.GameAxis.X;
import static elite.intel.devices.model.GameAxis.Y;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A description cannot claim what it does not know, and SDL's numbering must be the controller's own axes. */
class DeviceDescriptionTest {

    @Test
    void sdlsIndexNamesItsAxis() {
        DeviceDescription stick = new DeviceDescription(true, List.of(X, Y, RZ), 0, List.of(X, Y, RZ), 0);

        assertEquals(Optional.of(RZ), stick.axisAt(2));
        assertEquals(Optional.empty(), stick.axisAt(3));
        assertEquals(Optional.empty(), stick.axisAt(-1));
    }

    @Test
    void unmatchedNumberingNamesNoAxis() {
        DeviceDescription stick = new DeviceDescription(true, List.of(X, Y, RZ), 1, List.of(), 0);

        assertEquals(Optional.empty(), stick.axisAt(0));
    }

    @Test
    void unknownAxesCannotBeNamed() {
        assertThrows(IllegalArgumentException.class,
                () -> new DeviceDescription(false, List.of(X), 0, List.of(), 0));
    }

    @Test
    void sdlsNumberingMustBeTheControllersAxes() {
        assertThrows(IllegalArgumentException.class,
                () -> new DeviceDescription(true, List.of(X, Y), 0, List.of(X, RZ), 0));
    }

    /** The axes are kept in the game's order whatever order they are given in, so labels are written in it. */
    @Test
    void axesAreKeptInTheGamesOrder() {
        DeviceDescription stick = new DeviceDescription(true, List.of(RZ, X, Y), 0, List.of(X, Y, RZ), 0);

        assertEquals(List.of(X, Y, RZ), stick.axes());
    }

    @Test
    void gamepadUsesItsSixSdlAxesWithoutUAndV() {
        DeviceDescription gamepad = DeviceDescription.gamepad(6, 0);
        assertEquals(List.of(X, Y, GameAxis.Z, GameAxis.RX, GameAxis.RY, RZ), gamepad.sdlAxes());
    }

    @Test
    void anAxisCannotBeHeldTwice() {
        assertThrows(IllegalArgumentException.class,
                () -> new DeviceDescription(true, List.of(X, X), 0, List.of(), 0));
    }

    /** The description is trusted only when it counts every axis SDL does, slotted or not. */
    @Test
    void itAgreesWithSdlOnlyWhenItCountsTheSameAxes() {
        DeviceDescription tenAxes = new DeviceDescription(true, List.of(GameAxis.values()), 2, List.of(), 0);

        assertTrue(tenAxes.agreesWith(10));
        assertFalse(tenAxes.agreesWith(8));
        assertFalse(DeviceDescription.axesUnknown(0).agreesWith(0));
    }
}
