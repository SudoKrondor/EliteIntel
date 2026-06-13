package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

public class ClearMiningTargetsHandler implements CommandHandler {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    @Override public void handle(String action, JsonObject params, String responseText) {
        playerSession.clearMiningTargets();
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.mining.targetsCleared")));
        playerSession.setMiningAnnouncementOn(true);
    }
}
