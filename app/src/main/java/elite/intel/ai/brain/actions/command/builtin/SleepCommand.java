package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.PttModeChangedEvent;
import elite.intel.ui.event.VoiceInputModeToggleEvent;

/**
 * Stage-4b self-describing command for "sleep".
 * Side effects on the
 * listening/push-to-talk subsystem (SystemSession + PttModeChangedEvent/VoiceInputModeToggleEvent).
 */
@RegisterCommand
public final class SleepCommand implements IntelCommand {
    public static final String ID = "sleep";

    @Override public String llmDescription() { return "Put the companion to sleep so it stops responding until woken."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        SystemSession session = SystemSession.getInstance();
        if (session.isPushToTalkEnabled() && session.isPushToTalkToggleMode()) {
            session.setPushToTalkToggleMode(false);
            UiBus.publish(new PttModeChangedEvent(true));
        }
        session.stopStartListening(true);
        UiBus.publish(new VoiceInputModeToggleEvent(true));
        return null;
    }
}
