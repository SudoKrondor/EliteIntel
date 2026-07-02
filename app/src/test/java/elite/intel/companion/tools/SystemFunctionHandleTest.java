package elite.intel.companion.tools;

import com.google.gson.JsonObject;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.memory.MemoryAvailabilitySnapshot;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.model.ConversationTopic;
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

/**
 * Verifies the executable system-function {@code handle}s drive the companion services reached statically
 * via {@link CompanionRuntime}: speak submits speech, remember/recall go through the memory gateway,
 * find_action queries the reducer, and classify_turn moves the global topic and stamps the turn's
 * importance. Fakes back the services so everything is unit-testable.
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
    void classifyTurnSetsTopicAndStampsImportance() {
        JsonObject p = params("topic", "navigation");
        p.addProperty("importance", "high");
        JsonObject result = new ClassifyTurnFunction().handle("classify_turn", p, "");

        assertEquals(ConversationTopic.NAVIGATION, state.globalTopic());
        assertEquals("turn_classified", result.get("status").getAsString());
        assertEquals("navigation", result.get("topic").getAsString());
        assertEquals("high", result.get("importance").getAsString());
    }

    @Test
    void classifyTurnDefaultsUnknownImportanceToNormal() {
        JsonObject p = params("topic", "navigation");
        p.addProperty("importance", "bogus");
        JsonObject result = new ClassifyTurnFunction().handle("classify_turn", p, "");

        assertEquals("normal", result.get("importance").getAsString());
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
    void findActionReturnsReducerMatches() {
        JsonObject result = new FindActionFunction().handle("find_action", params("query", "gear"), "");

        assertEquals(1, result.getAsJsonArray("items").size());
        JsonObject item = result.getAsJsonArray("items").get(0).getAsJsonObject();
        assertEquals("lower_landing_gear", item.get("name").getAsString());
        assertEquals("Lower the landing gear", item.get("description").getAsString());
    }

    @Test
    void classifyTurnRejectsUnknownTopic() {
        JsonObject p = params("topic", "nonsense");
        p.addProperty("importance", "high");
        JsonObject result = new ClassifyTurnFunction().handle("classify_turn", p, "");

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
        @Override public MemoryAvailabilitySnapshot indexes() { throw new UnsupportedOperationException(); }
        @Override public String longTermSummary() { throw new UnsupportedOperationException(); }
        @Override public void replaceLongTermSummary(String summary) { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> longTermPinnedFacts() { throw new UnsupportedOperationException(); }
        @Override public void addLongTermPinned(MemoryEntry fact) { throw new UnsupportedOperationException(); }
    }
}
