package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.TimedReminderManager;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;
import java.util.Objects;

/**
 * Owns its own execution: body migrated 1:1 from the legacy SetTimedReminderHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class SetTimedReminderCommand implements IntelCommand {
    public static final String ID = "set_timed_reminder";

    @Override
    public String llmDescription() {
        return "Schedule a reminder (text in 'key') to be announced after the number of minutes in 'minutes' (a countdown timer).";
    }


    private static final String PARAM_KEY = "key";
    private static final String PARAM_MINUTES = "minutes";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "string", true,
                "The reminder text to store and announce when the timer elapses.",
                List.of("check fuel", "scoop fuel"),
                "Extract the reminder text the commander dictates, verbatim.");
        key.validate();
        ActionParameterSpec minutes = new ActionParameterSpec(
                PARAM_MINUTES, "number", true,
                "Number of minutes until the reminder fires.",
                List.of("5", "30"),
                "Extract the number of minutes from phrasing like 'remind me in 5 minutes' (the 5).");
        minutes.validate();
        return List.of(key, minutes);
    }

    @Override
    public String id() {
        return ID;
    }

    /** App-side bookkeeping (no game input); executable in any location. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        JsonElement keyEl = params.get(PARAM_KEY);
        JsonElement minutesEl = params.get(PARAM_MINUTES);

        if (isValidReminder(keyEl, minutesEl)) {
            return StringUtls.localizedLlm("handler.reminder.invalidText");
        }

        int minutes;
        try {
            minutes = Integer.parseInt(minutesEl.getAsString().trim());
        } catch (NumberFormatException e) {
            return StringUtls.localizedLlm("handler.reminder.invalidDuration");
        }

        if (minutes <= 0) {
            return StringUtls.localizedLlm("handler.reminder.durationZero");
        }

        String text = keyEl.getAsString();
        TimedReminderManager.getInstance().schedule(text, minutes);
        return StringUtls.localizedLlm(minutes == 1 ? "handler.reminder.setOne" : "handler.reminder.setMany", minutes);
    }

    private static boolean isValidReminder(JsonElement keyEl, JsonElement minutesEl) {
        return keyEl == null || minutesEl == null || Objects.equals(keyEl.getAsString(), "none") || keyEl.getAsString().trim().isEmpty() || Objects.equals(keyEl.getAsString(), "");
    }
}
