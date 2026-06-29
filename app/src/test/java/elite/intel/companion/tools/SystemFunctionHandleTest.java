package elite.intel.companion.tools;

import com.google.gson.JsonObject;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.memory.MemoryAvailabilitySnapshot;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.Verbosity;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.speech.SpeechRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the executable system-function {@code handle}s drive the companion services reached statically
 * via {@link CompanionRuntime}: speak/clarify submit speech, remember/recall go through the memory gateway,
 * change_verbosity sets shared state, find_action queries the reducer, nothing_to_do is a no-op, and the
 * still-deferred set_topic fails loudly. Fakes back the services so everything is unit-testable.
 */
class SystemFunctionHandleTest {

    /** Captures the last speech request. */
    private final java.util.List<SpeechRequest> spoken = new java.util.ArrayList<>();
    private final RecordingMemory memory = new RecordingMemory();
    private final CompanionState state = new CompanionState();

    @BeforeEach
    void install() {
        CompanionRuntime.install(
                null,
                request -> {
                    spoken.add(request);
                    return CompletableFuture.completedFuture(null);
                },
                null,
                memory,
                (categories, input) -> List.of(new LlmToolDefinition("lower_landing_gear", "Lower the landing gear", "", List.of())),
                state);
    }

    @AfterEach
    void clear() {
        CompanionRuntime.clear();
    }

    private static JsonObject params(String key, String value) {
        JsonObject o = new JsonObject();
        o.addProperty(key, value);
        return o;
    }

    @Test
    void speakSubmitsToSpeechGateway() {
        JsonObject result = new SpeakFunction().handle("speak", params("text", "docking now"), "");

        assertEquals(1, spoken.size());
        assertEquals("docking now", spoken.get(0).text());
        assertEquals("spoken", result.get("status").getAsString());
    }

    @Test
    void clarifySpeaksTheQuestion() {
        new ClarifyFunction().handle("clarify", params("question", "which target?"), "");

        assertEquals("which target?", spoken.get(0).text());
    }

    @Test
    void setImportanceValidatesAndEchoesTheLevel() {
        JsonObject result = new SetImportanceFunction().handle("set_importance", params("level", "high"), "");

        assertEquals("importance_set", result.get("status").getAsString());
        assertEquals("high", result.get("importance").getAsString());
    }

    @Test
    void recallSearchesAllMemoryWithASingleQuery() {
        memory.matchingItems = List.of("painite in cargo rack seven", "rendezvous at Maia");

        JsonObject result = new SearchInMemoryFunction().handle("search_in_memory", params("query", "painite"), "");

        assertEquals("painite", memory.recalledQuery);
        assertEquals(2, result.getAsJsonArray("items").size());
        assertEquals("painite in cargo rack seven", result.getAsJsonArray("items").get(0).getAsString());
    }

    @Test
    void changeVerbositySetsSharedState() {
        JsonObject result = new ChangeVerbosityFunction().handle("change_verbosity", params("verbosity", "chatty"), "");

        assertEquals(Verbosity.CHATTY, state.verbosity());
        assertEquals("chatty", result.get("verbosity").getAsString());
    }

    @Test
    void changeVerbosityRejectsUnknownMode() {
        JsonObject result = new ChangeVerbosityFunction().handle("change_verbosity", params("verbosity", "loud"), "");

        assertEquals("unknown verbosity", result.get("error").getAsString());
        assertEquals(Verbosity.NORMAL, state.verbosity()); // unchanged
    }

    @Test
    void findActionReturnsReducerMatches() {
        JsonObject result = new FindActionFunction().handle("find_action", params("query", "gear"), "");

        assertEquals(1, result.getAsJsonArray("items").size());
        JsonObject item = result.getAsJsonArray("items").get(0).getAsJsonObject();
        assertEquals("lower_landing_gear", item.get("name").getAsString());
        assertEquals("Lower the landing gear", item.get("description").getAsString());
    }

    @Test
    void nothingToDoIsNoOp() {
        assertNull(new NothingToDoFunction().handle("nothing_to_do", new JsonObject(), ""));
    }

    @Test
    void changeGlobalTopicSetsSharedState() {
        JsonObject result = new ChangeGlobalTopicFunction().handle("change_global_topic", params("topic", "navigation"), "");

        assertEquals(ConversationTopic.NAVIGATION, state.globalTopic());
        assertEquals("navigation", result.get("topic").getAsString());
    }

    @Test
    void changeGlobalTopicRejectsUnknownTopic() {
        JsonObject result = new ChangeGlobalTopicFunction().handle("change_global_topic", params("topic", "nonsense"), "");

        assertEquals("unknown topic", result.get("error").getAsString());
        assertEquals(ConversationTopic.SOCIAL, state.globalTopic()); // default, unchanged
    }

    /** Minimal MemoryGateway fake recording the calls the system functions make. */
    private static final class RecordingMemory implements MemoryGateway {
        String recalledQuery;
        List<String> matchingItems = List.of();

        @Override public void write(MemoryEntry entry) { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> readShortTermTimeline() { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit) {
            throw new UnsupportedOperationException();
        }
        @Override public List<String> recallMatching(String query, int limit) {
            recalledQuery = query;
            return matchingItems;
        }
        @Override public List<MemoryEntry> importantWorkingSet(int maxEntries, int tokenBudget) { throw new UnsupportedOperationException(); }
        @Override public MemoryAvailabilitySnapshot indexes() { throw new UnsupportedOperationException(); }
        @Override public String longTermSummary() { throw new UnsupportedOperationException(); }
        @Override public void replaceLongTermSummary(String summary) { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> longTermPinnedFacts() { throw new UnsupportedOperationException(); }
        @Override public void addLongTermPinned(MemoryEntry fact) { throw new UnsupportedOperationException(); }
    }
}
