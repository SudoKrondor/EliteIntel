package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
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
    public String execute(JsonObject params, String responseText) {
        PlayerSession playerSession = PlayerSession.getInstance();
        boolean hasFleetCarrier = playerSession.getFleetCarrierData() != null;
        boolean hasHomeSystem = playerSession.getHomeSystem() != null;

        String destination;
        if (hasFleetCarrier) {
            destination = playerSession.getLastKnownCarrierLocation();
        } else if (hasHomeSystem) {
            destination = playerSession.getHomeSystem().getStarName();
        } else {
            return StringUtls.localizedResponse("handler.navigate.noHomeSystem");
        }

        if (destination != null && !destination.isEmpty()) {
            RoutePlotter plotter = new RoutePlotter();
            return plotter.plotRoute(destination);
        } else {
            return StringUtls.localizedResponse("handler.navigate.carrierNotAvailable");
        }
    }
}
