package elite.intel.starvizion.event;

/**
 * Published by SdlInputService when SDL3 initialization completes — success or failure.
 * Subscribers should switch to the EDT before touching Swing components.
 */
public record SvServiceStateEvent(boolean available, String errorMessage) {}
