package elite.intel.devices.events;

/** Published on every button state transition — press and release, never while held. */
public record DeviceButtonEvent(int deviceId, int buttonIndex, boolean pressed) {
}
