package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

public class ToggleAllAnnouncementsHandler implements CommandHandler {

    @Override
    public void handle(String action, JsonObject params, String responseText) {
        if (params.get("state") == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.common.llmParamFailed")));
            return;
        }
        boolean isOn = params.get("state").getAsBoolean();

        PlayerSession playerSession = PlayerSession.getInstance();
        playerSession.setDiscoveryAnnouncementOn(isOn);
        playerSession.setRouteAnnouncementOn(isOn);
        playerSession.setRadarContactAnnouncementOn(isOn);
        playerSession.setMiningAnnouncementOn(isOn);
        playerSession.setNavigationAnnouncementOn(isOn);
        String state = StringUtls.localizedLlm(isOn ? "handler.state.on" : "handler.state.off");
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.announcements.all", state)));
    }
}
