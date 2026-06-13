package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

public class NavigateToMySquadronCarrier implements CommandHandler {

    @Override
    public void handle(String action, JsonObject params, String responseText) {
        Status status = Status.getInstance();
        if (status.isInSrv() || status.isInMainShip()) {
            PlayerSession playerSession = PlayerSession.getInstance();
            CarrierDataDto squadronCarrier = playerSession.getSquadronCarrierData();

            if (squadronCarrier == null || squadronCarrier.getStarName() == null || squadronCarrier.getStarName().isEmpty()) {
                EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.squadronCarrierNotAvailable")));
                return;
            }

            RoutePlotter plotter = new RoutePlotter();
            plotter.plotRoute(squadronCarrier.getStarName());
        } else {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.notInShipOrSrv")));
        }
    }
}
