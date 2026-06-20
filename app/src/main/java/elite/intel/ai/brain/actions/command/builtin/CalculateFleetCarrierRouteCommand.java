package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.util.FleetCarrierRouteCalculator;

/**
 * Owns its own execution: body migrated 1:1 from the legacy CalculateFleetCarrierRouteHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class CalculateFleetCarrierRouteCommand implements IntelCommand {
    public static final String ID = "calculate_fleet_carrier_route";


    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        FleetCarrierRouteCalculator.calculate();
    }
}
