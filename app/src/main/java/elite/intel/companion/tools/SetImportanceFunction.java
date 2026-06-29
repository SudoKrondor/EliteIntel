package elite.intel.companion.tools;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.util.json.JsonUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * System function: the companion rates how important the current turn is to its own memory. COMMANDER-only.
 * The chosen level is read pre-execution by {@code CommanderThought} and stamped onto the entries written
 * this turn; it drives mid-term retention and long-term consolidation. This {@code handle} only validates and
 * echoes the level back into the flow - the stamping is the thought's job (a turn-local property, not state).
 */
@RegisterSystemFunction
public final class SetImportanceFunction implements SystemFunction {

    public static final String ID = "set_importance";
    public static final String PARAM_LEVEL = "level";

    private static final String STATUS_SET = "importance_set";
    private static final String ERROR_UNKNOWN = "unknown importance";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Rate how important this exchange is to your own memory, so what matters is kept and small talk "
                + "is let go. Call it exactly once per commander turn; an unrated turn counts as normal. If more "
                + "than one level fits, pick the highest, and max overrides the rest: any explicit order to "
                + "remember, note, write down, save, log, or not forget is max. Examples: "
                + "\"remember the docking code is Sierra Nine Four\" -> max; \"the abort word is Granite\" -> high; "
                + "\"we're holding course\" -> normal; \"nice and quiet out here\" -> low.";
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return List.of(
                new ActionParameterSpec(PARAM_LEVEL, "string", true,
                        "Memory importance for this turn: 'low' = small talk, banter, idle chatter; "
                                + "'normal' = routine exchange, status check, acknowledgement, ordinary command; "
                                + "'high' = a durable fact worth keeping (plan, name, callsign, target, codeword, "
                                + "agreement, rendezvous point); 'max' = an explicit order to remember, note, write "
                                + "down, save, log, or not forget, kept word-for-word. Pick the highest that fits.",
                        List.of(), null, MemoryImportance.ids())
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER);
    }

    /** Validates the level and echoes it back; the actual stamping is done by the owning thought. */
    @Override
    public JsonObject handle(String action, JsonObject params, String text) {
        MemoryImportance level = MemoryImportance.fromId(JsonUtils.getAsStringOrEmpty(params, PARAM_LEVEL));
        JsonObject result = new JsonObject();
        if (level == null) {
            result.addProperty(SystemFunctionResultFields.ERROR, ERROR_UNKNOWN);
            return result;
        }
        result.addProperty(SystemFunctionResultFields.STATUS, STATUS_SET);
        result.addProperty(SystemFunctionResultFields.IMPORTANCE, level.name().toLowerCase(Locale.ROOT));
        return result;
    }
}
