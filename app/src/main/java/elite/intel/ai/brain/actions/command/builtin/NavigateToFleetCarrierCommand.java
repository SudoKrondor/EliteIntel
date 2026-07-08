package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.inputs.RoutePlotter;
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
    public static final String ID = "navigate_to_fleet_carrier";

    @Override
    public String llmDescription() {
        return "Plot a route to the commander's fleet carrier's last known location (falls back to the home system if no carrier is known).";
    }


    @Override
    public String id() {
        return ID;
    }

    /** Route plotting taps the ship-only GalaxyMapOpen bind; works only in the main-ship cockpit. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip();
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        PlayerSession playerSession = PlayerSession.getInstance();
        boolean hasFleetCarrier = playerSession.getFleetCarrierData() != null;
        boolean hasHomeSystem = playerSession.getHomeSystem() != null;

        String destination;
        if (hasFleetCarrier) {
            destination = playerSession.getLastKnownCarrierLocation();
        } else if (hasHomeSystem) {
            destination = playerSession.getHomeSystem().getStarName();
        } else {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.noHomeSystem")));
            return;
        }

        if (destination != null && !destination.isEmpty()) {
            RoutePlotter plotter = new RoutePlotter();
            plotter.plotRoute(destination);
        } else {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.carrierNotAvailable")));
        }
    }
}
