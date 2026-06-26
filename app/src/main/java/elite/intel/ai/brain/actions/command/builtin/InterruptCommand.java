package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.eventbus.GameEventBus;

/**
 * Stage-4b self-describing command for "interrupt speech".
 * Side effect on the TTS
 * subsystem (publishes TTSInterruptEvent).
 */
@RegisterCommand
public final class InterruptCommand implements IntelCommand {
    public static final String ID = "interrupt";

    @Override public String llmDescription() { return "Interrupt the companion's current speech or action."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        GameEventBus.publish(new TTSInterruptEvent());
    }
}
