package elite.intel.devices.events;

/**
 * Published when a hat's position changes - pushed, moved, released - never while it is held.
 *
 * @param hatIndex   SDL's hat index, 0-based; the game's {@code POV1} is hat 0
 * @param directions the directions pushed now, as SDL's bits: {@link #UP}, {@link #RIGHT}, {@link #DOWN},
 *                   {@link #LEFT}. Zero when centred; two bits at once on an 8-way hat's diagonal
 */
public record DeviceHatEvent(int deviceId, int hatIndex, int directions) {

    public static final int UP = 0x01;
    public static final int RIGHT = 0x02;
    public static final int DOWN = 0x04;
    public static final int LEFT = 0x08;
}
