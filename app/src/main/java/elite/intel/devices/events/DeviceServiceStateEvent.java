package elite.intel.devices.events;

/**
 * Published by DeviceService when SDL3 initialization completes — success or failure.
 * Subscribers should switch to the EDT before touching Swing components.
 */
public record DeviceServiceStateEvent(boolean available, String errorMessage) {
}
