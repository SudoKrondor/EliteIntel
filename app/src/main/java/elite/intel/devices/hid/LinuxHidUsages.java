package elite.intel.devices.hid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A Linux controller's HID inputs, from the {@code report_descriptor} the kernel shows under {@code /sys} for the
 * evdev node SDL opened - {@code /dev/input/event12} is {@code /sys/class/input/event12/device/device}. Every user
 * can read it, so no udev rule is needed, unlike reading the device itself through {@code /dev/hidraw}.
 */
final class LinuxHidUsages {

    private static final Logger log = LoggerFactory.getLogger(LinuxHidUsages.class);

    private static final Path SYS = Path.of("/sys");
    private static final Pattern EVDEV_NODE = Pattern.compile("/dev/input/(event\\d+)");

    private LinuxHidUsages() {
    }

    static Optional<List<HidUsage>> read(String devicePath) {
        return read(SYS, devicePath);
    }

    /**
     * WHY: empty rather than an error for every expected miss - a controller that is not HID (an old gameport
     * stick), a path SDL did not give or gave as null, a {@code /sys} a sandbox hides. The caller then knows nothing about the
     * axes, which is what it knew before this existed.
     */
    static Optional<List<HidUsage>> read(Path sys, String devicePath) {
        if (devicePath == null) {
            log.debug("SDL gave no device path, so there is no HID report descriptor to read");
            return Optional.empty();
        }
        var node = EVDEV_NODE.matcher(devicePath);
        if (!node.matches()) {
            log.debug("No evdev node in device path '{}'", devicePath);
            return Optional.empty();
        }
        Path descriptor = sys.resolve("class/input").resolve(node.group(1)).resolve("device/device/report_descriptor");
        try {
            return Optional.of(HidReportDescriptor.inputs(Files.readAllBytes(descriptor)));
        } catch (IOException e) {
            log.debug("No HID report descriptor at {}: {}", descriptor, e.toString());
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            log.warn("The HID report descriptor at {} could not be read: {}", descriptor, e.getMessage());
            return Optional.empty();
        }
    }
}
