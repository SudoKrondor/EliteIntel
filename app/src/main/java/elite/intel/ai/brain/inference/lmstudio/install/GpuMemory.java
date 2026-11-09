package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.util.NvidiaSmi;

import java.util.List;
import java.util.OptionalLong;

/**
 * GPU memory in use right now, summed over every NVIDIA card, read before and after the model loads.
 * <p>
 * WHY this and not timing: {@code lms load --gpu max} does not fail when the model cannot go on the GPU - on a
 * machine without a usable one it loads onto the CPU and says nothing (smoke test, round 1). The memory in use rising
 * by roughly the model's size is the direct sign it went where it should. Summed, because on a PC with two cards it
 * may land on either.
 */
final class GpuMemory {

    /**
     * The model takes about 6 GB on the card. A rise of less than this means it did not go there.
     */
    static final long MODEL_ON_GPU_MIB = 4_096;

    private GpuMemory() {
    }

    /**
     * MiB in use on all NVIDIA cards, or empty when {@code nvidia-smi} cannot say.
     */
    static OptionalLong usedMib() {
        if (NvidiaSmi.query("--query-gpu=memory.used", "--format=csv,noheader,nounits")
                instanceof NvidiaSmi.Answer.Lines(List<String> lines)) {
            return sum(lines);
        }
        return OptionalLong.empty();
    }

    static OptionalLong sum(List<String> lines) {
        long total = 0;
        for (String line : lines) {
            try {
                total += Long.parseLong(line.strip());
            } catch (NumberFormatException unreadable) {
                return OptionalLong.empty();
            }
        }
        return lines.isEmpty() ? OptionalLong.empty() : OptionalLong.of(total);
    }
}
