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
 * Stage-4b self-describing command for "wake up".
 * Owns its own execution (ownsExecution() == true): the dispatch map routes this
 * command's execute() in place of the legacy StartListeningHandler. Side effects on the
 * listening/push-to-talk subsystem (SystemSession + PttModeChangedEvent/VoiceInputModeToggleEvent).
 */
@RegisterCommand
public final class WakeupCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.WAKEUP;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        SystemSession session = SystemSession.getInstance();
        if (session.isPushToTalkEnabled() && !session.isPushToTalkToggleMode()) {
            session.setPushToTalkToggleMode(true);
            EventBusManager.publish(new PttModeChangedEvent(false));
        }
        session.stopStartListening(false);
        EventBusManager.publish(new VoiceInputModeToggleEvent(false));
    }
}
