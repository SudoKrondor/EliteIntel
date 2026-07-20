package elite.intel.ai.brain.vega.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.vega.clarify.ClarificationCoordinator;
import elite.intel.ai.brain.vega.confirm.ConfirmationCoordinator;
import elite.intel.ai.brain.vega.confirm.DangerousActionPolicy;
import elite.intel.ai.brain.vega.execution.ExecutionGateway;
import elite.intel.ai.brain.vega.mind.CompanionState;
import elite.intel.ai.brain.vega.mind.Thought;
import elite.intel.ai.brain.vega.mind.ThoughtDependencies;
import elite.intel.ai.brain.vega.model.GameStateSnapshot;
import elite.intel.ai.brain.vega.llm.LlmGateway;
import elite.intel.ai.brain.vega.memory.MemoryGateway;
import elite.intel.ai.brain.vega.memory.CompanionMemoryPolicy;
import elite.intel.ai.brain.vega.memory.MemorySearchResult;
import elite.intel.ai.brain.vega.memory.MemorySnapshot;
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        worker.join(2000);

        assertEquals(List.of("self_destruct"), execution.toolNames());
        assertTrue(memory.writes.isEmpty());
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

    private static LlmResult ok(LlmToolInvocation invocation) {
        return new LlmResult(LlmResult.Status.OK, List.of(invocation));
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

        @Override
        public CompletableFuture<Void> submit(SpeechRequest request) {
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
