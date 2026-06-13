package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.NeutronStarRouteDao;
import elite.intel.db.managers.NeutronStarRouteManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.util.StringUtls;

public class PlotRouteToNextNeutronStarHandler implements CommandHandler {

    private final NeutronStarRouteManager neutronStarRouteManager = NeutronStarRouteManager.getInstance();

    @Override
    public void handle(String action, JsonObject params, String responseText) {
        NeutronStarRouteDao.Route route = neutronStarRouteManager.getNeutronStarRoute();
        if (route == null || route.getLegs().isEmpty() || route.getLegs().getFirst() == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.neutronRoute.notFound")));
        }

        String systemName = route.getLegs().getFirst().getSystemName();
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.neutronRoute.plotting", systemName)));
        RoutePlotter plotter = new RoutePlotter();
        plotter.plotRoute(systemName);
    }
}
