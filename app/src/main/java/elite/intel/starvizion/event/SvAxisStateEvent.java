package elite.intel.starvizion.event;

/** Axis value normalized to [-1.0, 1.0]. */
public record SvAxisStateEvent(int deviceId, int axisIndex, float value) {}
