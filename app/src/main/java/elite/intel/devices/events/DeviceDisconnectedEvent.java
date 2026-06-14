package elite.intel.devices.events;

/** Published when a previously connected device is no longer detected. */
public record DeviceDisconnectedEvent(int deviceId) {
}
