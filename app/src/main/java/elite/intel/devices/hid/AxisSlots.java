package elite.intel.devices.hid;

import elite.intel.devices.model.DeviceDescription;
import elite.intel.devices.model.GameAxis;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Which game axis each of a controller's HID axes is, and which of them SDL's axis {@code k} is (spec,
 * alias-designer.md, "Which axes and hats a controller has").
 * <p>
 * <strong>The game slot is DirectInput's.</strong> The game's eight axis tokens are DirectInput's eight slots, and
 * DirectInput fills them by kind: each of X, Y, Z, Rx, Ry and Rz takes the first axis of its kind, and the two
 * sliders (the game's U and V) the first two slider-kind axes, in the order the HID description lists them. An axis
 * whose slot is taken, or past the second slider, is one the game cannot see. The kinds are Wine's
 * ({@code dlls/dinput/joystick_hid.c}), which is what the game runs on under Proton and which models Windows: Slider,
 * Dial and Simulation Throttle are sliders, Wheel is Z, Steering X, Accelerator Y, Rudder and Brake Rz. Read from
 * source 2026-10-10, not measured on the game.
 * <p>
 * <strong>SDL's numbering is matched only when nothing is in doubt.</strong> SDL numbers axes without gaps:
 * through DirectInput on Windows in slot order, and through evdev on Linux in the order of the kernel's axis codes.
 * Every axis must have a slot and SDL must count exactly those axes; otherwise SDL's numbering is left unmatched
 * rather than guessed, and a consumer lights no axis by its identity.
 */
public final class AxisSlots {

    /** How SDL numbers a controller's axes on this platform. */
    public enum SdlAxisOrder {
        /** Windows: SDL's DirectInput backend sorts axes by their DirectInput slot. */
        DIRECT_INPUT,
        /** Linux: SDL's evdev backend lists axes by the kernel's {@code ABS_} code. */
        EVDEV
    }

    /** DirectInput's axis kinds, as Wine assigns them from HID usages. */
    private enum AxisKind {X, Y, Z, RX, RY, RZ, SLIDER}

    private AxisSlots() {
    }

    /**
     * @param inputs       every input the HID description declares, in its order; inputs that are not axes are
     *                     ignored
     * @param sdlAxisCount how many axes SDL reports
     * @param hatCount     how many hats SDL reports
     */
    public static DeviceDescription describe(List<HidUsage> inputs, SdlAxisOrder order, int sdlAxisCount,
                                             int hatCount) {
        List<HidUsage> axes = inputs.stream().filter(usage -> kindOf(usage).isPresent()).toList();
        GameAxis[] slots = assignSlots(axes);
        List<GameAxis> gameAxes = Arrays.stream(slots).filter(Objects::nonNull).sorted().toList();
        int withoutSlot = axes.size() - gameAxes.size();
        List<GameAxis> sdlAxes = withoutSlot == 0 && axes.size() == sdlAxisCount
                ? sdlNumbering(axes, slots, order)
                : List.of();
        return new DeviceDescription(true, gameAxes, withoutSlot, sdlAxes, hatCount);
    }

    /** The slot each axis takes, or {@code null} for one the game cannot see. */
    private static GameAxis[] assignSlots(List<HidUsage> axes) {
        GameAxis[] slots = new GameAxis[axes.size()];
        for (GameAxis slot : GameAxis.values()) {
            AxisKind kind = kindTaking(slot);
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] == null && kindOf(axes.get(i)).orElseThrow() == kind) {
                    slots[i] = slot;
                    break;
                }
            }
        }
        return slots;
    }

    /** The slot behind each SDL axis index, or empty when two axes would share SDL's position. */
    private static List<GameAxis> sdlNumbering(List<HidUsage> axes, GameAxis[] slots, SdlAxisOrder order) {
        int[] keys = IntStream.range(0, slots.length)
                .map(i -> order == SdlAxisOrder.DIRECT_INPUT ? slots[i].ordinal() : evdevCode(axes.get(i)))
                .toArray();
        if (Arrays.stream(keys).distinct().count() != keys.length) return List.of();
        return IntStream.range(0, slots.length).boxed()
                .sorted(Comparator.comparingInt(i -> keys[i]))
                .map(i -> slots[i])
                .toList();
    }

    private static AxisKind kindTaking(GameAxis slot) {
        return switch (slot) {
            case X -> AxisKind.X;
            case Y -> AxisKind.Y;
            case Z -> AxisKind.Z;
            case RX -> AxisKind.RX;
            case RY -> AxisKind.RY;
            case RZ -> AxisKind.RZ;
            case U, V -> AxisKind.SLIDER;
        };
    }

    private static Optional<AxisKind> kindOf(HidUsage usage) {
        AxisKind kind = switch (usage.page()) {
            case HidUsage.GENERIC_DESKTOP -> switch (usage.id()) {
                case 0x30 -> AxisKind.X;
                case 0x31 -> AxisKind.Y;
                case 0x32, 0x38 -> AxisKind.Z;         // Z, Wheel
                case 0x33 -> AxisKind.RX;
                case 0x34 -> AxisKind.RY;
                case 0x35 -> AxisKind.RZ;
                case 0x36, 0x37 -> AxisKind.SLIDER;    // Slider, Dial
                default -> null;
            };
            case HidUsage.SIMULATION -> switch (usage.id()) {
                case 0xC8 -> AxisKind.X;               // Steering
                case 0xC4 -> AxisKind.Y;               // Accelerator
                case 0xBA, 0xC5 -> AxisKind.RZ;        // Rudder, Brake
                case 0xBB -> AxisKind.SLIDER;          // Throttle
                default -> null;
            };
            default -> null;
        };
        return Optional.ofNullable(kind);
    }

    /**
     * The kernel's {@code ABS_} code for an axis ({@code drivers/hid/hid-input.c}): Generic Desktop X through
     * Wheel are codes 0-8 in usage order, so Slider is {@code ABS_THROTTLE} and Dial {@code ABS_RUDDER}.
     */
    private static int evdevCode(HidUsage axis) {
        if (axis.page() == HidUsage.GENERIC_DESKTOP) return axis.id() - 0x30;
        return switch (axis.id()) {
            case 0xBB -> 6;     // ABS_THROTTLE
            case 0xBA -> 7;     // ABS_RUDDER
            case 0xC8 -> 8;     // ABS_WHEEL
            case 0xC4 -> 9;     // ABS_GAS
            case 0xC5 -> 10;    // ABS_BRAKE
            default -> throw new IllegalArgumentException("Not an axis: " + axis);
        };
    }
}
