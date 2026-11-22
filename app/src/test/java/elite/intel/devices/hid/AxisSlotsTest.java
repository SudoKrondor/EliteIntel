package elite.intel.devices.hid;

import elite.intel.devices.hid.AxisSlots.SdlAxisOrder;
import elite.intel.devices.model.DeviceDescription;
import org.junit.jupiter.api.Test;

import java.util.List;

import static elite.intel.devices.model.GameAxis.RX;
import static elite.intel.devices.model.GameAxis.RY;
import static elite.intel.devices.model.GameAxis.RZ;
import static elite.intel.devices.model.GameAxis.U;
import static elite.intel.devices.model.GameAxis.V;
import static elite.intel.devices.model.GameAxis.X;
import static elite.intel.devices.model.GameAxis.Y;
import static elite.intel.devices.model.GameAxis.Z;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which game slot each HID axis takes - DirectInput's rule, as Wine implements it - and which of them SDL's axis
 * {@code k} is, on each platform.
 */
class AxisSlotsTest {

    private static final HidUsage HID_X = desktop(0x30), HID_Y = desktop(0x31), HID_Z = desktop(0x32);
    private static final HidUsage HID_RX = desktop(0x33), HID_RY = desktop(0x34), HID_RZ = desktop(0x35);
    private static final HidUsage SLIDER = desktop(0x36), DIAL = desktop(0x37), WHEEL = desktop(0x38);
    private static final HidUsage HAT = desktop(0x39);
    private static final HidUsage THROTTLE = simulation(0xBB), RUDDER = simulation(0xBA);

    /** The case A8b exists for: a stick with its twist on RZ, which SDL numbers 0, 1, 2. */
    @Test
    void aTwistOnRzIsRzAndSdlsThirdAxis() {
        DeviceDescription stick = describe(List.of(new HidUsage(0x09, 1), HAT, HID_X, HID_Y, HID_RZ), 3);

        assertEquals(List.of(X, Y, RZ), stick.axes());
        assertEquals(0, stick.axesWithoutSlot());
        assertEquals(List.of(X, Y, RZ), stick.sdlAxes());
    }

    /** The other VIRPIL stick: the same three by position, a different third axis. */
    @Test
    void aTwistOnZIsZ() {
        assertEquals(List.of(X, Y, Z), describe(List.of(HID_X, HID_Y, HID_Z), 3).sdlAxes());
    }

    /** Buttons and hats are not axes, and an axis declared out of order still takes its own slot. */
    @Test
    void onlyAxesTakeSlotsWhateverTheirOrder() {
        DeviceDescription stick = describe(List.of(HID_RZ, HAT, new HidUsage(0x09, 3), HID_X), 2);

        assertEquals(List.of(X, RZ), stick.axes());
        assertEquals(List.of(X, RZ), stick.sdlAxes());
    }

    /** Slider and Dial both fill the slider slots, U then V, in the order the description lists them. */
    @Test
    void slidersAndDialsFillUThenVInDescriptorOrder() {
        DeviceDescription throttle = describe(List.of(HID_X, DIAL, SLIDER), 3);

        assertEquals(List.of(X, U, V), throttle.axes());
        assertEquals(List.of(X, U, V), throttle.sdlAxes());
    }

    /**
     * On Linux SDL lists axes by the kernel's code, and Slider (ABS_THROTTLE) comes before Dial (ABS_RUDDER)
     * whatever the description's order - so there SDL's second axis is the Slider, which is V.
     */
    @Test
    void onLinuxSdlListsTheSliderBeforeTheDial() {
        DeviceDescription throttle = AxisSlots.describe(List.of(HID_X, DIAL, SLIDER), SdlAxisOrder.EVDEV, 3, 0);

        assertEquals(List.of(X, U, V), throttle.axes());
        assertEquals(List.of(X, V, U), throttle.sdlAxes());
    }

    /** Wine's kinds beyond Generic Desktop's own: Throttle is a slider, Rudder is Rz, Wheel is Z. */
    @Test
    void simulationAxesTakeTheSlotsWineGivesThem() {
        assertEquals(List.of(Z, RZ, U), describe(List.of(THROTTLE, RUDDER, WHEEL), 3).axes());
    }

    /** Ten axes: the game sees eight, and the two left over are counted, so a user can be told. */
    @Test
    void axesPastTheEighthHaveNoSlot() {
        DeviceDescription tenAxes = describe(
                List.of(HID_X, HID_Y, HID_Z, HID_RX, HID_RY, HID_RZ, SLIDER, SLIDER, SLIDER, DIAL), 10);

        assertEquals(List.of(X, Y, Z, RX, RY, RZ, U, V), tenAxes.axes());
        assertEquals(2, tenAxes.axesWithoutSlot());
        assertTrue(tenAxes.sdlAxes().isEmpty(), "where SDL puts the other two is not known");
    }

    /** A second X finds its slot taken: the game cannot see it, and SDL's numbering is not trusted. */
    @Test
    void aSecondAxisOfOneKindHasNoSlot() {
        DeviceDescription doubled = describe(List.of(HID_X, HID_Y, HID_X), 3);

        assertEquals(List.of(X, Y), doubled.axes());
        assertEquals(1, doubled.axesWithoutSlot());
        assertTrue(doubled.sdlAxes().isEmpty());
    }

    /** SDL counting other than the description says means something is not understood: no numbering. */
    @Test
    void sdlCountingDifferentlyLeavesItsNumberingUnmatched() {
        DeviceDescription stick = describe(List.of(HID_X, HID_Y, HID_RZ), 4);

        assertEquals(List.of(X, Y, RZ), stick.axes());
        assertTrue(stick.sdlAxes().isEmpty());
    }

    /** On Linux a Slider and a Throttle share ABS_THROTTLE, and the kernel drops one, so nothing is trusted. */
    @Test
    void onLinuxTwoAxesOnOneCodeLeaveTheNumberingUnmatched() {
        DeviceDescription clash = AxisSlots.describe(List.of(SLIDER, THROTTLE), SdlAxisOrder.EVDEV, 2, 0);

        assertEquals(List.of(U, V), clash.axes());
        assertTrue(clash.sdlAxes().isEmpty());
    }

    @Test
    void aControllerWithNoAxesIsDescribedAsHavingNone() {
        DeviceDescription buttonBox = describe(List.of(new HidUsage(0x09, 1)), 0);

        assertTrue(buttonBox.axesKnown());
        assertEquals(List.of(), buttonBox.axes());
    }

    @Test
    void theHatCountIsSdls() {
        assertEquals(2, AxisSlots.describe(List.of(HAT), SdlAxisOrder.DIRECT_INPUT, 0, 2).hatCount());
    }

    private static DeviceDescription describe(List<HidUsage> inputs, int sdlAxisCount) {
        return AxisSlots.describe(inputs, SdlAxisOrder.DIRECT_INPUT, sdlAxisCount, 0);
    }

    private static HidUsage desktop(int id) {
        return new HidUsage(HidUsage.GENERIC_DESKTOP, id);
    }

    private static HidUsage simulation(int id) {
        return new HidUsage(HidUsage.SIMULATION, id);
    }
}
