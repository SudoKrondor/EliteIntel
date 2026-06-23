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
 * The {@code confirmation_request} marker flags a dangerous-action confirmation prompt (§2.13); only
 * such a speak runs immediately while a dangerous tool-call set is frozen. Execution is wired in a
 * later phase; this class only self-describes the tool.
 */
@RegisterSystemFunction
public final class SpeakFunction implements SystemFunction {

    public static final String ID = "speak";

    private static final String PARAM_TEXT = "text";
    private static final String PARAM_CONFIRMATION_REQUEST = "confirmation_request";
    private static final String STATUS_SPOKEN = "spoken";

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
                new ActionParameterSpec(PARAM_TEXT, "string", true,
                        "The exact words to speak to the commander.",
                        List.of(), null),
                new ActionParameterSpec(PARAM_CONFIRMATION_REQUEST, "boolean", false,
                        "True only if this phrase requests confirmation of a dangerous action; otherwise omit.",
                        List.of(), null)
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER, ThoughtSource.EVENT);
    }

    /**
     * Vocalizes the {@code text} through the companion {@link elite.intel.companion.speech.SpeechGateway}.
     * Fire-and-return: it does not block on playback (TTS runs async); the {@code confirmation_request}
     * marker is interpreted by the {@code Thought} (dangerous-confirmation gating), not here.
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
