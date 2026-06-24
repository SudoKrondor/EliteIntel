package elite.intel.companion.execution;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.confirm.DangerousActionPolicy;
import elite.intel.companion.llm.CompanionLlmGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.llm.LlmTransport;
import elite.intel.companion.llm.MistralLlmAdapter;
import elite.intel.companion.memory.SessionMemoryGateway;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.mind.ThoughtContext;
import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.prompt.PromptComposer;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deterministic end-to-end integration of the real companion graph - dispatcher, thought, real
 * {@link SessionMemoryGateway}/{@link PromptComposer}/{@link SystemFunctionProvider}/{@link CompanionState},
 * real {@link CompanionLlmGateway} + {@link MistralLlmAdapter} - driven by a scripted LLM transport (canned
 * Mistral responses) instead of a live model. It plays a short multi-topic conversation and asserts the
 * cross-cutting behaviour the unit tests cannot: the global topic moves across turns, a fact survives
 * remember -> recall, and a multi-round turn replays the assistant tool-call and round-trips the recalled
 * fact into the next prompt. No network, no real game input - the LLM transport and game tools are stubbed.
 */
class CompanionConversationIntegrationTest {

    private final ScriptedTransport transport = new ScriptedTransport();
    private final SessionMemoryGateway memory = new SessionMemoryGateway();
    private final CompanionState state = new CompanionState();
    private final RecordingSpeech speech = new RecordingSpeech();

    @AfterEach
    void clearRuntime() {
        CompanionRuntime.clear();
    }

    @Test
    void playsAMultiTopicConversationThroughTheRealGraph() {
        ThoughtDispatcher dispatcher = bootCompanion();

        // Turn 1: navigate -> topic moves to NAVIGATION, the companion speaks.
        transport.scripted.add(response(
                call("c1", "change_global_topic", "{\"topic\":\"navigation\"}"),
                call("c2", "speak", "{\"text\":\"Course plotted.\"}"),
                call("c3", "nothing_to_do", "{}")));
        // Turn 2: topic moves to SHIP_STATUS and a fact is remembered.
        transport.scripted.add(response(
                call("c4", "change_global_topic", "{\"topic\":\"ship_status\"}"),
                call("c5", "remember", "{\"content\":\"hull is solid\"}"),
                call("c6", "speak", "{\"text\":\"Noted.\"}"),
                call("c7", "nothing_to_do", "{}")));
        // Turn 3, round 1: search memory for the fact; round 2: speak using it (multi-round round-trip).
        transport.scripted.add(response(call("c8", "search_in_memory", "{\"query\":\"hull\"}")));
        transport.scripted.add(response(
                call("c9", "speak", "{\"text\":\"You said the hull is solid.\"}"),
                call("c10", "nothing_to_do", "{}")));

        dispatcher.start();
        dispatcher.submitCommanderInput("take us to the next system");
        dispatcher.submitCommanderInput("how is the ship");
        dispatcher.submitCommanderInput("what did I tell you to remember");
        dispatcher.stop(); // drains the lane: every turn has completed

        // The global topic moved across turns (real change_global_topic handle on the real state).
        assertEquals(ConversationTopic.SHIP_STATUS, state.globalTopic());
        // The fact survived remember -> llm_memory.
        assertTrue(memory.readLlmMemory().contains("hull is solid"));
        // The companion actually spoke the scripted phrases (real SpeakFunction -> SpeechGateway).
        assertTrue(speech.spoken.stream().anyMatch(t -> t.contains("Course plotted")));
        assertTrue(speech.spoken.stream().anyMatch(t -> t.contains("You said the hull is solid")));
        // The multi-round turn replayed the assistant tool-call and round-tripped the recalled fact.
        String lastRequestBody = transport.bodies.get(transport.bodies.size() - 1);
        assertTrue(lastRequestBody.contains("tool_calls"), "assistant tool-call turn must be replayed");
        assertTrue(lastRequestBody.contains("hull is solid"), "recall result must round-trip into the next round");
    }

    /** Wires the real companion graph against the scripted transport and stubbed game tools, then installs it. */
    private ThoughtDispatcher bootCompanion() {
        CompanionActionReducer reducer = (categories, input) -> List.of(); // no game tools this run
        LlmGateway llm = new CompanionLlmGateway(new MistralLlmAdapter(), transport);
        // Real execution gateway but with empty command/query maps (only system functions run) + synchronous lanes.
        ExecutionGateway execution = new CompanionExecutionGateway(
                Map.of(), Map.of(), systemFunctions(), Runnable::run, Runnable::run);
        DangerousActionPolicy notDangerous = invocation -> false;
        ConfirmationCoordinator coordinator = new ConfirmationCoordinator();

        CompanionRuntime.install(llm, speech, execution, memory, reducer, state);
        ThoughtContext ctx = new ThoughtContext(llm, speech, execution, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(), reducer, state,
                notDangerous, coordinator);
        return new ThoughtDispatcher(ctx);
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
