package elite.intel.companion.tools;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.ThoughtSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * COMMANDER-only control function declaring that one matching game action lacks a required argument. The
 * {@code CommanderThought} validates the referenced offered tool, voices the question, and persists the
 * continuation; {@link #handle} is deliberately side-effect free for defensive direct execution.
 */
@RegisterSystemFunction
public final class RequestInputFunction implements SystemFunction {

    public static final String ID = "request_input";
    public static final String PARAM_ACTION_ID = "action_id";
    public static final String PARAM_PARAMETER_NAME = "parameter_name";
    public static final String PARAM_QUESTION = "question";

    private static final String STATUS_INPUT_REQUESTED = "input_requested";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Ask the commander for exactly one missing required parameter of an offered function that clearly "
                + "matches the order. Use only when the function cannot be called without inventing that value. "
                + "action_id and parameter_name must exactly match the offered function schema. Do not use for chat, "
                + "unsupported requests, or ambiguity between different functions.";
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return List.of(
                new ActionParameterSpec(PARAM_ACTION_ID, "string", true,
                        "Exact id of the matching offered game tool that is waiting for input.",
                        List.of(), null),
                new ActionParameterSpec(PARAM_PARAMETER_NAME, "string", true,
                        "Exact name of one required parameter missing from that tool invocation.",
                        List.of(), null),
                new ActionParameterSpec(PARAM_QUESTION, "string", true,
                        "The exact concise question to speak to the commander in the configured response language.",
                        List.of(), null)
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER);
    }

    /** Returns metadata only; the owning thought performs the validated cross-turn state transition. */
    @Override
    public JsonObject handle(String action, JsonObject params, String text) {
        JsonObject result = new JsonObject();
        result.addProperty(SystemFunctionResultFields.STATUS, STATUS_INPUT_REQUESTED);
        return result;
    }
}
