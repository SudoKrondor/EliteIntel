package elite.intel.ui.support;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * The machine the app is running on, for the support bundle manifest.
 * <p>
 * WHY: "it is slow" and "the voice stutters" are reports whose first question is always the same - what is
 * this running on? A local LLM and two local speech engines share one machine with the game, and a
 * report from a laptop with an integrated GPU and eight gigabytes is a different bug from the same words
 * sent from a desktop with a dedicated card. Asking gets prose ("a decent PC"); this gets the numbers.
 * <p>
 * Every line is optional. The JDK volunteers cores, memory and disk space; the CPU model and the graphics
 * cards come from OSHI, which asks the OS through native calls that can fail on any machine for any reason
 * (WMI unreachable, a sysfs the user cannot read, a JNA library the antivirus quarantined). Each probe is
 * guarded on its own, so a GPU query that dies costs the GPU line and nothing else - the bundle is wanted
 * most on exactly the machine where something native is broken, and a hardware line is never worth a
 * bundle. Nothing here is a {@code fail early} candidate: the report is a courtesy, not a contract.
 * <p>
 * Reported once per bundle and never cached: a bundle is rare, the query is cheap on Linux and a few
 * hundred milliseconds of WMI on Windows, and it already runs off the event thread.
 */
final class HardwareReport {

    private static final Logger log = LogManager.getLogger(HardwareReport.class);

    private static final long MIB = 1024L * 1024L;
    private static final long GIB = 1024L * MIB;
    private static final Duration NVIDIA_SMI_TIMEOUT = Duration.ofSeconds(5);

    private HardwareReport() {
    }

    /**
     * The whole report as manifest lines, ending in a newline.
     *
     * @param volumes directories whose disks are worth a free-space line - the install and journal
     *                directories, since a full disk under either explains a class of report on its own.
     *                Nulls and directories that do not exist are skipped without comment.
     */
    static String describe(@Nullable Path... volumes) {
        HardwareAbstractionLayer hardware = probeOshi();
        StringBuilder text = new StringBuilder();
        text.append(guarded("CPU", () -> cpu(hardware)));
        text.append(guarded("Memory", HardwareReport::memory));
        text.append(guarded("JVM heap", HardwareReport::heap));
        text.append(guarded("GPU", () -> gpus(hardware)));
        text.append(guarded("GPU (nvidia-smi)", HardwareReport::nvidiaSmi));
        for (Path volume : volumes) {
            if (volume == null) continue;
            text.append(guarded("Disk " + volume, () -> disk(volume)));
        }
        return text.toString();
    }

    /**
     * One probe, one line: whatever it throws becomes its own "could not be queried" line rather than
     * anyone else's problem. {@link LinkageError} is caught because that is how a native library that
     * failed to load announces itself, and it is not a RuntimeException.
     */
    private static String guarded(String label, Supplier<String> probe) {
        try {
            String line = probe.get();
            return line.isEmpty() ? "" : line + '\n';
        } catch (RuntimeException | LinkageError e) {
            log.debug("Hardware probe '{}' failed", label, e);
            return label + ": could not be queried (" + e + ")\n";
        }
    }

    /**
     * OSHI's entry point, or null when it cannot be brought up at all - in which case the OSHI-backed
     * probes fall back to what the JDK knows rather than each failing the same way.
     */
    @Nullable
    private static HardwareAbstractionLayer probeOshi() {
        try {
            return new SystemInfo().getHardware();
        } catch (RuntimeException | LinkageError e) {
            log.debug("OSHI unavailable; hardware report falls back to the JDK", e);
            return null;
        }
    }

    private static String cpu(@Nullable HardwareAbstractionLayer hardware) {
        int logical = Runtime.getRuntime().availableProcessors();
        if (hardware == null) return "CPU: " + logical + " logical cores (model unavailable)";
        CentralProcessor cpu = hardware.getProcessor();
        return "CPU: " + cpu.getProcessorIdentifier().getName().trim()
                + "  " + cpu.getPhysicalProcessorCount() + " cores / "
                + cpu.getLogicalProcessorCount() + " threads";
    }

