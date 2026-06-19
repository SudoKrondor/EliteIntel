package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.gameapi.EventBusManager;

/**
 * Stage-4b self-describing command for "interrupt speech".
 * Owns its own execution (ownsExecution() == true): the dispatch map routes this
 * command's execute() in place of the legacy ShutUpHandler. Side effect on the TTS
 * subsystem (publishes TTSInterruptEvent).
 */
@RegisterCommand
public final class InterruptCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.INTERRUPT;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        EventBusManager.publish(new TTSInterruptEvent());
    }
}
