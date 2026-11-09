package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.util.NvidiaSmi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Whether this PC can run the game and the local model together: an RTX 3090 (24 GB) or better.
 * <p>
 * WHY that bar: the game takes 8.5 GB or more, the model about 6 GB, and browsers, OBS and Windows itself also use
 * the GPU, so 18 GB does not hold up in practice. A 12 GB card is enough only for a PC that runs nothing but the
 * model, which is the manual second-PC setup in the website guide, not this one-click one.
 * <p>
 * Read from {@code nvidia-smi}, because OSHI's VRAM figure is wrong on both platforms. AMD and Intel cards are not
 * seen at all and so never pass; that is a known limit of this release, not an oversight.
 * <p>
 * See {@code docs/LM_STUDIO_INSTALL_ASSISTANCE.md}, "Who sees the button".
 */
public final class GpuRequirement {

    private static final Logger log = LogManager.getLogger(GpuRequirement.class);

    /**
     * A 24 GB card reports a little under 24,576 MiB, and by how much varies with the card and the driver.
     */
    static final int MIN_MEMORY_MIB = 23_000;

    /**
     * Ampere's RTX 3090. Turns away the old 24 GB cards: the Titan RTX (7.5), Tesla P40 (6.1) and M40 (5.2).
     */
    static final int MIN_COMPUTE_MAJOR = 8;
    static final int MIN_COMPUTE_MINOR = 6;

    private GpuRequirement() {
    }

    /**
     * Whether any card in this PC meets the bar. Asked once per run - the cards do not change while the app is up -
     * and off the event thread, since {@code nvidia-smi} can take up to {@link NvidiaSmi#TIMEOUT} to answer.
     */
    public static boolean isMetByThisPc() {
        return Cached.MET;
    }

    private static final class Cached {
        private static final boolean MET = readThisPc();
    }

    private static boolean readThisPc() {
        NvidiaSmi.Answer answer = NvidiaSmi.query(
                "--query-gpu=name,memory.total,compute_cap", "--format=csv,noheader,nounits");
        if (!(answer instanceof NvidiaSmi.Answer.Lines(List<String> lines))) {
            log.debug("No NVIDIA card readable for the LM Studio install check: {}", answer);
            return false;
        }
        boolean met = isMetBy(lines);
        log.debug("LM Studio install check: GPUs {} meet the bar: {}", lines, met);
        return met;
    }

    /**
     * Whether any of {@code nvidia-smi}'s lines describes a card that meets the bar. Each line is
     * {@code name, memory.total in MiB, compute_cap}; a line it cannot read (a card answering {@code [N/A]}) is
     * simply not a card that passes.
     */
    static boolean isMetBy(List<String> lines) {
        return lines.stream().anyMatch(GpuRequirement::meetsTheBar);
    }

    private static boolean meetsTheBar(String line) {
        // WHY from the right: the card's name comes first and is free text, the two numbers after it are not.
        String[] fields = line.split(",");
        if (fields.length < 3) return false;
        try {
            int memoryMib = Integer.parseInt(fields[fields.length - 2].strip());
            String[] compute = fields[fields.length - 1].strip().split("\\.");
            int major = Integer.parseInt(compute[0]);
            int minor = compute.length > 1 ? Integer.parseInt(compute[1]) : 0;
            boolean newEnough = major > MIN_COMPUTE_MAJOR || (major == MIN_COMPUTE_MAJOR && minor >= MIN_COMPUTE_MINOR);
            return newEnough && memoryMib >= MIN_MEMORY_MIB;
        } catch (NumberFormatException unreadable) {
            return false;
        }
    }
}
