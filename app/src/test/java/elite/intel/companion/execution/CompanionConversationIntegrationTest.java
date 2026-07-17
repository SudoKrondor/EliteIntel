package elite.intel.companion.execution;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.command.builtin.RememberCommand;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.companion.CompanionRuntimeGraph;
import elite.intel.companion.CompanionRuntimeTestSupport;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.confirm.DangerousActionPolicy;
import elite.intel.companion.llm.CompanionLlmGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.llm.LlmTransport;
import elite.intel.companion.llm.MistralLlmAdapter;
import elite.intel.companion.memory.SessionMemoryGateway;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.mind.ThoughtDependencies;
import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.prompt.PromptComposer;
import elite.intel.companion.prompt.ReflexResolver;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.companion.tools.SystemFunction;
import elite.intel.companion.tools.SystemFunctionProvider;
import elite.intel.companion.tools.SystemFunctionRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deterministic end-to-end integration of the real companion graph - dispatcher, thought, real
 * {@link SessionMemoryGateway}/{@link PromptComposer}/{@link SystemFunctionProvider}/{@link CompanionState},
 * real {@link CompanionLlmGateway} + {@link MistralLlmAdapter} - driven by a scripted LLM transport (canned
 * Mistral responses) instead of a live model. It plays a short conversation and asserts the cross-cutting
 * behaviour the unit tests cannot: the built-in remember command stores only its extracted text argument and
 * durable memory is not automatically injected into a later prompt. No network or real game input is involved.
 */
class CompanionConversationIntegrationTest {

    private final ScriptedTransport transport = new ScriptedTransport();
    // Word-only memory (no embedding model) keeps this default-suite test off the heavy ONNX load; semantic
    // recall has its own coverage in SessionMemoryGatewayTest with a deterministic stand-in embedder.
    private final SessionMemoryGateway memory = new SessionMemoryGateway(() -> null);
    private final CompanionState state = new CompanionState();
    private final RecordingSpeech speech = new RecordingSpeech();
    private CompanionRuntimeGraph runtimeGraph;

    @AfterEach
    void clearRuntime() {
        CompanionRuntimeTestSupport.uninstall(runtimeGraph);
    }

    @Test
    void rememberedTextIsNotAutomaticallyInjectedIntoLaterPrompts() {
        ThoughtDispatcher dispatcher = bootCompanion();

        // Turn 1: ordinary dialogue.
        transport.scripted.add(response(
                call("c1", "speak", "{\"text\":\"Course plotted.\"}")));
        // Turn 2: the ordinary command stores only the extracted content and gets normal command acknowledgement.
        transport.scripted.add(response(
                call("c2", "remember", "{\"text\":\"the hull is solid\"}")));
        // Turn 3: no memory_search query is offered by this narrow test reducer, so durable memory stays absent.
        transport.scripted.add(response(
                call("c3", "speak", "{\"text\":\"I need a memory search for that.\"}")));

        // Submit the conversation as a real burst. The commander cognitive lane must preserve intake order, so
        // turn 3 sees the fact committed by turn 2 without the test manually draining between submissions.
        dispatcher.start();
        dispatcher.submitCommanderInput("take us to the next system");
        dispatcher.submitCommanderInput("remember that the hull is solid");
        dispatcher.submitCommanderInput("what did I tell you about the hull");
        awaitIdle(dispatcher);
        dispatcher.stop();

        assertEquals(MemoryKind.SAVED_TEXT, memory.savedTextRecords().get(0).kind());
        assertEquals("the hull is solid",
                memory.savedTextRecords().get(0).entries().get(0).content());
        // The companion actually spoke the scripted phrases (real SpeakFunction -> SpeechGateway).
        assertTrue(speech.spoken.stream().anyMatch(t -> t.contains("Course plotted")));
        assertTrue(speech.spoken.stream().anyMatch(t -> t.contains("memory search")));
        // The saved phrase is reachable only through memory_search, not through automatic prompt grounding.
        String lastRequestBody = transport.bodies.get(transport.bodies.size() - 1);
        assertFalse(lastRequestBody.contains("hull is solid"), "durable memory must not be injected before the turn");
    }

