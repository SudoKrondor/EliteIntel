package elite.intel.starvizion.event;

/**
 * Published by SdlInputService when SDL3 detects a keyboard scancode transitioning from
 * released to pressed (rising edge only). {@code keyName} is a display-ready name including
 * any held modifiers, e.g. "Ctrl+F", "Shift+A", "Alt+Tab".
 */
public record SvKeyPressedEvent(int scancode, String keyName) {}
