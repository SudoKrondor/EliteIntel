package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Abandons the plotted fleet carrier route.
 *
 * <p>WHY it has to exist: a plotted route is consumed one leg at a time by arrivals, and an arrival in a
 * system the route never mentions consumes nothing and re-plots from there to the same final destination.
 * A commander who plots a route and then stops following it therefore keeps that route forever - it
 * survives every manual jump, is quoted back at him on every arrival ("N jumps left"), and re-plots itself
 * from wherever he lands. Nothing else in the app clears it, so without this command the only way out was
 * to fly the route to its end.
 */
@RegisterCommand
public final class ClearFleetCarrierRouteCommand implements IntelCommand {
    public static final String ID = "clear_fleet_carrier_route";

    private final FleetCarrierRouteManager manager = FleetCarrierRouteManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Abandon the plotted fleet carrier route, so no legs, destination or jump countdown remain."
                + " Use for cancelling the CARRIER's voyage, never a trade route or a neutron route.";
    }

    /**
     * App-side bookkeeping (no game input); executable in any location.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        manager.clear();
        return StringUtls.localizedResponse("handler.fleetCarrierRoute.cleared");
    }
}
