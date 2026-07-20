package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.eventbus.UiBus;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.PttModeChangedEvent;
import elite.intel.ui.event.VoiceInputModeToggleEvent;

/**
 * Stage-4b self-describing command for "wake up".
 * Side effects on the
 * listening/push-to-talk subsystem (SystemSession + PttModeChangedEvent/VoiceInputModeToggleEvent).
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

    /** Companion-side control that must stay reachable everywhere (it wakes the companion from sleep). */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        SystemSession session = SystemSession.getInstance();
        if (session.isPushToTalkEnabled() && !session.isPushToTalkToggleMode()) {
            session.setPushToTalkToggleMode(true);
            UiBus.publish(new PttModeChangedEvent(false));
        }
        session.stopStartListening(false);
        UiBus.publish(new VoiceInputModeToggleEvent(false));
        return null;
    }
}
