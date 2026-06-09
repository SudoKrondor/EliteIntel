package elite.intel.starvizion.event;

public record SvButtonStateEvent(int deviceId, int buttonIndex, boolean pressed) {}
