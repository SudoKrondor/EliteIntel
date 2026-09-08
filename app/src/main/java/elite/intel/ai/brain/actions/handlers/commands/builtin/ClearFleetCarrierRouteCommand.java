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
 * <p>WHY it has to exist: a plotted route is consumed one leg at a time by arrivals, so a commander who
 * plots a route and then simply stops flying it has no other way to say so. An arrival off the route now
 * voids it, which covers the case where he jumps elsewhere instead; this covers the case where he changes
 * his mind while sitting still.
 *
 * <p>It used to be the only way out of a much worse hole: an off-route arrival re-plotted the route from
 * wherever the carrier had landed, towards the destination read out of the route it was replacing, so an
 * abandoned route survived every manual jump and every restart, and even this command only held until the
 * next one.
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
