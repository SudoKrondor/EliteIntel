package elite.intel.companion.tools;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.util.json.JsonUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * System function: classify the current commander turn for memory organization, in a single call carrying
 * both the turn's {@code topic} and its {@code importance}. It replaces the separate change-topic and
 * rate-importance tools - the two pieces of metadata that tag every commander turn now travel together.
 * COMMANDER-only: only the commander's conversation classifies a turn; an EVENT thought never calls it (an
 * event's topic comes from a static event-type map and its importance is fixed).
 * <p>
 * The {@code topic} has a side effect: it moves the sticky global topic on the shared
 * {@link elite.intel.companion.mind.CompanionState}, the single topic used to tag the commander's memory
 * entries. The {@code importance} is turn-local - {@link elite.intel.companion.mind.CommanderThought} reads
 * it pre-execution and stamps it onto the entries written this turn (it is not stored as state). This
 * {@code handle} sets the topic and echoes both back; the importance stamping is the thought's job.
 */
@RegisterSystemFunction
public final class ClassifyTurnFunction implements SystemFunction {

    public static final String ID = "classify_turn";
    public static final String PARAM_TOPIC = "topic";
    public static final String PARAM_IMPORTANCE = "importance";

    private static final String STATUS_CLASSIFIED = "turn_classified";
    private static final String ERROR_UNKNOWN_TOPIC = "unknown topic";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Classify this commander turn for memory organization. This function only sets the turn's "
                + "importance and topic; it never answers the commander and never stores new facts by itself.";
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return List.of(
                new ActionParameterSpec(PARAM_IMPORTANCE, "string", true,
                        "Memory importance for this turn: 'low' = small talk, banter, idle chatter; "
                                + "'normal' = routine exchange, status check, acknowledgement, ordinary command; "
                                + "'high' = a durable fact worth keeping (plan, name, callsign, target, codeword, "
                                + "agreement, rendezvous point); 'max' = an explicit order to remember, note, write "
                                + "down, save, log, or not forget, kept word-for-word. Pick the highest that fits.",
                        List.of(), null, MemoryImportance.ids()),
                new ActionParameterSpec(PARAM_TOPIC, "string", true,
                        "The topic this turn belongs to, chosen from the valid topic ids listed in the TOPICS "
                                + "section. Keep the current topic when the turn still fits it; move it only on a "
                                + "real subject change.",
                        List.of(), null, ConversationTopic.selectableIds())
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER);
    }

    /**
     * Moves the global topic on the shared {@link elite.intel.companion.mind.CompanionState} and echoes both
     * the topic and the importance back. An unknown topic is an error; a missing or unknown importance defaults
     * to {@link MemoryImportance#NORMAL}, mirroring the thought's own fallback for an unrated turn.
     */
    @Override
    public JsonObject handle(String action, JsonObject params, String text) {
        ConversationTopic topic = ConversationTopic.fromSelectableId(JsonUtils.getAsStringOrEmpty(params, PARAM_TOPIC));
        JsonObject result = new JsonObject();
        if (topic == null) {
            result.addProperty(SystemFunctionResultFields.ERROR, ERROR_UNKNOWN_TOPIC);
            return result;
        }
        CompanionRuntime.state().setGlobalTopic(topic);
        MemoryImportance importance = MemoryImportance.fromId(JsonUtils.getAsStringOrEmpty(params, PARAM_IMPORTANCE));
        // WHY: unlike topic (whose side effect this handle owns, so a bad value must fail loud), importance is
        // only echoed back - CommanderThought stamps it from the same args and also defaults an unrated turn to
        // NORMAL, so an unknown value defaults here too instead of erroring.
        if (importance == null) {
            importance = MemoryImportance.NORMAL;
        }
        result.addProperty(SystemFunctionResultFields.STATUS, STATUS_CLASSIFIED);
        result.addProperty(SystemFunctionResultFields.TOPIC, topic.id());
        result.addProperty(SystemFunctionResultFields.IMPORTANCE, importance.name().toLowerCase(Locale.ROOT));
        return result;
    }
}
