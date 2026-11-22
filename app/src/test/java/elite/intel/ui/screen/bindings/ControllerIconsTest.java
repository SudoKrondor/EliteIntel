package elite.intel.ui.screen.bindings;

import elite.intel.ui.screen.bindings.ControllerIcons.Kind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The picture beside a controller is guessed from its names; anything unrecognised is a joystick. */
class ControllerIconsTest {

    @Test
    void theNamesPickThePicture() {
        assertEquals(Kind.PEDALS, ControllerIcons.kindOf(List.of("T-Rudder")));
        assertEquals(Kind.PEDALS, ControllerIcons.kindOf(List.of("Pedals", "Thrustmaster TPR Pendular Rudder")));
        assertEquals(Kind.YOKE, ControllerIcons.kindOf(List.of("Logitech Flight Yoke System")));
        assertEquals(Kind.THROTTLE, ControllerIcons.kindOf(List.of("VPC Throttle CM3")));
        assertEquals(Kind.THROTTLE, ControllerIcons.kindOf(List.of("TWCS")));
        assertEquals(Kind.CONTROLLER, ControllerIcons.kindOf(List.of("Spare", "Xbox Wireless Controller")));
        assertEquals(Kind.CONTROLLER, ControllerIcons.kindOf(List.of("Spare gamepad")));
    }

    @Test
    void anythingElseIsAJoystick() {
        assertEquals(Kind.JOYSTICK, ControllerIcons.kindOf(List.of("RVWAP", "VPC Stick WarBRD+")));
        assertEquals(Kind.JOYSTICK, ControllerIcons.kindOf(List.of("VPC Control Panel")));
        assertEquals(Kind.JOYSTICK, ControllerIcons.kindOf(List.of()));
    }
}
