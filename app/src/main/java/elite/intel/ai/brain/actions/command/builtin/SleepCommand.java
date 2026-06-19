package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.gameapi.EventBusManager;
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

    @Override
    public String id() {
        return CommandIds.SLEEP;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        SystemSession session = SystemSession.getInstance();
        if (session.isPushToTalkEnabled() && session.isPushToTalkToggleMode()) {
            session.setPushToTalkToggleMode(false);
            EventBusManager.publish(new PttModeChangedEvent(true));
        }
        session.stopStartListening(true);
        EventBusManager.publish(new VoiceInputModeToggleEvent(true));
    }
}
