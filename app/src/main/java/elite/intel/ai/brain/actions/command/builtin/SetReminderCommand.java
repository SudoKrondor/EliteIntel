package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.db.managers.ReminderManager;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Owns its own execution: body migrated 1:1 from the legacy SetReminderHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class SetReminderCommand implements IntelCommand {
    public static final String ID = "set_reminder";

    @Override public String llmDescription() { return "Set a reminder."; }


    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                "key", "string", true,
                "The reminder text to store.",
                List.of("check fuel", "restock limpets"),
                "Extract the reminder text the commander dictates, verbatim.");
        key.validate();
        return List.of(key);
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
    public JsonObject execute(JsonObject params, String responseText) {
        ReminderManager reminderManager = ReminderManager.getInstance();
        JsonElement key = params.get("key");
        if (key == null) {
            return CommandOutcome.speak(StringUtls.localizedLlm("handler.reminder.noKey"));
        }
        reminderManager.setReminder(key.getAsString(), null);
        return null;
    }
}
