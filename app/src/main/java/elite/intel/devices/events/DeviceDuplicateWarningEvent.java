package elite.intel.devices.events;

import elite.intel.devices.model.Device;

/**
 * Published when two connected devices share the same VID/PID. The Elite Dangerous game itself
 * cannot distinguish between them; consumers should warn the user and may use {@code usbPath}
 * to differentiate the two devices for display purposes only.
 */
public record DeviceDuplicateWarningEvent(Device device1, Device device2) {
}
