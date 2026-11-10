package elite.intel.ai.brain.inference.lmstudio.install;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GpuMemoryTest {

    @Test
    void memoryInUseIsSummedOverEveryCard() {
        assertEquals(OptionalLong.of(9_216), GpuMemory.sum(List.of("8704", "512")));
    }

    @Test
    void aCardThatCannotSayLeavesTheTotalUnknown() {
        assertEquals(OptionalLong.empty(), GpuMemory.sum(List.of("8704", "[N/A]")));
    }

    @Test
    void noCardsIsUnknown() {
        assertEquals(OptionalLong.empty(), GpuMemory.sum(List.of()));
    }

    @Test
    void bothVendorsAreAddedAndEitherStandsAlone() {
        assertEquals(OptionalLong.of(9_000), GpuMemory.add(OptionalLong.of(1_000), OptionalLong.of(8_000)));
        assertEquals(OptionalLong.of(8_000), GpuMemory.add(OptionalLong.empty(), OptionalLong.of(8_000)));
        assertEquals(OptionalLong.of(1_000), GpuMemory.add(OptionalLong.of(1_000), OptionalLong.empty()));
        assertEquals(OptionalLong.empty(), GpuMemory.add(OptionalLong.empty(), OptionalLong.empty()));
    }
}
