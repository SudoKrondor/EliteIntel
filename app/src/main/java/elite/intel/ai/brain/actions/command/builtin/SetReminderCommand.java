package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.managers.ReminderManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Owns its own execution: body migrated 1:1 from the legacy SetReminderHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class SetReminderCommand implements IntelCommand {
    public static final String ID = "set_reminder";

    @Override
    public String llmDescription() {
        return "Save a free-text spoken reminder note (in 'key') to recall later; no timer.";
    }


    private static final String PARAM_KEY = "key";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "string", true,
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
    public void execute(JsonObject params, String responseText) {
        ReminderManager reminderManager = ReminderManager.getInstance();
        JsonElement key = params.get(PARAM_KEY);
        if (key != null) {
            reminderManager.setReminder(
                    key.getAsString(),
                    null
            );
        } else {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.reminder.noKey")));
        }
    }
}
