package elite.intel.devices.events;

import elite.intel.devices.model.Device;

/** Published when a new device is detected by the {@code elite.intel.devices} poll loop. */
public record DeviceConnectedEvent(Device device) {
}
