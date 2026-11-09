package elite.intel.ai.brain.inference.lmstudio.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bar is an RTX 3090 or better: compute capability 8.6 or higher with at least 23,000 MiB. Lines are what
 * {@code nvidia-smi --query-gpu=name,memory.total,compute_cap --format=csv,noheader,nounits} prints.
 */
class GpuRequirementTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "NVIDIA GeForce RTX 3090, 24576, 8.6",
            "NVIDIA GeForce RTX 3090, 24268, 8.6",
            "NVIDIA GeForce RTX 4090, 24564, 8.9",
            "NVIDIA GeForce RTX 5090, 32607, 12.0",
            "NVIDIA RTX A5000, 24564, 8.6",
    })
    void aCardAtOrAboveTheBarPasses(String line) {
        assertTrue(GpuRequirement.isMetBy(List.of(line)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "NVIDIA TITAN RTX, 24576, 7.5",
            "Tesla P40, 24576, 6.1",
            "Tesla M40 24GB, 24576, 5.2",
            "Quadro P6000, 24576, 6.1",
            "NVIDIA GeForce RTX 4080, 16376, 8.9",
            "NVIDIA GeForce RTX 3060, 12288, 8.6",
    })
    void aCardBelowTheBarFails(String line) {
        assertFalse(GpuRequirement.isMetBy(List.of(line)));
    }

    @Test
    void oneQualifyingCardIsEnoughAlongsideAWeakerOne() {
        assertTrue(GpuRequirement.isMetBy(List.of(
                "Tesla P40, 24576, 6.1",
                "NVIDIA GeForce RTX 3090, 24576, 8.6")));
    }

    @Test
    void aCardNameWithACommaIsReadFromTheRight() {
        assertTrue(GpuRequirement.isMetBy(List.of("NVIDIA GeForce RTX 3090, Founders Edition, 24576, 8.6")));
    }

    @Test
    void aLineThatCannotBeReadIsNotAPass() {
        assertFalse(GpuRequirement.isMetBy(List.of("NVIDIA GeForce RTX 3090, [N/A], [N/A]")));
        assertFalse(GpuRequirement.isMetBy(List.of("Failed to initialize NVML: Driver/library version mismatch")));
    }

    @Test
    void noCardsIsNotAPass() {
        assertFalse(GpuRequirement.isMetBy(List.of()));
    }
}
