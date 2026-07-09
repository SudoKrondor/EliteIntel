package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.session.Status;
import elite.intel.util.FleetCarrierRouteCalculator;

/**
 * Owns its own execution: body migrated 1:1 from the legacy CalculateFleetCarrierRouteHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class CalculateFleetCarrierRouteCommand implements IntelCommand {
    public static final String ID = "calculate_fleet_carrier_route";

    @Override
    public String llmDescription() {
        return "Calculate a multi-jump tritium route for the commander's own personal fleet carrier.";
    }


    @Override
    public String id() {
        return ID;
    }

    /** App-side bookkeeping (no game input); executable in any location. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        return FleetCarrierRouteCalculator.calculate();
    }
}
