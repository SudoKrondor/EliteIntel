package elite.intel.ai.brain.vega.tools;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.ai.brain.vega.model.ThoughtSource;
import elite.intel.ai.brain.vega.model.Urgency;
import elite.intel.ai.brain.vega.model.speech.SpeechRequest;
import elite.intel.ai.brain.vega.speech.SpeechGateway;
import elite.intel.util.json.JsonUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** System function that sends a phrase to the commander through the speech gateway. */
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
        return "Speak a message to the commander.";
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
     * Vocalizes the {@code text} through the companion {@link SpeechGateway}.
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
