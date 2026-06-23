package elite.intel.companion.tools;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.util.json.JsonUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * System function: ask the commander a short clarifying question and wait for the reply before acting.
 * Distinct from speak: it expects an answer that continues the exchange, rather than just uttering a
 * phrase. COMMANDER-only (an EVENT thought never converses). The awaiting-reply state is wired with the
 * thought/dispatcher in a later phase; this class only self-describes the tool.
 */
@RegisterSystemFunction
public final class ClarifyFunction implements SystemFunction {

    public static final String ID = "clarify";

    private static final String PARAM_QUESTION = "question";
    private static final String STATUS_ASKED = "asked";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String descriptionKey() {
        return ID;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return List.of(
                new ActionParameterSpec(PARAM_QUESTION, "string", true,
                        "The clarifying question to ask the commander.",
                        List.of(), null)
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER);
    }

    /**
     * Speaks the clarifying {@code question} via the companion
     * {@link elite.intel.companion.speech.SpeechGateway}. The awaiting-reply state (suspending the thought
     * until the commander answers) is owned by the thought/dispatcher in a later phase; this only utters it.
     */
    @Override
    public JsonObject handle(String action, JsonObject params, String text) {
        String question = JsonUtils.getAsStringOrEmpty(params, PARAM_QUESTION);
        CompanionRuntime.speech().submit(new SpeechRequest(UUID.randomUUID().toString(), question, Urgency.NORMAL));
        JsonObject result = new JsonObject();
        result.addProperty(SystemFunctionResultFields.STATUS, STATUS_ASKED);
        return result;
    }
}
