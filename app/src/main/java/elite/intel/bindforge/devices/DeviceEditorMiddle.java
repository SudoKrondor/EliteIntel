package elite.intel.bindforge.devices;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * The Device Editor's middle button - CLEAR, or RESET LABELS on a built-in - and whether it can be pressed
 * (alias-designer.md, <em>Actions</em>, Alan 2026-10-11, A10). Pure: the screen reads the edits, the working copy and
 * the binds files.
 * <p>
 * <strong>A device cleared in the edits, not yet applied, offers its undo</strong> - UNDO CLEAR, or UNDO RESET -
 * and its labels cannot be edited: the dialog says APPLY will remove it.
 * <p>
 * <strong>CLEAR is refused while the binds files name the device.</strong> An edit to an entry {@code .binds} depends
 * on must update {@code .binds} with it (Alan, 2026-10-11): here, the bindings would move to the device's hardware ID,
 * which is slice A19's. Without it the game would reject the whole binds file. RESET LABELS is never refused for it:
 * {@code .binds} names inputs by token, never by label.
 *
 * @param kind     what the button is
 * @param enabled  whether it can be pressed
 * @param why      why it cannot, or {@link Why#NONE}
 * @param bindings how many bindings name the device, for {@link Why#NAMED_IN_BINDS}
 */
public record DeviceEditorMiddle(Kind kind, boolean enabled, Why why, int bindings) {

    public enum Kind {
        CLEAR,
        RESET_LABELS,
        /** The device is cleared in the edits; pressing this puts it back as the working copy holds it. */
        UNDO_CLEAR,
        /** The built-in's labels are reset in the edits; pressing this puts them back. */
        UNDO_RESET
    }

    public enum Why {
        NONE,
        /** The device has no name yet, so there is nothing of it to clear. */
        NO_NAME,
        /** Neither the edits nor the working copy holds it. */
        NOT_ADDED,
        /** A built-in with no labels of the user's: there is nothing to reset. */
        NOTHING_TO_RESET,
        /** The binds files name it. */
        NAMED_IN_BINDS,
        /**
         * The binds files could not be read, so whether they name the device is unknown, and CLEAR is refused rather
         * than guessed (review, 2026-10-11).
         */
        BINDS_UNREAD
    }

    public DeviceEditorMiddle {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(why, "why");
    }

    /** Whether the device is cleared or reset in the edits, so its labels are not editable. */
    public boolean undoing() {
        return kind == Kind.UNDO_CLEAR || kind == Kind.UNDO_RESET;
    }

    /**
     * @param deviceName the name the device is under, or {@code null} for a device with no name yet
     * @param builtIn    whether that name is one of Frontier's - or, with no name, whether Frontier covers the hardware
     * @param applied    whether the working copy holds it - the user's element, or a built-in's labels of the user's
     * @param held       whether the edits hold it, the same way
     * @param bindings   how many bindings name the device
     */
    public static DeviceEditorMiddle of(String deviceName, boolean builtIn, boolean applied, boolean held,
                                        int bindings) {
        return of(deviceName, builtIn, applied, held, OptionalInt.of(bindings));
    }

    /** As above, with {@code bindings} empty when the binds files could not be read. */
    public static DeviceEditorMiddle of(String deviceName, boolean builtIn, boolean applied, boolean held,
                                        OptionalInt bindings) {
        Kind kind = builtIn ? Kind.RESET_LABELS : Kind.CLEAR;
        if (deviceName == null) return new DeviceEditorMiddle(kind, false, Why.NO_NAME, 0);
        if (applied && !held) {
            return new DeviceEditorMiddle(builtIn ? Kind.UNDO_RESET : Kind.UNDO_CLEAR, true, Why.NONE, 0);
        }
        if (!held) return new DeviceEditorMiddle(kind, false, builtIn ? Why.NOTHING_TO_RESET : Why.NOT_ADDED, 0);
        if (builtIn) return new DeviceEditorMiddle(kind, true, Why.NONE, 0);
        if (bindings.isEmpty()) return new DeviceEditorMiddle(kind, false, Why.BINDS_UNREAD, 0);
        if (bindings.getAsInt() > 0) {
            return new DeviceEditorMiddle(kind, false, Why.NAMED_IN_BINDS, bindings.getAsInt());
        }
        return new DeviceEditorMiddle(kind, true, Why.NONE, 0);
    }
}
