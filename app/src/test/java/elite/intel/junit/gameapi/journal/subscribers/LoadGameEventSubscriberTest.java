package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.gamestate.dtos.NavRouteDto;
import elite.intel.gameapi.journal.events.LoadGameEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.subscribers.LoadGameEventSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class LoadGameEventSubscriberTest {

    private final LoadGameEventSubscriber subscriber = new LoadGameEventSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();
    private final ShipRouteManager shipRoute = ShipRouteManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @BeforeEach
    void clearRoute() {
        shipRoute.clearRoute();
    }

    @Test
    void commanderNameAndCreditsAreStoredFromEvent() throws InterruptedException {
        subscriber.onEvent(loadGameEvent("CMDR Hawkins", "cobra", 1_500_000L));

        awaitTrue(() -> "CMDR Hawkins".equals(session.getInGameName()));
        assertEquals("CMDR Hawkins", session.getInGameName());
        assertEquals(1_500_000L, session.getPersonalCredits());
    }

    @Test
    void playerNameDefaultsToCommanderNameWhenNoAlternativeNameSet() throws InterruptedException {
        session.setPlayerName(null);
        subscriber.onEvent(loadGameEvent("CMDR Rivera", "sidewinder", 0L));

        awaitTrue(() -> "CMDR Rivera".equals(session.getPlayerName()));
        assertEquals("CMDR Rivera", session.getPlayerName());
    }

    @Test
    void cleanUpRouteRemovesCurrentSystemLegFromNavRoute() throws InterruptedException {
        // Set player's current location to a known systemAddress/bodyId
        long systemAddress = 999_888_777L;
        long bodyId = 7L;
        session.setCurrentLocationId(bodyId, systemAddress);

        // Save a location entry so cleanUpRoute can find the star name
        LocationDto loc = new LocationDto(bodyId, systemAddress);
        loc.setStarName("Deciat");
        loc.setStationName("Deciat Station"); // needed as locationName for upsert
        locationManager.save(loc);

        // Set up a nav route with Deciat as leg 0
        NavRouteDto leg = new NavRouteDto();
        leg.setLeg(0);
        leg.setName("Deciat");
        leg.setStarClass("G");
        leg.setScoopable(true);
        shipRoute.setNavRoute(Map.of(0, leg));
        assertFalse(shipRoute.getOrderedRoute().isEmpty());

        subscriber.onEvent(loadGameEvent("CMDR Test", "asp", 0L));

        awaitTrue(() -> "CMDR Test".equals(session.getInGameName()));
        assertTrue(shipRoute.getOrderedRoute().isEmpty(), "Deciat leg should have been removed from route");
    }

    @Test
    void cleanUpRouteDoesNothingWhenNoRouteIsSet() throws InterruptedException {
        // No route → exits cleanUpRoute early, no exception
        assertDoesNotThrow(() -> {
            subscriber.onEvent(loadGameEvent("CMDR NoRoute", "cobra", 0L));
            Thread.sleep(300);
        });
        assertTrue(shipRoute.getOrderedRoute().isEmpty());
    }

    private static LoadGameEvent loadGameEvent(String commander, String ship, long credits) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "LoadGame");
        j.addProperty("FID", "F1234567");
        j.addProperty("Commander", commander);
        j.addProperty("Horizons", true);
        j.addProperty("Odyssey", true);
        j.addProperty("Ship", ship);
        j.addProperty("ShipID", 1);
        j.addProperty("ShipName", "My Ship");
        j.addProperty("ShipIdent", "SH-01");
        j.addProperty("FuelLevel", 8.0);
        j.addProperty("FuelCapacity", 16.0);
        j.addProperty("GameMode", "Open");
        j.addProperty("Credits", credits);
        j.addProperty("Loan", 0);
        j.addProperty("language", "English\\UK");
        j.addProperty("gameversion", "4.0.0");
        j.addProperty("build", "r321306");
        return new LoadGameEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
