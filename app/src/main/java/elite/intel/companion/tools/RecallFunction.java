package elite.intel.companion.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.CompanionGateways;
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
                new ActionParameterSpec("scope", "string", true,
                        "Either llm_memory or topic_memory.",
                        List.of(), null),
                new ActionParameterSpec("topic", "string", false,
                        "Required when scope is topic_memory: the topic id to recall.",
                        List.of(), null),
                new ActionParameterSpec("query", "string", false,
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
        String scope = JsonUtils.getAsStringOrEmpty(params, "scope").trim().toLowerCase(Locale.ROOT);
        JsonObject result = new JsonObject();
        result.addProperty("scope", scope);
        JsonArray items = new JsonArray();
        if (scope.equals("topic_memory")) {
            ConversationTopic topic = parseTopic(JsonUtils.getAsStringOrEmpty(params, "topic"));
            if (topic == null) {
                result.addProperty("error", "unknown topic");
                result.add("items", items);
                return result;
            }
            for (MemoryEntry entry : CompanionGateways.memory()
                    .recallTopicMemory(topic, JsonUtils.getAsStringOrEmpty(params, "query"), TOPIC_RECALL_LIMIT)) {
                items.add(entry.content());
            }
        } else {
            CompanionGateways.memory().readLlmMemory().forEach(items::add);
        }
        result.add("items", items);
        return result;
    }

    /** Resolves a topic id to a selectable {@link ConversationTopic}, or null when unknown/non-selectable. */
    private static ConversationTopic parseTopic(String id) {
        try {
            ConversationTopic topic = ConversationTopic.valueOf(id.trim().toUpperCase(Locale.ROOT));
            return topic.selectable() ? topic : null;
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
