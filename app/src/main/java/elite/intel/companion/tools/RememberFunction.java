package elite.intel.companion.tools;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.CompanionGateways;
import elite.intel.companion.memory.LlmMemory;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.util.json.JsonUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * System function: store a short fact in llm_memory (max 50 characters; code truncates). COMMANDER-only.
 * Execution is wired in a later phase; this class only self-describes the tool.
 */
@RegisterSystemFunction
public final class RememberFunction implements SystemFunction {

    public static final String ID = "remember";

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
                new ActionParameterSpec("content", "string", true,
                        "The fact to remember; keep it to at most " + LlmMemory.MAX_CONTENT_LENGTH + " characters.",
                        List.of(), null)
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER);
    }

    /**
     * Stores the {@code content} in llm_memory via the {@link elite.intel.companion.memory.MemoryGateway}
     * (length/dedup enforced there). The backing store lands in Phase 4; until then the gateway call
     * surfaces its not-yet-implemented state.
     */
    @Override
    public JsonObject handle(String action, JsonObject params, String text) {
        CompanionGateways.memory().writeLlmMemory(JsonUtils.getAsStringOrEmpty(params, "content"));
        JsonObject result = new JsonObject();
        result.addProperty("status", "remembered");
        return result;
    }
}
