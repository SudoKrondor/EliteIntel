package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

public class NavigateToMyFleetCarrier implements CommandHandler {



    @Override public void handle(String action, JsonObject params, String responseText) {

        Status status = Status.getInstance();
        if (status.isInSrv() || status.isInMainShip()) {
            PlayerSession playerSession = PlayerSession.getInstance();
            boolean hasFleetCarrier = playerSession.getFleetCarrierData() != null;
            boolean hasHomeSystem = playerSession.getHomeSystem() != null;

            String destination;
            if (hasFleetCarrier) {
                destination = playerSession.getLastKnownCarrierLocation();
            } else if (hasHomeSystem) {
                destination = playerSession.getHomeSystem().getStarName();
            } else {
                EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.noHomeSystem")));
                return;
            }

            if (destination != null && !destination.isEmpty()) {
                RoutePlotter plotter = new RoutePlotter();
                plotter.plotRoute(destination);
            } else {
                EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.carrierNotAvailable")));
            }
        } else {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.notInShipOrSrv")));
        }
    }
}
