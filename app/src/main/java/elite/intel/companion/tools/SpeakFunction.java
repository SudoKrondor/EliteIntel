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
 * System function: speak a phrase to the commander via the SpeechGateway. Available to both sources.
 * Dangerous-action confirmation is not the model's concern: it is detected and voiced by the
 * {@code CommanderThought} after the response (§2.13), so this function carries no confirmation marker.
 */
@RegisterSystemFunction
public final class SpeakFunction implements SystemFunction {

    public static final String ID = "speak";

    /** Argument carrying the text to vocalize; read by the {@code Thought} to record the companion's words. */
    public static final String PARAM_TEXT = "text";
    private static final String STATUS_SPOKEN = "spoken";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Speak only when no offered function matches the commander's request. Use it for conversation, a "
                + "grounded answer, ambiguity between functions, or an unsupported request. Never "
                + "use it to request a missing required argument; call request_input instead. Never use it to "
                + "acknowledge, promise, or describe a matching function; call that function instead.";
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return List.of(
                new ActionParameterSpec(PARAM_TEXT, "string", true,
                        "The exact words to speak to the commander.",
                        List.of(), null)
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER, ThoughtSource.EVENT);
    }

    /**
     * Vocalizes the {@code text} through the companion {@link elite.intel.companion.speech.SpeechGateway}.
     * Fire-and-return: it does not block on playback (TTS runs async).
     */
    @Override
    public JsonObject handle(String action, JsonObject params, String text) {
        String toSpeak = JsonUtils.getAsStringOrEmpty(params, PARAM_TEXT);
        CompanionRuntime.speech().submit(new SpeechRequest(UUID.randomUUID().toString(), toSpeak, Urgency.NORMAL));
        JsonObject result = new JsonObject();
        result.addProperty(SystemFunctionResultFields.STATUS, STATUS_SPOKEN);
        return result;
    }
}
