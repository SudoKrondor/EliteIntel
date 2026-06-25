package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.confirm.DangerousActionPolicy;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MemoryAvailabilitySnapshot;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.Verbosity;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryProcessingState;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.prompt.PromptComposer;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.companion.tools.ChangeGlobalTopicFunction;
import elite.intel.companion.tools.NothingToDoFunction;
import elite.intel.companion.tools.SpeakFunction;
import elite.intel.companion.tools.SystemFunctionProvider;
import elite.intel.gameapi.journal.events.BaseEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The consciousness loop: the happy path (single round, multi-round tool round-trip), the
 * change_global_topic pre-execution step before the input is recorded, the EVENT memory tag with
 * query-only access and verbosity-gated speak, dangerous-action confirmation, interrupt/safe-flush, and
 * the INVALID/provider-failure handling per source (§2.5/§2.6/§2.8/§2.9/§2.13/§5.1). Real
 * {@link PromptComposer}/{@link IntelActionAccessPolicy}/{@link SystemFunctionProvider}; the gateways are
 * hand-written fakes.
 */
class ThoughtTest {

    private final FakeLlm llm = new FakeLlm();
    private final FakeSpeech speech = new FakeSpeech();
    private final FakeExecution execution = new FakeExecution();
    private final FakeMemory memory = new FakeMemory();
    private final RecordingReducer reducer = new RecordingReducer();
    private final CompanionState state = new CompanionState();
    private DangerousActionPolicy dangerousPolicy = invocation -> false;
    private final ConfirmationCoordinator coordinator = new ConfirmationCoordinator();

    private ThoughtContext ctx() {
        return new ThoughtContext(llm, speech, execution, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                reducer, state, dangerousPolicy, coordinator);
    }

    @Test
    void commanderSpeaksThenEndsInOneRound() {
        llm.scripted.add(ok(call(SpeakFunction.ID, text("on it")), call(NothingToDoFunction.ID, new JsonObject())));

        Thought.commander(Urgency.NORMAL, "set speed to 50", ctx()).run();

        assertEquals(1, llm.requests.size(), "nothing_to_do ends the turn; no extra LLM round");
        assertEquals(List.of(SpeakFunction.ID), execution.toolNames(), "only speak is executed; nothing_to_do is not");
        // memory: commander input under the global topic, then the speak tool result.
        assertEquals(2, memory.writes.size());
        MemoryEntry input = memory.writes.get(0);
        assertEquals(MemorySource.COMMANDER, input.source());
        assertEquals(ConversationTopic.SOCIAL, input.topic());
        assertEquals(MemoryProcessingState.PROCESSED, input.processingState());
        assertEquals("set speed to 50", input.content());
        assertEquals(MemorySource.TOOL_RESULT, memory.writes.get(1).source());
    }

    @Test
    void multiRoundReplaysAssistantCallAndToolResult() {
        llm.scripted.add(ok(call("ship_status", new JsonObject())));
        llm.scripted.add(ok(call(NothingToDoFunction.ID, new JsonObject())));

        Thought.commander(Urgency.NORMAL, "how is the ship", ctx()).run();

        assertEquals(2, llm.requests.size(), "a non-terminating round triggers another LLM round");
        // The second request's flow must carry the protocol-valid assistant(tool_calls) -> tool(result) pair.
        List<LlmMessage> secondFlow = llm.requests.get(1).messages();
        assertTrue(secondFlow.stream().anyMatch(m -> m.role() == LlmMessageRole.ASSISTANT && !m.toolCalls().isEmpty()),
                "assistant tool-call turn must be replayed");
        assertTrue(secondFlow.stream().anyMatch(m -> m.role() == LlmMessageRole.TOOL && m.toolCallId() != null),
                "tool result must reference its tool_call_id");
        assertEquals(List.of("ship_status"), execution.toolNames());
    }

