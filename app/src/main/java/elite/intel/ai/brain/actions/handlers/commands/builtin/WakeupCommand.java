package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.eventbus.UiBus;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.ToggleSleepWakeEvent;

/**
 * "Wake up" - reopens the Sleep/Wake gate by voice.
 * <p>
 * Reachable only because the STT pipeline lets a wake phrase past a closed gate; everything else spoken while
 * asleep is discarded before it gets this far.
 */
@RegisterCommand
public final class WakeupCommand implements IntelCommand {
    public static final String ID = "wakeup";

    @Override
    public String llmDescription() {
        return "Start listening to voice commands: the commander is telling the companion to wake up, pay attention, "
                + "or begin accepting voice input. Being told to listen is this action, not conversation - call it "
                + "even when the companion appears to be awake already.";
    }

    @Override
    public String id() {
        return ID;
    }

    /**
     * Must stay reachable everywhere: it is what wakes the companion from sleep. Withheld only under
     * push-to-talk, where there is no sleep to wake from.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return !SystemSession.getInstance().isPushToTalkEnabled();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        UiBus.publish(new ToggleSleepWakeEvent(false));
        return null;
    }
}
