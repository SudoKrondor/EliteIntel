package elite.intel.ai.brain.inference.lmstudio.install;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import oshi.util.platform.windows.PerfCounterWildcardQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery.PdhCounterWildcardProperty;
import oshi.util.tuples.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.regex.Pattern;

/**
 * The AMD cards in this PC and their memory, read from what the driver itself publishes. AMD ships no
 * {@code nvidia-smi}: {@code rocm-smi} comes only with ROCm, which a gaming PC seldom has.
 * <ul>
 *   <li><b>Linux:</b> the {@code amdgpu} driver's own files under {@code /sys/class/drm/cardN/device}:
 *       {@code mem_info_vram_total} and {@code mem_info_vram_used}, in bytes.</li>
 *   <li><b>Windows:</b> the card's memory from the display adapter's registry key
 *       ({@code HardwareInformation.qwMemorySize}, which the driver writes; the WMI figure is 32-bit and stops at
 *       4 GB), and the memory in use from the {@code GPU Adapter Memory} performance counter.</li>
 * </ul>
 * Beta (V1.2): written without an AMD card to test on. See {@code docs/LM_STUDIO_INSTALL_ASSISTANCE.md},
 * "AMD cards (beta)".
 */
final class AmdCards {

    private static final Logger log = LogManager.getLogger(AmdCards.class);

    /**
     * AMD's PCI vendor id.
     */
    static final String VENDOR_ID = "0x1002";

    private static final long BYTES_PER_MIB = 1024L * 1024;

    private static final Path LINUX_DRM = Path.of("/sys/class/drm");

    /**
     * {@code card0}, {@code card1}; not the connectors beside them ({@code card1-DP-1}) or {@code renderD128}.
     */
    private static final Pattern LINUX_CARD = Pattern.compile("card\\d+");

    /**
     * The display adapters' device class.
     */
    private static final String WINDOWS_DISPLAY_CLASS =
            "SYSTEM\\CurrentControlSet\\Control\\Class\\{4d36e968-e325-11ce-bfc1-08002be10318}";

    /**
     * One adapter's key under the class: {@code 0000}, {@code 0001}. Not {@code Properties}, which only the system
     * may open.
     */
    private static final Pattern WINDOWS_ADAPTER_KEY = Pattern.compile("\\d{4}");

    private AmdCards() {
    }

    /**
     * One AMD card.
     *
     * @param name       what the driver calls it; on Linux, which names no product, its PCI device id
     * @param memoryMib  its own memory, or empty when the driver does not say
     */
    record Card(String name, OptionalLong memoryMib) {
    }

    /**
     * Every AMD card on this PC; empty on any other OS, with no AMD card, or when none can be read.
     */
    static List<Card> onThisPc() {
        try {
            if (Platform.isLinux()) return linuxCards(LINUX_DRM);
            if (Platform.isWindows()) return windowsCards();
        } catch (RuntimeException | LinkageError e) {
            // WHY broad: a registry or sysfs surprise must cost the AMD check, never the LMStudio Setup panel.
            log.info("AMD cards could not be read for the LM Studio install check", e);
        }
        return List.of();
    }

    /**
     * Memory in use, in MiB. On Linux it is the AMD cards'. On Windows it is <b>every</b> card's, AMD or not:
     * the counter names each adapter by a number, not a vendor - see {@link GpuMemory#usedMib()}, which therefore
     * does not add NVIDIA's on top. Empty when it cannot be read.
     */
    static OptionalLong usedMib() {
        try {
            if (Platform.isLinux()) return linuxUsedMib(LINUX_DRM);
            if (Platform.isWindows()) return windowsAdaptersUsedMib();
        } catch (RuntimeException | LinkageError e) {
            log.info("AMD graphics memory in use could not be read", e);
        }
        return OptionalLong.empty();
    }

    // -------------------------------------------------------------------------
    // Linux
    // -------------------------------------------------------------------------

    static List<Card> linuxCards(Path drm) {
        List<Card> cards = new ArrayList<>();
        for (Path device : linuxAmdDevices(drm)) {
            String name = read(device.resolve("product_name"))
                    .filter(product -> !product.isBlank())
                    .orElseGet(() -> "AMD device " + read(device.resolve("device")).orElse("?"));
            cards.add(new Card(name, mib(read(device.resolve("mem_info_vram_total")))));
        }
        return cards;
    }

    static OptionalLong linuxUsedMib(Path drm) {
        long total = 0;
        boolean any = false;
        for (Path device : linuxAmdDevices(drm)) {
            OptionalLong used = mib(read(device.resolve("mem_info_vram_used")));
            if (used.isEmpty()) return OptionalLong.empty();
            total += used.getAsLong();
            any = true;
        }
        return any ? OptionalLong.of(total) : OptionalLong.empty();
    }

