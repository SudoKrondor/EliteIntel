package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.companion.CompanionRuntime;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.db.dao.NeutronStarRouteDao;
import elite.intel.db.managers.NeutronStarRouteManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy PlotRouteToNextNeutronStarHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class PlotRouteNextNeutronStarWaypointCommand implements IntelCommand {
    public static final String ID = "plot_route_next_neutron_star_waypoint";

    @Override
    public String llmDescription() {
        return "Plot a route to the next waypoint on the previously calculated neutron-star route.";
    }


    private final NeutronStarRouteManager neutronStarRouteManager = NeutronStarRouteManager.getInstance();

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
        NeutronStarRouteDao.Route route = neutronStarRouteManager.getNeutronStarRoute();
        if (route == null || route.getLegs().isEmpty() || route.getLegs().getFirst() == null) {
            return StringUtls.localizedLlm("handler.neutronRoute.notFound");
        }

        String systemName = route.getLegs().getFirst().getSystemName();
        CompanionRuntime.narrator().filler(StringUtls.localizedLlm("handler.neutronRoute.plotting", systemName), false);
        RoutePlotter plotter = new RoutePlotter();
        return plotter.plotRoute(systemName);
    }
}
