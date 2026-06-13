package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

public class SetRadioTransmissionOnOff implements CommandHandler {

    @Override public void handle(String action, JsonObject params, String responseText) {

        boolean isOn = params.get("state").getAsBoolean();
        PlayerSession playerSession = PlayerSession.getInstance();
        playerSession.setRadioTransmissionOn(isOn);
        String state = StringUtls.localizedLlm(isOn ? "handler.state.on" : "handler.state.off");
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.announcements.radio", state)));
    }
}
