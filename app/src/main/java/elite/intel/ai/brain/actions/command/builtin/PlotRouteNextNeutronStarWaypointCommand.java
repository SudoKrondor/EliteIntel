package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.NeutronStarRouteDao;
import elite.intel.db.managers.NeutronStarRouteManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy PlotRouteToNextNeutronStarHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class PlotRouteNextNeutronStarWaypointCommand implements IntelCommand {
    public static final String ID = "plot_route_next_neutron_star_waypoint";


    private final NeutronStarRouteManager neutronStarRouteManager = NeutronStarRouteManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        NeutronStarRouteDao.Route route = neutronStarRouteManager.getNeutronStarRoute();
        if (route == null || route.getLegs().isEmpty() || route.getLegs().getFirst() == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.neutronRoute.notFound")));
            return;
        }

        String systemName = route.getLegs().getFirst().getSystemName();
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.neutronRoute.plotting", systemName)));
        RoutePlotter plotter = new RoutePlotter();
        plotter.plotRoute(systemName);
    }
}
