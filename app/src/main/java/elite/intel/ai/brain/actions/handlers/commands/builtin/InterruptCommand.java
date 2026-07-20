package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.Status;

/**
 * Stage-4b self-describing command for "interrupt speech".
 * Side effect on the TTS
 * subsystem (publishes TTSInterruptEvent).
 */
@RegisterCommand
public final class InterruptCommand implements IntelCommand {
    public static final String ID = "interrupt";

    @Override
    public String llmDescription() {
        return "Interrupt and immediately stop the companion's current speech.";
    }


    @Override
    public String id() {
        return ID;
    }

    /** Companion-side control (interrupts TTS); executable in any location. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        GameEventBus.publish(new TTSInterruptEvent());
        return null;
    }
}
