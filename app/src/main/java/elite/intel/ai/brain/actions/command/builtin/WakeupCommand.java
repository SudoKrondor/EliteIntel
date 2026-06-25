package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.eventbus.UiBus;
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

    @Override public String llmDescription() { return "Wake the companion from sleep."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
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
