package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.ShipMakeManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.journal.events.LoadGameEvent;
import elite.intel.session.PlayerSession;

import static org.apache.commons.lang3.StringUtils.trimToNull;

public class LoadGameEventSubscriber {

    private final ShipRouteManager shipRoute = ShipRouteManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final ShipMakeManager shipMakeManager = ShipMakeManager.getInstance();
    @Subscribe
    public void onEvent(LoadGameEvent event) {
        Thread.ofVirtual().start(() -> {
            playerSession.clearShipScans();
            String alternativeName = trimToNull(playerSession.getAlternativeName());
            playerSession.setPlayerName(alternativeName != null ? alternativeName : event.getCommander());
            playerSession.setInGameName(event.getCommander());
            playerSession.setCurrentShip(event.getShip());
            if (event.getShipLocalised() != null) {
                shipMakeManager.upsert(event.getShip(), event.getShipLocalised());
            }
            playerSession.setCurrentShipName(event.getShipName());
            // Credits are owned by FinanceSubscriber (single home for money events).
            playerSession.setGameVersion(event.getGameversion());
            playerSession.setGameBuild(event.getBuild());
            cleanUpRoute(playerSession);
        });
    }

    /**
     * Drops the legs already flown: the current system's and every one before it, not just the current one -
     * the stored route may come from a NavRoute.json the game has not rewritten since the route was plotted.
     */
    private void cleanUpRoute(PlayerSession playerSession) {
        shipRoute.arrivedAt(playerSession.currentStarSystem());
    }
}
