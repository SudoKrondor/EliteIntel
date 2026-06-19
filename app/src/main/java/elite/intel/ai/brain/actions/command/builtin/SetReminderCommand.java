package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.customcommand.CustomCommandParameterSpec;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Owns its own execution: body migrated 1:1 from the legacy SetReminderHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class SetReminderCommand implements IntelCommand {

    private static final List<CustomCommandParameterSpec> PARAMETERS = buildParameters();

    private static List<CustomCommandParameterSpec> buildParameters() {
        CustomCommandParameterSpec key = new CustomCommandParameterSpec(
                "key", "string", true,
                "The reminder text to store.",
                List.of("check fuel", "restock limpets"),
                "Extract the reminder text the commander dictates, verbatim.");
        key.validate();
        return List.of(key);
    }

    @Override
    public String id() {
        return CommandIds.SET_REMINDER;
    }

    @Override
    public List<CustomCommandParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        ReminderManager reminderManager = ReminderManager.getInstance();
        JsonElement key = params.get("key");
        if (key != null) {
            reminderManager.setReminder(
                    key.getAsString(),
                    null
            );
        } else {
            EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.reminder.noKey")));
        }
    }
}
