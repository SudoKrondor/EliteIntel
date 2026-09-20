package elite.intel.ui.support;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import elite.intel.util.OsDetector;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The keyboard and the mouse, for the support bundle manifest.
 * <p>
 * WHY: a binding that "does nothing", a key the game "does not see", a push-to-talk mouse button that never
 * fires - these reports are about a particular keyboard or mouse, and the commander rarely names it. Gaming
 * keyboards run vendor software that remaps keys before the OS sees them, some mice present a keyboard
 * interface of their own, and a laptop keyboard and a PS/2 board answer to different key names than a USB
 * one. Knowing the exact model turns a guess into a lookup.
 * <p>
 * Each device is named as the OS names it and pinned by its USB vendor:product pair, which identifies the
 * model even where the name is generic. Linux is read from {@code /proc/bus/input/devices}, which every
 * user can read and which lists every input device the kernel has, whatever the desktop - SDL cannot serve
 * here, because on Wayland the app runs it on XWayland, which hides the real hardware behind a virtual
 * pointer and keyboard. Windows is asked through WMI, the same road the graphics card line travels. WMI
 * names a plain HID device after its class driver ("HID Keyboard Device"), so on Windows the vendor:product
 * pair is the identification and the name is a courtesy; the keyboard's layout id rides along because a
 * key-name report is often a layout report in disguise.
 * <p>
 * Every line is optional, like the rest of the hardware report: the caller runs this behind a guard and a
 * failure costs these lines and nothing else.
 */
final class InputDeviceReport {

    private static final Path LINUX_INPUT_DEVICES = Path.of("/proc/bus/input/devices");

    /**
     * Linux {@code EV=} bits: a device that can move a pointer, and one with a Caps Lock light.
     */
    private static final int EV_REL = 1 << 2;
    private static final int EV_LED = 1 << 17;

    private static final Pattern WINDOWS_VID_PID =
            Pattern.compile("VID[_&]([0-9A-Fa-f]{4,8}).*?PID[_&]([0-9A-Fa-f]{4})");

    private InputDeviceReport() {
    }

    /**
     * The keyboard and mouse lines for this machine, without a trailing newline; empty on a platform that
     * has no probe.
     */
    static String describe() {
        return switch (OsDetector.getOs()) {
            case LINUX -> linux(LINUX_INPUT_DEVICES);
            case WINDOWS -> windows();
            case MAC -> "";
        };
    }

    // ---------------------------------------------------------------- Linux

