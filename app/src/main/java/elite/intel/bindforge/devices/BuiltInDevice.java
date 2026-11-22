package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;

import java.util.List;

/**
 * One row of the Built-in Devices list: an entry in Frontier's shipped {@code DeviceMappings.xml}.
 * <p>
 * A readout of the reference BindForge carries in the jar, never of a game file - so it says what Frontier
 * shipped, not what any installation holds now.
 *
 * @param name         the element tag, as Frontier spells it
 * @param primary      the element's own VID/PID pair, or {@code null} for an element carrying none
 * @param alternatives every {@code <Alternative>} pair, in file order - 79 of them on {@code <GamePad>}
 * @param attached     whether a controller plugged in right now resolves to this entry
 */
public record BuiltInDevice(String name, HardwareId primary, List<HardwareId> alternatives, boolean attached) {

    public BuiltInDevice {
        alternatives = List.copyOf(alternatives);
    }
}
