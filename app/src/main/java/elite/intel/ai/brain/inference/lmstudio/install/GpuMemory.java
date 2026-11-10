package elite.intel.ai.brain.inference.lmstudio.install;

import com.sun.jna.Platform;
import elite.intel.util.NvidiaSmi;

import java.util.List;
import java.util.OptionalLong;

/**
 * GPU memory in use right now, summed over every card, read before and after the model loads.
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
     * MiB in use on all cards, or empty when no driver can say.
     */
    static OptionalLong usedMib() {
        OptionalLong amd = AmdCards.usedMib();
        // WHY not asked on Windows once the AMD reading answers: that reading already counts every adapter, NVIDIA's
        // too, so adding nvidia-smi's would count an NVIDIA card twice.
        if (Platform.isWindows() && amd.isPresent()) return amd;
        return add(nvidiaUsedMib(), amd);
    }

    private static OptionalLong nvidiaUsedMib() {
        if (NvidiaSmi.query("--query-gpu=memory.used", "--format=csv,noheader,nounits")
                instanceof NvidiaSmi.Answer.Lines(List<String> lines)) {
            return sum(lines);
        }
        return OptionalLong.empty();
    }

    /**
     * The two vendors' readings together; either alone when only it answered.
     */
    static OptionalLong add(OptionalLong nvidia, OptionalLong amd) {
        if (nvidia.isEmpty()) return amd;
        if (amd.isEmpty()) return nvidia;
        return OptionalLong.of(nvidia.getAsLong() + amd.getAsLong());
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
