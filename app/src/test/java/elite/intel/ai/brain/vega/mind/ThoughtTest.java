package elite.intel.ai.brain.vega.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.vega.clarify.ClarificationCoordinator;
import elite.intel.ai.brain.vega.confirm.ConfirmationCoordinator;
import elite.intel.ai.brain.vega.confirm.DangerousActionPolicy;
import elite.intel.ai.brain.vega.execution.ExecutionGateway;
import elite.intel.ai.brain.vega.llm.LlmGateway;
import elite.intel.ai.brain.vega.memory.CompanionMemoryPolicy;
import elite.intel.ai.brain.vega.memory.MemoryGateway;
import elite.intel.ai.brain.vega.memory.MemorySearchResult;
import elite.intel.ai.brain.vega.memory.MemorySnapshot;
import elite.intel.ai.brain.vega.model.GameStateSnapshot;
import elite.intel.ai.brain.vega.model.IntelActionCategory;
import elite.intel.ai.brain.vega.model.Urgency;
import elite.intel.ai.brain.vega.model.execution.ExecutionRequest;
import elite.intel.ai.brain.vega.model.llm.LlmRequest;
import elite.intel.ai.brain.vega.model.llm.LlmResult;
import elite.intel.ai.brain.vega.model.llm.LlmToolDefinition;
import elite.intel.ai.brain.vega.model.llm.LlmToolInvocation;
import elite.intel.ai.brain.vega.model.memory.MemoryKind;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;
import elite.intel.ai.brain.vega.model.memory.MemorySource;
import elite.intel.ai.brain.vega.model.speech.SpeechRequest;
import elite.intel.ai.brain.vega.prompt.CompanionActionReducer;
import elite.intel.ai.brain.vega.prompt.IntelActionAccessPolicy;
import elite.intel.ai.brain.vega.prompt.PromptComposer;
import elite.intel.ai.brain.vega.speech.SpeechGateway;
import elite.intel.ai.brain.vega.tools.IntelActionTypeResolver;
import elite.intel.ai.brain.vega.tools.IntelActionTypeResolver.IntelActionType;
import elite.intel.ai.brain.vega.tools.RequestInputFunction;
import elite.intel.ai.brain.vega.tools.SpeakFunction;
import elite.intel.ai.brain.vega.tools.SystemFunctionProvider;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class ThoughtTest {

    private final FakeLlm llm = new FakeLlm();
    private final FakeSpeech speech = new FakeSpeech();
    private final FakeExecution execution = new FakeExecution();
    private final FakeMemory memory = new FakeMemory();
    private final RecordingReducer reducer = new RecordingReducer();
    private final CompanionState state = new CompanionState();
    private final ConfirmationCoordinator confirmation = new ConfirmationCoordinator();
    private final ClarificationCoordinator clarification = new ClarificationCoordinator();
    private DangerousActionPolicy dangerous = invocation -> false;

    private ThoughtDependencies dependencies() {
        return dependencies(new IntelActionTypeResolver(id -> IntelActionType.SYSTEM));
    }

    private ThoughtDependencies dependencies(IntelActionTypeResolver actionTypes) {
        return new ThoughtDependencies(
                llm, speech, execution, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                reducer, state, dangerous, confirmation, clarification, actionTypes);
    }

    @Test
    void conversationalSpeakPublishesOneDialogueRecord() {
        llm.results.add(ok(call(SpeakFunction.ID, text("on it"))));

        Thought.commander(Urgency.NORMAL, "set speed to 50", dependencies()).run();

        assertEquals(1, llm.requests.size());
        assertEquals(List.of(SpeakFunction.ID), execution.toolNames());
        assertEquals(1, memory.writes.size());
        MemoryRecord record = memory.writes.get(0);
        assertEquals(MemoryKind.DIALOGUE, record.kind());
        assertEquals(List.of(MemorySource.COMMANDER, MemorySource.COMPANION),
                record.entries().stream().map(entry -> entry.source()).toList());
        assertEquals("set speed to 50", record.entries().get(0).content());
        assertEquals("on it", record.entries().get(1).content());
    }

    @Test
    void requestInputOpensTransientContinuationWithoutMemory() {
        LlmToolDefinition setSpeed = new LlmToolDefinition(
                "set_speed", "Set speed", "set speed",
                List.of(new ActionParameterSpec(
                        "amount", "number", true, "Speed amount", List.of("50"), null)));
        reducer.tools = List.of(setSpeed);
        reducer.catalog = List.of(setSpeed);
        JsonObject args = new JsonObject();
        args.addProperty(RequestInputFunction.PARAM_ACTION_ID, "set_speed");
        args.addProperty(RequestInputFunction.PARAM_PARAMETER_NAME, "amount");
        args.addProperty(RequestInputFunction.PARAM_QUESTION, "By how much?");
        llm.results.add(ok(call(RequestInputFunction.ID, args)));
        IntelActionTypeResolver types = new IntelActionTypeResolver(id ->
                "set_speed".equals(id) ? IntelActionType.COMMAND : IntelActionType.SYSTEM);

        Thought.commander(Urgency.NORMAL, "set speed", dependencies(types)).run();

        assertEquals("set speed", clarification.peek().orElseThrow().originalInput());
        assertEquals(List.of("By how much?"), speech.requests.stream().map(SpeechRequest::text).toList());
        assertTrue(memory.writes.isEmpty());
        assertTrue(execution.requests.isEmpty(), "request_input is owned by CommanderThought");
    }

    /**
     * A weak model asked "what commodity do you want to find?" about "find market where I can buy tritium". The
     * commodity was already in the utterance, and the action's own trigger says where it ends, so the turn runs
     * the action instead of asking the commander to repeat themselves.
     */
    @Test
    void requestInputForAnArgumentTheCommanderAlreadySpokeRunsTheActionInstead() {
        reducer.tools = List.of(findCommodity());
        llm.results.add(ok(call(RequestInputFunction.ID, inputRequest("find_commodity", "key"))));
        IntelActionTypeResolver types = new IntelActionTypeResolver(id ->
                "find_commodity".equals(id) ? IntelActionType.COMMAND : IntelActionType.SYSTEM);

        Thought.commander(Urgency.NORMAL, "find market where i can buy tritium", dependencies(types)).run();

        assertEquals(List.of("find_commodity"), execution.toolNames());
        assertEquals("tritium", execution.requests.get(0).arguments().get("key").getAsString());
        assertTrue(clarification.peek().isEmpty(), "nothing was left to clarify");
    }

    @Test
    void aDangerousActionStillAsksInsteadOfRunningARecoveredArgument() {
        reducer.tools = List.of(findCommodity());
        llm.results.add(ok(call(RequestInputFunction.ID, inputRequest("find_commodity", "key"))));
        dangerous = invocation -> "find_commodity".equals(invocation.name());
        IntelActionTypeResolver types = new IntelActionTypeResolver(id ->
                "find_commodity".equals(id) ? IntelActionType.COMMAND : IntelActionType.SYSTEM);

        Thought.commander(Urgency.NORMAL, "find market where i can buy tritium", dependencies(types)).run();

        assertTrue(execution.requests.isEmpty(), "a dangerous action keeps its confirmation flow");
        assertEquals("find_commodity", clarification.peek().orElseThrow().actionId());
    }

    @Test
    void anUtteranceThatDoesNotStartWithTheTriggerStillAsks() {
        reducer.tools = List.of(findCommodity());
        llm.results.add(ok(call(RequestInputFunction.ID, inputRequest("find_commodity", "key"))));
        IntelActionTypeResolver types = new IntelActionTypeResolver(id ->
                "find_commodity".equals(id) ? IntelActionType.COMMAND : IntelActionType.SYSTEM);

        Thought.commander(Urgency.NORMAL, "we should go shopping", dependencies(types)).run();

        assertTrue(execution.requests.isEmpty(), "no trigger matched, so no value was spoken");
        assertEquals("key", clarification.peek().orElseThrow().parameterName());
    }

    /**
     * "enter next fleet carrier destination" was answered with a question about a system name, for a command that
     * declares no parameters at all (it types the next leg of the stored carrier route). Nothing can be missing
     * there, so the order runs instead of being handed back as conversation.
     */
    @Test
    void requestInputForAnActionThatDeclaresNoParameterRunsItInstead() {
        reducer.tools = List.of(enterCarrierDestination());
        llm.results.add(ok(call(RequestInputFunction.ID,
                inputRequest("enter_fleet_carrier_destination", "system_name"))));

        Thought.commander(Urgency.NORMAL, "enter next fleet carrier destination",
                dependencies(carrierDestinationIsACommand())).run();

        assertEquals(List.of("enter_fleet_carrier_destination"), execution.toolNames());
        assertTrue(execution.requests.get(0).arguments().entrySet().isEmpty(),
                "a parameterless action never receives the invented argument");
        assertTrue(clarification.peek().isEmpty(), "nothing was left to clarify");
    }

    @Test
    void aDangerousParameterlessActionStillKeepsItsConfirmationFlow() {
        reducer.tools = List.of(enterCarrierDestination());
        llm.results.add(ok(call(RequestInputFunction.ID,
                inputRequest("enter_fleet_carrier_destination", "system_name"))));
        dangerous = invocation -> "enter_fleet_carrier_destination".equals(invocation.name());

        Thought.commander(Urgency.NORMAL, "enter next fleet carrier destination",
                dependencies(carrierDestinationIsACommand())).run();

        assertTrue(execution.requests.isEmpty(), "a dangerous action is never recovered into execution");
    }

    private static LlmToolDefinition enterCarrierDestination() {
        return new LlmToolDefinition(
                "enter_fleet_carrier_destination", "Type the next carrier route leg and confirm it",
                "enter carrier destination, enter next fleet carrier destination", List.of());
    }

    private static IntelActionTypeResolver carrierDestinationIsACommand() {
        return new IntelActionTypeResolver(id -> "enter_fleet_carrier_destination".equals(id)
                ? IntelActionType.COMMAND
                : IntelActionType.SYSTEM);
    }

    private static LlmToolDefinition findCommodity() {
        return new LlmToolDefinition(
                "find_commodity", "Find where to buy a commodity",
                "find market where I can buy {key:X}, where can I buy {key:X}",
                List.of(new ActionParameterSpec(
                        "key", "string", true, "The commodity", List.of("gold"), null)));
    }

    private static JsonObject inputRequest(String actionId, String parameterName) {
        JsonObject arguments = new JsonObject();
        arguments.addProperty(RequestInputFunction.PARAM_ACTION_ID, actionId);
        arguments.addProperty(RequestInputFunction.PARAM_PARAMETER_NAME, parameterName);
        arguments.addProperty(RequestInputFunction.PARAM_QUESTION, "What commodity do you want to find?");
        return arguments;
    }

    @Test
    void commandExecutionDoesNotEnterConversationMemory() {
        reducer.tools = List.of(new LlmToolDefinition("close_panel", "Close panel", "close panel", List.of()));
        llm.results.add(ok(call("close_panel", new JsonObject())));
        IntelActionTypeResolver types = new IntelActionTypeResolver(id ->
                "close_panel".equals(id) ? IntelActionType.COMMAND : IntelActionType.SYSTEM);

        Thought.commander(Urgency.NORMAL, "close the panel", dependencies(types)).run();

        assertEquals(List.of("close_panel"), execution.toolNames());
        assertTrue(memory.writes.isEmpty());
        assertEquals(1, speech.requests.size(), "accepted commands receive a code-owned acknowledgement");
    }

    @Test
    void queryIsInvisibleUntilItsCompleteRecordCanBePublished() throws Exception {
        reducer.tools = List.of(new LlmToolDefinition("query_system", "Query system", "system status", List.of()));
        JsonObject arguments = new JsonObject();
        arguments.addProperty("technical_argument", "must not enter memory");
        llm.results.add(ok(call("query_system", arguments)));
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        execution.futures.put("query_system", result);
        IntelActionTypeResolver types = new IntelActionTypeResolver(id ->
                "query_system".equals(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);
        Thought thought = Thought.commander(Urgency.NORMAL, "inspect the system", dependencies(types));
        Thread worker = new Thread(thought::run, "query-thought-test");
        worker.start();
        waitUntil(() -> execution.toolNames().contains("query_system"));

        assertTrue(memory.writes.isEmpty());
        result.complete(outcome("two stars"));
        worker.join(2000);

        assertFalse(worker.isAlive());
        assertEquals(1, memory.writes.size());
        MemoryRecord record = memory.writes.get(0);
        assertEquals(MemoryKind.QUERY, record.kind());
        assertEquals(2, record.entryCount());
        assertEquals("inspect the system", record.entries().get(0).content());
        assertEquals("two stars", record.entries().get(1).content());
        assertTrue(record.entries().stream().map(entry -> entry.content())
                .noneMatch(content -> content.contains("query_system") || content.contains("technical_argument")));
        assertEquals(List.of("two stars"), speech.requests.stream().map(SpeechRequest::text).toList());
    }

    @Test
    void longQueryAnswerIsPassedWholeToMemoryAndVoicedVerbatim() {
        reducer.tools = List.of(new LlmToolDefinition("query_system", "Query system", "system status", List.of()));
        llm.results.add(ok(call("query_system", new JsonObject())));
        String fullAnswer = "First route leg has detailed coordinates. "
                + "Second route leg has another destination. ".repeat(8);
        execution.results.put("query_system", outcome(fullAnswer));
        IntelActionTypeResolver types = new IntelActionTypeResolver(id ->
                "query_system".equals(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);

        Thought.commander(Urgency.NORMAL, "show the current route", dependencies(types)).run();

        assertEquals(fullAnswer, memory.writes.getFirst().companionText(),
                "the memory gateway, not the thought, owns eventual gist compression");
        assertEquals(List.of(fullAnswer), speech.requests.stream().map(SpeechRequest::text).toList());
    }

    @Test
    void failedCommanderQueryIsVoicedButNeverRemembered() {
        reducer.tools = List.of(new LlmToolDefinition("query_system", "Query system", "system status", List.of()));
        llm.results.add(ok(call("query_system", new JsonObject())));
        execution.futures.put("query_system",
                CompletableFuture.failedFuture(new IllegalStateException("offline")));
        IntelActionTypeResolver types = new IntelActionTypeResolver(id ->
                "query_system".equals(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);

        Thought.commander(Urgency.NORMAL, "inspect the system", dependencies(types)).run();

        assertTrue(memory.writes.isEmpty());
        assertEquals(1, speech.requests.size());
        assertFalse(speech.requests.get(0).text().isBlank());
    }

    @Test
    void emptyCommanderQueryOutcomeIsNotRememberedOrVoiced() {
        reducer.tools = List.of(new LlmToolDefinition("query_system", "Query system", "system status", List.of()));
        llm.results.add(ok(call("query_system", new JsonObject())));
        IntelActionTypeResolver types = new IntelActionTypeResolver(id ->
                "query_system".equals(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);

        Thought.commander(Urgency.NORMAL, "inspect the system", dependencies(types)).run();

        assertTrue(memory.writes.isEmpty());
        assertTrue(speech.requests.isEmpty());
    }

    @Test
    void interruptedQueryCannotPublishLatePartialOrCompleteState() throws Exception {
        reducer.tools = List.of(new LlmToolDefinition("slow_query", "Slow query", "inspect", List.of()));
        llm.results.add(ok(call("slow_query", new JsonObject())));
        CompletableFuture<JsonObject> result = new CompletableFuture<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }
        };
        execution.futures.put("slow_query", result);
        IntelActionTypeResolver types = new IntelActionTypeResolver(id ->
                "slow_query".equals(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);
        Thought thought = Thought.commander(Urgency.NORMAL, "inspect", dependencies(types));
        Thread worker = new Thread(thought::run, "interrupted-query-test");
        worker.start();
        waitUntil(() -> execution.toolNames().contains("slow_query"));

        thought.interrupt();
        result.complete(outcome("late answer"));
        worker.join(2000);

        assertTrue(memory.writes.isEmpty());
        assertTrue(speech.requests.stream().noneMatch(request -> request.text().contains("late answer")));
    }

    @Test
    void eventNarrationPublishesOneEventRecord() {
        llm.results.add(ok(call(SpeakFunction.ID, text("Signals found."))));

        Thought.eventReaction(Urgency.NORMAL, "three biological signals", "Report briefly", dependencies()).run();

        assertEquals(1, memory.writes.size());
        MemoryRecord record = memory.writes.get(0);
        assertEquals(MemoryKind.EVENT, record.kind());
        assertEquals(1, record.entryCount());
        assertEquals("Signals found.", record.entries().get(0).content());
    }

    @Test
    void eventNarrationBoundsTransientPayloadAndStoresOnlyTheFinalLine() {
        llm.results.add(ok(call(SpeakFunction.ID, text("Bounded report."))));
        String eventData = "d".repeat(CompanionMemoryPolicy.eventDataMaxChars() + 500);
        String instructions = "i".repeat(CompanionMemoryPolicy.eventInstructionsMaxChars() + 500);

        Thought.eventReaction(Urgency.NORMAL, eventData, instructions, dependencies()).run();

        String currentInput = llm.requests.getFirst().messages().getLast().content();
        assertFalse(currentInput.contains(eventData));
        assertFalse(currentInput.contains(instructions));
        assertTrue(currentInput.contains("..."));
        assertEquals("Bounded report.", memory.writes.getFirst().eventFact());
    }

    @Test
    void verbatimEventPublishesAndVoicesOneEventRecordWithoutLlm() {
        Thought.eventVerbatim(Urgency.NORMAL, "Surface scan complete.", dependencies()).run();

        assertTrue(llm.requests.isEmpty());
        assertEquals(1, memory.writes.size());
        assertEquals(MemoryKind.EVENT, memory.writes.get(0).kind());
        assertEquals(1, memory.writes.get(0).entryCount());
        assertEquals("Surface scan complete.", memory.writes.get(0).entries().get(0).content());
        assertEquals(List.of("Surface scan complete."),
                speech.requests.stream().map(SpeechRequest::text).toList());
    }

    @Test
    void invalidResponseVoicesServiceFailureWithoutMemory() {
        llm.results.add(new LlmResult(LlmResult.Status.INVALID_RESPONSE, List.of()));

        Thought.commander(Urgency.NORMAL, "do it", dependencies()).run();

        assertEquals(1, speech.requests.size());
        assertFalse(speech.requests.get(0).text().isBlank());
        assertTrue(memory.writes.isEmpty());
        assertTrue(execution.requests.isEmpty());
    }

    @Test
    void dangerousActionExecutesOnlyAfterConfirmation() throws Exception {
        reducer.tools = List.of(new LlmToolDefinition("self_destruct", "Self destruct", "", List.of()));
        llm.results.add(ok(call("self_destruct", new JsonObject())));
        dangerous = invocation -> "self_destruct".equals(invocation.name());
        IntelActionTypeResolver types = new IntelActionTypeResolver(id ->
                "self_destruct".equals(id) ? IntelActionType.COMMAND : IntelActionType.SYSTEM);
        Thought thought = Thought.commander(Urgency.NORMAL, "self destruct", dependencies(types));
        Thread worker = new Thread(thought::run, "confirmation-test");
        worker.start();
        waitUntil(() -> !speech.requests.isEmpty());

        assertTrue(execution.requests.isEmpty());
        confirmation.confirm();
        // Wait for the dispatch itself, not just for the worker to look done: a bare join(2000) lapses on a
        // loaded machine and the assertions below then read a queue the confirmed action has not reached yet
        // (seen as "expected: <[self_destruct]> but was: <[]>" under a full suite run).
        waitUntil(() -> execution.toolNames().contains("self_destruct"));
        worker.join(2000);
        assertFalse(worker.isAlive(), "worker did not finish; the assertions below would be vacuous");

        assertEquals(List.of("self_destruct"), execution.toolNames());
        assertTrue(memory.writes.isEmpty());
    }

    /**
     * One utterance, two questions. Observed with mistral-small on "check the loadout, what is our cargo
     * capacity": both queries are the commander's, so both run and both are answered, in the order asked.
     */
    @Test
    void twoQuestionsInOneUtteranceAreBothAnswered() {
        execution.results.put("query_loadout", outcome("A-rated everything"));
        execution.results.put("query_cargo", outcome("256 tons"));
        llm.results.add(ok(call("query_loadout", new JsonObject()), call("query_cargo", new JsonObject())));

        Thought.commander(Urgency.NORMAL, "check the loadout what's our cargo capacity",
                dependencies(queriesNamed("query_loadout", "query_cargo"))).run();

        assertEquals(List.of("query_loadout", "query_cargo"), execution.toolNames(),
                "a batch runs in the order the commander asked, never at once");
        assertEquals(List.of("A-rated everything", "256 tons"),
                speech.requests.stream().map(SpeechRequest::text).toList());
    }

    /**
     * The pair is what the next turn reads as history, so one utterance stays one question: a record per call
     * would show the commander asking the same thing twice.
     */
    @Test
    void aBatchPublishesOneRecordJoiningItsAnswers() {
        execution.results.put("query_loadout", outcome("A-rated everything"));
        execution.results.put("query_cargo", outcome("256 tons"));
        llm.results.add(ok(call("query_loadout", new JsonObject()), call("query_cargo", new JsonObject())));

        Thought.commander(Urgency.NORMAL, "check the loadout what's our cargo capacity",
                dependencies(queriesNamed("query_loadout", "query_cargo"))).run();

        assertEquals(1, memory.writes.size());
        MemoryRecord record = memory.writes.get(0);
        assertEquals(MemoryKind.QUERY, record.kind());
        assertEquals("check the loadout what's our cargo capacity", record.entries().get(0).content());
        assertEquals("A-rated everything\n256 tons", record.entries().get(1).content());
    }

    /**
     * A single-call turn keeps filing its own record; the batch rule must not change the ordinary turn.
     */
    @Test
    void aSingleQueryStillPublishesItsOwnRecord() {
        execution.results.put("query_cargo", outcome("256 tons"));
        llm.results.add(ok(call("query_cargo", new JsonObject())));

        Thought.commander(Urgency.NORMAL, "what's our cargo capacity",
                dependencies(queriesNamed("query_cargo"))).run();

        assertEquals(1, memory.writes.size());
        assertEquals("256 tons", memory.writes.get(0).entries().get(1).content());
    }

    /**
     * A batch is several independent requests, not one transaction, so a handler that fails takes only its own
     * answer down. The commander still hears the failure and still gets the answer that did work.
     */
    @Test
    void aFailedCallDoesNotCancelTheRestOfTheBatch() {
        execution.futures.put("query_loadout",
                CompletableFuture.failedFuture(new IllegalStateException("offline")));
        execution.results.put("query_cargo", outcome("256 tons"));
        llm.results.add(ok(call("query_loadout", new JsonObject()), call("query_cargo", new JsonObject())));

        Thought.commander(Urgency.NORMAL, "check the loadout what's our cargo capacity",
                dependencies(queriesNamed("query_loadout", "query_cargo"))).run();

        assertEquals(List.of("query_loadout", "query_cargo"), execution.toolNames());
        assertEquals(2, speech.requests.size(), "the failure is voiced, then the answer that worked");
        assertEquals("256 tons", speech.requests.get(1).text());
        assertEquals(1, memory.writes.size());
        assertEquals("256 tons", memory.writes.get(0).entries().get(1).content(),
                "a failed query is voiced but never remembered, even inside a batch");
    }

    /**
     * The isolation has to hold for a defect in settlement itself, not only for a handler that reports failure:
     * that is what the batch's exception stage exists for. Here the speech gateway throws while the first answer
     * is being settled.
     */
    @Test
    void aThrowingSettlementDoesNotCancelTheRestOfTheBatch() {
        execution.results.put("query_loadout", outcome("A-rated everything"));
        execution.results.put("query_cargo", outcome("256 tons"));
        speech.failNextSubmit = true;
        llm.results.add(ok(call("query_loadout", new JsonObject()), call("query_cargo", new JsonObject())));

        Thought.commander(Urgency.NORMAL, "check the loadout what's our cargo capacity",
                dependencies(queriesNamed("query_loadout", "query_cargo"))).run();

        assertEquals(List.of("query_loadout", "query_cargo"), execution.toolNames(),
                "the second call must still run after the first one's settlement threw");
        assertEquals(1, memory.writes.size());
        assertEquals("256 tons", memory.writes.get(0).entries().get(1).content(),
                "only the answer that settled reaches memory");
    }

    /**
     * The action's own outcome is the answer, so a speak paired with it would only talk over the result.
     */
    @Test
    void speakAlongsideAnActionIsDropped() {
        execution.results.put("query_cargo", outcome("256 tons"));
        llm.results.add(ok(call(SpeakFunction.ID, text("checking the hold")),
                call("query_cargo", new JsonObject())));

        Thought.commander(Urgency.NORMAL, "what's our cargo capacity",
                dependencies(queriesNamed("query_cargo"))).run();

        assertEquals(List.of("query_cargo"), execution.toolNames());
        assertEquals(List.of("256 tons"), speech.requests.stream().map(SpeechRequest::text).toList());
    }

    /**
     * A clarification suspends the turn until the commander answers, so nothing may be batched around it: the
     * calls beside it belong to a turn that can actually finish.
     */
    @Test
    void requestInputInABatchReducesTheTurnToTheClarification() {
        LlmToolDefinition setSpeed = new LlmToolDefinition(
                "set_speed", "Set speed", "set speed",
                List.of(new ActionParameterSpec("amount", "number", true, "Speed amount", List.of("50"), null)));
        reducer.tools = List.of(setSpeed);
        reducer.catalog = List.of(setSpeed);
        JsonObject args = new JsonObject();
        args.addProperty(RequestInputFunction.PARAM_ACTION_ID, "set_speed");
        args.addProperty(RequestInputFunction.PARAM_PARAMETER_NAME, "amount");
        args.addProperty(RequestInputFunction.PARAM_QUESTION, "By how much?");
        llm.results.add(ok(call("query_cargo", new JsonObject()), call(RequestInputFunction.ID, args)));
        IntelActionTypeResolver types = new IntelActionTypeResolver(id -> switch (id) {
            case "set_speed" -> IntelActionType.COMMAND;
            case "query_cargo" -> IntelActionType.QUERY;
            default -> IntelActionType.SYSTEM;
        });

        Thought.commander(Urgency.NORMAL, "set speed and check the hold", dependencies(types)).run();

        assertTrue(execution.requests.isEmpty(), "nothing runs while the turn waits on the commander");
        assertEquals("set_speed", clarification.peek().orElseThrow().actionId());
        assertEquals(List.of("By how much?"), speech.requests.stream().map(SpeechRequest::text).toList());
    }

    /**
     * Confirmation gates the whole turn: a dangerous action cannot ride along with calls that just run.
     */
    @Test
    void aDangerousActionInABatchStillOwnsTheTurn() throws Exception {
        llm.results.add(ok(call("query_cargo", new JsonObject()), call("self_destruct", new JsonObject())));
        dangerous = invocation -> "self_destruct".equals(invocation.name());
        IntelActionTypeResolver types = new IntelActionTypeResolver(id -> switch (id) {
            case "self_destruct" -> IntelActionType.COMMAND;
            case "query_cargo" -> IntelActionType.QUERY;
            default -> IntelActionType.SYSTEM;
        });
        Thought thought = Thought.commander(Urgency.NORMAL, "check the hold then self destruct",
                dependencies(types));
        Thread worker = new Thread(thought::run, "batched-confirmation-test");
        worker.start();
        waitUntil(() -> !speech.requests.isEmpty());

        assertTrue(execution.requests.isEmpty(), "the query must not run before the confirmation is answered");
        confirmation.confirm();
        waitUntil(() -> execution.toolNames().contains("self_destruct"));
        worker.join(2000);
        assertFalse(worker.isAlive(), "worker did not finish; the assertions below would be vacuous");

        assertEquals(List.of("self_destruct"), execution.toolNames());
    }

    /**
     * The danger policy is an interface, so a query can be marked dangerous even though the production policy
     * flags only commands. Such a call settles outside the batch chain, through the confirmation path, and the
     * turn's record must not depend on which path ran it.
     */
    @Test
    void aConfirmedDangerousQueryStillPublishesItsAnswer() throws Exception {
        execution.results.put("scan_system", outcome("two stars"));
        llm.results.add(ok(call("scan_system", new JsonObject())));
        dangerous = invocation -> "scan_system".equals(invocation.name());
        Thought thought = Thought.commander(Urgency.NORMAL, "scan the system",
                dependencies(queriesNamed("scan_system")));
        Thread worker = new Thread(thought::run, "dangerous-query-test");
        worker.start();
        waitUntil(() -> !speech.requests.isEmpty()); // the code-voiced confirmation prompt

        confirmation.confirm();
        waitUntil(() -> !memory.writes.isEmpty());
        worker.join(2000);
        assertFalse(worker.isAlive(), "worker did not finish; the assertions below would be vacuous");

        assertEquals(1, memory.writes.size());
        assertEquals("two stars", memory.writes.get(0).entries().get(1).content());
    }

    /**
     * One order, one "affirmative" - each command still speaks its own outcome when it finishes.
     */
    @Test
    void severalCommandsAreAcknowledgedOnce() {
        execution.results.put("deploy_hardpoints", outcome("hardpoints deployed"));
        execution.results.put("full_stop", outcome("all stop"));
        llm.results.add(ok(call("deploy_hardpoints", new JsonObject()), call("full_stop", new JsonObject())));
        IntelActionTypeResolver types = new IntelActionTypeResolver(id ->
                "deploy_hardpoints".equals(id) || "full_stop".equals(id)
                        ? IntelActionType.COMMAND : IntelActionType.SYSTEM);

        Thought.commander(Urgency.NORMAL, "hardpoints and full stop", dependencies(types)).run();

        List<String> spoken = speech.requests.stream().map(SpeechRequest::text).toList();
        assertEquals(3, spoken.size(), "one acknowledgement, then one outcome per command: " + spoken);
        assertEquals(List.of("hardpoints deployed", "all stop"), spoken.subList(1, 3));
        assertTrue(memory.writes.isEmpty(), "commands stay out of conversational memory");
    }

    /**
     * Only a turn that settles a batch asks for one; every other round stays at a single call.
     */
    @Test
    void onlyTheCommanderRoundAsksForSeveralCalls() {
        llm.results.add(ok(call(SpeakFunction.ID, text("acknowledged"))));
        llm.results.add(ok(call(SpeakFunction.ID, text("scanning complete"))));

        Thought.commander(Urgency.NORMAL, "status", dependencies()).run();
        Thought.eventReaction(Urgency.NORMAL, "scan finished", null, dependencies()).run();

        assertTrue(llm.requests.get(0).maxToolCalls() > 1, "a commander utterance may hold several requests");
        assertEquals(1, llm.requests.get(1).maxToolCalls(), "a narration turn is one line");
    }

    private IntelActionTypeResolver queriesNamed(String... queryIds) {
        Set<String> queries = Set.of(queryIds);
        return new IntelActionTypeResolver(id ->
                queries.contains(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);
    }

    @Test
    void reflexQueryPublishesCompleteQueryRecordWithoutLlm() {
        execution.results.put("scan_system", outcome("two stars"));
        IntelActionTypeResolver types = new IntelActionTypeResolver(id ->
                "scan_system".equals(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);

        Thought.reflex(Urgency.NORMAL, "scan the system", "scan_system", dependencies(types)).run();

        assertTrue(llm.requests.isEmpty());
        assertEquals(1, memory.writes.size());
        assertEquals(MemoryKind.QUERY, memory.writes.get(0).kind());
        assertEquals("scan the system", memory.writes.get(0).entries().get(0).content());
        assertEquals("two stars", memory.writes.get(0).entries().get(1).content());
    }

    @Test
    void failedReflexQueryIsVoicedButNeverRemembered() {
        execution.futures.put("scan_system",
                CompletableFuture.failedFuture(new IllegalStateException("offline")));
        IntelActionTypeResolver types = new IntelActionTypeResolver(id ->
                "scan_system".equals(id) ? IntelActionType.QUERY : IntelActionType.SYSTEM);

        Thought.reflex(Urgency.NORMAL, "scan the system", "scan_system", dependencies(types)).run();

        assertTrue(memory.writes.isEmpty());
        assertEquals(1, speech.requests.size());
        assertFalse(speech.requests.get(0).text().isBlank());
    }

    private static LlmResult ok(LlmToolInvocation... invocations) {
        return new LlmResult(LlmResult.Status.OK, List.of(invocations));
    }

    private static LlmToolInvocation call(String name, JsonObject arguments) {
        return new LlmToolInvocation(UUID.randomUUID().toString(), name, arguments);
    }

    private static JsonObject text(String value) {
        JsonObject arguments = new JsonObject();
        arguments.addProperty(SpeakFunction.PARAM_TEXT, value);
        return arguments;
    }

    private static JsonObject outcome(String value) {
        JsonObject result = new JsonObject();
        result.addProperty("text_to_speech_response", value);
        return result;
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
        assertTrue(condition.getAsBoolean(), "condition was not reached before timeout");
    }

    private static final class FakeLlm implements LlmGateway {
        private final Deque<LlmResult> results = new ArrayDeque<>();
        private final List<LlmRequest> requests = new CopyOnWriteArrayList<>();

        @Override
        public CompletableFuture<LlmResult> submit(LlmRequest request) {
            requests.add(request);
            return CompletableFuture.completedFuture(results.removeFirst());
        }

        @Override
        public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class FakeSpeech implements SpeechGateway {
        private final List<SpeechRequest> requests = new CopyOnWriteArrayList<>();
        /** Makes the next submit throw, standing in for a defect raised while a turn settles. */
        private volatile boolean failNextSubmit;

        @Override
        public CompletableFuture<Void> submit(SpeechRequest request) {
            if (failNextSubmit) {
                failNextSubmit = false;
                throw new IllegalStateException("speech engine unavailable");
            }
            requests.add(request);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class FakeExecution implements ExecutionGateway {
        private final List<ExecutionRequest> requests = new CopyOnWriteArrayList<>();
        private final Map<String, JsonObject> results = new java.util.concurrent.ConcurrentHashMap<>();
        private final Map<String, CompletableFuture<JsonObject>> futures =
                new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public CompletableFuture<JsonObject> submit(ExecutionRequest request) {
            requests.add(request);
            return futures.getOrDefault(request.toolName(),
                    CompletableFuture.completedFuture(results.getOrDefault(request.toolName(), new JsonObject())));
        }

        private List<String> toolNames() {
            return requests.stream().map(ExecutionRequest::toolName).toList();
        }
    }

    private static final class FakeMemory implements MemoryGateway {
        private final List<MemoryRecord> writes = new CopyOnWriteArrayList<>();
        private final Map<MemoryKind, String> summaries = new EnumMap<>(MemoryKind.class);

        @Override
        public void write(MemoryRecord record) {
            writes.add(record);
        }

        @Override
        public List<MemoryRecord> readRecentHistory() {
            return List.of();
        }

        @Override
        public MemorySearchResult recallMatching(String query, int limit) {
            return MemorySearchResult.empty();
        }

        @Override
        public Map<MemoryKind, String> longTermSummaries() {
            return Map.copyOf(summaries);
        }

        @Override
        public void commitConsolidation(
                MemoryKind kind, List<MemoryRecord> batch, String summary
        ) {
            summaries.put(kind, summary);
        }

        @Override
        public List<MemoryRecord> savedTextRecords() {
            return List.of();
        }

        @Override
        public MemorySnapshot snapshot() {
            return new MemorySnapshot(List.of(), Map.of(), Map.of(), Map.copyOf(summaries), List.of());
        }
    }

    private static final class RecordingReducer implements CompanionActionReducer {
        private List<LlmToolDefinition> tools = List.of();
        private List<LlmToolDefinition> catalog = List.of();

        @Override
        public List<LlmToolDefinition> selectTools(Set<IntelActionCategory> categories, String input) {
            return tools;
        }

        @Override
        public Optional<LlmToolDefinition> findToolById(
                Set<IntelActionCategory> categories, String actionId,
                GameStateSnapshot gameStateSnapshot) {
            return catalog.stream().filter(tool -> actionId.equals(tool.name())).findFirst();
        }
    }
}
