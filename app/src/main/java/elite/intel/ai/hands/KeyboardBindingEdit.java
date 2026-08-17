package elite.intel.ai.hands;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * Immutable request to assign one known keyboard key to one binding slot, or
 * clear that slot when {@link #key()} is {@code null}.
 * <p>
 * The expected timestamp and file size are part of the request so the writer can
 * refuse to overwrite changes made by Elite Dangerous after the UI loaded the
 * file. Size is checked with timestamp because some filesystems have coarse
 * timestamp precision.
 */
public record KeyboardBindingEdit(
        Path file,
        String bindingId,
        BindingSlotType slotType,
        String key,
        FileTime expectedLastModified,
        long expectedFileSize,
        boolean replacesControllerSlot
) {
    /**
     * An edit that only fills an empty slot or rewrites an existing keyboard one -
     * the default, and the only kind the manual editor ever makes.
     */
    public KeyboardBindingEdit(
            Path file,
            String bindingId,
            BindingSlotType slotType,
            String key,
            FileTime expectedLastModified,
            long expectedFileSize
    ) {
        this(file, bindingId, slotType, key, expectedLastModified, expectedFileSize, false);
    }

    /**
     * Clearing uses Elite Dangerous' built-in empty-slot representation:
     * {@code Device="{NoDevice}" Key=""}.
     */
    public boolean clearsSlot() {
        return key == null;
    }
}
