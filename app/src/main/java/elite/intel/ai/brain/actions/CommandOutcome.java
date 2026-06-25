package elite.intel.ai.brain.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;

/**
 * Single owner of the shape of an {@link IntelAction} outcome: the structured result an action returns
 * from {@link IntelAction#handle} instead of narrating itself by publishing voice events. The active
 * conversational owner of the current mode renders it - the legacy {@code ResponseRouter} speaks it, while
 * in companion mode {@code CompanionExecutionGateway} hands it back as a tool result for the consciousness
 * to narrate.
 * <p>
 * The spoken text reuses {@link AIConstants#PROPERTY_TEXT_TO_SPEECH_RESPONSE}, so this is the same outcome
 * contract query handlers already produce; commands simply stop being the exception. Criticality selects
 * the legacy voice channel: a normal outcome maps to an interruptible {@code AiVoxResponseEvent}, a
 * {@link #critical} outcome to a non-interruptible {@code MissionCriticalAnnouncementEvent}.
 * <p>
 * An outcome carries only the final result; progress / "stand by" chatter is intentionally not modeled
 * (the mode owner decides any interim speech). A side-effect action with nothing to say keeps returning
 * {@code null} from {@code handle} - it does not need an outcome at all.
 */
public final class CommandOutcome {

    /** Outcome-owned flag selecting the non-interruptible mission-critical voice channel. */
    private static final String MISSION_CRITICAL = "mission_critical";

    private CommandOutcome() {
    }

    /** Interruptible spoken outcome. */
    public static JsonObject speak(String text) {
        return withText(text, false);
    }

    /** Non-interruptible, mission-critical spoken outcome. */
    public static JsonObject critical(String text) {
        return withText(text, true);
    }

    private static JsonObject withText(String text, boolean critical) {
        JsonObject outcome = new JsonObject();
        outcome.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, text == null ? "" : text);
        if (critical) {
            outcome.addProperty(MISSION_CRITICAL, true);
        }
        return outcome;
    }

    /** The spoken text of an outcome, or empty string when it carries none / is null. */
    public static String spokenText(JsonObject outcome) {
        if (outcome == null || !outcome.has(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE)) {
            return "";
        }
        JsonElement el = outcome.get(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE);
        if (el == null || el.isJsonNull() || !el.isJsonPrimitive()) {
            return "";
        }
        return el.getAsString();
    }

    /** Whether the outcome should use the non-interruptible mission-critical voice channel. */
    public static boolean isCritical(JsonObject outcome) {
        if (outcome == null || !outcome.has(MISSION_CRITICAL)) {
            return false;
        }
        JsonElement el = outcome.get(MISSION_CRITICAL);
        return el != null && el.isJsonPrimitive() && el.getAsBoolean();
    }
}
