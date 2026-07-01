package elite.intel.companion.mind;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
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
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.tools.IntelActionTypeResolver;
import elite.intel.companion.tools.IntelActionTypeResolver.IntelActionType;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.prompt.PromptComposer;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.companion.tools.ClassifyTurnFunction;
import elite.intel.companion.tools.SearchInMemoryFunction;
import elite.intel.companion.tools.SpeakFunction;
import elite.intel.companion.tools.SystemFunctionProvider;
import elite.intel.eventbus.GameEventBus;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The consciousness loop: the happy path (single round, multi-round tool round-trip), the
 * classify_turn pre-execution step before the input is recorded, the EVENT memory tag with
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
        llm.scripted.add(ok(call(SpeakFunction.ID, text("on it"))));

        Thought.commander(Urgency.NORMAL, "set speed to 50", ctx()).run();

        assertEquals(1, llm.requests.size(), "speak settles the turn; no extra LLM round");
        assertEquals(List.of(SpeakFunction.ID), execution.toolNames(), "only speak is executed");
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
                call(SpeakFunction.ID, text("closing the panel"))));

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
    void commanderCommandWithholdsLlmSpeakAndRecordsExecution() {
        // A command self-narrates its own outcome (via the handler's events, owned by the bridge), so the
        // LLM's own speak for the same turn is dropped; only the immediate ack is voiced here, and the
        // execution is recorded. An IntelCommand returns no text_to_speech_response, so nothing is voiced
        // inline for the outcome.
        execution.resultsByTool.put("ship_status", new JsonObject());
        llm.scripted.add(ok(call("ship_status", new JsonObject()),
                call(SpeakFunction.ID, text("let me check the ship"))));

        Thought.commander(Urgency.NORMAL, "how is the ship", ctx(actionTypes())).run();

        assertEquals(1, speech.requests.size(),
                "only the immediate command ack is voiced; the command self-narrates the rest");
        assertFalse(speech.requests.get(0).text().isBlank(), "the voice is the immediate command ack");
        assertFalse(execution.toolNames().contains(SpeakFunction.ID),
                "the LLM's own speak is withheld once a command owns the spoken outcome");
        assertTrue(memory.writes.stream().anyMatch(e -> e.source() == MemorySource.TOOL_RESULT
                        && e.content().contains("command ship_status executed")),
                "the command execution is recorded");
    }

    @Test
    void commandMemoryDropsExamplePhrasesFromToolDescription() {
        reducer.tools = List.of(new LlmToolDefinition("ship_status",
                "Read ship status. Example phrases: ship status, status report.", "", List.of()));
        execution.resultsByTool.put("ship_status", new JsonObject());
        llm.scripted.add(ok(call("ship_status", new JsonObject())));

        Thought.commander(Urgency.NORMAL, "how is the ship", ctx(actionTypes())).run();

        MemoryEntry outcome = memory.writes.stream()
                .filter(e -> e.source() == MemorySource.TOOL_RESULT)
                .findFirst()
                .orElseThrow();
        assertEquals("command ship_status executed: Read ship status.", outcome.content());
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
    void commanderQueryAnswerIsPublishedAsAiVoxForTheBridge() {
        // Self-narrating queries: the answer is published as an AiVoxResponseEvent (mirroring the legacy
        // router) for CompanionAnnouncementBridge to voice and remember; recordOutcome no longer voices it.
        IntelActionTypeResolver asQuery = new IntelActionTypeResolver(
                id -> "scan_system".equals(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);
        execution.resultsByTool.put("scan_system", outcomeText("two stars and a gas giant"));
        llm.scripted.add(ok(call("scan_system", new JsonObject())));

        List<String> voxTexts = new ArrayList<>();
        Object collector = new Object() {
            @Subscribe public void on(AiVoxResponseEvent e) { voxTexts.add(e.getText()); }
        };
        GameEventBus.register(collector);
        try {
            Thought.commander(Urgency.NORMAL, "scan the system", ctx(asQuery)).run();
        } finally {
            GameEventBus.unregister(collector);
        }

        assertEquals(List.of("two stars and a gas giant"), voxTexts,
                "the query answer is published as an AiVoxResponseEvent for the bridge to voice and remember");
        assertTrue(speech.requests.isEmpty(), "recordOutcome no longer voices the query directly");
        assertTrue(memory.writes.stream().noneMatch(e -> e.source() == MemorySource.COMPANION),
                "recordOutcome no longer records the query answer; the bridge owns that");
    }

    @Test
    void commanderCommandSettlesTurnInOneRound() {
        // A command is self-narrating and terminal: it settles the turn in its own round, so the scripted
        // second round is never reached (the companion turn is single-round by design).
        llm.scripted.add(ok(call("close_panel", new JsonObject())));
        llm.scripted.add(ok(call(SpeakFunction.ID, text("done"))));

        Thought.commander(Urgency.NORMAL, "close the panel", ctx(actionTypes())).run();

        assertEquals(1, llm.requests.size(),
                "the command settles the turn; the scripted second round is never requested");
        assertEquals(List.of("close_panel"), execution.toolNames(),
                "only the command runs; the later speak is never executed");
    }

    @Test
    void memoryLookupReplaysAssistantCallAndToolResultForOneMoreRound() {
        // search_in_memory is the one continuation: round 0 runs the lookup, round 1 speaks the recalled answer.
        llm.scripted.add(ok(call(SearchInMemoryFunction.ID, new JsonObject())));
        llm.scripted.add(ok(call(SpeakFunction.ID, text("the hull is solid"))));

        Thought.commander(Urgency.NORMAL, "what did I say about the hull", ctx()).run();

        assertEquals(2, llm.requests.size(), "a memory lookup triggers one more LLM round to speak the answer");
        // The second request's flow must carry the protocol-valid assistant(tool_calls) -> tool(result) pair.
        List<LlmMessage> secondFlow = llm.requests.get(1).messages();
        assertTrue(secondFlow.stream().anyMatch(m -> m.role() == LlmMessageRole.ASSISTANT && !m.toolCalls().isEmpty()),
                "assistant tool-call turn must be replayed");
        assertTrue(secondFlow.stream().anyMatch(m -> m.role() == LlmMessageRole.TOOL && m.toolCallId() != null),
                "tool result must reference its tool_call_id");
        assertEquals(List.of(SearchInMemoryFunction.ID, SpeakFunction.ID), execution.toolNames());
    }

    @Test
    void classifyTurnAppliedBeforeInputIsRecorded() {
        execution.stateToMutate = state; // the fake mirrors the classify_turn handle effect on the topic
        llm.scripted.add(ok(call(ClassifyTurnFunction.ID, classifyArgs("navigation", "high"))));

        Thought.commander(Urgency.NORMAL, "let's talk routes", ctx()).run();

        assertEquals(ConversationTopic.NAVIGATION, state.globalTopic());
        // The recorded commander input is tagged with the NEW topic (not the default SOCIAL) and stamped with
        // the chosen importance.
        assertEquals(ConversationTopic.NAVIGATION, memory.writes.get(0).topic());
        assertEquals(MemoryImportance.HIGH, memory.writes.get(0).importance());
        assertEquals(1, execution.toolNames().stream().filter(ClassifyTurnFunction.ID::equals).count(),
                "classify_turn runs once (pre-execution result reused, not run twice)");
    }

    @Test
    void questionTurnInputIsNotFiledButTheAnswerIs() {
        execution.stateToMutate = state; // the fake mirrors the classify_turn handle effect on the topic
        llm.scripted.add(ok(call(ClassifyTurnFunction.ID, classifyArgs("navigation", "normal", true)),
                call(SpeakFunction.ID, text("forty percent"))));

        Thought.commander(Urgency.NORMAL, "how much fuel is left", ctx()).run();

        // A question carries no new fact, so the commander's input is not filed; only the answer (which carries
        // the fact) is recorded, as the companion's own words.
        assertEquals(1, memory.writes.size(), "the question input is not filed, only the answer is");
        MemoryEntry spoken = memory.writes.get(0);
        assertEquals(MemorySource.COMPANION, spoken.source());
        assertEquals("forty percent", spoken.content());
    }

    @Test
    void eventThoughtWithSummaryRecordsMemoryWithoutEngagingLlm() {
        // An event that provides a readable summary is recorded under its static topic and ends - no LLM, no
        // speech, no tools (spontaneous event speech belongs to NarrationThought now).
        Thought.event(Urgency.NORMAL, "jumped to Sol", ConversationTopic.NAVIGATION, ctx()).run();

        assertTrue(llm.requests.isEmpty(), "EVENT thought must not engage the LLM");
        assertTrue(speech.requests.isEmpty(), "EVENT thought never speaks");
        assertEquals(1, memory.writes.size(), "the event summary is recorded once");
        MemoryEntry input = memory.writes.get(0);
        assertEquals(MemorySource.EVENT, input.source());
        assertEquals(ConversationTopic.NAVIGATION, input.topic(), "event memory tag comes from the event topic");
        assertEquals("jumped to Sol", input.content());
    }

    @Test
    void eventThoughtWithBlankSummaryRecordsNothing() {
        // No summary (the event opted out of being remembered): nothing is recorded, no LLM, no speech.
        Thought.event(Urgency.NORMAL, "", ConversationTopic.NAVIGATION, ctx()).run();

        assertTrue(llm.requests.isEmpty(), "EVENT thought must not engage the LLM");
        assertTrue(speech.requests.isEmpty(), "EVENT thought is never spoken");
        assertTrue(memory.writes.isEmpty(), "an event with no summary is not retained in memory");
    }

    @Test
    void narrationThoughtSpeaksAndRecordsOnlyTheSpokenLine() {
        // One short round: phrase the sensor data, voice it, remember only the spoken line (no raw data).
        llm.scripted.add(ok(call(SpeakFunction.ID, text("Fuel is running low, Commander."))));

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
    void verbatimNarrationCompletesSpokenSignalWhenPlaybackEnds() {
        // A bridged synchronous caller (e.g. a macro SPEAK step) blocks on this signal until playback finishes.
        CompletableFuture<Void> signal = new CompletableFuture<>();

        Thought.verbatimNarration(Urgency.URGENT, "Reminder set, Commander.",
                ConversationTopic.NAVIGATION, ctx(), signal).run();

        assertTrue(signal.isDone() && !signal.isCompletedExceptionally(),
                "the signal completes when the gateway reports playback finished");
    }

    @Test
    void verbatimNarrationCompletesSpokenSignalEvenIfVoicingFails() {
        // If voicing cannot even start, the caller must not be stranded for its full timeout.
        SpeechGateway throwing = request -> { throw new RuntimeException("tts down"); };
        ThoughtContext failingCtx = new ThoughtContext(llm, throwing, execution, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                reducer, state, dangerousPolicy, coordinator);
        CompletableFuture<Void> signal = new CompletableFuture<>();

        assertThrows(RuntimeException.class, () -> Thought.verbatimNarration(Urgency.URGENT, "boom",
                ConversationTopic.NAVIGATION, failingCtx, signal).run());

        assertTrue(signal.isCompletedExceptionally(),
                "a startup failure completes the signal (exceptionally) instead of stranding the caller");
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

    private static JsonObject classifyArgs(String topic, String importance) {
        return classifyArgs(topic, importance, false);
    }

    private static JsonObject classifyArgs(String topic, String importance, boolean isQuestion) {
        JsonObject o = new JsonObject();
        o.addProperty("topic", topic);
        o.addProperty("importance", importance);
        o.addProperty("is_question", isQuestion);
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
            if (stateToMutate != null && ClassifyTurnFunction.ID.equals(request.toolName())) {
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
        @Override public MemoryAvailabilitySnapshot indexes() { return new MemoryAvailabilitySnapshot(List.of()); }
        @Override public String longTermSummary() { return ""; }
        @Override public void replaceLongTermSummary(String summary) { }
        @Override public List<MemoryEntry> longTermPinnedFacts() { return List.of(); }
        @Override public void addLongTermPinned(MemoryEntry fact) { }
    }

    private static final class RecordingReducer implements CompanionActionReducer {
        Set<IntelActionCategory> lastCategories;
        List<LlmToolDefinition> tools = List.of();

        @Override public List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories, String currentInput) {
            lastCategories = allowedCategories;
            return tools;
        }
    }
}
