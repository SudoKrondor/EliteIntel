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
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.llm.*;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.tools.IntelActionTypeResolver;
import elite.intel.companion.tools.IntelActionTypeResolver.IntelActionType;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.prompt.PromptComposer;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.companion.tools.ChangeGlobalTopicFunction;
import elite.intel.companion.tools.NothingToDoFunction;
import elite.intel.companion.tools.SpeakFunction;
import elite.intel.companion.tools.SystemFunctionProvider;
import elite.intel.gameapi.journal.events.BaseEvent;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

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

    private ThoughtContext ctx(IntelActionTypeResolver resolver) {
        return new ThoughtContext(llm, speech, execution, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                reducer, state, dangerousPolicy, coordinator, resolver);
    }

    @Test
    void commanderSpeaksThenEndsInOneRound() {
        llm.scripted.add(ok(call(SpeakFunction.ID, text("on it")), call(NothingToDoFunction.ID, new JsonObject())));

        Thought.commander(Urgency.NORMAL, "set speed to 50", ctx()).run();

        assertEquals(1, llm.requests.size(), "nothing_to_do ends the turn; no extra LLM round");
        assertEquals(List.of(SpeakFunction.ID), execution.toolNames(), "only speak is executed; nothing_to_do is not");
        // memory: commander input under the global topic, then the companion's own spoken words (not an ack).
        assertEquals(2, memory.writes.size());
        MemoryEntry input = memory.writes.get(0);
        assertEquals(MemorySource.COMMANDER, input.source());
        assertEquals(ConversationTopic.SOCIAL, input.topic());
        assertEquals("set speed to 50", input.content());
        MemoryEntry spoken = memory.writes.get(1);
        assertEquals(MemorySource.COMPANION, spoken.source(), "the companion's reply is recorded as COMPANION");
        assertEquals("on it", spoken.content(), "the spoken words are recorded, not a {status:spoken} ack");
        assertEquals(ConversationTopic.SOCIAL, spoken.topic());
    }

    /**
     * Action-type stub: close_panel and ship_status are commands, everything else a system function.
     * close_panel returns no spoken text (a side-effect command); ship_status returns an outcome.
     */
    private static IntelActionTypeResolver actionTypes() {
        return new IntelActionTypeResolver(id -> switch (id) {
            case "close_panel", "ship_status" -> IntelActionType.COMMAND;
            default -> IntelActionType.SYSTEM;
        });
    }

    @Test
    void commanderSilentCommandTurnDropsSpeak() {
        llm.scripted.add(ok(call("close_panel", new JsonObject()),
                call(SpeakFunction.ID, text("closing the panel")),
                call(NothingToDoFunction.ID, new JsonObject())));

        Thought.commander(Urgency.NORMAL, "close the panel", ctx(actionTypes())).run();

        assertEquals(List.of("close_panel"), execution.toolNames(),
                "silent command runs; the co-occurring speak is withheld (never executed)");
        assertTrue(memory.writes.stream().noneMatch(e -> e.content().contains("narration_suppressed")),
                "the withheld speak said nothing, so it leaves no narration_suppressed noise in memory");
        assertEquals(1, speech.requests.size(), "LLM-selected commands are acknowledged immediately before execution");
        assertFalse(speech.requests.get(0).text().isBlank(), "the immediate command ack is a spoken phrase");
        assertTrue(memory.writes.stream().noneMatch(e -> e.source() == MemorySource.COMPANION),
                "the immediate command ack is not recorded as a COMPANION line");
    }

    @Test
    void commanderQueryOutcomeVocalizedDeterministicallyAndLlmSpeakDropped() {
        // A query owns its spoken outcome: its text_to_speech_response is voiced verbatim through the speech
        // gateway, and the LLM's own speak for the same turn is dropped (never re-voiced or rephrased).
        execution.resultsByTool.put("ship_status", outcomeText("hull at 100 percent"));
        llm.scripted.add(ok(call("ship_status", new JsonObject()),
                call(SpeakFunction.ID, text("let me check the ship")),
                call(NothingToDoFunction.ID, new JsonObject())));

        Thought.commander(Urgency.NORMAL, "how is the ship", ctx(actionTypes())).run();

        assertEquals(2, speech.requests.size(),
                "the command is acknowledged immediately, then its outcome text is vocalized deterministically");
        assertFalse(speech.requests.get(0).text().isBlank(), "the first voice is the immediate command ack");
        assertEquals("hull at 100 percent", speech.requests.get(1).text());
        assertFalse(execution.toolNames().contains(SpeakFunction.ID),
                "the LLM's own speak is withheld once a command/query owns the spoken outcome");
    }

    @Test
    void commanderCommandTextResultIsVoicedAndRecordedSynchronously() {
        // Fire-and-forget reverted: the command runs in-thread; its deterministic outcome is voiced and the
        // real result is recorded (and fed back into the flow for the LLM to chain on).
        execution.resultsByTool.put("ship_status", outcomeText("hull at 100 percent"));
        llm.scripted.add(ok(call("ship_status", new JsonObject()), call(NothingToDoFunction.ID, new JsonObject())));

        Thought.commander(Urgency.NORMAL, "how is the ship", ctx(actionTypes())).run();

        assertEquals(2, speech.requests.size(),
                "the immediate ack is voiced before execution; the deterministic outcome is voiced in-thread");
        assertFalse(speech.requests.get(0).text().isBlank(), "the first voice is the immediate command ack");
        assertEquals("hull at 100 percent", speech.requests.get(1).text());
        assertTrue(memory.writes.stream().anyMatch(e -> e.source() == MemorySource.TOOL_RESULT
                        && e.content().contains("hull at 100 percent")),
                "the command result is recorded synchronously");
    }

    @Test
    void reflexExecutesCommandVoicesOutcomeAndRemembersWithoutLlm() {
        // A reflex runs the resolved command directly - no LLM round - and voices/remembers its outcome
        // through the same per-type handling as the full loop.
        execution.resultsByTool.put("ship_status", outcomeText("hull at 100 percent"));

        Thought.reflex(Urgency.NORMAL, "how is the ship", "ship_status", ctx(actionTypes())).run();

        assertTrue(llm.requests.isEmpty(), "a reflex never engages the LLM");
        assertEquals(List.of("ship_status"), execution.toolNames(), "the resolved command is executed directly");
        assertEquals(List.of("hull at 100 percent"), speech.requests.stream().map(SpeechRequest::text).toList(),
                "the command's outcome is voiced");
        assertEquals(2, memory.writes.size());
        MemoryEntry input = memory.writes.get(0);
        assertEquals(MemorySource.COMMANDER, input.source());
        assertEquals("how is the ship", input.content());
        MemoryEntry outcome = memory.writes.get(1);
        assertEquals(MemorySource.TOOL_RESULT, outcome.source());
        assertEquals("command ship_status executed: hull at 100 percent", outcome.content());
    }

    @Test
    void reflexSilentCommandRemembersExecutionWithoutAck() {
        // Command handlers are self-narrating after the command-outcome revert: a blank command result is
        // remembered, but the companion must not add a second affirmative voice.
        Thought.reflex(Urgency.NORMAL, "close the panel", "close_panel", ctx(actionTypes())).run();

        assertTrue(llm.requests.isEmpty());
        assertEquals(List.of("close_panel"), execution.toolNames());
        assertTrue(speech.requests.isEmpty(), "silent self-narrating commands are not acknowledged by companion");
        assertEquals(2, memory.writes.size());
        assertEquals("close the panel", memory.writes.get(0).content());
        assertEquals(MemorySource.TOOL_RESULT, memory.writes.get(1).source());
        assertEquals("command close_panel executed", memory.writes.get(1).content());
    }

    @Test
    void commanderQueryAnswerIsVoicedAndRememberedAsCompanionLine() {
        IntelActionTypeResolver asQuery = new IntelActionTypeResolver(
                id -> "scan_system".equals(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);
        execution.resultsByTool.put("scan_system", outcomeText("two stars and a gas giant"));
        llm.scripted.add(ok(call("scan_system", new JsonObject()), call(NothingToDoFunction.ID, new JsonObject())));

        Thought.commander(Urgency.NORMAL, "scan the system", ctx(asQuery)).run();

        assertEquals(List.of("two stars and a gas giant"),
                speech.requests.stream().map(SpeechRequest::text).toList(), "the query answer is voiced");
        MemoryEntry answer = memory.writes.get(memory.writes.size() - 1);
        assertEquals(MemorySource.COMPANION, answer.source(), "the answer is the companion's own remembered line");
        assertEquals("two stars and a gas giant", answer.content());
    }

    @Test
    void commanderMissionCriticalOutcomeVocalizedOnUrgentChannel() {
        // A mission-critical command outcome (e.g. a plotted trade-stop instruction) is voiced on the
        // urgent channel so it preempts current speech, exactly as the legacy MissionCritical channel did.
        JsonObject critical = outcomeText("travel to Sol and buy gold");
        critical.addProperty("mission_critical", true);
        execution.resultsByTool.put("close_panel", critical); // a command stub (NARRATABLE) carrying a critical outcome
        llm.scripted.add(ok(call("close_panel", new JsonObject()), call(NothingToDoFunction.ID, new JsonObject())));

        Thought.commander(Urgency.NORMAL, "next trade stop", ctx(actionTypes())).run();

        assertEquals(2, speech.requests.size());
        assertFalse(speech.requests.get(0).text().isBlank(), "the first voice is the immediate command ack");
        assertEquals(Urgency.NORMAL, speech.requests.get(0).urgency(), "immediate command ack is normal priority");
        assertEquals("travel to Sol and buy gold", speech.requests.get(1).text());
        assertEquals(Urgency.URGENT, speech.requests.get(1).urgency(), "mission-critical outcome preempts");
    }

    @Test
    void commanderTrailingSpeakRoundIsSuppressedForSilentTurn() {
        // The silent command runs in round 0; the LLM speaks only in round 1. Turn-level accounting must
        // still suppress that trailing speak.
        llm.scripted.add(ok(call("close_panel", new JsonObject())));
        llm.scripted.add(ok(call(SpeakFunction.ID, text("done")), call(NothingToDoFunction.ID, new JsonObject())));

        Thought.commander(Urgency.NORMAL, "close the panel", ctx(actionTypes())).run();

        assertEquals(List.of("close_panel"), execution.toolNames(),
                "speak emitted in a later round of a silent-only turn is still withheld");
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
    void highEventThoughtRecordsMemoryWithoutEngagingLlm() {
        // HIGH importance: recorded to memory under its static topic and ends - no LLM, no speech, no tools
        // (spontaneous event speech belongs to NarrationThought now).
        Thought.event(Urgency.NORMAL, "jumped to Sol", ConversationTopic.NAVIGATION,
                BaseEvent.Importance.HIGH, ctx()).run();

        assertTrue(llm.requests.isEmpty(), "EVENT thought must not engage the LLM");
        assertTrue(speech.requests.isEmpty(), "EVENT thought never speaks");
        assertEquals(1, memory.writes.size(), "the HIGH event is recorded once");
        MemoryEntry input = memory.writes.get(0);
        assertEquals(MemorySource.EVENT, input.source());
        assertEquals(ConversationTopic.NAVIGATION, input.topic(), "event memory tag comes from the event topic");
        assertEquals("jumped to Sol", input.content());
    }

    @Test
    void normalEventThoughtIsDroppedAndNotRecorded() {
        // NORMAL importance: dropped entirely - not recorded (would clutter the timeline), no LLM, no speech.
        Thought.event(Urgency.NORMAL, "docked at station", ConversationTopic.NAVIGATION,
                BaseEvent.Importance.NORMAL, ctx()).run();

        assertTrue(llm.requests.isEmpty(), "NORMAL event must not engage the LLM");
        assertTrue(speech.requests.isEmpty(), "NORMAL event is never spoken");
        assertTrue(memory.writes.isEmpty(), "NORMAL event is not retained in memory");
    }

    @Test
    void narrationThoughtSpeaksAndRecordsOnlyTheSpokenLine() {
        // One short round: phrase the sensor data, voice it, remember only the spoken line (no raw data).
        llm.scripted.add(ok(call(SpeakFunction.ID, text("Fuel is running low, Commander.")),
                call(NothingToDoFunction.ID, new JsonObject())));

        Thought.sensorNarration(Urgency.URGENT, "fuel reserve 12%", ConversationTopic.NAVIGATION, ctx()).run();

        assertEquals(1, llm.requests.size(), "narration is a single short round");
        assertEquals(List.of(SpeakFunction.ID), execution.toolNames(), "the phrased line is voiced via speak");
        assertEquals(1, memory.writes.size(), "only the spoken line is recorded - the raw sensor data is not");
        MemoryEntry spoken = memory.writes.get(0);
        assertEquals(MemorySource.COMPANION, spoken.source());
        assertEquals("Fuel is running low, Commander.", spoken.content());
        assertEquals(ConversationTopic.NAVIGATION, spoken.topic());
    }

    @Test
    void verbatimNarrationRecordsThenVoicesTheLineWithoutLlm() {
        // A curated announcement already carries finished text: remember it, then voice it verbatim, no LLM.
        Thought.verbatimNarration(Urgency.URGENT, "Target material detected, Commander.",
                ConversationTopic.MINING, ctx()).run();

        assertTrue(llm.requests.isEmpty(), "verbatim narration never engages the LLM");
        assertEquals(1, memory.writes.size(), "the curated line is remembered as the companion's words");
        MemoryEntry spoken = memory.writes.get(0);
        assertEquals(MemorySource.COMPANION, spoken.source());
        assertEquals(ConversationTopic.MINING, spoken.topic());
        assertEquals("Target material detected, Commander.", spoken.content());
        assertEquals(List.of("Target material detected, Commander."),
                speech.requests.stream().map(SpeechRequest::text).toList(), "and voiced verbatim");
    }

    @Test
    void commanderInvalidResponseRecordsUnresolvedAndSpeaks() {
        llm.scripted.add(invalid());

        Thought.commander(Urgency.NORMAL, "do the thing", ctx()).run();

        assertEquals(1, memory.writes.size());
        MemoryEntry entry = memory.writes.get(0);
        assertEquals(ConversationTopic.UNRESOLVED_COMMANDER_INPUT, entry.topic());
        assertEquals(1, speech.requests.size(), "commander hears a service phrase");
        assertNotNull(speech.requests.get(0).text());
        assertFalse(speech.requests.get(0).text().isBlank());
        assertTrue(execution.toolNames().isEmpty());
    }

    @Test
    void providerFailureIsTreatedAsInvalid() {
        llm.failWith = new RuntimeException("provider down");

        Thought.commander(Urgency.NORMAL, "anything", ctx()).run();

        assertEquals(ConversationTopic.UNRESOLVED_COMMANDER_INPUT, memory.writes.get(0).topic());
        assertEquals(1, speech.requests.size());
    }

    @Test
    void dangerousActionWaitsForConfirmationThenExecutesOnConfirm() throws InterruptedException {
        // The model is unaware of danger: it just calls the action. The thought voices the confirmation itself.
        dangerousPolicy = invocation -> "self_destruct".equals(invocation.name());
        llm.scripted.add(ok(call("self_destruct", new JsonObject())));

        runResolving(Thought.commander(Urgency.NORMAL, "self destruct", ctx()), coordinator::confirm);

        assertTrue(execution.toolNames().contains("self_destruct"), "dangerous action runs only after confirm");
        assertFalse(speech.requests.isEmpty(), "the thought voices a confirmation prompt before running it");
        assertTrue(hasContent("dangerous action requires confirmation"));
        assertTrue(hasContent("dangerous action confirmed"));
    }

    @Test
    void dangerousActionIsDiscardedOnCancel() throws InterruptedException {
        dangerousPolicy = invocation -> "self_destruct".equals(invocation.name());
        llm.scripted.add(ok(call("self_destruct", new JsonObject())));

        runResolving(Thought.commander(Urgency.NORMAL, "self destruct", ctx()), coordinator::cancel);

        assertFalse(execution.toolNames().contains("self_destruct"), "cancelled dangerous action must not run");
        assertTrue(hasContent("dangerous action cancelled"));
    }

    @Test
    void overlappingConfirmationIsRefused() {
        dangerousPolicy = invocation -> "self_destruct".equals(invocation.name());
        coordinator.open(); // occupy the single confirmation slot
        llm.scripted.add(ok(call("self_destruct", new JsonObject())));

        Thought.commander(Urgency.NORMAL, "self destruct", ctx()).run(); // open() returns null -> no blocking

        assertFalse(execution.toolNames().contains("self_destruct"));
        assertTrue(hasContent("dangerous action cancelled"));
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
    }

    // --- helpers ---

    private boolean hasContent(String content) {
        return memory.writes.stream().anyMatch(e -> content.equals(e.content()));
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
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

    /**
     * A command/query outcome carrying a spoken text_to_speech_response, as a handler's handle() returns.
     */
    private static JsonObject outcomeText(String value) {
        JsonObject o = new JsonObject();
        o.addProperty("text_to_speech_response", value);
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
