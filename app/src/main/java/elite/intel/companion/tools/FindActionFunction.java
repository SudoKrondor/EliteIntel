package elite.intel.companion.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.util.json.JsonUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * System function: search the action catalog for a ship/game action matching a description. COMMANDER-only.
 */
@RegisterSystemFunction
public final class FindActionFunction implements SystemFunction {

    public static final String ID = "find_action";

    private static final String PARAM_QUERY = "query";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Search the full catalog for a ship action or information query matching a description. Use this when what the commander wants, an action or a piece of information, is not among the functions offered this turn (only a relevant subset is offered each turn).";
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return List.of(
                new ActionParameterSpec(PARAM_QUERY, "string", true,
                        "Description of the action to find.",
                        List.of(), null)
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER);
    }

    /**
     * Searches the game-action catalog for actions matching {@code query}, reusing the shared
     * {@link elite.intel.companion.prompt.CompanionActionReducer} over all categories (COMMANDER-only tool),
     * and returns the matches as {@code {items:[{name, description}]}}.
     */
    @Override
    public JsonObject handle(String action, JsonObject params, String text) {
        List<LlmToolDefinition> matches = CompanionRuntime.reducer()
                .selectTools(EnumSet.allOf(IntelActionCategory.class), JsonUtils.getAsStringOrEmpty(params, PARAM_QUERY));
        JsonArray items = new JsonArray();
        for (LlmToolDefinition tool : matches) {
            JsonObject item = new JsonObject();
            item.addProperty(SystemFunctionResultFields.NAME, tool.name());
            item.addProperty(SystemFunctionResultFields.DESCRIPTION, tool.description());
            items.add(item);
        }
        JsonObject result = new JsonObject();
        result.add(SystemFunctionResultFields.ITEMS, items);
        return result;
    }
}
