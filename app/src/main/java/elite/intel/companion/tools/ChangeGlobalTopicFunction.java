package elite.intel.companion.tools;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.util.json.JsonUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * System function: change the global conversation topic, chosen from the valid topics. COMMANDER-only -
 * only the commander's conversation moves the topic; an EVENT thought never changes it (an event's topic
 * for memory tagging comes from a static event-type map, not from the LLM).
 * <p>
 * The global topic is the single topic used to tag the commander's memory entries; it lives on the shared
 * {@link elite.intel.companion.mind.CompanionState}.
 */
@RegisterSystemFunction
public final class ChangeGlobalTopicFunction implements SystemFunction {

    public static final String ID = "change_global_topic";

    private static final String PARAM_TOPIC = "topic";
    private static final String STATUS_CHANGED = "topic_changed";
    private static final String ERROR_UNKNOWN_TOPIC = "unknown topic";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Switch the global conversation topic to a different one, chosen from the listed valid topics. Call this only when this turn actually moves the conversation to a topic different from the current topic shown in the current input; otherwise leave the topic unchanged.";
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return List.of(
                new ActionParameterSpec(PARAM_TOPIC, "string", true,
                        "One of the valid topic ids listed in the TOPICS section.",
                        List.of(), null)
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER);
    }

    /** Sets the global topic on the shared {@link elite.intel.companion.mind.CompanionState}. */
    @Override
    public JsonObject handle(String action, JsonObject params, String text) {
        ConversationTopic topic = ConversationTopic.fromSelectableId(JsonUtils.getAsStringOrEmpty(params, PARAM_TOPIC));
        JsonObject result = new JsonObject();
        if (topic == null) {
            result.addProperty(SystemFunctionResultFields.ERROR, ERROR_UNKNOWN_TOPIC);
            return result;
        }
        CompanionRuntime.state().setGlobalTopic(topic);
        result.addProperty(SystemFunctionResultFields.STATUS, STATUS_CHANGED);
        result.addProperty(SystemFunctionResultFields.TOPIC, topic.name().toLowerCase(Locale.ROOT));
        return result;
    }
}
