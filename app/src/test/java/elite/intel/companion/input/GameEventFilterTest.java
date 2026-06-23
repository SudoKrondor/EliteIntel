package elite.intel.companion.input;

import com.google.gson.JsonObject;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MemoryAvailabilitySnapshot;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.mind.ThoughtContext;
import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.prompt.PromptComposer;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.companion.tools.SystemFunctionProvider;
import elite.intel.gameapi.journal.events.BaseEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mechanical allow-list behaviour: only gameplay-taxonomy event types pass, the non-gameplay
 * {@code BaseEvent}s sharing the bus (notably {@code UserInput}) are rejected, and an accepted event is
 * routed to the dispatcher while a rejected one is dropped.
 */
class GameEventFilterTest {

    @Test
    void mappedGameplayEventIsAccepted() {
        GameEventFilter filter = new GameEventFilter(new ThoughtDispatcher(null));
        assertTrue(filter.accept(event("FSDJump")));
    }

    @Test
    void unmappedEventIsRejected() {
        GameEventFilter filter = new GameEventFilter(new ThoughtDispatcher(null));
        assertFalse(filter.accept(event("Music")));
    }

    @Test
    void userInputEventIsRejected() {
        // UserInput extends BaseEvent and shares the bus; it must not spawn an EVENT thought.
        GameEventFilter filter = new GameEventFilter(new ThoughtDispatcher(null));
        assertFalse(filter.accept(event("UserInput")));
    }

    @Test
    void nullEventIsRejected() {
        GameEventFilter filter = new GameEventFilter(new ThoughtDispatcher(null));
        assertFalse(filter.accept(null));
    }

    @Test
    void acceptedEventIsRoutedToTheDispatcher() {
        FakeMemory memory = new FakeMemory();
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctx(memory));
        GameEventFilter filter = new GameEventFilter(dispatcher);
        dispatcher.start();

        filter.onGameEvent(event("MarketSell"));
        dispatcher.stop();

        assertEquals(1, memory.writes.size());
        assertEquals(MemorySource.EVENT, memory.writes.get(0).source());
    }

    @Test
    void rejectedEventIsDropped() {
        FakeMemory memory = new FakeMemory();
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctx(memory));
        GameEventFilter filter = new GameEventFilter(dispatcher);
        dispatcher.start();

        filter.onGameEvent(event("Music"));
        dispatcher.stop();

        assertTrue(memory.writes.isEmpty());
    }

    @Test
    void rapidRepeatsOfSameTypeAreDropped() {
        FakeMemory memory = new FakeMemory();
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctx(memory));
        AtomicLong now = new AtomicLong(0);
        GameEventFilter filter = new GameEventFilter(dispatcher, now::get);
        dispatcher.start();

        filter.onGameEvent(event("Cargo")); // first occurrence -> forwarded
        now.set(1000);
        filter.onGameEvent(event("Cargo")); // within cooldown -> dropped
        now.set(6000);
        filter.onGameEvent(event("Cargo")); // past cooldown -> forwarded
        dispatcher.stop();

        assertEquals(2, memory.writes.size());
    }

    @Test
    void differentTypesHaveIndependentCooldowns() {
        FakeMemory memory = new FakeMemory();
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctx(memory));
        AtomicLong now = new AtomicLong(0);
        GameEventFilter filter = new GameEventFilter(dispatcher, now::get);
        dispatcher.start();

        filter.onGameEvent(event("Cargo"));
        filter.onGameEvent(event("FSDJump")); // different type at the same instant -> also forwarded
        dispatcher.stop();

        assertEquals(2, memory.writes.size());
    }

    // --- helpers ---

    private static BaseEvent event(String type) {
        return new BaseEvent(Instant.now().toString(), Duration.ofMinutes(1), type) {
            @Override public String getEventType() { return type; }
            @Override public JsonObject toJsonObject() { return new JsonObject(); }
        };
    }

    private static ThoughtContext ctx(MemoryGateway memory) {
        return new ThoughtContext(
                new InvalidLlm(), new NoSpeech(), new NoExecution(), memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> List.of(), new CompanionState(),
                invocation -> false, new ConfirmationCoordinator());
    }

    /** Returns INVALID, so an EVENT thought records exactly one unresolved entry and stops (silently). */
    private static final class InvalidLlm implements LlmGateway {
        @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
            return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.INVALID_RESPONSE, List.of()));
        }

        @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class FakeMemory implements MemoryGateway {
        final List<MemoryEntry> writes = new CopyOnWriteArrayList<>();

        @Override public void write(MemoryEntry entry) { writes.add(entry); }
        @Override public List<MemoryEntry> readShortTermTimeline() { return List.of(); }
        @Override public List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit) { return List.of(); }
        @Override public List<String> readLlmMemory() { return List.of(); }
        @Override public void writeLlmMemory(String content) { }
        @Override public MemoryAvailabilitySnapshot indexes() { return new MemoryAvailabilitySnapshot(0, 15, List.of()); }
        @Override public String longTermSummary() { return ""; }
        @Override public void replaceLongTermSummary(String summary) { }
    }

    private static final class NoSpeech implements SpeechGateway {
        @Override public CompletableFuture<Void> submit(SpeechRequest request) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class NoExecution implements ExecutionGateway {
        @Override public CompletableFuture<JsonObject> submit(ExecutionRequest request) {
            return CompletableFuture.completedFuture(new JsonObject());
        }
    }
}
