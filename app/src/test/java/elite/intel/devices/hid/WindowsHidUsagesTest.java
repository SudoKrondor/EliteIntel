package elite.intel.devices.hid;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reading {@code HIDP_VALUE_CAPS} as Windows lays them out: ranges expanded, aliases dropped, data-index order. */
class WindowsHidUsagesTest {

    @Test
    void valueCapsAreReadInDataIndexOrder() {
        ByteBuffer caps = ByteBuffer.allocate(3 * WindowsHidUsages.VALUE_CAPS_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        range(caps, 0, 0x30, 0x31, 5);          // X and Y, data indices 5 and 6
        single(caps, 1, 0x35, 4, false);         // Rz, data index 4
        single(caps, 2, 0x32, 4, true);          // an alias of index 4 - the same data again

        List<HidUsage> usages = WindowsHidUsages.usagesFromValueCaps(caps, 3);

        assertEquals(List.of(desktop(0x35), desktop(0x30), desktop(0x31)), usages);
    }

    /** SDL upper-cases the path, but Windows often gives it in lower case; either is a HID path. */
    @Test
    void aHidPathIsRecognisedInEitherCase() {
        assertTrue(WindowsHidUsages.isHidPath("\\\\?\\HID#VID_3344&PID_83F4#7&1A2B3C4D&0&0000#{4D1E55B2-F16F}"));
        assertTrue(WindowsHidUsages.isHidPath("\\\\?\\hid#vid_3344&pid_83f4#7&1a2b3c4d&0&0000#{4d1e55b2-f16f}"));
    }

    /** An XInput pad SDL names by slot, or no path at all, is not one hid.dll can open. */
    @Test
    void anythingElseIsNotAHidPath() {
        assertFalse(WindowsHidUsages.isHidPath("XInput#0"));
        assertFalse(WindowsHidUsages.isHidPath(""));
        assertFalse(WindowsHidUsages.isHidPath(null));
    }

    private static void range(ByteBuffer caps, int entry, int first, int last, int dataIndex) {
        int base = entry * WindowsHidUsages.VALUE_CAPS_SIZE;
        caps.putShort(base, (short) 0x01);
        caps.put(base + 12, (byte) 1);
        caps.putShort(base + 56, (short) first);
        caps.putShort(base + 58, (short) last);
        caps.putShort(base + 68, (short) dataIndex);
    }

    private static void single(ByteBuffer caps, int entry, int usage, int dataIndex, boolean alias) {
        int base = entry * WindowsHidUsages.VALUE_CAPS_SIZE;
        caps.putShort(base, (short) 0x01);
        caps.put(base + 3, (byte) (alias ? 1 : 0));
        caps.putShort(base + 56, (short) usage);
        caps.putShort(base + 68, (short) dataIndex);
    }

    private static HidUsage desktop(int id) {
        return new HidUsage(HidUsage.GENERIC_DESKTOP, id);
    }
}