    @Test
    void changeGlobalTopicAppliedBeforeInputIsRecorded() {
        execution.stateToMutate = state; // the fake mirrors the change_global_topic handle effect
        llm.scripted.add(ok(call(ChangeGlobalTopicFunction.ID, topicArgs("navigation")),
                call(NothingToDoFunction.ID, new JsonObject())));

        Thought.commander(Urgency.NORMAL, "let's talk routes", ctx()).run();

        assertEquals(ConversationTopic.NAVIGATION, state.globalTopic());
        // The recorded commander input is tagged with the NEW topic, not the default SOCIAL.
        assertEquals(ConversationTopic.NAVIGATION, memory.writes.get(0).topic());
        assertEquals(1, execution.toolNames().stream().filter(ChangeGlobalTopicFunction.ID::equals).count(),
                "change_global_topic runs once (pre-execution result reused, not run twice)");
    }

    @Test
    void eventThoughtIsQueryOnlyAndTaggedFromEventTopic() {
        llm.scripted.add(ok(call(NothingToDoFunction.ID, new JsonObject())));

        Thought.event(Urgency.NORMAL, "jumped to Sol", ConversationTopic.NAVIGATION, BaseEvent.Importance.HIGH, ctx()).run();

        assertEquals(EnumSet.of(IntelActionCategory.QUERY), reducer.lastCategories,
                "EVENT thought is offered only QUERY game tools");
        MemoryEntry input = memory.writes.get(0);
        assertEquals(MemorySource.EVENT, input.source());
        assertEquals(ConversationTopic.NAVIGATION, input.topic(), "event memory tag comes from the event topic");
        assertTrue(speech.requests.isEmpty());
    }

    @Test
    void normalEventThoughtRecordsMemoryWithoutEngagingLlm() {
        // NORMAL importance: the thought records the event to memory and ends - no LLM round, no speech.
        Thought.event(Urgency.NORMAL, "docked at station", ConversationTopic.NAVIGATION,
                BaseEvent.Importance.NORMAL, ctx()).run();

        assertTrue(llm.requests.isEmpty(), "NORMAL event must not engage the LLM");
        assertTrue(speech.requests.isEmpty(), "NORMAL event is never spoken");
        assertEquals(1, memory.writes.size(), "NORMAL event is recorded once");
        MemoryEntry input = memory.writes.get(0);
        assertEquals(MemorySource.EVENT, input.source());
        assertEquals(ConversationTopic.NAVIGATION, input.topic());
        assertEquals(MemoryProcessingState.PROCESSED, input.processingState());
        assertEquals("docked at station", input.content());
    }

    @Test
    void commanderInvalidResponseRecordsUnresolvedAndSpeaks() {
        llm.scripted.add(invalid());

        Thought.commander(Urgency.NORMAL, "do the thing", ctx()).run();

        assertEquals(1, memory.writes.size());
        MemoryEntry entry = memory.writes.get(0);
        assertEquals(ConversationTopic.UNRESOLVED_COMMANDER_INPUT, entry.topic());
        assertEquals(MemoryProcessingState.UNRESOLVED, entry.processingState());
        assertEquals(1, speech.requests.size(), "commander hears a service phrase");
        assertNotNull(speech.requests.get(0).text());
        assertFalse(speech.requests.get(0).text().isBlank());
        assertTrue(execution.toolNames().isEmpty());
    }

    @Test
    void eventInvalidResponseRecordsUnresolvedSilently() {
        llm.scripted.add(invalid());

        Thought.event(Urgency.NORMAL, "scanned by ship", ConversationTopic.COMBAT, BaseEvent.Importance.HIGH, ctx()).run();

        assertEquals(ConversationTopic.UNRESOLVED_GAME_EVENT, memory.writes.get(0).topic());
        assertEquals(MemoryProcessingState.UNRESOLVED, memory.writes.get(0).processingState());
        assertTrue(speech.requests.isEmpty(), "event thought ends silently");
    }

    @Test
    void providerFailureIsTreatedAsInvalid() {
        llm.failWith = new RuntimeException("provider down");

        Thought.commander(Urgency.NORMAL, "anything", ctx()).run();

        assertEquals(MemoryProcessingState.UNRESOLVED, memory.writes.get(0).processingState());
        assertEquals(1, speech.requests.size());
    }

