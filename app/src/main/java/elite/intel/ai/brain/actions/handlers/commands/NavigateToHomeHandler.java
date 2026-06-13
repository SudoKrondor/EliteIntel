package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

public class NavigateToHomeHandler implements CommandHandler {

    private final PlayerSession playerSession = PlayerSession.getInstance();


    @Override public void handle(String action, JsonObject params, String responseText) {

        Status status = Status.getInstance();
        if(status.isInSrv() || status.isInMainShip()) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.plottingHome")));
            LocationDto location = playerSession.getHomeSystem();
            if (location.getBodyId() == -1) {
                EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.homeNotSet")));
                return;
            }
            RoutePlotter plotter = new RoutePlotter();
            plotter.plotRoute(location.getStarName());
        } else {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.notInShipOrSrv")));
        }
    }
}
