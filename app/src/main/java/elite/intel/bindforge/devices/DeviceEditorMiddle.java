package elite.intel.bindforge.devices;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * The Device Editor's middle button - CLEAR, or RESET LABELS on a built-in - and whether it can be pressed
 * (alias-designer.md, <em>Actions</em>, Alan 2026-10-11, A10). Pure: the screen reads the master, the draft and
 * {@code .binds}.
 * <p>
 * <strong>A device cleared in the draft, not yet applied, offers its undo</strong> - UNDO CLEAR, or UNDO RESET -
 * and its labels cannot be edited: the dialog says APPLY will remove it.
 * <p>
 * <strong>CLEAR is refused while {@code .binds} names the device.</strong> An edit to an entry {@code .binds}
 * depends on must update {@code .binds} with it (Alan, 2026-10-11): here, the bindings would move to the device's
 * hardware ID. That rewrite is not built, and without it the game would reject the whole preset. RESET LABELS is
 * never refused for it: {@code .binds} names inputs by token, never by label.
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
        /** The device is cleared in the draft; pressing this puts it back as the master holds it. */
        UNDO_CLEAR,
        /** The built-in's labels are reset in the draft; pressing this puts them back. */
        UNDO_RESET
    }

    public enum Why {
        NONE,
        /** The device has no name yet, so there is nothing of it to clear. */
        NO_NAME,
        /** Neither the master nor the draft holds it. */
        NOT_ADDED,
        /** A built-in with no saved labels: the file on disk is already the generated one, or Frontier's. */
        NOTHING_TO_RESET,
        /** {@code .binds} names it. */
        NAMED_IN_BINDS,
        /**
         * {@code .binds} could not be read - the folder is not there, or no preset in it parses - so whether it names
         * the device is unknown, and CLEAR is refused rather than guessed (review, 2026-10-11).
         */
        BINDS_UNREAD,
        /** Its last rename has not reached every installation and {@code .binds} yet (A11). */
        RENAME_PENDING
    }

    public DeviceEditorMiddle {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(why, "why");
    }

    /** Whether the device is cleared or reset in the draft, so its labels are not editable. */
    public boolean undoing() {
        return kind == Kind.UNDO_CLEAR || kind == Kind.UNDO_RESET;
    }

    /**
     * @param deviceName   the name the record is saved under, or {@code null} for a device with no name yet
     * @param builtIn      whether that name is one of Frontier's - or, with no name, whether Frontier covers the
     *                     hardware
     * @param inMaster     whether the master holds that name
     * @param draftStarted whether a draft is started
     * @param inDraft      whether the draft holds that name
     * @param bindings     how many bindings name the device
     */
    public static DeviceEditorMiddle of(String deviceName, boolean builtIn, boolean inMaster, boolean draftStarted,
                                        boolean inDraft, int bindings) {
        return of(deviceName, builtIn, inMaster, draftStarted, inDraft, OptionalInt.of(bindings));
    }

    /** As above, with {@code bindings} empty when {@code .binds} could not be read. */
    public static DeviceEditorMiddle of(String deviceName, boolean builtIn, boolean inMaster, boolean draftStarted,
                                        boolean inDraft, OptionalInt bindings) {
        Kind kind = builtIn ? Kind.RESET_LABELS : Kind.CLEAR;
        if (deviceName == null) return new DeviceEditorMiddle(kind, false, Why.NO_NAME, 0);
        if (draftStarted && inMaster && !inDraft) {
            return new DeviceEditorMiddle(builtIn ? Kind.UNDO_RESET : Kind.UNDO_CLEAR, true, Why.NONE, 0);
        }
        // WHY: the draft when there is one, since it is what SAVE and Apply act on; the master otherwise.
        boolean held = draftStarted ? inDraft : inMaster;
        if (!held) return new DeviceEditorMiddle(kind, false, builtIn ? Why.NOTHING_TO_RESET : Why.NOT_ADDED, 0);
        if (builtIn) return new DeviceEditorMiddle(kind, true, Why.NONE, 0);
        if (bindings.isEmpty()) return new DeviceEditorMiddle(kind, false, Why.BINDS_UNREAD, 0);
        if (bindings.getAsInt() > 0) {
            return new DeviceEditorMiddle(kind, false, Why.NAMED_IN_BINDS, bindings.getAsInt());
        }
        return new DeviceEditorMiddle(kind, true, Why.NONE, 0);
    }

    /**
     * CLEAR refused while the device's rename is still under way (review, 2026-10-15, A11): its old name may still be
     * in {@code .binds} or in an installation that missed the rename, and clearing would drop the old name the next
     * Apply needs to finish it - leaving bindings on a device with no entry, or an element nothing cleans up. An undo,
     * and RESET LABELS, are left as they are.
     */
    public DeviceEditorMiddle whileRenamePending() {
        return kind == Kind.CLEAR ? new DeviceEditorMiddle(kind, false, Why.RENAME_PENDING, 0) : this;
    }
}
