package elite.intel.devices.model;

import java.util.List;
import java.util.Optional;

/**
 * Which axes and hats a connected controller has - what SDL's counts alone cannot say.
 * <p>
 * SDL numbers a controller's axes without gaps, so a stick reporting X, Y and RZ is axes 0, 1 and 2 to it. The
 * axes here are read from the controller's HID description and named by the game slot each one takes.
 *
 * @param axesKnown       whether the HID description could be read; when it could not, nothing is said about the
 *                        axes and a consumer falls back to what it did before it knew
 * @param axes            the game axes the controller has, each once; kept in the game's order
 * @param axesWithoutSlot how many of the controller's axes the game cannot see: past the eighth, or a second axis
 *                        of a kind whose slot another already took
 * @param sdlAxes         the game axis behind each SDL axis index, or empty when SDL's numbering could not be
 *                        matched to the description with certainty
 * @param hatCount        how many hats SDL reports; a hat the firmware reports as buttons is buttons
 */
public record DeviceDescription(boolean axesKnown, List<GameAxis> axes, int axesWithoutSlot,
                                List<GameAxis> sdlAxes, int hatCount) {

    public DeviceDescription {
        axes = axes.stream().sorted().toList();
        sdlAxes = List.copyOf(sdlAxes);
        if (axes.stream().distinct().count() != axes.size()) {
            throw new IllegalArgumentException("A controller has each game axis at most once: " + axes);
        }
        if (hatCount < 0 || axesWithoutSlot < 0) {
            throw new IllegalArgumentException("Counts cannot be negative: hats " + hatCount
                    + ", axes without a slot " + axesWithoutSlot);
        }
        if (!axesKnown && (!axes.isEmpty() || axesWithoutSlot > 0 || !sdlAxes.isEmpty())) {
            throw new IllegalArgumentException("A description whose axes are unknown cannot name any");
        }
        if (!sdlAxes.isEmpty() && !sdlAxes.stream().sorted().toList().equals(axes)) {
            throw new IllegalArgumentException("SDL's axes " + sdlAxes + " are not the controller's axes " + axes);
        }
    }

    /** A controller whose HID description could not be read. */
    public static DeviceDescription axesUnknown(int hatCount) {
        return new DeviceDescription(false, List.of(), 0, List.of(), hatCount);
    }

    /** SDL's axis order for a mapped gamepad: sticks first, then the two triggers. */
    private static final List<GameAxis> GAMEPAD_AXES =
            List.of(GameAxis.X, GameAxis.Y, GameAxis.Z, GameAxis.RX, GameAxis.RY, GameAxis.RZ);

    public static DeviceDescription gamepad(int axisCount, int hatCount) {
        List<GameAxis> order = GAMEPAD_AXES.subList(0, Math.min(axisCount, GAMEPAD_AXES.size()));
        return new DeviceDescription(true, order, Math.max(0, axisCount - order.size()), order, hatCount);
    }

    /**
     * Whether the description's axes are trusted: it was read, and it counts as many axes as SDL does. A description
     * that finds no axis where SDL counts three has missed something - a vendor-defined usage, say - and a consumer
     * should treat it as unknown rather than leave those axes unlabelled.
     */
    public boolean agreesWith(int sdlAxisCount) {
        return axesKnown && axes.size() + axesWithoutSlot == sdlAxisCount;
    }

    /** The game axis SDL's axis {@code sdlIndex} is, when SDL's numbering is matched. */
    public Optional<GameAxis> axisAt(int sdlIndex) {
        if (sdlIndex < 0 || sdlIndex >= sdlAxes.size()) return Optional.empty();
        return Optional.of(sdlAxes.get(sdlIndex));
    }
}
