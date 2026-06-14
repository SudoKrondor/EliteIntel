package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.PttModeChangedEvent;
import elite.intel.ui.event.VoiceInputModeToggleEvent;

public class StartListeningHandler implements CommandHandler {

    @Override
    public void handle(String action, JsonObject params, String responseText) {
        SystemSession session = SystemSession.getInstance();
        if (session.isPushToTalkEnabled() && !session.isPushToTalkToggleMode()) {
            session.setPushToTalkToggleMode(true);
            EventBusManager.publish(new PttModeChangedEvent(false));
        }
        session.stopStartListening(false);
        EventBusManager.publish(new VoiceInputModeToggleEvent(false));
    }
}
