package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.NeutronStarRouteManager;
import elite.intel.gameapi.EventBusManager;

public class ClearNeutronRouteHandler implements CommandHandler {

    private final NeutronStarRouteManager manager = NeutronStarRouteManager.getInstance();

    @Override
    public void handle(String action, JsonObject params, String responseText) {
        manager.clear();
        EventBusManager.publish(new MissionCriticalAnnouncementEvent("Neutron Route Cleared."));
    }
}
