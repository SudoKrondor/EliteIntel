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
}
