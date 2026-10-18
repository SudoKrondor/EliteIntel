package elite.intel.gameapi.gamestate.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.gamestate.dtos.NavRouteDto;
import elite.intel.session.PlayerSession;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused") //registered in SubscriberRegistration
public class RoutePlottedSubscriber {

    @Subscribe
    public void onGameEvent(GameEvents.NavRouteEvent event) {
        int totalJumps = event.getRoute().size();
        PlayerSession playerSession = PlayerSession.getInstance();
        ShipRouteManager shipRoute = ShipRouteManager.getInstance();

        List<GameEvents.NavRouteEvent.RouteEntry> route = event.getRoute();
        Map<Integer, NavRouteDto> routeMap = new LinkedHashMap<>();
        int leg = 0;
        if (!route.isEmpty()) {
            for (GameEvents.NavRouteEvent.RouteEntry entry : route.subList(1, route.size())) {
                NavRouteDto dto = new NavRouteDto();
                dto.setLeg(++leg);
                dto.setRemainingJumps(totalJumps - leg);
                dto.setStarClass(entry.getStarClass());
                dto.setName(entry.getStarSystem());
                dto.setX(entry.getStarPos()[0]);
                dto.setY(entry.getStarPos()[1]);
                dto.setZ(entry.getStarPos()[2]);
                dto.setScoopable("KGBFOAM".contains(entry.getStarClass().toUpperCase()));
                routeMap.put(leg, dto);
            }
            NavRouteDto finalDestination;
            if (!routeMap.isEmpty()) {
                finalDestination = routeMap.entrySet().stream()
                        .reduce((first, second) -> second) // Keep the last entry
                        .map(Map.Entry::getValue)
                        .orElse(null);

                playerSession.setFinalDestination(finalDestination.getName());
                shipRoute.setNavRoute(routeMap);
                // WHY: the game writes NavRoute.json when a route is plotted and never as it is flown, so the
                // copy read at app start still lists every leg from the plot's origin, including those flown in
                // an earlier session. Where the commander stands says which are behind them. A freshly plotted
                // route starts at the current system, which the loop above already skipped, so this is a no-op.
                shipRoute.arrivedAt(playerSession.currentStarSystem());
            } else {
                shipRoute.clearRoute();
            }
        }
    }
}