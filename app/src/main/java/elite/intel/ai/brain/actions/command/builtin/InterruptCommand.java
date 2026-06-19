package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.gameapi.EventBusManager;

/**
 * Stage-4b self-describing command for "interrupt speech".
 * Side effect on the TTS
 * subsystem (publishes TTSInterruptEvent).
 */
@RegisterCommand
public final class InterruptCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.INTERRUPT;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        EventBusManager.publish(new TTSInterruptEvent());
    }
}
