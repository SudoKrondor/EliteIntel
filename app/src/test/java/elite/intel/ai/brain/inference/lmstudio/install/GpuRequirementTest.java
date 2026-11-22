package elite.intel.ai.brain.inference.lmstudio.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bar is an RTX 3090 or better - compute capability 8.6 or higher with at least 23,000 MiB - or two or more
 * cards of one vendor with 19,500 MiB together, each counted only from 7,500 MiB (and, for NVIDIA, cc 8.6). Lines
 * are what {@code nvidia-smi --query-gpu=name,memory.total,compute_cap --format=csv,noheader,nounits} prints.
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

    /** Reported 2026-11-10: this pair runs the game and the model together, neither told which card to use. */
    @Test
    void twoCardsWithAbout20GbBetweenThemPass() {
        assertTrue(GpuRequirement.isMetBy(List.of(
                "NVIDIA GeForce RTX 5070, 12227, 12.0",
                "NVIDIA GeForce RTX 3070 Ti, 8192, 8.6")));
    }

    @Test
    void twoCardsWithLessBetweenThemFail() {
        assertFalse(GpuRequirement.isMetBy(List.of(
                "NVIDIA GeForce RTX 3060 Ti, 8192, 8.6",
                "NVIDIA GeForce RTX 3070, 8192, 8.6")));
    }

    @Test
    void aCardTooOldToCountAddsNothing() {
        assertFalse(GpuRequirement.isMetBy(List.of(
                "NVIDIA GeForce RTX 5070, 12227, 12.0",
                "NVIDIA GeForce RTX 2080 Ti, 11264, 7.5")));
    }

    @Test
    void aCardTooSmallToCountAddsNothing() {
        assertFalse(GpuRequirement.isMetBy(List.of(
                "NVIDIA GeForce RTX 4080, 16376, 8.9",
                "NVIDIA GeForce RTX 3050, 6144, 8.6")));
    }

    @Test
    void oneCardWithTheCombinedBarAloneFails() {
        assertFalse(GpuRequirement.isMetBy(List.of("NVIDIA GeForce RTX 5070 Ti Hypothetical, 20480, 12.0")));
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

    @Test
    void anAmdCardWith24GbPasses() {
        assertTrue(GpuRequirement.isMetByAmd(List.of(
                new AmdCards.Card("AMD Radeon RX 7900 XTX", OptionalLong.of(24_560)))));
    }

    @ParameterizedTest
    @ValueSource(longs = {20_464, 16_304})
    void anAmdCardWithLessFailsAlone(long mib) {
        assertFalse(GpuRequirement.isMetByAmd(List.of(new AmdCards.Card("AMD Radeon", OptionalLong.of(mib)))));
    }

    @Test
    void twoAmdCardsWithAbout20GbBetweenThemPass() {
        assertTrue(GpuRequirement.isMetByAmd(List.of(
                new AmdCards.Card("AMD Radeon RX 7700 XT", OptionalLong.of(12_272)),
                new AmdCards.Card("AMD Radeon RX 6600", OptionalLong.of(8_176)))));
    }

    @Test
    void anAmdCardThatDoesNotReportItsMemoryFails() {
        assertFalse(GpuRequirement.isMetByAmd(List.of(new AmdCards.Card("AMD card 0000", OptionalLong.empty()))));
    }
}
