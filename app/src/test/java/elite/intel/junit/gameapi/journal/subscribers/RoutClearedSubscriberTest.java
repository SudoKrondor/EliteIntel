package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.gamestate.dtos.NavRouteDto;
import elite.intel.gameapi.journal.events.NavRouteClearEvent;
import elite.intel.gameapi.journal.subscribers.RoutClearedSubscriber;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNull;

class RoutClearedSubscriberTest {

    private final RoutClearedSubscriber subscriber = new RoutClearedSubscriber();
    private final ShipRouteManager routeManager = ShipRouteManager.getInstance();

    @Test
    void routeClearedRemovesAllRouteLegs() {
        NavRouteDto leg = new NavRouteDto();
        leg.setLeg(1);
        leg.setName("Sol");
        leg.setRemainingJumps(3);
        leg.setScoopable(true);
        leg.setStarClass("G");
        routeManager.updateRouteNode(leg);

        subscriber.onRouteCleared(navRouteClearEvent());

        assertNull(routeManager.getDestination());
    }

    @Test
    void routeClearedIsNoOpOnEmptyRoute() {
        routeManager.clearRoute();

        subscriber.onRouteCleared(navRouteClearEvent());

        assertNull(routeManager.getDestination());
    }

    private static NavRouteClearEvent navRouteClearEvent() {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "NavRouteClear");
        return new NavRouteClearEvent(j);
    }
}
