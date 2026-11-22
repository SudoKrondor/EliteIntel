package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.util.NvidiaSmi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.OptionalLong;

/**
 * Whether this PC can run the game and the local model together: an RTX 3090 (24 GB) or better, or two or more
 * cards of one vendor with about 20 GB between them.
 * <p>
 * WHY that bar: the game takes 8.5 GB or more, the model about 6 GB, and browsers, OBS and Windows itself also use
 * the GPU, so one 18 GB card does not hold up in practice. A 12 GB card is enough only for a PC that runs nothing
 * but the model, which is the manual second-PC setup in the website guide, not this one-click one.
 * <p>
 * WHY a lower bar for several cards: with neither the game nor LM Studio told which card to use, LM Studio spreads
 * the model over them and the load on each card evens out. A 12 GB RTX 5070 with an 8 GB RTX 3070 Ti runs the game
 * and the model together (reported 2026-11-10). Cards are added up per vendor only, because LM Studio's CUDA engine
 * does not split a model between NVIDIA and AMD, and a card too old or too small to help ({@link #counts}) adds
 * nothing.
 * <p>
 * NVIDIA cards are read from {@code nvidia-smi}, because OSHI's VRAM figure is wrong on both platforms. AMD cards
 * ({@link AmdCards}) are a <b>beta</b>: the same memory bar, nothing about the chip, and untested on real hardware,
 * so the install dialog says so. Intel has no card with 24 GB and is not looked for.
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
     * Two or more cards pass with this much between them: a 12 GB and an 8 GB card report about 20,400 MiB.
     */
    static final int MIN_COMBINED_MIB = 19_500;

    /**
     * The least a card must have to count towards {@link #MIN_COMBINED_MIB}. An 8 GB card reports about 8,192 MiB,
     * so this keeps out the 6 GB and smaller ones, which would hold too little of the model to be worth the split.
     */
    static final int MIN_COUNTED_MIB = 7_500;

    /**
     * Ampere's RTX 3090. Turns away the old 24 GB cards: the Titan RTX (7.5), Tesla P40 (6.1) and M40 (5.2).
     */
    static final int MIN_COMPUTE_MAJOR = 8;
    static final int MIN_COMPUTE_MINOR = 6;

    private GpuRequirement() {
    }

    /**
     * Which card, if any, lets this PC pass.
     */
    public enum Verdict {
        NOT_MET,
        /**
         * An NVIDIA card meets the bar: the tested path.
         */
        MET_BY_NVIDIA,
        /**
         * No NVIDIA card does, an AMD card does: offered as a beta, with a warning that it is untested.
         */
        MET_BY_AMD
    }

    /**
     * Which card in this PC meets the bar. Asked once per run - the cards do not change while the app is up - and off
     * the event thread, since {@code nvidia-smi} can take up to {@link NvidiaSmi#TIMEOUT} to answer.
     */
    public static Verdict forThisPc() {
        return Cached.VERDICT;
    }

    /**
     * Whether any card in this PC meets the bar; see {@link #forThisPc()}.
     */
    public static boolean isMetByThisPc() {
        return forThisPc() != Verdict.NOT_MET;
    }

    private static final class Cached {
        private static final Verdict VERDICT = readThisPc();
    }

    private static Verdict readThisPc() {
        NvidiaSmi.Answer answer = NvidiaSmi.query(
                "--query-gpu=name,memory.total,compute_cap", "--format=csv,noheader,nounits");
        if (answer instanceof NvidiaSmi.Answer.Lines(List<String> lines)) {
            boolean met = isMetBy(lines);
            log.info("LM Studio install check: NVIDIA GPUs {} meet the bar: {}", lines, met);
            // WHY NVIDIA first: it is the tested path, so a PC with both kinds of passing card gets no beta warning.
            if (met) return Verdict.MET_BY_NVIDIA;
        } else {
            log.info("No NVIDIA card readable for the LM Studio install check: {}", answer);
        }
        List<AmdCards.Card> amd = AmdCards.onThisPc();
        boolean amdMet = isMetByAmd(amd);
        log.info("LM Studio install check: AMD GPUs {} meet the bar: {}", amd, amdMet);
        return amdMet ? Verdict.MET_BY_AMD : Verdict.NOT_MET;
    }

    /**
     * Whether any of these AMD cards meets the bar. Memory only: unlike NVIDIA's compute capability, AMD publishes
     * nothing that ranks its chips, and the 24 GB cards a gaming PC holds are all RDNA 2 or newer.
     */
    static boolean isMetByAmd(List<AmdCards.Card> cards) {
        return isMetByMemory(cards.stream().map(GpuRequirement::countedMib).toList());
    }

    /**
     * Whether this AMD card counts towards the bar.
     */
    static boolean counts(AmdCards.Card card) {
        return countedMib(card).isPresent();
    }

    private static OptionalLong countedMib(AmdCards.Card card) {
        OptionalLong memory = card.memoryMib();
        return memory.isPresent() && memory.getAsLong() >= MIN_COUNTED_MIB ? memory : OptionalLong.empty();
    }

    /**
     * Whether {@code nvidia-smi}'s lines describe cards that meet the bar together. Each line is
     * {@code name, memory.total in MiB, compute_cap}; a line it cannot read (a card answering {@code [N/A]}) is
     * simply not a card that counts.
     */
    static boolean isMetBy(List<String> lines) {
        return isMetByMemory(lines.stream().map(GpuRequirement::countedMib).toList());
    }

    /**
     * Whether the card on this line of {@code nvidia-smi} counts towards the bar.
     */
    static boolean counts(String line) {
        return countedMib(line).isPresent();
    }

    /**
     * The memory of the card on one line of {@code nvidia-smi}, when it is new enough and big enough to count. Its
     * last two fields are {@code memory.total} in MiB and {@code compute_cap}; whatever comes before them is not read.
     */
    private static OptionalLong countedMib(String line) {
        // WHY from the right: the card's name comes first and is free text, the two numbers after it are not.
        String[] fields = line.split(",");
        if (fields.length < 3) return OptionalLong.empty();
        try {
            int memoryMib = Integer.parseInt(fields[fields.length - 2].strip());
            String[] compute = fields[fields.length - 1].strip().split("\\.");
            int major = Integer.parseInt(compute[0]);
            int minor = compute.length > 1 ? Integer.parseInt(compute[1]) : 0;
            boolean newEnough = major > MIN_COMPUTE_MAJOR || (major == MIN_COMPUTE_MAJOR && minor >= MIN_COMPUTE_MINOR);
            return newEnough && memoryMib >= MIN_COUNTED_MIB ? OptionalLong.of(memoryMib) : OptionalLong.empty();
        } catch (NumberFormatException unreadable) {
            return OptionalLong.empty();
        }
    }

    /**
     * One card with {@link #MIN_MEMORY_MIB}, or two or more with {@link #MIN_COMBINED_MIB} between them.
     *
     * @param counted each card's memory, empty for a card that does not count
     */
    private static boolean isMetByMemory(List<OptionalLong> counted) {
        List<Long> cards = counted.stream().filter(OptionalLong::isPresent).map(OptionalLong::getAsLong).toList();
        if (cards.stream().anyMatch(mib -> mib >= MIN_MEMORY_MIB)) return true;
        return cards.size() >= 2 && cards.stream().mapToLong(Long::longValue).sum() >= MIN_COMBINED_MIB;
    }
}
