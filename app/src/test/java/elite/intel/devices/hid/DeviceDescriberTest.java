package elite.intel.devices.hid;

import elite.intel.devices.hid.AxisSlots.SdlAxisOrder;
import elite.intel.devices.model.DeviceDescription;
import elite.intel.devices.model.GameAxis;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What {@code DeviceService} relies on: a description it could not read still carries SDL's hats. */
class DeviceDescriberTest {

    @Test
    void anUnreadDescriptionKeepsTheHatsAndSaysTheAxesAreUnknown() {
        DeviceDescription unread = DeviceDescriber.describe(Optional.empty(), SdlAxisOrder.DIRECT_INPUT, 3, 2);

        assertEquals(DeviceDescription.axesUnknown(2), unread);
    }

    @Test
    void aReadDescriptionNamesItsAxes() {
        List<HidUsage> rudder = List.of(new HidUsage(HidUsage.SIMULATION, 0xBA));

        DeviceDescription read = DeviceDescriber.describe(Optional.of(rudder), SdlAxisOrder.DIRECT_INPUT, 1, 0);

        assertEquals(List.of(GameAxis.RZ), read.axes());
    }
}
