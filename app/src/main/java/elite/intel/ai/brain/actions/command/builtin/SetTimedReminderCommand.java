package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.TimedReminderManager;
import elite.intel.eventbus.GameEventBus;
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


    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                "key", "string", true,
                "The reminder text to store and announce when the timer elapses.",
                List.of("check fuel", "scoop fuel"),
                "Extract the reminder text the commander dictates, verbatim.");
        key.validate();
        ActionParameterSpec minutes = new ActionParameterSpec(
                "minutes", "number", true,
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

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        JsonElement keyEl = params.get("key");
        JsonElement minutesEl = params.get("minutes");

        if (isValidReminder(keyEl, minutesEl)) {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.reminder.invalidText")));
            return;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(minutesEl.getAsString().trim());
        } catch (NumberFormatException e) {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.reminder.invalidDuration")));
            return;
        }

        if (minutes <= 0) {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.reminder.durationZero")));
            return;
        }

        String text = keyEl.getAsString();
        TimedReminderManager.getInstance().schedule(text, minutes);
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(
                StringUtls.localizedLlm(minutes == 1 ? "handler.reminder.setOne" : "handler.reminder.setMany", minutes)));
    }

    private static boolean isValidReminder(JsonElement keyEl, JsonElement minutesEl) {
        return keyEl == null || minutesEl == null || Objects.equals(keyEl.getAsString(), "none") || keyEl.getAsString().trim().isEmpty() || Objects.equals(keyEl.getAsString(), "");
    }
}
