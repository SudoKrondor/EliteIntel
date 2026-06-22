package elite.intel.companion.memory;

import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryProcessingState;
import elite.intel.companion.model.memory.MemorySource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 memory spine: short-term timeline, count/token eviction into mid-term by topic, and the
 * index snapshot. A fixed-cost token estimator makes the budget eviction deterministic.
 */
class SessionMemoryGatewayTest {

    /** Every entry costs a constant number of tokens, independent of content length. */
    private static final class FixedTokenEstimator implements TokenEstimator {
        private final int perCall;

        FixedTokenEstimator(int perCall) {
            this.perCall = perCall;
        }

        @Override
        public int estimate(String text) {
            return perCall;
        }
    }

    private static MemoryEntry entry(ConversationTopic topic, String content) {
        return new MemoryEntry(Instant.now(), topic, MemorySource.COMMANDER, content, MemoryProcessingState.PROCESSED);
    }

    @Test
    void shortTermKeepsEntriesBelowLimits() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));

        gateway.write(entry(ConversationTopic.NAVIGATION, "first"));
        gateway.write(entry(ConversationTopic.COMBAT, "second"));

        List<MemoryEntry> timeline = gateway.readShortTermTimeline();
        assertEquals(2, timeline.size());
        assertEquals("first", timeline.get(0).content());
        assertEquals("second", timeline.get(1).content());
        // Nothing evicted yet, so mid-term has no topics.
        assertTrue(gateway.indexes().topicsWithMemory().isEmpty());
    }

    @Test
    void countOverflowEvictsOldestIntoMidTermByTopic() {
        // Cost 1 per entry keeps the token budget irrelevant; only the count cap can bite.
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));

        for (int i = 0; i < ShortTermMemory.MAX_ENTRIES + 3; i++) {
            ConversationTopic topic = i < 3 ? ConversationTopic.MINING : ConversationTopic.TRADE;
            gateway.write(entry(topic, "entry-" + i));
        }

        List<MemoryEntry> timeline = gateway.readShortTermTimeline();
        assertEquals(ShortTermMemory.MAX_ENTRIES, timeline.size());
        // The three oldest (MINING) were evicted; the newest entry is still the last one written.
        assertEquals("entry-" + (ShortTermMemory.MAX_ENTRIES + 2), timeline.get(timeline.size() - 1).content());

        List<ConversationTopic> topics = gateway.indexes().topicsWithMemory();
        assertTrue(topics.contains(ConversationTopic.MINING));
        assertFalse(topics.contains(ConversationTopic.TRADE));
    }

    @Test
    void sustainedWritesFillMidTermTopicsAndKeepTimelineBounded() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));

        // Two full timelines' worth, alternating topics, so both topics accumulate in mid-term.
        int total = ShortTermMemory.MAX_ENTRIES * 2;
        for (int i = 0; i < total; i++) {
            ConversationTopic topic = (i % 2 == 0) ? ConversationTopic.NAVIGATION : ConversationTopic.COMBAT;
            gateway.write(entry(topic, "e" + i));
        }

        // Hot timeline stays capped at the newest MAX_ENTRIES.
        List<MemoryEntry> timeline = gateway.readShortTermTimeline();
        assertEquals(ShortTermMemory.MAX_ENTRIES, timeline.size());
        assertEquals("e" + (total - 1), timeline.get(timeline.size() - 1).content());

        // Both topics filled mid-term, reported once each in enum order.
        assertEquals(
                List.of(ConversationTopic.NAVIGATION, ConversationTopic.COMBAT),
                gateway.indexes().topicsWithMemory());
    }

    @Test
    void tokenBudgetEvictsButAlwaysKeepsNewestEntry() {
        // One entry alone exceeds the whole budget.
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(ShortTermMemory.TOKEN_BUDGET + 1));

        gateway.write(entry(ConversationTopic.EXPLORATION, "a"));
        gateway.write(entry(ConversationTopic.EXPLORATION, "b"));

        // The token budget evicts down to the single newest entry, never to empty.
        List<MemoryEntry> timeline = gateway.readShortTermTimeline();
        assertEquals(1, timeline.size());
        assertEquals("b", timeline.get(0).content());
        assertTrue(gateway.indexes().topicsWithMemory().contains(ConversationTopic.EXPLORATION));
    }

    @Test
    void indexesReportLlmMemoryCapacity() {
        SessionMemoryGateway gateway = new SessionMemoryGateway();
        assertEquals(0, gateway.indexes().llmMemoryUsed());
        assertEquals(LlmMemory.MAX_ENTRIES, gateway.indexes().llmMemoryCapacity());
    }

    @Test
    void heuristicEstimatorIsConservativeAndNonNegative() {
        TokenEstimator estimator = new HeuristicTokenEstimator();
        assertEquals(0, estimator.estimate(null));
        assertEquals(0, estimator.estimate("   "));
        // 6 chars / 3 = 2 tokens (ceiling division).
        assertEquals(2, estimator.estimate("привет"));
        // Any non-blank text costs at least one token.
        assertEquals(1, estimator.estimate("a"));
    }
}
