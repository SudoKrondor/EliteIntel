package elite.intel.companion.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.util.json.JsonUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * System function: load stored memory. {@code scope=llm_memory} returns all remembered facts;
 * {@code scope=topic_memory} returns entries for one topic (topic required, query optional). Short-term
 * timeline and long-term summary are not recallable here (already in the prompt). COMMANDER-only.
 * Execution is wired in a later phase; this class only self-describes the tool.
 */
@RegisterSystemFunction
public final class RecallFunction implements SystemFunction {

    public static final String ID = "recall";

    private static final String PARAM_SCOPE = "scope";
    private static final String PARAM_TOPIC = "topic";
    private static final String PARAM_QUERY = "query";
    private static final String SCOPE_TOPIC_MEMORY = "topic_memory";
    private static final String ERROR_UNKNOWN_TOPIC = "unknown topic";

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
                new ActionParameterSpec(PARAM_SCOPE, "string", true,
                        "Either llm_memory or topic_memory.",
                        List.of(), null),
                new ActionParameterSpec(PARAM_TOPIC, "string", false,
                        "Required when scope is topic_memory: the topic id to recall.",
                        List.of(), null),
                new ActionParameterSpec(PARAM_QUERY, "string", false,
                        "Optional plain-text filter within the topic.",
                        List.of(), null)
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER);
    }

    /** Default cap for topic-memory recall until it is made a setting. */
    private static final int TOPIC_RECALL_LIMIT = 10;

    /**
     * Reads memory via the {@link elite.intel.companion.memory.MemoryGateway}: {@code scope=llm_memory}
     * returns all remembered facts; {@code scope=topic_memory} returns entries for the given topic
     * (optional plain-text {@code query} filter). The backing store lands in Phase 4; until then the
     * gateway call surfaces its not-yet-implemented state.
     */
    @Override
    public JsonObject handle(String action, JsonObject params, String text) {
        String scope = JsonUtils.getAsStringOrEmpty(params, PARAM_SCOPE).trim().toLowerCase(Locale.ROOT);
        JsonObject result = new JsonObject();
        result.addProperty(SystemFunctionResultFields.SCOPE, scope);
        JsonArray items = new JsonArray();
        if (scope.equals(SCOPE_TOPIC_MEMORY)) {
            ConversationTopic topic = ConversationTopic.fromSelectableId(JsonUtils.getAsStringOrEmpty(params, PARAM_TOPIC));
            if (topic == null) {
                result.addProperty(SystemFunctionResultFields.ERROR, ERROR_UNKNOWN_TOPIC);
                result.add(SystemFunctionResultFields.ITEMS, items);
                return result;
            }
            for (MemoryEntry entry : CompanionRuntime.memory()
                    .recallTopicMemory(topic, JsonUtils.getAsStringOrEmpty(params, PARAM_QUERY), TOPIC_RECALL_LIMIT)) {
                items.add(entry.content());
            }
        } else {
            CompanionRuntime.memory().readLlmMemory().forEach(items::add);
        }
        result.add(SystemFunctionResultFields.ITEMS, items);
        return result;
    }
}
