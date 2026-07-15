package elite.intel.companion.prompt;

import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.memory.MemorySnapshot;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemorySource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryFactCandidatesTest {

    @Test
    void keepsHighCanonicalMaxVerbatimAndEventsWhileDroppingRoutineCommanderLines() {
        MemoryGateway memory = new FakeMemory(List.of(
                entry(MemorySource.COMMANDER, MemoryImportance.NORMAL,
                        "целься в двигатели", "целься в двигатели"),
                entry(MemorySource.COMMANDER, MemoryImportance.HIGH,
                        "наш связной — дельгадо", "связной — дельгадо"),
                entry(MemorySource.COMMANDER, MemoryImportance.HIGH,
                        "план пока не сформулирован", null),
                entry(MemorySource.COMMANDER, MemoryImportance.MAX,
                        "запомни дословно: сьерра девять", "код — сьерра девять"),
                entry(MemorySource.COMMANDER, MemoryImportance.LOW, "тихо тут, красота", null),
                entry(MemorySource.COMPANION, MemoryImportance.HIGH, "понял. поле бедлам. записано.", null),
                entry(MemorySource.EVENT, MemoryImportance.NORMAL, "прибыли в систему вольф", null),
                entry(MemorySource.TOOL_RESULT, MemoryImportance.MAX, "command add_mining_target executed", null),
                entry(MemorySource.SYSTEM, MemoryImportance.NORMAL, "long-term summary", null)));

        assertEquals(
                List.of(new Fact("связной — дельгадо", "commander"),
                        new Fact("запомни дословно: сьерра девять", "commander"),
                        new Fact("прибыли в систему вольф", "event")),
                MemoryFactCandidates.forInput(memory, "что помним"));
    }

    @Test
    void rendersTheCanonicalFactWhenPresent() {
        MemoryGateway memory = new FakeMemory(List.of(
                entry(MemorySource.COMMANDER, MemoryImportance.HIGH,
                        "и запиши: покупатель утиля — халлоран", "покупатель утиля — халлоран")));

        assertEquals(List.of(new Fact("покупатель утиля — халлоран", "commander")),
                MemoryFactCandidates.forInput(memory, "утиль"));
    }

    @Test
    void capsAtThreeCandidates() {
        MemoryGateway memory = new FakeMemory(List.of(
                entry(MemorySource.COMMANDER, MemoryImportance.HIGH, "факт один", "факт один"),
                entry(MemorySource.COMMANDER, MemoryImportance.HIGH, "факт два", "факт два"),
                entry(MemorySource.COMMANDER, MemoryImportance.HIGH, "факт три", "факт три"),
                entry(MemorySource.COMMANDER, MemoryImportance.HIGH, "факт четыре", "факт четыре")));

        assertEquals(3, MemoryFactCandidates.forInput(memory, "факты").size());
    }

    @Test
    void blankInputYieldsNoCandidates() {
        assertTrue(MemoryFactCandidates.forInput(new FakeMemory(List.of()), "   ").isEmpty());
    }

    private static MemoryEntry entry(MemorySource source, MemoryImportance importance, String content, String canonical) {
        return new MemoryEntry(Instant.now(), ConversationTopic.SOCIAL, source, content, importance, null, canonical);
    }

    /** Minimal gateway returning a fixed candidate list; the other operations are unused by this test. */
    private static final class FakeMemory implements MemoryGateway {
        private final List<MemoryEntry> candidates;

        private FakeMemory(List<MemoryEntry> candidates) {
            this.candidates = candidates;
        }

        @Override public MemorySnapshot snapshot() { throw new UnsupportedOperationException(); }

        @Override public List<MemoryEntry> recallCandidates(String query, int limit) { return candidates; }
        @Override public void write(MemoryEntry entry) { throw new UnsupportedOperationException(); }
        @Override public void writeBatch(List<MemoryEntry> entries) { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> readShortTermTimeline() { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit) { throw new UnsupportedOperationException(); }
        @Override public List<String> recallMatching(String query, int limit) { throw new UnsupportedOperationException(); }
        @Override public String longTermSummary() { throw new UnsupportedOperationException(); }
        @Override public void replaceLongTermSummary(String summary) { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> longTermPinnedFacts() { throw new UnsupportedOperationException(); }
        @Override public void addLongTermPinned(MemoryEntry fact) { throw new UnsupportedOperationException(); }
    }
}
