package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * The bookmark number a command acts on - the number the bookmark card shows next to it.
 */
final class BookmarkNumber {

    static final String PARAM_KEY = "key";

    static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private BookmarkNumber() {
    }

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY,
                "number",
                true,
                "The bookmark's number as shown on the bookmark list (1 is the newest).",
                List.of("1", "3"),
                "Extract the bookmark number the commander said, as digits."
        );
        key.validate();
        return List.of(key);
    }

    /**
     * The number in the call, or null when there is none to read.
     */
    static Integer from(JsonObject params) {
        if (params == null) return null;
        JsonElement value = params.get(PARAM_KEY);
        if (value == null || value.isJsonNull()) return null;
        return StringUtls.getIntSafely(value.getAsString());
    }
}
