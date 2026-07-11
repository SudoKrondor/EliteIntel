package elite.intel.companion.mind;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.embed.AngleEmbedder;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticQuery;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.confirm.DangerousActionPolicy;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.memory.MemorySnapshot;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.llm.*;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.memory.TurnBoundaryMarkers;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.tools.IntelActionTypeResolver;
import elite.intel.companion.tools.IntelActionTypeResolver.IntelActionType;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.prompt.PromptComposer;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.companion.tools.ClassifyTurnFunction;
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

    private ThoughtDependencies dependencies() {
        return new ThoughtDependencies(llm, speech, execution, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                reducer, state, dangerousPolicy, coordinator);
    }

    private ThoughtDependencies dependencies(IntelActionTypeResolver resolver) {
        return new ThoughtDependencies(llm, speech, execution, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                reducer, state, dangerousPolicy, coordinator, resolver);
    }

    @Test
    void commanderSpeaksThenEndsInOneRound() {
        llm.scripted.add(ok(call(SpeakFunction.ID, text("on it"))));

        Thought.commander(Urgency.NORMAL, "set speed to 50", dependencies()).run();

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

    @Test
    void preparedSemanticQueryFlowsToReducerAndMemoryFactRecall() {
        SemanticQuery prepared = new SemanticPhraseMatcher(new AngleEmbedder(Map.of("route", 0.0)))
                .embedQueryContext("route");
        llm.scripted.add(ok(call(SpeakFunction.ID, text("on it"))));
        ThoughtContext context = ThoughtContext.commander(Urgency.NORMAL, "plot a route", "route")
                .withSemanticQuery(prepared);

        Thought.commander(context, dependencies()).run();

        assertSame(prepared, reducer.lastSemanticQuery,
                "the thought must pass its intake query to the game-tool reducer");
        assertSame(prepared, memory.lastSemanticQuery,
                "the thought must pass the same query to pre-turn memory recall");
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
    void commanderSilentCommandTurnFilesInputAndAck() {
        llm.scripted.add(ok(call("close_panel", new JsonObject()),
                call(SpeakFunction.ID, text("closing the panel"))));

        Thought.commander(Urgency.NORMAL, "close the panel", dependencies(actionTypes())).run();

        assertEquals(List.of("close_panel"), execution.toolNames(),
                "silent command runs; the co-occurring speak is withheld (never executed)");
        assertEquals(1, speech.requests.size(), "LLM-selected commands are acknowledged immediately before execution");
        assertFalse(speech.requests.get(0).text().isBlank(), "the immediate command ack is a spoken phrase");
        // A command turn is remembered as a user->assistant pair: the commander imperative and the immediate
        // acknowledgement (the silent command voiced no outcome of its own). The withheld speak and the command's
        // call echo stay unrecorded.
        assertTrue(memory.writes.stream().anyMatch(e -> e.source() == MemorySource.COMMANDER
                        && "close the panel".equals(e.content())),
                "the commander imperative is filed as the user turn");
        assertTrue(memory.writes.stream().anyMatch(e -> e.source() == MemorySource.COMPANION
                        && speech.requests.get(0).text().equals(e.content())),
                "the immediate ack is filed as the companion reply, so the turn is not a dangling commander line");
        assertTrue(memory.writes.stream().noneMatch(e -> e.toolLink() != null && e.toolLink().isCall()),
                "the command records no call echo, and the withheld speak leaves no entry");
    }

    @Test
    void commanderMixedQueryAndCommandTurnKeepsInputSoTheQueryPairHasItsUserTurn() {
        // A turn that runs both a QUERY and a COMMAND: the commander input is filed (as it is for every turn now),
        // the query records its call/result pair keeping that preceding user turn (else it replays as an assistant
        // tool-call with no user before it), and the co-occurring command still records no call echo.
        IntelActionTypeResolver mixed = new IntelActionTypeResolver(id -> switch (id) {
            case "scan_system" -> IntelActionType.QUERY;
            case "close_panel" -> IntelActionType.COMMAND;
            default -> IntelActionType.SYSTEM;
        });
        execution.resultsByTool.put("scan_system", outcomeText("two stars"));
        llm.scripted.add(ok(call("scan_system", new JsonObject()), call("close_panel", new JsonObject())));

        Thought.commander(Urgency.NORMAL, "scan the system and close the panel", dependencies(mixed)).run();

        assertTrue(memory.writes.stream().anyMatch(e -> e.source() == MemorySource.COMMANDER
                        && "scan the system and close the panel".equals(e.content())),
                "a turn that also runs a query keeps its commander input");
        assertTrue(memory.writes.stream().anyMatch(e -> e.source() == MemorySource.COMPANION
                        && e.toolLink() != null && e.toolLink().isCall() && "scan_system".equals(e.toolLink().toolName())),
                "the query call is recorded for pair replay");
        assertTrue(memory.writes.stream().noneMatch(e -> e.toolLink() != null && e.toolLink().isCall()
                        && "close_panel".equals(e.toolLink().toolName())),
                "the co-occurring command still records no call echo");
    }

    @Test
    void reflexSilentCommandFilesNoMemory() {
        // A reflex is a COMMAND - a side effect, not dialogue - so neither the imperative nor the call echo is
        // filed. A silent command (no handler-voiced outcome) therefore leaves no memory entry, and the
        // companion adds no affirmative voice of its own.
        Thought.reflex(Urgency.NORMAL, "close the panel", "close_panel", dependencies(actionTypes())).run();

        assertTrue(llm.requests.isEmpty());
        assertEquals(List.of("close_panel"), execution.toolNames());
        assertTrue(speech.requests.isEmpty(), "silent self-narrating commands are not acknowledged by companion");
        assertTrue(memory.writes.isEmpty(),
                "a reflex command files nothing: neither the imperative nor the call echo is recorded");
    }

    @Test
    void commanderQueryAnswerIsRecordedAsResultAndVoicedDirectly() {
        // The query answer is in the execution result: recordOutcome records it as the call's RESULT and voices
        // it directly - no AiVoxResponseEvent / CompanionAnnouncementBridge detour (that event is system-only now).
        IntelActionTypeResolver asQuery = new IntelActionTypeResolver(
                id -> "scan_system".equals(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);
        execution.resultsByTool.put("scan_system", outcomeText("two stars and a gas giant"));
        llm.scripted.add(ok(call("scan_system", new JsonObject())));

        Thought.commander(Urgency.NORMAL, "scan the system", dependencies(asQuery)).run();

        assertEquals(List.of("two stars and a gas giant"),
                speech.requests.stream().map(SpeechRequest::text).toList(),
                "the query answer is voiced directly");
        assertTrue(memory.writes.stream().anyMatch(e -> e.source() == MemorySource.TOOL_RESULT
                        && e.toolLink() != null && e.toolLink().isResult()
                        && "two stars and a gas giant".equals(e.content())),
                "the query answer is recorded as the call's RESULT");
        assertTrue(memory.writes.stream().anyMatch(e -> e.source() == MemorySource.COMPANION
                        && e.toolLink() != null && e.toolLink().isCall() && "scan_system".equals(e.toolLink().toolName())),
                "the query call is recorded for pair replay");
    }

    @Test
    void commanderCommandSettlesTurnInOneRound() {
        // A command is self-narrating and terminal: it settles the turn in its own round, so the scripted
        // second round is never reached (the companion turn is single-round by design).
        llm.scripted.add(ok(call("close_panel", new JsonObject())));
        llm.scripted.add(ok(call(SpeakFunction.ID, text("done"))));

        Thought.commander(Urgency.NORMAL, "close the panel", dependencies(actionTypes())).run();

        assertEquals(1, llm.requests.size(),
                "the command settles the turn; the scripted second round is never requested");
        assertEquals(List.of("close_panel"), execution.toolNames(),
                "only the command runs; the later speak is never executed");
    }

    @Test
    void classifyTurnAppliedBeforeInputIsRecorded() {
        execution.stateToMutate = state; // the fake mirrors the classify_turn handle effect on the topic
        llm.scripted.add(ok(call(ClassifyTurnFunction.ID, classifyArgs("navigation", "high"))));

        Thought.commander(Urgency.NORMAL, "let's talk routes", dependencies()).run();

        assertEquals(ConversationTopic.NAVIGATION, state.globalTopic());
        // The recorded commander input is tagged with the NEW topic (not the default SOCIAL) and stamped with
        // the chosen importance.
        assertEquals(ConversationTopic.NAVIGATION, memory.writes.get(0).topic());
        assertEquals(MemoryImportance.HIGH, memory.writes.get(0).importance());
        assertEquals(1, execution.toolNames().stream().filter(ClassifyTurnFunction.ID::equals).count(),
                "classify_turn runs once (pre-execution result reused, not run twice)");
    }

    @Test
    void questionTurnInputIsFiledAtLowSoItIsNotAFactCandidate() {
        execution.stateToMutate = state; // the fake mirrors the classify_turn handle effect on the topic
        llm.scripted.add(ok(call(ClassifyTurnFunction.ID, classifyArgs("navigation", "normal", true)),
                call(SpeakFunction.ID, text("forty percent"))));

        Thought.commander(Urgency.NORMAL, "how much fuel is left", dependencies()).run();

        // Every commander turn is recorded so the dialogue history alternates user/assistant. A question carries
        // no durable fact, so its input is stamped LOW (forced, even though classify_turn said "normal") - kept
        // out of fact-recall - while the answer that carries the fact is recorded as the companion's own words.
        assertEquals(2, memory.writes.size(), "both the question input and the answer are recorded");
        MemoryEntry input = memory.writes.get(0);
        assertEquals(MemorySource.COMMANDER, input.source());
        assertEquals("how much fuel is left", input.content());
        assertEquals(MemoryImportance.LOW, input.importance(), "a question is forced LOW so it is not a fact candidate");
        MemoryEntry spoken = memory.writes.get(1);
        assertEquals(MemorySource.COMPANION, spoken.source());
        assertEquals("forty percent", spoken.content());
    }

    @Test
    void eventReactionRecordsStimulusThenSpokenReply() {
        // A reactive event: phrase the pre-digested stimulus, voice it, and leave a clean user->assistant pair in
        // memory - the event stimulus as the (EVENT-source) user turn, the spoken reply as the companion's words.
        // The phrasing instructions steer only the prompt; they are never recorded.
        llm.scripted.add(ok(call(SpeakFunction.ID, text("Signals detected on the ring, Commander."))));

        Thought.eventReaction(Urgency.NORMAL, "surface scan: alexandrite, void opals",
                "Report the signals briefly.", ConversationTopic.EXPLORATION, dependencies()).run();

        assertEquals(1, llm.requests.size(), "an event reaction is a single short round");
        List<LlmMessage> promptMessages = llm.requests.get(0).messages();
        LlmMessage promptInput = promptMessages.get(promptMessages.size() - 1);
        assertEquals(LlmMessageRole.USER, promptInput.role());
        assertTrue(promptInput.content().contains("<event_data>\nsurface scan: alexandrite, void opals\n</event_data>"),
                "event data is tagged in the LLM-visible prompt");
        assertTrue(promptInput.content().contains(
                        "<narration_instructions>\nReport the signals briefly.\n</narration_instructions>"),
                "phrasing instructions are tagged separately");
        assertEquals(List.of(SpeakFunction.ID), execution.toolNames(), "the phrased line is voiced via speak");
        assertEquals(2, memory.writes.size(), "the stimulus and the reply are both recorded, in order");
        MemoryEntry stimulus = memory.writes.get(0);
        assertEquals(MemorySource.EVENT, stimulus.source(), "the stimulus is an EVENT turn (rendered as user)");
        assertEquals("surface scan: alexandrite, void opals", stimulus.content(),
                "the clean event data is recorded, not the phrasing instructions");
        assertEquals(ConversationTopic.EXPLORATION, stimulus.topic());
        MemoryEntry reply = memory.writes.get(1);
        assertEquals(MemorySource.COMPANION, reply.source());
        assertEquals("Signals detected on the ring, Commander.", reply.content());
    }

    @Test
    void eventReactionVoicesOnlyTheFirstSpeakWhenTheModelEmitsTwo() {
        // A small model sometimes splits its line into two speak calls in one round. Only the first is voiced and
        // recorded; the extra is dropped, so the reaction stays a single clean user->assistant pair.
        llm.scripted.add(ok(
                call(SpeakFunction.ID, text("Signals on the ring.")),
                call(SpeakFunction.ID, text("Alexandrite and void opals."))));

        Thought.eventReaction(Urgency.NORMAL, "surface scan: alexandrite, void opals",
                "Report the signals briefly.", ConversationTopic.EXPLORATION, dependencies()).run();

        assertEquals(List.of(SpeakFunction.ID), execution.toolNames(), "only the first speak is voiced");
        assertEquals(2, memory.writes.size(), "still one clean user->assistant pair, the extra speak is dropped");
        assertEquals("Signals on the ring.", memory.writes.get(1).content(),
                "the first speak is the recorded reply");
    }

    @Test
    void eventVerbatimRecordsSourceIdThenPhraseWithoutLlm() {
        // A verbatim result: no LLM. The short source id is the user turn (never raw data), the finished phrase is
        // voiced and recorded as the companion's reply - the same clean user->assistant order as narration.
        Thought.eventVerbatim(Urgency.URGENT, "SAAScanComplete", "Surface scan complete, Commander.",
                ConversationTopic.EXPLORATION, dependencies()).run();

        assertTrue(llm.requests.isEmpty(), "verbatim never engages the LLM");
        assertEquals(2, memory.writes.size(), "the source id and the phrase are both recorded, in order");
        MemoryEntry stimulus = memory.writes.get(0);
        assertEquals(MemorySource.EVENT, stimulus.source());
        assertEquals("SAAScanComplete", stimulus.content(), "the user turn is the short source id, not raw data");
        MemoryEntry reply = memory.writes.get(1);
        assertEquals(MemorySource.COMPANION, reply.source());
        assertEquals("Surface scan complete, Commander.", reply.content());
        assertEquals(ConversationTopic.EXPLORATION, reply.topic());
        assertEquals(List.of("Surface scan complete, Commander."),
                speech.requests.stream().map(SpeechRequest::text).toList(), "and voiced verbatim");
    }

    @Test
    void commanderInvalidResponseRecordsUnresolvedAndSpeaks() {
        llm.scripted.add(invalid());

        Thought.commander(Urgency.NORMAL, "do the thing", dependencies()).run();

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

        Thought.commander(Urgency.NORMAL, "anything", dependencies()).run();

        assertEquals(ConversationTopic.UNRESOLVED_COMMANDER_INPUT, memory.writes.get(0).topic());
        assertEquals(1, speech.requests.size());
    }

    @Test
    void dangerousActionWaitsForConfirmationThenExecutesOnConfirm() throws InterruptedException {
        // The model is unaware of danger: it just calls the action. The thought voices the confirmation itself.
        dangerousPolicy = invocation -> "self_destruct".equals(invocation.name());
        llm.scripted.add(ok(call("self_destruct", new JsonObject())));

        runResolving(Thought.commander(Urgency.NORMAL, "self destruct", dependencies()), coordinator::confirm);

        assertTrue(execution.toolNames().contains("self_destruct"), "dangerous action runs only after confirm");
        assertFalse(speech.requests.isEmpty(), "the thought voices a confirmation prompt before running it");
        assertTrue(hasContent("dangerous action requires confirmation"));
        assertTrue(hasContent("dangerous action confirmed"));
        // The confirmation is recorded as its own user turn (a <confirmed/> marker) so the executed outcome pairs
        // with it as a distinct exchange rather than trailing the confirmation prompt as a second assistant line.
        assertTrue(memory.writes.stream().anyMatch(
                        e -> e.source() == MemorySource.COMMANDER && "<confirmed/>".equals(e.content())),
                "the commander's confirmation is filed as a distinct user turn");
    }

    @Test
    void dangerousActionIsDiscardedOnCancel() throws InterruptedException {
        dangerousPolicy = invocation -> "self_destruct".equals(invocation.name());
        llm.scripted.add(ok(call("self_destruct", new JsonObject())));

        runResolving(Thought.commander(Urgency.NORMAL, "self destruct", dependencies()), coordinator::cancel);

        assertFalse(execution.toolNames().contains("self_destruct"), "cancelled dangerous action must not run");
        assertTrue(hasContent("dangerous action cancelled"));
    }

    @Test
    void overlappingConfirmationIsRefused() {
        dangerousPolicy = invocation -> "self_destruct".equals(invocation.name());
        coordinator.open(); // occupy the single confirmation slot
        llm.scripted.add(ok(call("self_destruct", new JsonObject())));

        Thought.commander(Urgency.NORMAL, "self destruct", dependencies()).run(); // open() returns null -> no blocking

        assertFalse(execution.toolNames().contains("self_destruct"));
        assertTrue(hasContent("dangerous action cancelled"));
    }

    @Test
    void interruptWhileWaitingOnLlmSafeFlushesUnresolvedInput() throws InterruptedException {
        llm.blockForever = true; // the thought will block on the LLM future until interrupted
        Thought thought = Thought.commander(Urgency.NORMAL, "do something", dependencies());
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

    @Test
    void interruptedDetachedCommandDiscardsItsLateOutcome() throws InterruptedException {
        llm.scripted.add(ok(call("slow_command", new JsonObject())));
        CompletableFuture<JsonObject> startedHandler = new CompletableFuture<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false; // a started game handler may continue, but its owning thought must ignore the result
            }
        };
        execution.futuresByTool.put("slow_command", startedHandler);
        IntelActionTypeResolver asCommand = new IntelActionTypeResolver(
                id -> "slow_command".equals(id) ? IntelActionType.COMMAND : IntelActionType.SYSTEM);
        Thought thought = Thought.commander(Urgency.NORMAL, "start the slow command", dependencies(asCommand));
        Thread worker = new Thread(thought::run, "thought-detached-test");
        worker.start();
        waitUntil(() -> execution.toolNames().contains("slow_command"));

        thought.interrupt();
        JsonObject late = outcomeText("late completion must stay silent");
        startedHandler.complete(late);
        worker.join(2000);

        assertFalse(worker.isAlive(), "the thought completes when its non-cancellable handler eventually returns");
        assertTrue(memory.writes.stream().noneMatch(e -> late.get("text_to_speech_response").getAsString()
                        .equals(e.content())),
                "an interrupted thought never records its handler's late outcome");
        assertTrue(speech.requests.stream().noneMatch(r -> "late completion must stay silent".equals(r.text())),
                "an interrupted thought never voices its handler's late outcome");
    }

    @Test
    void detachedQueryClosesItsTurnBeforeAppendingTheLateCallResultPair() throws InterruptedException {
        llm.scripted.add(ok(call("slow_query", new JsonObject())));
        CompletableFuture<JsonObject> slowQuery = new CompletableFuture<>();
        execution.futuresByTool.put("slow_query", slowQuery);
        IntelActionTypeResolver asQuery = new IntelActionTypeResolver(
                id -> "slow_query".equals(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);
        Thought thought = Thought.commander(Urgency.NORMAL, "inspect the system", dependencies(asQuery));
        Thread worker = new Thread(thought::run, "thought-query-test");
        worker.start();
        waitUntil(() -> execution.toolNames().contains("slow_query"));
        waitUntil(() -> memory.writes.stream()
                .anyMatch(e -> TurnBoundaryMarkers.PROCESSING.equals(e.content())));

        assertTrue(memory.writes.stream().anyMatch(e -> TurnBoundaryMarkers.PROCESSING.equals(e.content())),
                "the next commander turn sees a closed processing boundary while the query is pending");
        assertTrue(memory.writes.stream().noneMatch(e -> e.toolLink() != null),
                "a pending query is not replayed with a synthetic result");

        slowQuery.complete(outcomeText("system inspected"));
        worker.join(2000);

        assertFalse(worker.isAlive());
        assertTrue(memory.writes.stream().anyMatch(e -> e.toolLink() != null && e.toolLink().isCall()),
                "the query CALL is appended once the real result exists");
        assertTrue(memory.writes.stream().anyMatch(e -> e.toolLink() != null && e.toolLink().isResult()
                        && "system inspected".equals(e.content())),
                "the matching RESULT is appended with the delayed CALL");
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
        final List<ExecutionRequest> requests = new CopyOnWriteArrayList<>();
        final Map<String, JsonObject> resultsByTool = new HashMap<>();
        final Map<String, CompletableFuture<JsonObject>> futuresByTool = new HashMap<>();
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
            CompletableFuture<JsonObject> deferred = futuresByTool.get(request.toolName());
            if (deferred != null) {
                return deferred;
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
        SemanticQuery lastSemanticQuery;

        @Override public void write(MemoryEntry entry) { writes.add(entry); }
        @Override public MemorySnapshot snapshot() { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> readShortTermTimeline() { return List.of(); }
        @Override public List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit) { return List.of(); }
        @Override public List<String> recallMatching(String query, int limit) { return List.of(); }
        @Override public List<MemoryEntry> recallCandidates(String query, int limit) { return List.of(); }
        @Override public List<MemoryEntry> recallCandidates(String query, int limit, SemanticQuery semanticQuery) {
            lastSemanticQuery = semanticQuery;
            return recallCandidates(query, limit);
        }
        @Override public String longTermSummary() { return ""; }
        @Override public void replaceLongTermSummary(String summary) { }
        @Override public List<MemoryEntry> longTermPinnedFacts() { return List.of(); }
        @Override public void addLongTermPinned(MemoryEntry fact) { }
    }

    private static final class RecordingReducer implements CompanionActionReducer {
        Set<IntelActionCategory> lastCategories;
        SemanticQuery lastSemanticQuery;
        List<LlmToolDefinition> tools = List.of();

        @Override public List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories, String currentInput) {
            lastCategories = allowedCategories;
            return tools;
        }

        @Override public List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories,
                                                              String currentInput, SemanticQuery semanticQuery) {
            lastSemanticQuery = semanticQuery;
            return selectTools(allowedCategories, currentInput);
        }
    }
}
