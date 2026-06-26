package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
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

    @Override public String llmDescription() { return "Plot a route to the fleet carrier."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();
        if (!status.isInSrv() && !status.isInMainShip()) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.navigate.notInShipOrSrv"));
        }
        PlayerSession playerSession = PlayerSession.getInstance();
        boolean hasFleetCarrier = playerSession.getFleetCarrierData() != null;
        boolean hasHomeSystem = playerSession.getHomeSystem() != null;

        String destination;
        if (hasFleetCarrier) {
            destination = playerSession.getLastKnownCarrierLocation();
        } else if (hasHomeSystem) {
            destination = playerSession.getHomeSystem().getStarName();
        } else {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.navigate.noHomeSystem"));
        }

        if (destination == null || destination.isEmpty()) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.navigate.carrierNotAvailable"));
        }
        RoutePlotter plotter = new RoutePlotter();
        return plotter.plotRoute(destination);
    }
}