    /**
     * Physical RAM through the {@code com.sun} bean, which both platforms honour. The {@code java.lang}
     * bean of the same name knows only the load average.
     */
    private static String memory() {
        com.sun.management.OperatingSystemMXBean os =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return "Memory: " + gib(os.getTotalMemorySize()) + " total, " + gib(os.getFreeMemorySize()) + " free";
    }

    /**
     * What the JVM is allowed and what it has taken. WHY it sits beside physical RAM: a machine with
     * plenty of memory and a heap capped by a small {@code -Xmx} looks, from the inside, exactly like a
     * machine that is out of memory, and only the pair of numbers tells the two apart.
     */
    private static String heap() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        return "JVM heap: " + mib(runtime.maxMemory()) + " max, " + mib(used) + " in use";
    }

    /**
     * Every card the OS reports, by name. The VRAM figure the OS gives is labelled as such because it is
     * wrong in both directions on both platforms: Linux exposes the card's PCI window (256 MiB on a card
     * with 24 GiB), and the WMI field Windows answers from is 32-bit, so anything above 4 GiB reads as
     * 4 GiB. It is kept because "0.3 GiB" beside a card's name still identifies the card, and the honest
     * figure, where one can be had, is on the {@link #nvidiaSmi()} line beneath it.
     */
    private static String gpus(@Nullable HardwareAbstractionLayer hardware) {
        if (hardware == null) return "GPU: unavailable (OSHI could not start)";
        List<GraphicsCard> cards = hardware.getGraphicsCards();
        if (cards.isEmpty()) return "GPU: none reported";
        StringBuilder text = new StringBuilder();
        for (GraphicsCard card : cards) {
            if (!text.isEmpty()) text.append('\n');
            text.append("GPU: ").append(card.getName().trim());
            long vram = card.getVRam();
            text.append(vram > 0 ? "  " + gib(vram) + " VRAM as the OS reports it" : "  VRAM not reported by the OS");
        }
        return text.toString();
    }

    /**
     * The one exact VRAM figure available without a vendor SDK: NVIDIA's own tool, which the driver
     * installs on the PATH on both platforms. Absent on every other vendor's machine, and that absence is
     * silent - a line saying "no nvidia-smi" tells the reader nothing the GPU line above has not.
     * <p>
     * Total AND in use, in that order. WHY the second: "it runs slow" with a card that is full to the brim
     * is a different report from the same words with an idle card - the first is a model that does not
     * fit and is spilling to system RAM, the second is somewhere else entirely. The figure is a snapshot
     * taken while the game, the LLM and the speech engines are all resident, which is the moment asked about.
     * <p>
     * Bounded: a wedged driver can hang the tool, and this is a courtesy line in a bundle, so it gets a few
     * seconds and is then killed. Whatever the tool prints is passed through as-is, one line per card.
     */
    private static String nvidiaSmi() {
        Process process;
        try {
            process = new ProcessBuilder("nvidia-smi", "--query-gpu=name,memory.total,memory.used", "--format=csv,noheader")
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException notInstalled) {
            return "";
        }
        try {
            if (!process.waitFor(NVIDIA_SMI_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "GPU (nvidia-smi): no answer within " + NVIDIA_SMI_TIMEOUT.toSeconds() + "s";
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0 || output.isBlank()) return "";
            StringBuilder text = new StringBuilder();
            for (String line : output.strip().split("\\R")) {
                if (!text.isEmpty()) text.append('\n');
                text.append("GPU (nvidia-smi): ").append(line.strip());
            }
            return text.toString();
        } catch (IOException e) {
            return "GPU (nvidia-smi): could not be read (" + e + ")";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return "";
        }
    }

    private static String disk(Path volume) {
        if (!Files.exists(volume)) return "";
        try {
            FileStore store = Files.getFileStore(volume);
            return "Disk " + volume + ": " + gib(store.getUsableSpace()) + " free of " + gib(store.getTotalSpace());
        } catch (java.io.IOException e) {
            return "Disk " + volume + ": could not be queried (" + e + ")";
        }
    }

    private static String gib(long bytes) {
        return String.format("%.1f GiB", bytes / (double) GIB);
    }

    private static String mib(long bytes) {
        return (bytes / MIB) + " MiB";
    }
}
