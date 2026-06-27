package elite.intel.companion.tools;

import com.google.gson.JsonArray;
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
 * System function: search stored memory with a single plain-text query. The gateway looks across the
 * short-term timeline, all topics' mid-term memory and the conscious llm_memory facts at once and returns
 * the most recent matches (newest first), so the model never has to pick a scope or a topic. Short-term is
 * also inlined into the prompt, but searching it too gives a recall a complete picture. The long-term
 * summary is not searched here - it is always inlined whole. COMMANDER-only.
 */
@RegisterSystemFunction
public final class SearchInMemoryFunction implements SystemFunction {

    public static final String ID = "search_in_memory";

    private static final String PARAM_QUERY = "query";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Search your memory for what you already know. Call this before answering, or before saying you "
                + "don't know, when the commander asks about something they told you or that you remembered. "
                + "Pass a short query; it returns the most recent matching entries from across all topics and your "
                + "remembered facts.";
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return List.of(
                new ActionParameterSpec(PARAM_QUERY, "string", true,
                        "What to search your memory for (a word or short phrase).",
                        List.of(), null)
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER);
    }

    /** Reads memory via the {@link elite.intel.companion.memory.MemoryGateway}: one query across all areas. */
    @Override
    public JsonObject handle(String action, JsonObject params, String text) {
        String query = JsonUtils.getAsStringOrEmpty(params, PARAM_QUERY);
        JsonArray items = new JsonArray();
        CompanionRuntime.memory().recallMatching(query, CompanionMemoryLimits.MID_TERM_RECALL_LIMIT).forEach(items::add);
        JsonObject result = new JsonObject();
        result.add(SystemFunctionResultFields.ITEMS, items);
        return result;
    }
}
