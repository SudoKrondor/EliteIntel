package elite.intel.bindforge.io;

import elite.intel.bindforge.model.BindingInput;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * One change of device name across {@code .binds} files: every {@code Device="..."} whose value is one of
 * {@link #from()} becomes {@link #to()}, on {@code <Primary>}, {@code <Secondary>}, {@code <Binding>} and
 * {@code <Modifier>} alike.
 * <p>
 * {@code .binds} names a device by its {@code DeviceMappings.xml} element when one matches, and by its VID and PID
 * in hex when none does, so naming, renaming and clearing a device each move its references:
 * {@link #hardwareIdToName}, {@link #rename} and {@link #nameToHardwareId}. Matching ignores case - the comparison
 * that decides whether a device is referenced at all - except that a reference already spelled exactly as the new
 * name is left alone, so a case-only change ({@code lvwap} to {@code LVWAP}) rewrites the other spellings only. {@code Keyboard}, {@code Mouse} and {@code {NoDevice}} are
 * the game's own literals and are never rewritten, from or to.
 */
public record DeviceReferenceRewrite(Set<String> from, String to) {

    private static final Pattern HEX_HALF = Pattern.compile("[0-9A-Fa-f]{4}");

    public DeviceReferenceRewrite {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.isEmpty()) {
            throw new IllegalArgumentException("A device rewrite needs at least one name to replace");
        }
        for (String name : from) {
            requireDeviceName(name, "from");
        }
        requireDeviceName(to, "to");
        // WHY: exact, not ignoring case - a case-only change is a real rewrite. In .binds LVWAP and lvwap are not the
        // same name, and a file spelling one device two ways is rejected, so every spelling becomes the one chosen
        // (format section 4.3; Storage S2b, Alan, 2026-10-25).
        if (from.contains(to)) {
            throw new IllegalArgumentException("A device rewrite cannot rewrite " + to + " to itself");
        }
        from = Set.copyOf(from);
    }

    /** A rename: every reference to {@code oldName} moves to {@code newName}. */
    public static DeviceReferenceRewrite rename(String oldName, String newName) {
        return new DeviceReferenceRewrite(Set.of(Objects.requireNonNull(oldName, "oldName")), newName);
    }

    /**
     * Naming a device the bindings reference by hardware id: {@code vid}+{@code pid}, the order the game writes,
     * becomes {@code name}.
     * <p>
     * The reverse order is not matched: a rewrite writes, and a second device whose VID and PID are exactly this
     * one's PID and VID would be renamed with it. A caller that found the id in the other order builds the rewrite
     * from the value it found, with the constructor.
     */
    public static DeviceReferenceRewrite hardwareIdToName(String vid, String pid, String name) {
        return new DeviceReferenceRewrite(Set.of(hexHalf(vid, "vid") + hexHalf(pid, "pid")), name);
    }

    /**
     * Clearing a device the bindings reference by name: {@code name} becomes the hardware id the game writes for a
     * device with no entry, VID then PID, upper case.
     */
    public static DeviceReferenceRewrite nameToHardwareId(String name, String vid, String pid) {
        return new DeviceReferenceRewrite(Set.of(Objects.requireNonNull(name, "name")),
                hexHalf(vid, "vid") + hexHalf(pid, "pid"));
    }

    /**
     * True when {@code device} is one of the names this rewrite replaces, ignoring case - and is not already spelled
     * exactly as {@link #to()}, which a case-only change leaves as it is and does not count as moved.
     */
    public boolean matches(String device) {
        return device != null && !device.equals(to) && from.stream().anyMatch(device::equalsIgnoreCase);
    }

    private static void requireDeviceName(String name, String role) {
        Objects.requireNonNull(name, role);
        if (!BindingInput.isDeviceName(name)) {
            throw new IllegalArgumentException("Not a device name a rewrite may use as " + role + ": '" + name + "'");
        }
    }

    private static String hexHalf(String value, String role) {
        Objects.requireNonNull(value, role);
        if (!HEX_HALF.matcher(value).matches()) {
            throw new IllegalArgumentException(role + " must be four hex digits, not '" + value + "'");
        }
        return value.toUpperCase(Locale.ROOT);
    }
}
