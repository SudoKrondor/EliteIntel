package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.RoutePlotter;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Self-describing "navigate to fleet carrier" command.
 * Owns its own execution: body migrated 1:1 from the legacy NavigateToMyFleetCarrier,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateToFleetCarrierCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.NAVIGATE_TO_FLEET_CARRIER;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
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