    /**
     * Reads the kernel's input device list. One physical device appears there once per HID interface it
     * exposes - a gaming keyboard brings a pointer interface, a gaming mouse a keyboard one - so the
     * interfaces are folded back together by vendor:product and the roles are judged on capability bits
     * rather than on the handler alone: a keyboard is an interface with a Caps Lock light, since every
     * consumer-control and macro interface registers as {@code kbd} too, and a mouse is one that reports
     * relative motion. A device that is a keyboard by that test is not also listed as a mouse, because that
     * "mouse" is its media wheel.
     */
    static String linux(Path inputDevices) {
        String listing;
        try {
            listing = Files.readString(inputDevices, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "Input devices: could not be read (" + e + ")";
        }
        Map<String, LinuxDevice> byIdentity = new LinkedHashMap<>();
        for (String block : listing.split("\\R\\s*\\R")) {
            LinuxDevice parsed = LinuxDevice.parse(block);
            if (parsed == null) continue;
            byIdentity.merge(parsed.identity(), parsed, LinuxDevice::merge);
        }
        List<String> keyboards = new ArrayList<>();
        List<String> mice = new ArrayList<>();
        for (LinuxDevice device : byIdentity.values()) {
            if (device.keyboard) keyboards.add(device.describe());
            else if (device.mouse) mice.add(device.describe());
        }
        return lines("Keyboard", keyboards) + "\n" + lines("Mouse", mice);
    }

    /**
     * One {@code /proc/bus/input/devices} block, or several merged - the name kept is the shortest, since a
     * sub-interface's name is the product string with a suffix ("... Consumer Control").
     */
    private record LinuxDevice(String name, int bus, int vendor, int product, boolean keyboard, boolean mouse) {

        private static final Pattern IDENTITY = Pattern.compile("^I: Bus=([0-9a-f]+) Vendor=([0-9a-f]+) Product=([0-9a-f]+)", Pattern.MULTILINE);
        private static final Pattern NAME = Pattern.compile("^N: Name=\"(.*)\"", Pattern.MULTILINE);
        private static final Pattern HANDLERS = Pattern.compile("^H: Handlers=(.*)$", Pattern.MULTILINE);
        private static final Pattern EVENT_BITS = Pattern.compile("^B: EV=([0-9a-f]+)", Pattern.MULTILINE);

        static LinuxDevice parse(String block) {
            Matcher identity = IDENTITY.matcher(block);
            Matcher name = NAME.matcher(block);
            Matcher handlers = HANDLERS.matcher(block);
            Matcher eventBits = EVENT_BITS.matcher(block);
            if (!identity.find() || !name.find() || !handlers.find() || !eventBits.find()) return null;
            long ev = Long.parseLong(eventBits.group(1), 16);
            String handled = " " + handlers.group(1).trim() + " ";
            boolean keyboard = handled.contains(" kbd ") && (ev & EV_LED) != 0;
            boolean mouse = handled.matches(".* mouse\\d+ .*") && (ev & EV_REL) != 0;
            if (!keyboard && !mouse) return null;
            return new LinuxDevice(name.group(1).trim(),
                    Integer.parseInt(identity.group(1), 16),
                    Integer.parseInt(identity.group(2), 16),
                    Integer.parseInt(identity.group(3), 16),
                    keyboard, mouse);
        }

        /**
         * Vendor:product when the bus assigns them, otherwise the name - a virtual device reports zeros.
         */
        String identity() {
            return vendor == 0 && product == 0 ? name : bus + ":" + vendor + ":" + product;
        }

        LinuxDevice merge(LinuxDevice other) {
            String shorter = other.name.length() < name.length() ? other.name : name;
            return new LinuxDevice(shorter, bus, vendor, product, keyboard || other.keyboard, mouse || other.mouse);
        }

        String describe() {
            return name + " [" + busName(bus) + " " + String.format("%04x:%04x", vendor, product) + "]";
        }

        /**
         * The kernel's {@code BUS_*} constants, named for the ones a desktop has.
         */
        private static String busName(int bus) {
            return switch (bus) {
                case 0x03 -> "usb";
                case 0x05 -> "bluetooth";
                case 0x06 -> "virtual";
                case 0x11 -> "ps/2";
                case 0x18 -> "i2c";
                case 0x19 -> "host";
                default -> "bus 0x" + Integer.toHexString(bus);
            };
        }
    }

    // -------------------------------------------------------------- Windows

    private enum KeyboardProperty {DESCRIPTION, DEVICEID, LAYOUT}

    private enum PointingDeviceProperty {DESCRIPTION, DEVICEID, NUMBEROFBUTTONS}

    /**
     * {@code Win32_Keyboard} and {@code Win32_PointingDevice}, one line each per device. WHY no folding as
     * on Linux: WMI exposes no capability bits, so a gaming mouse's keyboard interface is a keyboard here
     * and the reader ties the two together by the vendor:product pair on each line.
     */
    private static String windows() {
        WmiQueryHandler wmi = WmiQueryHandler.createInstance();
        List<String> keyboards = new ArrayList<>();
        WmiResult<KeyboardProperty> keyboardRows = wmi.queryWMI(new WmiQuery<>("Win32_Keyboard", KeyboardProperty.class));
        for (int i = 0; i < keyboardRows.getResultCount(); i++) {
            String layout = WmiUtil.getString(keyboardRows, KeyboardProperty.LAYOUT, i);
            keyboards.add(windowsDevice(WmiUtil.getString(keyboardRows, KeyboardProperty.DESCRIPTION, i),
                    WmiUtil.getString(keyboardRows, KeyboardProperty.DEVICEID, i))
                    + (layout.isEmpty() ? "" : " layout " + layout));
        }
        List<String> mice = new ArrayList<>();
        WmiResult<PointingDeviceProperty> mouseRows = wmi.queryWMI(new WmiQuery<>("Win32_PointingDevice", PointingDeviceProperty.class));
        for (int i = 0; i < mouseRows.getResultCount(); i++) {
            long buttons = safeUint(mouseRows.getValue(PointingDeviceProperty.NUMBEROFBUTTONS, i));
            mice.add(windowsDevice(WmiUtil.getString(mouseRows, PointingDeviceProperty.DESCRIPTION, i),
                    WmiUtil.getString(mouseRows, PointingDeviceProperty.DEVICEID, i))
                    + (buttons > 0 ? " " + buttons + " buttons" : ""));
        }
        return lines("Keyboard", keyboards) + "\n" + lines("Mouse", mice);
    }

    /**
     * The description with the bus and the vendor:product pair pulled out of the PnP instance id, which
     * reads {@code HID\VID_1B1C&PID_1B5C&MI_00\...} for USB and carries the pair in a longer form for
     * Bluetooth ({@code VID&0002046D}), where the model is the last four digits.
     */
    private static String windowsDevice(String description, String deviceId) {
        String bus = deviceId.contains("\\") ? deviceId.substring(0, deviceId.indexOf('\\')).toLowerCase(Locale.ROOT) : "";
        Matcher ids = WINDOWS_VID_PID.matcher(deviceId);
        if (!ids.find()) return description + (bus.isEmpty() ? "" : " [" + bus + "]");
        String vid = ids.group(1);
        return description + " [" + bus + " " + vid.substring(vid.length() - 4).toLowerCase(Locale.ROOT)
                + ":" + ids.group(2).toLowerCase(Locale.ROOT) + "]";
    }

    // --------------------------------------------------------------- shared

    private static String lines(String label, List<String> devices) {
        if (devices.isEmpty()) return label + ": none recognised";
        return String.join("\n", devices.stream().map(device -> label + ": " + device).toList());
    }

    private static long safeUint(Object value) {
    if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString().trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
