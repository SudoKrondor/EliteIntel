package elite.intel.vega.tools;

import com.google.gson.JsonObject;
import elite.intel.vega.CompanionRuntime;
import elite.intel.vega.CompanionRuntimeGraph;
import elite.intel.vega.CompanionRuntimeTestSupport;
import elite.intel.vega.mind.CompanionState;
import elite.intel.vega.model.llm.LlmToolDefinition;
import elite.intel.vega.model.speech.SpeechRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the executable system-function {@code handle}s drive the companion services reached statically
 * via {@link CompanionRuntime}: speak submits speech, request_input remains metadata-only, and find_action
 * queries the reducer. Fakes keep every boundary unit-testable.
 */
class SystemFunctionHandleTest {

    /**
     * Captures the last speech request.
     */
    private final java.util.List<SpeechRequest> spoken = new java.util.ArrayList<>();
    private final CompanionState state = new CompanionState();
    private CompanionRuntimeGraph runtimeGraph;

    @BeforeEach
    void install() {
        runtimeGraph = CompanionRuntimeTestSupport.install(
                null,
                request -> {
                    spoken.add(request);
                    return CompletableFuture.completedFuture(null);
                },
                null,
                null,
                (categories, input) -> List.of(new LlmToolDefinition("lower_landing_gear", "Lower the landing gear", "", List.of())),
                state);
    }

    @AfterEach
    void clear() {
        CompanionRuntimeTestSupport.uninstall(runtimeGraph);
    }

    private static JsonObject params(String key, String value) {
        JsonObject o = new JsonObject();
        o.addProperty(key, value);
        return o;
    }

    @Test
    void speakSubmitsToSpeechGateway() {
        JsonObject result = new SpeakFunction().handle("speak", params("text", "docking now"), "");

        assertEquals(1, spoken.size());
        assertEquals("docking now", spoken.get(0).text());
        assertEquals("spoken", result.get("status").getAsString());
    }

    @Test
    void speakRejectsBlankTextBeforeSubmittingToTts() {
        assertThrows(IllegalArgumentException.class,
                () -> new SpeakFunction().handle("speak", params("text", "   "), ""));
        assertEquals(0, spoken.size());
    }

    @Test
    void requestInputHandleIsMetadataOnly() {
        JsonObject p = params("action_id", "set_speed");
        p.addProperty("parameter_name", "amount");
        p.addProperty("question", "By how much?");

        JsonObject result = new RequestInputFunction().handle(RequestInputFunction.ID, p, "");

        assertEquals("input_requested", result.get("status").getAsString());
        assertEquals(0, spoken.size(), "CommanderThought owns validated clarification speech");
    }

    @Test
    void findActionReturnsReducerMatches() {
        JsonObject result = new FindActionFunction().handle("find_action", params("query", "gear"), "");

        assertEquals(1, result.getAsJsonArray("items").size());
        JsonObject item = result.getAsJsonArray("items").get(0).getAsJsonObject();
        assertEquals("lower_landing_gear", item.get("name").getAsString());
        assertEquals("Lower the landing gear", item.get("description").getAsString());
    }

}