    @Test
    void dangerousActionWaitsForConfirmationThenExecutesOnConfirm() throws InterruptedException {
        dangerousPolicy = invocation -> "self_destruct".equals(invocation.name());
        llm.scripted.add(ok(call("self_destruct", new JsonObject()),
                call(SpeakFunction.ID, confirmationRequest("Confirm self destruct?"))));

        runResolving(Thought.commander(Urgency.NORMAL, "self destruct", ctx()), coordinator::confirm);

        assertTrue(execution.toolNames().contains("self_destruct"), "dangerous action runs only after confirm");
        assertTrue(hasState(MemoryProcessingState.AWAITING_CONFIRMATION));
        assertTrue(hasState(MemoryProcessingState.CONFIRMED));
    }

    @Test
    void dangerousActionIsDiscardedOnCancel() throws InterruptedException {
        dangerousPolicy = invocation -> "self_destruct".equals(invocation.name());
        llm.scripted.add(ok(call("self_destruct", new JsonObject()),
                call(SpeakFunction.ID, confirmationRequest("Confirm self destruct?"))));

        runResolving(Thought.commander(Urgency.NORMAL, "self destruct", ctx()), coordinator::cancel);

        assertFalse(execution.toolNames().contains("self_destruct"), "cancelled dangerous action must not run");
        assertTrue(hasState(MemoryProcessingState.CANCELLED));
    }

    @Test
    void overlappingConfirmationIsRefused() {
        dangerousPolicy = invocation -> "self_destruct".equals(invocation.name());
        coordinator.open(); // occupy the single confirmation slot
        llm.scripted.add(ok(call("self_destruct", new JsonObject()),
                call(SpeakFunction.ID, confirmationRequest("Confirm?"))));

        Thought.commander(Urgency.NORMAL, "self destruct", ctx()).run(); // open() returns null -> no blocking

        assertFalse(execution.toolNames().contains("self_destruct"));
        assertTrue(hasState(MemoryProcessingState.CANCELLED));
    }

    @Test
    void interruptWhileWaitingOnLlmSafeFlushesUnresolvedInput() throws InterruptedException {
        llm.blockForever = true; // the thought will block on the LLM future until interrupted
        Thought thought = Thought.commander(Urgency.NORMAL, "do something", ctx());
        Thread worker = new Thread(thought::run, "thought-test");
        worker.start();
        waitUntil(() -> !llm.requests.isEmpty()); // it has submitted and is now blocked
        thought.interrupt();
        worker.join(2000);

        assertFalse(worker.isAlive(), "interrupted thought must die");
        assertEquals(1, memory.writes.size(), "safe-flush must not leave a memory hole");
        MemoryEntry flushed = memory.writes.get(0);
        assertEquals(MemorySource.COMMANDER, flushed.source());
        assertEquals(ConversationTopic.UNRESOLVED_COMMANDER_INPUT, flushed.topic());
        assertEquals(MemoryProcessingState.INTERRUPTED, flushed.processingState());
    }

    @Test
    void quietEventThoughtIsNotOfferedSpeak() {
        state.setVerbosity(Verbosity.QUIET);
        llm.scripted.add(ok(call(NothingToDoFunction.ID, new JsonObject())));

        Thought.event(Urgency.NORMAL, "jumped to Sol", ConversationTopic.NAVIGATION, BaseEvent.Importance.HIGH, ctx()).run();

        assertFalse(offeredTool(SpeakFunction.ID), "QUIET non-urgent event must not be offered speak");
    }

    @Test
    void chattyEventThoughtIsOfferedSpeak() {
        state.setVerbosity(Verbosity.CHATTY);
        llm.scripted.add(ok(call(NothingToDoFunction.ID, new JsonObject())));

        Thought.event(Urgency.NORMAL, "jumped to Sol", ConversationTopic.NAVIGATION, BaseEvent.Importance.HIGH, ctx()).run();

        assertTrue(offeredTool(SpeakFunction.ID), "CHATTY event may comment");
    }

    // --- helpers ---

    private boolean offeredTool(String name) {
        return llm.requests.get(0).tools().stream().anyMatch(tool -> name.equals(tool.name()));
    }

    private boolean hasState(MemoryProcessingState state) {
        return memory.writes.stream().anyMatch(e -> e.processingState() == state);
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
    }

