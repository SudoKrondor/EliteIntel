package elite.intel.companion.mind;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.embed.AngleEmbedder;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticQuery;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.confirm.DangerousActionPolicy;
import elite.intel.companion.clarify.ClarificationCoordinator;
import elite.intel.companion.clarify.PendingClarification;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.memory.MemorySnapshot;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.GameStateSnapshot;
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
import elite.intel.companion.tools.RequestInputFunction;
import elite.intel.companion.tools.SpeakFunction;
import elite.intel.companion.tools.SystemFunctionProvider;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The consciousness loop: the happy path (single round, multi-round tool round-trip), the
 * classify_turn pre-execution step before a dialogue pair is recorded, the EVENT memory tag with
 * query-only access and verbosity-gated speak, dangerous-action confirmation, interruption, and
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
    private final ClarificationCoordinator clarificationCoordinator = new ClarificationCoordinator();

    private ThoughtDependencies dependencies() {
        return new ThoughtDependencies(llm, speech, execution, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                reducer, state, dangerousPolicy, coordinator, clarificationCoordinator);
    }

    private ThoughtDependencies dependencies(IntelActionTypeResolver resolver) {
        return new ThoughtDependencies(llm, speech, execution, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                reducer, state, dangerousPolicy, coordinator, clarificationCoordinator, resolver);
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
    void requestInputOpensContinuationAndReoffersItsTargetOnTheReplyTurn() {
        LlmToolDefinition setSpeed = new LlmToolDefinition(
                "set_speed", "Increase speed by an amount", "increase speed",
                List.of(new ActionParameterSpec(
                        "amount", "number", true, "Relative speed increase", List.of("10"), null)));
        reducer.tools = List.of(setSpeed);
        reducer.catalog = List.of(setSpeed);
        IntelActionTypeResolver types = new IntelActionTypeResolver(id ->
                "set_speed".equals(id) ? IntelActionType.COMMAND : IntelActionType.SYSTEM);

        JsonObject requestArgs = new JsonObject();
        requestArgs.addProperty(RequestInputFunction.PARAM_ACTION_ID, "set_speed");
        requestArgs.addProperty(RequestInputFunction.PARAM_PARAMETER_NAME, "amount");
        requestArgs.addProperty(RequestInputFunction.PARAM_QUESTION, "By how much?");
        llm.scripted.add(ok(call(RequestInputFunction.ID, requestArgs)));

        Thought.commander(Urgency.NORMAL, "increase speed", dependencies(types)).run();

        PendingClarification pending = clarificationCoordinator.peek().orElseThrow();
        assertEquals("increase speed", pending.originalInput());
        assertEquals(List.of("By how much?"), speech.requests.stream().map(SpeechRequest::text).toList());
        assertTrue(execution.toolNames().isEmpty(), "request_input must not enter the execution gateway");
        assertTrue(llm.requests.get(0).tools().stream()
                        .anyMatch(tool -> RequestInputFunction.ID.equals(tool.name())),
                "a required game parameter makes request_input available");

        PendingClarification claimed = clarificationCoordinator.claim().orElseThrow();
        reducer.tools = List.of(); // "by 10" has no standalone reducer match
        JsonObject speedArgs = new JsonObject();
        speedArgs.addProperty("amount", 10);
        llm.scripted.add(ok(call("set_speed", speedArgs)));
        ThoughtContext replyContext = ThoughtContext.commander(Urgency.NORMAL, "by 10", "by 10")
                .withPendingClarification(claimed);

        Thought.commander(replyContext, dependencies(types)).run();

        LlmRequest continuationRequest = llm.requests.get(1);
        assertTrue(continuationRequest.tools().stream().anyMatch(tool -> "set_speed".equals(tool.name())),
                "the pending target is re-offered even when the new phrase does not reduce to it");
        String continuationInput = continuationRequest.messages().get(
                continuationRequest.messages().size() - 1).content();
        assertTrue(continuationInput.contains("<pending_clarification>"));
        assertTrue(continuationInput.contains("<original_command>increase speed</original_command>"));
        assertEquals(List.of("set_speed"), execution.toolNames());
        assertEquals(10, execution.requests.get(0).arguments().get("amount").getAsInt());
        assertEquals("increase speed\nby 10", execution.requests.get(0).commanderInput(),
                "handler fallback parsing sees the originating order and the terse answer");
        assertTrue(clarificationCoordinator.peek().isEmpty());
        assertTrue(memory.writes.isEmpty(),
                "request_input and the completed command stay in transient clarification/execution state");
    }

    @Test
    void parameterlessGameToolDoesNotOfferRequestInput() {
        LlmToolDefinition openMap = new LlmToolDefinition(
                "open_map", "Open the system map", "open map", List.of());
        reducer.tools = List.of(openMap);
        reducer.catalog = List.of(openMap);
        IntelActionTypeResolver types = new IntelActionTypeResolver(id ->
                "open_map".equals(id) ? IntelActionType.COMMAND : IntelActionType.SYSTEM);
        llm.scripted.add(ok(call("open_map", new JsonObject())));

        Thought.commander(Urgency.NORMAL, "open map", dependencies(types)).run();

        List<LlmToolDefinition> offered = llm.requests.get(0).tools();
        assertTrue(offered.stream().anyMatch(tool -> "open_map".equals(tool.name())));
        assertFalse(offered.stream().anyMatch(tool -> RequestInputFunction.ID.equals(tool.name())),
                "a parameterless game tool leaves nothing for request_input to ask");
        assertEquals(List.of("open_map"), execution.toolNames());
        assertTrue(clarificationCoordinator.peek().isEmpty());
    }

    @Test
    void aDifferentActionOnTheReplyTurnSupersedesPendingContext() {
        LlmToolDefinition setSpeed = new LlmToolDefinition(
                "set_speed", "Increase speed by an amount", "increase speed",
                List.of(new ActionParameterSpec(
                        "amount", "number", true, "Relative speed increase", List.of("10"), null)));
        LlmToolDefinition openMap = new LlmToolDefinition(
                "open_map", "Open the system map", "open map", List.of());
        reducer.tools = List.of(openMap);
        reducer.catalog = List.of(setSpeed, openMap);
        PendingClarification claimed = clarificationCoordinator
                .open("set_speed", "amount", "increase speed", "By how much?");
        assertSame(claimed, clarificationCoordinator.claim().orElseThrow());
        IntelActionTypeResolver types = new IntelActionTypeResolver(id -> switch (id) {
            case "set_speed", "open_map" -> IntelActionType.COMMAND;
            default -> IntelActionType.SYSTEM;
        });
        llm.scripted.add(ok(call("open_map", new JsonObject())));
        ThoughtContext replyContext = ThoughtContext.commander(Urgency.NORMAL, "open map", "open map")
                .withPendingClarification(claimed);

        Thought.commander(replyContext, dependencies(types)).run();

        assertEquals(List.of("open_map"), execution.toolNames());
        assertEquals("open map", execution.requests.get(0).commanderInput(),
                "a new action must not inherit the abandoned speed order");
        assertTrue(llm.requests.get(0).tools().stream().anyMatch(tool -> "set_speed".equals(tool.name())),
                "the model can compare the pending target with normal candidates before superseding it");
        assertTrue(clarificationCoordinator.peek().isEmpty());
    }

    @Test
    void preparedSemanticQueryFlowsToReducerAndMemoryFactRecall() {
        SemanticQuery prepared = new SemanticPhraseMatcher(new AngleEmbedder(Map.of("route", 0.0)))
                .embedQueryContext("route");
        GameStateSnapshot turnState = GameStateSnapshot.capture(Status.detached(PlayerSituation.IN_SHIP_DEEP_SPACE));
        llm.scripted.add(ok(call(SpeakFunction.ID, text("on it"))));
        ThoughtContext context = ThoughtContext.commander(Urgency.NORMAL, "plot a route", "route", turnState)
                .withSemanticQuery(prepared);

        Thought.commander(context, dependencies()).run();

        assertSame(prepared, reducer.lastSemanticQuery,
                "the thought must pass its intake query to the game-tool reducer");
        assertSame(prepared, memory.lastSemanticQuery,
                "the thought must pass the same query to pre-turn memory recall");
        assertSame(turnState, reducer.lastGameStateSnapshot,
                "the thought must pass its intake visibility state to the game-tool reducer");
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
    void commanderSilentCommandTurnFilesNoMemory() {
        llm.scripted.add(ok(call("close_panel", new JsonObject()),
                call(SpeakFunction.ID, text("closing the panel"))));

        Thought.commander(Urgency.NORMAL, "close the panel", dependencies(actionTypes())).run();

        assertEquals(List.of("close_panel"), execution.toolNames(),
                "silent command runs; the co-occurring speak is withheld (never executed)");
        assertEquals(1, speech.requests.size(), "LLM-selected commands are acknowledged immediately before execution");
        assertFalse(speech.requests.get(0).text().isBlank(), "the immediate command ack is a spoken phrase");
        assertTrue(memory.writes.isEmpty(),
                "command input, acknowledgement, call echo, and withheld speak are execution rather than dialogue");
    }

    @Test
    void repeatedCommandTurnsDoNotAccumulateConversationalMemory() {
        llm.scripted.add(ok(call("close_panel", new JsonObject())));
        llm.scripted.add(ok(call("close_panel", new JsonObject())));

        Thought.commander(Urgency.NORMAL, "close the panel", dependencies(actionTypes())).run();
        Thought.commander(Urgency.NORMAL, "close the panel", dependencies(actionTypes())).run();

        assertEquals(List.of("close_panel", "close_panel"), execution.toolNames(),
                "the same command remains independently executable in one session");
        assertTrue(memory.writes.isEmpty(),
                "neither execution leaves history that could bias the next repeated command");
    }

    @Test
    void commanderCommandFailureKeepsSpeechButFilesNoMemory() throws InterruptedException {
        llm.scripted.add(ok(call("close_panel", new JsonObject())));
        CompletableFuture<JsonObject> failedCommand = new CompletableFuture<>();
        execution.futuresByTool.put("close_panel", failedCommand);
        Thought thought = Thought.commander(Urgency.NORMAL, "close the panel", dependencies(actionTypes()));
        Thread worker = new Thread(thought::run, "thought-command-failure-test");
        worker.start();
        waitUntil(() -> execution.toolNames().contains("close_panel") && speech.requests.size() == 1);

        String acknowledgement = speech.requests.get(0).text();
        assertFalse(acknowledgement.isBlank(), "the intention acknowledgement must not wait for execution");

        failedCommand.completeExceptionally(new IllegalStateException("binding missing"));
        worker.join(2000);

        assertFalse(worker.isAlive());
        assertEquals(2, speech.requests.size(), "a failed command receives a second, explicit outcome reply");
        String failureReply = speech.requests.get(1).text();
        assertFalse(failureReply.isBlank());
        assertTrue(memory.writes.isEmpty(),
                "command acknowledgement, failure reply, and internal execution details stay out of memory");
    }

    @Test
    void commanderMixedQueryAndCommandTurnKeepsInputSoTheQueryPairHasItsUserTurn() {
        // A turn that runs both a QUERY and a COMMAND: QUERY files the single commander input anchor and its
        // call/result pair (else it would replay as an assistant tool-call with no user before it), while the
        // co-occurring command still records no call echo or plain outcome.
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
        assertTrue(memory.writes.stream().noneMatch(e -> e.source() == MemorySource.COMPANION
                        && e.toolLink() == null),
                "the co-occurring command acknowledgement/outcome is not recorded as plain dialogue");
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
    void reflexCommandFailureIsVoicedWithoutMemory() {
        CompletableFuture<JsonObject> failedCommand = new CompletableFuture<>();
        failedCommand.completeExceptionally(new IllegalStateException("input binding missing"));
        execution.futuresByTool.put("close_panel", failedCommand);

        Thought.reflex(Urgency.NORMAL, "close the panel", "close_panel", dependencies(actionTypes())).run();

        assertTrue(llm.requests.isEmpty());
        assertEquals(1, speech.requests.size());
        String failureReply = speech.requests.get(0).text();
        assertFalse(failureReply.isBlank());
        assertTrue(memory.writes.isEmpty(),
                "a failed reflex command receives visible feedback without becoming dialogue memory");
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
    void detachedQueryFailureIsRecordedAndVoiced() throws InterruptedException {
        llm.scripted.add(ok(call("slow_query", new JsonObject())));
        CompletableFuture<JsonObject> failedQuery = new CompletableFuture<>();
        execution.futuresByTool.put("slow_query", failedQuery);
        IntelActionTypeResolver asQuery = new IntelActionTypeResolver(
                id -> "slow_query".equals(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);
        Thought thought = Thought.commander(Urgency.NORMAL, "inspect the system", dependencies(asQuery));
        Thread worker = new Thread(thought::run, "thought-query-failure-test");
        worker.start();
        waitUntil(() -> execution.toolNames().contains("slow_query"));
        assertTrue(memory.writes.isEmpty(), "a pending query has no completed contract to publish");

        failedQuery.completeExceptionally(new IllegalStateException("query backend unavailable"));
        worker.join(2000);

        assertFalse(worker.isAlive());
        assertEquals(1, speech.requests.size(), "a failed query must not end as a silent processing turn");
        String failureReply = speech.requests.get(0).text();
        assertFalse(failureReply.isBlank());
        assertTrue(memory.writes.stream().anyMatch(e -> e.source() == MemorySource.TOOL_RESULT
                        && e.toolLink() != null && e.toolLink().isResult() && failureReply.equals(e.content())),
                "the failure is the query's linked tool result");
        assertTrue(memory.writes.stream().anyMatch(e -> e.toolLink() != null && e.toolLink().isCall()
                        && "slow_query".equals(e.toolLink().toolName())),
                "the failed query keeps its matching call for replay");
        assertTrue(memory.writes.stream().noneMatch(e -> TurnBoundaryMarkers.NO_ANSWER.equals(e.content())),
                "a reported query failure is an answer, not an omitted reply");
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
    void classifyTurnAppliedBeforeDialoguePairIsRecorded() {
        execution.stateToMutate = state; // the fake mirrors the classify_turn handle effect on the topic
        llm.scripted.add(ok(
                call(ClassifyTurnFunction.ID,
                        classifyArgs("navigation", "high", false, "routes are the subject")),
                call(SpeakFunction.ID, text("let's talk routes"))));

        Thought.commander(Urgency.NORMAL, "let's talk routes", dependencies()).run();

        assertEquals(ConversationTopic.NAVIGATION, state.globalTopic());
        // The recorded commander input is tagged with the NEW topic (not the default SOCIAL) and stamped with
        // the chosen importance.
        assertEquals(ConversationTopic.NAVIGATION, memory.writes.get(0).topic());
        assertEquals(MemoryImportance.HIGH, memory.writes.get(0).importance());
        assertEquals("routes are the subject", memory.writes.get(0).canonicalFact());
        assertEquals(1, execution.toolNames().stream().filter(ClassifyTurnFunction.ID::equals).count(),
                "classify_turn runs once (pre-execution result reused, not run twice)");
    }

    @Test
    void routineAndMaxTurnsCannotStoreModelSuppliedCanonicalFacts() {
        execution.stateToMutate = state;
        llm.scripted.add(ok(
                call(ClassifyTurnFunction.ID,
                        classifyArgs("combat", "normal", false, "target the drives")),
                call(SpeakFunction.ID, text("understood"))));
        llm.scripted.add(ok(
                call(ClassifyTurnFunction.ID,
                        classifyArgs("navigation", "max", false, "docking code is sierra nine")),
                call(SpeakFunction.ID, text("remembered"))));

        Thought.commander(Urgency.NORMAL, "target the drives", dependencies()).run();
        Thought.commander(Urgency.NORMAL, "remember verbatim: docking code sierra nine", dependencies()).run();

        List<MemoryEntry> inputs = memory.writes.stream()
                .filter(entry -> entry.source() == MemorySource.COMMANDER)
                .toList();
        assertEquals(2, inputs.size());
        assertNull(inputs.get(0).canonicalFact(), "NORMAL dialogue does not trust a model-supplied fact");
        assertEquals(MemoryImportance.NORMAL, inputs.get(0).importance());
        assertNull(inputs.get(1).canonicalFact(), "MAX keeps the original input verbatim");
        assertEquals(MemoryImportance.MAX, inputs.get(1).importance());
        assertEquals("remember verbatim: docking code sierra nine", inputs.get(1).content());
    }

    @Test
    void highTurnFallsBackToCurrentInputWhenCanonicalFactWasCopiedFromAnotherTurn() {
        execution.stateToMutate = state;
        llm.scripted.add(ok(
                call(ClassifyTurnFunction.ID,
                        classifyArgs("combat", "high", false, "docking code is sierra nine")),
                call(SpeakFunction.ID, text("understood"))));

        String current = "if pirates corner us, retreat codeword is granite";
        Thought.commander(Urgency.NORMAL, current, dependencies()).run();

        MemoryEntry input = memory.writes.stream()
                .filter(entry -> entry.source() == MemorySource.COMMANDER)
                .findFirst().orElseThrow();
        assertEquals(MemoryImportance.HIGH, input.importance());
        assertEquals(current, input.canonicalFact(), "ungrounded prior-turn text is replaced by current ground truth");
    }

    @Test
    void gameActionTurnFilesNeitherDialogueNorHighFact() {
        execution.stateToMutate = state;
        llm.scripted.add(ok(
                call(ClassifyTurnFunction.ID, classifyArgs("combat", "high", false, "target the drives")),
                call("close_panel", new JsonObject())));

        Thought.commander(Urgency.NORMAL, "target the drives", dependencies(actionTypes())).run();

        assertTrue(memory.writes.isEmpty(),
                "classification cannot turn a command execution into dialogue or trusted fact memory");
    }

    @Test
    void questionTurnInputIsFiledAtLowSoItIsNotAFactCandidate() {
        execution.stateToMutate = state; // the fake mirrors the classify_turn handle effect on the topic
        llm.scripted.add(ok(call(ClassifyTurnFunction.ID,
                        classifyArgs("navigation", "normal", true, "fuel is forty percent")),
                call(SpeakFunction.ID, text("forty percent"))));

        Thought.commander(Urgency.NORMAL, "how much fuel is left", dependencies()).run();

        // This question has a non-blank LLM answer, so the complete pair is recorded. The input is stamped LOW
        // (forced, even though classify_turn said "normal") and stays out of fact recall; the answer is the
        // companion half of the pair.
        assertEquals(2, memory.writes.size(), "both the question input and the answer are recorded");
        MemoryEntry input = memory.writes.get(0);
        assertEquals(MemorySource.COMMANDER, input.source());
        assertEquals("how much fuel is left", input.content());
        assertEquals(MemoryImportance.LOW, input.importance(), "a question is forced LOW so it is not a fact candidate");
        assertNull(input.canonicalFact(), "questions cannot store model-supplied canonical facts");
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
    void commanderInvalidResponseSpeaksWithoutMemory() {
        llm.scripted.add(invalid());

        Thought.commander(Urgency.NORMAL, "do the thing", dependencies()).run();

        assertEquals(1, speech.requests.size(), "commander hears a service phrase");
        assertNotNull(speech.requests.get(0).text());
        assertFalse(speech.requests.get(0).text().isBlank());
        assertTrue(memory.writes.isEmpty(), "an invalid response forms no LLM dialogue pair");
        assertTrue(execution.toolNames().isEmpty());
    }

    @Test
    void providerFailureIsTreatedAsInvalid() {
        llm.failWith = new RuntimeException("provider down");

        Thought.commander(Urgency.NORMAL, "anything", dependencies()).run();

        assertEquals(1, speech.requests.size());
        assertTrue(memory.writes.isEmpty(), "a provider failure forms no LLM dialogue pair");
    }

    @Test
    void dangerousActionWaitsForConfirmationThenExecutesOnConfirm() throws InterruptedException {
        // The model is unaware of danger: it just calls the action. The thought voices the confirmation itself.
        dangerousPolicy = invocation -> "self_destruct".equals(invocation.name());
        llm.scripted.add(ok(call("self_destruct", new JsonObject())));

        runResolving(Thought.commander(Urgency.NORMAL, "self destruct", dependencies()), coordinator::confirm);

        assertTrue(execution.toolNames().contains("self_destruct"), "dangerous action runs only after confirm");
        assertFalse(speech.requests.isEmpty(), "the thought voices a confirmation prompt before running it");
        assertTrue(memory.writes.isEmpty(),
                "confirmation prompts, markers, and executed dangerous commands are runtime state, not dialogue");
    }

    @Test
    void dangerousActionIsDiscardedOnCancel() throws InterruptedException {
        dangerousPolicy = invocation -> "self_destruct".equals(invocation.name());
        llm.scripted.add(ok(call("self_destruct", new JsonObject())));

        runResolving(Thought.commander(Urgency.NORMAL, "self destruct", dependencies()), coordinator::cancel);

        assertFalse(execution.toolNames().contains("self_destruct"), "cancelled dangerous action must not run");
        assertTrue(memory.writes.isEmpty(), "cancelled confirmation leaves no conversational memory");
    }

    @Test
    void overlappingConfirmationIsRefused() {
        dangerousPolicy = invocation -> "self_destruct".equals(invocation.name());
        coordinator.open(); // occupy the single confirmation slot
        llm.scripted.add(ok(call("self_destruct", new JsonObject())));

        Thought.commander(Urgency.NORMAL, "self destruct", dependencies()).run(); // open() returns null -> no blocking

        assertFalse(execution.toolNames().contains("self_destruct"));
        assertTrue(memory.writes.isEmpty(), "overlapping confirmation leaves no conversational memory");
    }

    @Test
    void interruptWhileWaitingOnLlmFilesNoPartialTurn() throws InterruptedException {
        llm.blockForever = true; // the thought will block on the LLM future until interrupted
        Thought thought = Thought.commander(Urgency.NORMAL, "do something", dependencies());
        Thread worker = new Thread(thought::run, "thought-test");
        worker.start();
        waitUntil(() -> !llm.requests.isEmpty()); // it has submitted and is now blocked
        thought.interrupt();
        worker.join(2000);

        assertFalse(worker.isAlive(), "interrupted thought must die");
        assertTrue(memory.writes.isEmpty(), "an interrupted thought has no complete dialogue pair to file");
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
    void interruptedDetachedQueryDiscardsItsEntireLateContract() throws InterruptedException {
        llm.scripted.add(ok(call("slow_query", new JsonObject())));
        CompletableFuture<JsonObject> startedHandler = new CompletableFuture<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }
        };
        execution.futuresByTool.put("slow_query", startedHandler);
        IntelActionTypeResolver asQuery = new IntelActionTypeResolver(
                id -> "slow_query".equals(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);
        Thought thought = Thought.commander(Urgency.NORMAL, "inspect the system", dependencies(asQuery));
        Thread worker = new Thread(thought::run, "thought-detached-query-interrupt-test");
        worker.start();
        waitUntil(() -> execution.toolNames().contains("slow_query"));

        assertTrue(memory.writes.isEmpty(), "pending QUERY publishes neither input nor processing marker");
        thought.interrupt();
        startedHandler.complete(outcomeText("late query answer"));
        worker.join(2000);

        assertFalse(worker.isAlive());
        assertTrue(memory.writes.isEmpty(), "interrupted QUERY rolls back the whole semantic contract");
        assertTrue(speech.requests.stream().noneMatch(r -> "late query answer".equals(r.text())),
                "an interrupted QUERY never voices its late result");
    }

    @Test
    void detachedQueryPublishesOnlyTheCompletedContract() throws InterruptedException {
        llm.scripted.add(ok(call("slow_query", new JsonObject())));
        CompletableFuture<JsonObject> slowQuery = new CompletableFuture<>();
        execution.futuresByTool.put("slow_query", slowQuery);
        IntelActionTypeResolver asQuery = new IntelActionTypeResolver(
                id -> "slow_query".equals(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);
        Thought thought = Thought.commander(Urgency.NORMAL, "inspect the system", dependencies(asQuery));
        Thread worker = new Thread(thought::run, "thought-query-test");
        worker.start();
        waitUntil(() -> execution.toolNames().contains("slow_query"));

        assertTrue(memory.writes.isEmpty(),
                "a pending query is invisible until its input/CALL/RESULT contract is complete");

        slowQuery.complete(outcomeText("system inspected"));
        worker.join(2000);

        assertFalse(worker.isAlive());
        assertEquals(4, memory.writes.size());
        assertEquals(MemorySource.COMMANDER, memory.writes.get(0).source());
        assertEquals("inspect the system", memory.writes.get(0).content());
        assertEquals(TurnBoundaryMarkers.PROCESSING, memory.writes.get(1).content());
        assertTrue(memory.writes.get(2).toolLink() != null && memory.writes.get(2).toolLink().isCall());
        assertTrue(memory.writes.get(3).toolLink() != null && memory.writes.get(3).toolLink().isResult());
        assertEquals("system inspected", memory.writes.get(3).content());
    }

    @Test
    void blankDetachedQueryPublishesNothing() throws InterruptedException {
        llm.scripted.add(ok(call("slow_query", new JsonObject())));
        CompletableFuture<JsonObject> slowQuery = new CompletableFuture<>();
        execution.futuresByTool.put("slow_query", slowQuery);
        IntelActionTypeResolver asQuery = new IntelActionTypeResolver(
                id -> "slow_query".equals(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);
        Thought thought = Thought.commander(Urgency.NORMAL, "inspect the system", dependencies(asQuery));
        Thread worker = new Thread(thought::run, "thought-blank-query-test");
        worker.start();
        waitUntil(() -> execution.toolNames().contains("slow_query"));

        slowQuery.complete(new JsonObject());
        worker.join(2000);

        assertFalse(worker.isAlive());
        assertTrue(memory.writes.isEmpty(), "a blank QUERY result forms no semantic contract");
        assertTrue(speech.requests.isEmpty());
    }

    @Test
    void reflexQueryPublishesItsCompleteContractWithoutLlm() {
        IntelActionTypeResolver asQuery = new IntelActionTypeResolver(
                id -> "scan_system".equals(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);
        execution.resultsByTool.put("scan_system", outcomeText("two stars"));

        Thought.reflex(Urgency.NORMAL, "scan the system", "scan_system", dependencies(asQuery)).run();

        assertTrue(llm.requests.isEmpty());
        assertEquals(3, memory.writes.size());
        assertEquals("scan the system", memory.writes.get(0).content());
        assertTrue(memory.writes.get(1).toolLink() != null && memory.writes.get(1).toolLink().isCall());
        assertTrue(memory.writes.get(2).toolLink() != null && memory.writes.get(2).toolLink().isResult());
        assertEquals("two stars", memory.writes.get(2).content());
    }

    // --- helpers ---

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
        return classifyArgs(topic, importance, isQuestion, "");
    }

    private static JsonObject classifyArgs(String topic, String importance, boolean isQuestion, String canonicalFact) {
        JsonObject o = new JsonObject();
        o.addProperty("topic", topic);
        o.addProperty("importance", importance);
        o.addProperty("is_question", isQuestion);
        o.addProperty("canonical_fact", canonicalFact);
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
        final List<SpeechRequest> requests = new CopyOnWriteArrayList<>();

        @Override public CompletableFuture<Void> submit(SpeechRequest request) {
            requests.add(request);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class FakeMemory implements MemoryGateway {
        final List<MemoryEntry> writes = new CopyOnWriteArrayList<>();
        SemanticQuery lastSemanticQuery;

        @Override public void write(MemoryEntry entry) { writes.add(entry); }
        @Override public void writeBatch(List<MemoryEntry> entries) { writes.addAll(entries); }
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
        GameStateSnapshot lastGameStateSnapshot;
        List<LlmToolDefinition> tools = List.of();
        List<LlmToolDefinition> catalog = List.of();

        @Override public List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories, String currentInput) {
            lastCategories = allowedCategories;
            return tools;
        }

        @Override public List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories,
                                                              String currentInput, SemanticQuery semanticQuery) {
            lastSemanticQuery = semanticQuery;
            return selectTools(allowedCategories, currentInput);
        }

        @Override public List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories,
                                                              String currentInput, SemanticQuery semanticQuery,
                                                              GameStateSnapshot gameStateSnapshot) {
            lastGameStateSnapshot = gameStateSnapshot;
            return selectTools(allowedCategories, currentInput, semanticQuery);
        }

        @Override public Optional<LlmToolDefinition> findToolById(Set<IntelActionCategory> allowedCategories,
                                                                  String actionId,
                                                                  GameStateSnapshot gameStateSnapshot) {
            return catalog.stream().filter(tool -> actionId.equals(tool.name())).findFirst();
        }
    }
}
