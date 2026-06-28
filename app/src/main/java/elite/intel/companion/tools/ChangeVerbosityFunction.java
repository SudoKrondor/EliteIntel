package elite.intel.companion.tools;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Verbosity;
import elite.intel.util.json.JsonUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * System function: change the companion's verbosity mode (how freely EVENT thoughts comment).
 * COMMANDER-only; its {@code handle} sets the verbosity slot on the shared {@code CompanionState}.
 */
@RegisterSystemFunction
public final class ChangeVerbosityFunction implements SystemFunction {

    public static final String ID = "change_verbosity";

    private static final String PARAM_VERBOSITY = "verbosity";
    private static final String STATUS_CHANGED = "verbosity_changed";
    private static final String ERROR_UNKNOWN = "unknown verbosity";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Change how talkative you are by switching the verbosity mode. Use this when the commander asks you to be quieter or more talkative.";
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return List.of(
                new ActionParameterSpec(PARAM_VERBOSITY, "string", true,
                        "The new verbosity mode: quiet, normal, or chatty.",
                        List.of(), null, Verbosity.ids())
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER);
    }

    /** Sets the verbosity slot on the shared {@link elite.intel.companion.mind.CompanionState}. */
    @Override
    public JsonObject handle(String action, JsonObject params, String text) {
        String raw = JsonUtils.getAsStringOrEmpty(params, PARAM_VERBOSITY).trim().toUpperCase(Locale.ROOT);
        JsonObject result = new JsonObject();
        try {
            Verbosity verbosity = Verbosity.valueOf(raw);
            CompanionRuntime.state().setVerbosity(verbosity);
            result.addProperty(SystemFunctionResultFields.STATUS, STATUS_CHANGED);
            result.addProperty(SystemFunctionResultFields.VERBOSITY, verbosity.name().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            result.addProperty(SystemFunctionResultFields.ERROR, ERROR_UNKNOWN);
        }
        return result;
    }
}
