package elite.intel.devices.hid;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A Windows controller's HID value inputs, read through {@code hid.dll} from the device path SDL's DirectInput
 * backend gives ({@code \\?\HID#VID_3344&PID_83F4#...}).
 * <p>
 * Plain C calls only - open the device with no access rights, ask for its preparsed data, read its value caps -
 * so a failure is a status code, never a crash. The device is opened for neither reading nor writing, which
 * Windows allows on a controller another program holds.
 */
final class WindowsHidUsages {

    private static final Logger log = LoggerFactory.getLogger(WindowsHidUsages.class);

    private static final String HID_PATH_PREFIX = "\\\\?\\HID#";
    private static final int HIDP_STATUS_SUCCESS = 0x00110000;
    private static final int HIDP_INPUT = 0;
    private static final int HIDP_CAPS_SIZE = 64;
    private static final int CAPS_NUMBER_INPUT_VALUE_CAPS = 48;

    /** {@code HIDP_VALUE_CAPS}: 72 bytes, with the usage fields at these offsets. */
    static final int VALUE_CAPS_SIZE = 72;
    private static final int VALUE_USAGE_PAGE = 0, VALUE_IS_ALIAS = 3, VALUE_IS_RANGE = 12;
    private static final int VALUE_USAGE_MIN = 56, VALUE_USAGE_MAX = 58, VALUE_DATA_INDEX_MIN = 68;

    /** {@code hid.dll}. Its {@code BOOLEAN} is one byte, so those returns are read as {@code byte}. */
    private interface Hid extends Library {
        Hid INSTANCE = Native.load("hid", Hid.class);

        byte HidD_GetPreparsedData(WinNT.HANDLE device, PointerByReference preparsedData);

        byte HidD_FreePreparsedData(Pointer preparsedData);

        int HidP_GetCaps(Pointer preparsedData, Pointer caps);

        int HidP_GetValueCaps(int reportType, Pointer valueCaps, ShortByReference valueCapsLength,
                              Pointer preparsedData);
    }

    private WindowsHidUsages() {
    }

    /**
     * WHY: empty rather than an error for every expected miss - an XInput pad SDL names by slot, not by HID
     * path, or a device that refuses to be opened. The caller then knows nothing about the axes, which is what
     * it knew before this existed.
     */
    static Optional<List<HidUsage>> read(String devicePath) {
        if (!isHidPath(devicePath)) {
            log.debug("Not a HID device path: '{}'", devicePath);
            return Optional.empty();
        }
        WinNT.HANDLE device = Kernel32.INSTANCE.CreateFile(devicePath, 0,
                WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE, null, WinNT.OPEN_EXISTING, 0, null);
        if (WinBase.INVALID_HANDLE_VALUE.equals(device)) {
            log.debug("Could not open {}: error {}", devicePath, Kernel32.INSTANCE.GetLastError());
            return Optional.empty();
        }
        try {
            return readPreparsed(device, devicePath);
        } finally {
            Kernel32.INSTANCE.CloseHandle(device);
        }
    }

    /**
     * SDL upper-cases the path it takes from {@code DIPROP_GUIDANDPATH} ({@code SDL_dinputjoystick.c},
     * {@code QueryDevicePath}), but Windows itself often gives them in lower case, so case is not relied on.
     */
    static boolean isHidPath(String devicePath) {
        return devicePath != null && devicePath.regionMatches(true, 0, HID_PATH_PREFIX, 0, HID_PATH_PREFIX.length());
    }

    private static Optional<List<HidUsage>> readPreparsed(WinNT.HANDLE device, String devicePath) {
        PointerByReference preparsedRef = new PointerByReference();
        if (Hid.INSTANCE.HidD_GetPreparsedData(device, preparsedRef) == 0) {
            log.debug("No HID preparsed data for {}", devicePath);
            return Optional.empty();
        }
        Pointer preparsed = preparsedRef.getValue();
        try {
            Memory caps = new Memory(HIDP_CAPS_SIZE);
            caps.clear();
            if (Hid.INSTANCE.HidP_GetCaps(preparsed, caps) != HIDP_STATUS_SUCCESS) return Optional.empty();
            short count = caps.getShort(CAPS_NUMBER_INPUT_VALUE_CAPS);
            if (count <= 0) return Optional.of(List.of());

            Memory valueCaps = new Memory((long) count * VALUE_CAPS_SIZE);
            valueCaps.clear();
            ShortByReference length = new ShortByReference(count);
            if (Hid.INSTANCE.HidP_GetValueCaps(HIDP_INPUT, valueCaps, length, preparsed) != HIDP_STATUS_SUCCESS) {
                return Optional.empty();
            }
            return Optional.of(usagesFromValueCaps(valueCaps.getByteBuffer(0, valueCaps.size()), length.getValue()));
        } finally {
            Hid.INSTANCE.HidD_FreePreparsedData(preparsed);
        }
    }

    /**
     * The usages in an array of {@code HIDP_VALUE_CAPS}, in descriptor order - the order of their data indices -
     * a range expanded to one usage per id. An alias describes the same data again under another usage, so it is
     * left out.
     */
    static List<HidUsage> usagesFromValueCaps(ByteBuffer caps, int count) {
        ByteBuffer buffer = caps.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        record Indexed(int dataIndex, HidUsage usage) {
        }
        List<Indexed> indexed = new ArrayList<>();
        for (int entry = 0; entry < count; entry++) {
            int base = entry * VALUE_CAPS_SIZE;
            if (buffer.get(base + VALUE_IS_ALIAS) != 0) continue;
            int page = Short.toUnsignedInt(buffer.getShort(base + VALUE_USAGE_PAGE));
            int first = Short.toUnsignedInt(buffer.getShort(base + VALUE_USAGE_MIN));
            int last = buffer.get(base + VALUE_IS_RANGE) != 0
                    ? Short.toUnsignedInt(buffer.getShort(base + VALUE_USAGE_MAX))
                    : first;
            int dataIndex = Short.toUnsignedInt(buffer.getShort(base + VALUE_DATA_INDEX_MIN));
            for (int id = first; id <= last; id++) indexed.add(new Indexed(dataIndex + id - first, new HidUsage(page, id)));
        }
        return indexed.stream().sorted(Comparator.comparingInt(Indexed::dataIndex)).map(Indexed::usage).toList();
    }
}