    private static List<Path> linuxAmdDevices(Path drm) {
        List<Path> devices = new ArrayList<>();
        if (!Files.isDirectory(drm)) return devices;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(drm)) {
            for (Path entry : entries) {
                if (!LINUX_CARD.matcher(entry.getFileName().toString()).matches()) continue;
                Path device = entry.resolve("device");
                if (read(device.resolve("vendor")).filter(VENDOR_ID::equalsIgnoreCase).isPresent()) {
                    devices.add(device);
                }
            }
        } catch (IOException e) {
            log.info("Could not list {}", drm, e);
        }
        devices.sort(null);
        return devices;
    }

    private static Optional<String> read(Path file) {
        try {
            return Optional.of(Files.readString(file, StandardCharsets.UTF_8).strip());
        } catch (IOException | RuntimeException unreadable) {
            return Optional.empty();
        }
    }

    private static OptionalLong mib(Optional<String> bytes) {
        try {
            return bytes.isPresent() ? OptionalLong.of(Long.parseLong(bytes.get()) / BYTES_PER_MIB) : OptionalLong.empty();
        } catch (NumberFormatException unreadable) {
            return OptionalLong.empty();
        }
    }

    // -------------------------------------------------------------------------
    // Windows
    // -------------------------------------------------------------------------

    private static List<Card> windowsCards() {
        List<Card> cards = new ArrayList<>();
        for (String adapter : Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, WINDOWS_DISPLAY_CLASS)) {
            if (!WINDOWS_ADAPTER_KEY.matcher(adapter).matches()) continue;
            String key = WINDOWS_DISPLAY_CLASS + "\\" + adapter;
            try {
                if (!isAmdDeviceId(stringValue(key, "MatchingDeviceId"))) continue;
                String name = stringValue(key, "DriverDesc");
                cards.add(new Card(name == null || name.isBlank() ? "AMD card " + adapter : name.strip(),
                        registryMib(value(key, "HardwareInformation.qwMemorySize"))));
            } catch (Win32Exception unreadable) {
                log.info("Display adapter {} could not be read", key, unreadable);
            }
        }
        return cards;
    }

    private static String stringValue(String key, String name) {
        return value(key, name) instanceof String text ? text : null;
    }

    private static Object value(String key, String name) {
        return Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, key, name)
                ? Advapi32Util.registryGetValue(WinReg.HKEY_LOCAL_MACHINE, key, name)
                : null;
    }

    /**
     * Whether a {@code MatchingDeviceId} such as {@code pci\ven_1002&dev_744c} names an AMD device.
     */
    static boolean isAmdDeviceId(String matchingDeviceId) {
        return matchingDeviceId != null && matchingDeviceId.toLowerCase(Locale.ROOT).contains("ven_1002");
    }

    /**
     * {@code HardwareInformation.qwMemorySize} in MiB. Most drivers write it as a QWORD; some as eight bytes of
     * binary, low byte first.
     */
    static OptionalLong registryMib(Object value) {
        if (value instanceof Long bytes && bytes > 0) return OptionalLong.of(bytes / BYTES_PER_MIB);
        if (value instanceof byte[] raw && raw.length == Long.BYTES) {
            long bytes = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).getLong();
            return bytes > 0 ? OptionalLong.of(bytes / BYTES_PER_MIB) : OptionalLong.empty();
        }
        return OptionalLong.empty();
    }

    /**
     * The {@code GPU Adapter Memory} counters' {@code Dedicated Usage}, one instance per adapter. The first constant
     * is OSHI's instance filter, the rest the counters read; under WMI the names are the property names.
     */
    enum AdapterMemory implements PdhCounterWildcardProperty {
        NAME("*"),
        DEDICATEDUSAGE("Dedicated Usage");

        private final String counter;

        AdapterMemory(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    private static OptionalLong windowsAdaptersUsedMib() {
        // WHY OSHI's query: it asks PDH by the counters' English names, which non-English Windows renames, and falls
        // back to WMI, whose class name is never translated.
        Pair<List<String>, Map<AdapterMemory, List<Long>>> read = PerfCounterWildcardQuery.queryInstancesAndValues(
                AdapterMemory.class, "GPU Adapter Memory", "Win32_PerfRawData_GPUPerformanceCounters_GPUAdapterMemory");
        return sumBytesAsMib(read.getB().get(AdapterMemory.DEDICATEDUSAGE));
    }

    static OptionalLong sumBytesAsMib(List<Long> bytesPerAdapter) {
        if (bytesPerAdapter == null || bytesPerAdapter.isEmpty()) return OptionalLong.empty();
        long total = 0;
        for (Long bytes : bytesPerAdapter) {
            if (bytes == null || bytes < 0) return OptionalLong.empty();
            total += bytes;
        }
        return OptionalLong.of(total / BYTES_PER_MIB);
    }
}
