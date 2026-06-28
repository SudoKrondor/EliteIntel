package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.gamestate.dtos.NavRouteDto;
import elite.intel.gameapi.journal.events.LoadGameEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.shiploadout.LoadoutConverter;
import elite.intel.session.PlayerSession;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.trimToNull;

public class LoadGameEventSubscriber {

    private final ShipRouteManager shipRoute = ShipRouteManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Subscribe
    public void onEvent(LoadGameEvent event) {
        Thread.ofVirtual().start(() -> {
            String alternativeName = trimToNull(playerSession.getAlternativeName());
            playerSession.setPlayerName(alternativeName != null ? alternativeName : event.getCommander());
            playerSession.setInGameName(event.getCommander());
            playerSession.setCurrentShip(event.getShip());
            if (event.getShipLocalised() != null) {
                LoadoutConverter.upsertDisplayName(event.getShip(), event.getShipLocalised());
            }
            playerSession.setCurrentShipName(event.getShipName());
            // Credits are owned by FinanceSubscriber (single home for money events).
            playerSession.setGameVersion(event.getGameversion());
            playerSession.setGameBuild(event.getBuild());
            cleanUpRoute(playerSession);
        });
    }

    private void cleanUpRoute(PlayerSession playerSession) {
        List<NavRouteDto> orderedRoute = shipRoute.getOrderedRoute();
        boolean roueSet = !orderedRoute.isEmpty();
        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        if (!roueSet) {
            return;
        }
        if (currentLocation == null) {
            return;
        }
        shipRoute.removeLeg(currentLocation.getStarName());
    }
}