    /** Wires the real companion graph against the scripted transport and stubbed game tools, then installs it. */
    private ThoughtDispatcher bootCompanion() {
        RememberCommand remember = new RememberCommand();
        LlmToolDefinition rememberTool = new LlmToolDefinition(
                remember.id(), remember.llmDescription(), "remember {text:X}, remember that {text:X}",
                remember.parameters());
        CompanionActionReducer reducer = (categories, input) ->
                input.startsWith("remember that ") ? List.of(rememberTool) : List.of();
        LlmGateway llm = new CompanionLlmGateway(new MistralLlmAdapter(), transport);
        // Real execution gateway with the real remember command and synchronous lanes.
        Map<String, IntelAction> commands = Map.of(RememberCommand.ID, remember);
        ExecutionGateway execution = new CompanionExecutionGateway(
                commands, Map.of(), systemFunctions(), Runnable::run, Runnable::run);
        DangerousActionPolicy notDangerous = invocation -> false;
        ConfirmationCoordinator coordinator = new ConfirmationCoordinator();

        runtimeGraph = CompanionRuntimeTestSupport.install(llm, speech, execution, memory, reducer, state);
        ThoughtDependencies dependencies = new ThoughtDependencies(llm, speech, execution, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(), reducer, state,
                notDangerous, coordinator, runtimeGraph.runtimeGeneration());
        // Parameterized commands cannot use the reflex gate, so every turn stays LLM-driven.
        return new ThoughtDispatcher(dependencies, new ReflexResolver(() -> List.of(), notDangerous));
    }

    /** Waits for every cognitive stage and detached handler owned by the dispatcher to settle. */
    private static void awaitIdle(ThoughtDispatcher dispatcher) {
        long deadline = System.currentTimeMillis() + 5000;
        while (!dispatcher.isIdle() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        assertTrue(dispatcher.isIdle(), "conversation did not settle before the test deadline");
    }

    private static Map<String, SystemFunction> systemFunctions() {
        SystemFunctionRegistry registry = SystemFunctionRegistry.getInstance();
        if (registry.byId().isEmpty()) {
            registry.load();
        }
        return registry.byId();
    }

    // --- canned Mistral responses (arguments are JSON strings, as the real provider sends them) ---

    private static JsonObject call(String id, String name, String argumentsJson) {
        JsonObject function = new JsonObject();
        function.addProperty("name", name);
        function.addProperty("arguments", argumentsJson);
        JsonObject toolCall = new JsonObject();
        toolCall.addProperty("id", id);
        toolCall.addProperty("type", "function");
        toolCall.add("function", function);
        return toolCall;
    }

    private static JsonObject response(JsonObject... toolCalls) {
        JsonArray calls = new JsonArray();
        for (JsonObject c : toolCalls) {
            calls.add(c);
        }
        JsonObject message = new JsonObject();
        message.add("tool_calls", calls);
        JsonObject choice = new JsonObject();
        choice.add("message", message);
        JsonArray choices = new JsonArray();
        choices.add(choice);
        JsonObject root = new JsonObject();
        root.add("choices", choices);
        return root;
    }

    // --- doubles ---

    /** Returns scripted responses in order; records each rendered request body for round-trip assertions. */
    private static final class ScriptedTransport implements LlmTransport {
        final Queue<JsonObject> scripted = new ConcurrentLinkedQueue<>();
        final List<String> bodies = new CopyOnWriteArrayList<>();

        @Override
        public JsonObject send(String requestBody) {
            bodies.add(requestBody);
            JsonObject next = scripted.poll();
            return next != null ? next : new JsonObject(); // empty -> parsed as INVALID if ever over-polled
        }
    }

    private static final class RecordingSpeech implements SpeechGateway {
        final List<String> spoken = new CopyOnWriteArrayList<>();

        @Override
        public CompletableFuture<Void> submit(SpeechRequest request) {
            spoken.add(request.text());
            return CompletableFuture.completedFuture(null);
        }
    }
}
