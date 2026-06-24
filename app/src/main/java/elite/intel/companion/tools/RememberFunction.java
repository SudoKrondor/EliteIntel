package elite.intel.companion.tools;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.memory.CompanionMemoryLimits;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.util.json.JsonUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * System function: store a short fact in llm_memory (max 50 characters; code truncates). COMMANDER-only.
 */
@RegisterSystemFunction
public final class RememberFunction implements SystemFunction {

    public static final String ID = "remember";

    private static final String PARAM_CONTENT = "content";
    private static final String STATUS_REMEMBERED = "remembered";

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
                new ActionParameterSpec(PARAM_CONTENT, "string", true,
                        "The fact to remember; keep it to at most " + CompanionMemoryLimits.LLM_MEMORY_MAX_CONTENT_LENGTH + " characters.",
                        List.of(), null)
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER);
    }

    /**
     * Stores the {@code content} in llm_memory via the {@link elite.intel.companion.memory.MemoryGateway}
     * (length/dedup enforced there).
     */
    @Override
    public JsonObject handle(String action, JsonObject params, String text) {
        CompanionRuntime.memory().writeLlmMemory(JsonUtils.getAsStringOrEmpty(params, PARAM_CONTENT));
        JsonObject result = new JsonObject();
        result.addProperty(SystemFunctionResultFields.STATUS, STATUS_REMEMBERED);
        return result;
    }
}