    private static JsonObject confirmationRequest(String text) {
        JsonObject o = new JsonObject();
        o.addProperty("text", text);
        o.addProperty("confirmation_request", true);
        return o;
    }

    /** Runs the thought on a worker, nudging the resolver (confirm/cancel) until it finishes. */
    private static void runResolving(Thought thought, Runnable resolve) throws InterruptedException {
        Thread worker = new Thread(thought::run, "thought-test");
        worker.start();
        long deadline = System.currentTimeMillis() + 5000;
        while (worker.isAlive() && System.currentTimeMillis() < deadline) {
            resolve.run();
            Thread.sleep(10);
        }
        worker.join(2000);
        assertFalse(worker.isAlive(), "thought did not finish");
    }

    private static LlmResult ok(LlmToolInvocation... calls) {
        return new LlmResult(LlmResult.Status.OK, List.of(calls));
    }

    private static LlmResult invalid() {
        return new LlmResult(LlmResult.Status.INVALID_RESPONSE, List.of());
    }

    private static LlmToolInvocation call(String name, JsonObject args) {
        return new LlmToolInvocation(UUID.randomUUID().toString(), name, args);
    }

    private static JsonObject text(String value) {
        JsonObject o = new JsonObject();
        o.addProperty("text", value);
        return o;
    }

    private static JsonObject topicArgs(String topic) {
        JsonObject o = new JsonObject();
        o.addProperty("topic", topic);
        return o;
    }

    // --- fakes ---

    private static final class FakeLlm implements LlmGateway {
        final Deque<LlmResult> scripted = new ConcurrentLinkedDeque<>();
        final List<LlmRequest> requests = new CopyOnWriteArrayList<>();
        RuntimeException failWith;
        volatile boolean blockForever;

        @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
            requests.add(request);
            if (failWith != null) {
                return CompletableFuture.failedFuture(failWith);
            }
            if (blockForever) {
                return new CompletableFuture<>(); // never completes; only interrupt (cancel) unblocks it
            }
            return CompletableFuture.completedFuture(scripted.poll());
        }

        @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class FakeExecution implements ExecutionGateway {
        final List<ExecutionRequest> requests = new ArrayList<>();
        final Map<String, JsonObject> resultsByTool = new HashMap<>();
        CompanionState stateToMutate;

        @Override public CompletableFuture<JsonObject> submit(ExecutionRequest request) {
            requests.add(request);
            if (stateToMutate != null && ChangeGlobalTopicFunction.ID.equals(request.toolName())) {
                ConversationTopic topic = ConversationTopic.fromSelectableId(
                        request.arguments().get("topic").getAsString());
                if (topic != null) {
                    stateToMutate.setGlobalTopic(topic);
                }
            }
            return CompletableFuture.completedFuture(resultsByTool.getOrDefault(request.toolName(), new JsonObject()));
        }

        List<String> toolNames() {
            return requests.stream().map(ExecutionRequest::toolName).toList();
        }
    }

    private static final class FakeSpeech implements SpeechGateway {
        final List<SpeechRequest> requests = new ArrayList<>();

        @Override public CompletableFuture<Void> submit(SpeechRequest request) {
            requests.add(request);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class FakeMemory implements MemoryGateway {
        final List<MemoryEntry> writes = new ArrayList<>();

        @Override public void write(MemoryEntry entry) { writes.add(entry); }
        @Override public List<MemoryEntry> readShortTermTimeline() { return List.of(); }
        @Override public List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit) { return List.of(); }
        @Override public List<String> recallMatching(String query, int limit) { return List.of(); }
        @Override public List<String> readLlmMemory() { return List.of(); }
        @Override public void writeLlmMemory(String content) { }
        @Override public MemoryAvailabilitySnapshot indexes() { return new MemoryAvailabilitySnapshot(0, 15, List.of()); }
        @Override public String longTermSummary() { return ""; }
        @Override public void replaceLongTermSummary(String summary) { }
    }

    private static final class RecordingReducer implements CompanionActionReducer {
        Set<IntelActionCategory> lastCategories;

        @Override public List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories, String currentInput) {
            lastCategories = allowedCategories;
            return List.of();
        }
    }
}
