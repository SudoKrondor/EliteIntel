package elite.intel.companion.tools;

import com.google.gson.JsonObject;
import elite.intel.companion.CompanionGateways;
import elite.intel.companion.memory.MemoryAvailabilitySnapshot;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryProcessingState;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the executable system-function {@code handle}s drive the companion gateways reached statically
 * via {@link CompanionGateways}: speak/clarify submit speech, remember/recall go through the memory gateway,
 * nothing_to_do is a no-op, and the not-yet-wired functions fail loudly. Fakes back the gateways so the
 * memory-backed functions are testable even though the real store lands in Phase 4.
 */
class SystemFunctionHandleTest {

    /** Captures the last speech request. */
    private final java.util.List<SpeechRequest> spoken = new java.util.ArrayList<>();
    private final RecordingMemory memory = new RecordingMemory();

    @BeforeEach
    void install() {
        CompanionGateways.install(null, request -> {
            spoken.add(request);
            return CompletableFuture.completedFuture(null);
        }, null, memory);
    }

    @AfterEach
    void clear() {
        CompanionGateways.clear();
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
    void rememberWritesToLlmMemory() {
        JsonObject result = new RememberFunction().handle("remember", params("content", "owes me 5cr"), "");

        assertEquals("owes me 5cr", memory.remembered);
        assertEquals("remembered", result.get("status").getAsString());
    }

    @Test
    void recallLlmMemoryReturnsAllItems() {
        memory.llmItems = List.of("a", "b");

        JsonObject result = new RecallFunction().handle("recall", params("scope", "llm_memory"), "");

        assertEquals(2, result.getAsJsonArray("items").size());
        assertEquals("a", result.getAsJsonArray("items").get(0).getAsString());
    }

    @Test
    void recallTopicMemoryReturnsEntryContents() {
        memory.topicEntries = List.of(new MemoryEntry(Instant.now(), ConversationTopic.NAVIGATION,
                MemorySource.COMMANDER, "jumped to Sol", MemoryProcessingState.PROCESSED));
        JsonObject p = new JsonObject();
        p.addProperty("scope", "topic_memory");
        p.addProperty("topic", "navigation");

        JsonObject result = new RecallFunction().handle("recall", p, "");

        assertEquals(ConversationTopic.NAVIGATION, memory.recalledTopic);
        assertEquals("jumped to Sol", result.getAsJsonArray("items").get(0).getAsString());
    }

    @Test
    void recallUnknownTopicReportsError() {
        JsonObject p = new JsonObject();
        p.addProperty("scope", "topic_memory");
        p.addProperty("topic", "does_not_exist");

        JsonObject result = new RecallFunction().handle("recall", p, "");

        assertEquals("unknown topic", result.get("error").getAsString());
    }

    @Test
    void nothingToDoIsNoOp() {
        assertNull(new NothingToDoFunction().handle("nothing_to_do", new JsonObject(), ""));
    }

    @Test
    void deferredFunctionsFailLoudly() {
        assertThrows(UnsupportedOperationException.class,
                () -> new SetTopicFunction().handle("set_topic", params("topic", "navigation"), ""));
        assertThrows(UnsupportedOperationException.class,
                () -> new ChangeVerbosityFunction().handle("change_verbosity", params("verbosity", "quiet"), ""));
        assertThrows(UnsupportedOperationException.class,
                () -> new FindActionFunction().handle("find_action", params("query", "lower gear"), ""));
    }

    /** Minimal MemoryGateway fake recording the calls the system functions make. */
    private static final class RecordingMemory implements MemoryGateway {
        String remembered;
        List<String> llmItems = List.of();
        List<MemoryEntry> topicEntries = List.of();
        ConversationTopic recalledTopic;

        @Override public void write(MemoryEntry entry) { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> readShortTermTimeline() { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit) {
            recalledTopic = topic;
            return topicEntries;
        }
        @Override public List<String> readLlmMemory() { return llmItems; }
        @Override public void writeLlmMemory(String content) { remembered = content; }
        @Override public MemoryAvailabilitySnapshot indexes() { throw new UnsupportedOperationException(); }
        @Override public String longTermSummary() { throw new UnsupportedOperationException(); }
        @Override public void replaceLongTermSummary(String summary) { throw new UnsupportedOperationException(); }
    }
}
