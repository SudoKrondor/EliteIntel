package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.TimedReminderManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.util.StringUtls;

import java.util.Objects;

public class SetTimedReminderHandler implements CommandHandler {

    @Override
    public void handle(String action, JsonObject params, String responseText) {
        JsonElement keyEl = params.get("key");
        JsonElement minutesEl = params.get("minutes");
        String text;
        if (keyEl == null || keyEl.getAsString().equalsIgnoreCase("")) {
            text = StringUtls.localizedLlm("handler.reminder.genericText");
        } else {
            text = keyEl.getAsString();
        }

        if (isValidReminder(minutesEl)) {
            EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.reminder.invalidText")));
            return;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(minutesEl.getAsString().trim());
        } catch (NumberFormatException e) {
            EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.reminder.invalidDuration")));
            return;
        }

        if (minutes <= 0) {
            EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.reminder.durationZero")));
            return;
        }

        TimedReminderManager.getInstance().schedule(text, minutes);
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(
                StringUtls.localizedLlm(minutes == 1 ? "handler.reminder.setOne" : "handler.reminder.setMany", minutes)));
    }

    private static boolean isValidReminder(JsonElement minutesEl) {
        return minutesEl == null || Objects.equals(minutesEl.getAsString(), "");
    }
}
