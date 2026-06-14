package elite.intel.devices.events;

/** Published when an axis value changes beyond the deadzone threshold. {@code value} is in [-1.0, 1.0]. */
public record DeviceAxisEvent(int deviceId, int axisIndex, float value) {
}
