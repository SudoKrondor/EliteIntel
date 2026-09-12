package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;

import java.util.List;

import static elite.intel.util.StringUtls.getIntSafely;

/**
 * The optional "how far to look" argument the hunting-ground searches share: one parameter spec offered to
 * the LLM, and one reading of what it sent back.
 */
final class SearchRange {

    private static final String PARAM_KEY = "key";

    /**
     * The one optional parameter these commands take, in the form the LLM is shown.
     */
    static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private SearchRange() {
    }

    /**
     * The range in light years the commander stated, or {@code defaultLy} when they stated none - or
     * nothing usable, since a range of zero or less is not a range.
     */
    static int lightYears(JsonObject params, int defaultLy) {
        if (params == null || params.get(PARAM_KEY) == null || params.get(PARAM_KEY).isJsonNull()) {
            return defaultLy;
        }
        Integer stated = getIntSafely(params.get(PARAM_KEY).getAsString());
        return stated == null || stated <= 0 ? defaultLy : stated;
    }

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "number", false,
                "Maximum search range in light years (ly). If omitted, a default range is used.",
                List.of("50", "100"),
                "Extract the range limit in light years if the commander states one; otherwise omit it.");
        key.validate();
        return List.of(key);
    }
}
