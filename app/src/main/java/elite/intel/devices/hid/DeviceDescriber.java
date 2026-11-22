package elite.intel.devices.hid;

import elite.intel.devices.hid.AxisSlots.SdlAxisOrder;
import elite.intel.devices.model.DeviceDescription;
import elite.intel.util.OsDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Describes a controller as it connects: its HID description read on this platform, named by {@link AxisSlots}.
 * <p>
 * <strong>It never throws.</strong> It runs on the device thread, which also carries push-to-talk, so anything
 * that goes wrong reading a description costs that description and nothing else: the controller is described
 * with its axes unknown.
 */
public final class DeviceDescriber {

    private static final Logger log = LoggerFactory.getLogger(DeviceDescriber.class);

    private DeviceDescriber() {
    }

    /**
     * @param name         the controller's name, for the log
     * @param devicePath   the path SDL gives for the controller - a HID path on Windows, an evdev node on Linux
     * @param sdlAxisCount how many axes SDL reports
     * @param hatCount     how many hats SDL reports
     */
    public static DeviceDescription describe(String name, String devicePath, int sdlAxisCount, int hatCount) {
        DeviceDescription description = DeviceDescription.axesUnknown(hatCount);
        try {
            description = switch (OsDetector.getOs()) {
                case WINDOWS -> describe(WindowsHidUsages.read(devicePath), SdlAxisOrder.DIRECT_INPUT,
                        sdlAxisCount, hatCount);
                case LINUX -> describe(LinuxHidUsages.read(devicePath), SdlAxisOrder.EVDEV, sdlAxisCount, hatCount);
                case MAC -> description;
            };
        } catch (RuntimeException | LinkageError e) {
            // WHY: broad on purpose - the device thread must outlive a native call that fails in a way nobody
            // listed, and hid.dll failing to load is a LinkageError.
            log.warn("Could not describe the axes of '{}'", name, e);
        }
        String axes = description.axesKnown() ? description.axes().toString() : "unknown";
        String numbering = description.sdlAxes().isEmpty() ? "not matched" : "matched";
        log.info("Device '{}': axes {}, {} without a game slot, SDL numbering {}, {} hat(s)",
                name, axes, description.axesWithoutSlot(), numbering, description.hatCount());
        return description;
    }

    /** Names the axes read, or - when none could be read - keeps SDL's hat count and says the axes are unknown. */
    static DeviceDescription describe(Optional<List<HidUsage>> inputs, SdlAxisOrder order,
                                      int sdlAxisCount, int hatCount) {
        return inputs.map(usages -> AxisSlots.describe(usages, order, sdlAxisCount, hatCount))
                .orElseGet(() -> DeviceDescription.axesUnknown(hatCount));
    }
}
