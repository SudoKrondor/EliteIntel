package elite.intel.gameapi.gamestate.subscribers;

import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.gamestate.dtos.NavRouteDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Position;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The game writes NavRoute.json when a route is plotted and never as it is flown. A commander who flies part of a
 * route, quits, and starts again gets the whole plot back from that file, so the legs already behind them have to
 * be dropped by where they stand.
 */
class RoutePlottedSubscriberTest {

    private static final String ROUTE = """
            { "timestamp":"2026-10-18T15:10:44Z", "event":"NavRoute", "Route":[
              { "StarSystem":"Firdaus", "SystemAddress":83986682586, "StarPos":[62.96875,-89.56250,35.62500], "StarClass":"K" },
              { "StarSystem":"Condovichs", "SystemAddress":671759476145, "StarPos":[64.93750,-93.53125,25.46875], "StarClass":"TTS" },
              { "StarSystem":"ICZ FW-V b2-1", "SystemAddress":2870782731697, "StarPos":[63.50000,-97.75000,22.00000], "StarClass":"M" },
              { "StarSystem":"Vequess", "SystemAddress":1733254091482, "StarPos":[62.56250,-106.00000,22.12500], "StarClass":"K" },
              { "StarSystem":"Coastakal", "SystemAddress":5069805921713, "StarPos":[66.50000,-113.43750,15.15625], "StarClass":"K" },
              { "StarSystem":"Miphifa", "SystemAddress":83986649810, "StarPos":[60.59375,-114.87500,12.34375], "StarClass":"K" }
            ] }
            """;

    private final PlayerSession session = PlayerSession.getInstance();
    private final ShipRouteManager route = ShipRouteManager.getInstance();
    private Optional<Position> positionBefore;

    @BeforeEach
    void rememberPosition() {
        positionBefore = session.position();
        route.clearRoute();
    }

    @AfterEach
    void restore() {
        positionBefore.ifPresent(session::moveTo);
        route.clearRoute();
        session.setFinalDestination(null);
    }

    @Test
    void aStaleRouteFileLosesTheLegsAlreadyFlown() {
        session.moveTo(new Position(2870782731697L, "ICZ FW-V b2-1", 0L));

        new RoutePlottedSubscriber().onGameEvent(navRoute());

        assertEquals(List.of("Vequess", "Coastakal", "Miphifa"), legs());
        assertEquals("Miphifa", route.getDestination());
    }

    @Test
    void aFreshlyPlottedRouteIsKeptWhole() {
        session.moveTo(new Position(83986682586L, "Firdaus", 0L));

        new RoutePlottedSubscriber().onGameEvent(navRoute());

        assertEquals(List.of("Condovichs", "ICZ FW-V b2-1", "Vequess", "Coastakal", "Miphifa"), legs());
    }

    private static GameEvents.NavRouteEvent navRoute() {
        return GsonFactory.getGson().fromJson(ROUTE, GameEvents.NavRouteEvent.class);
    }

    private List<String> legs() {
        return route.getOrderedRoute().stream().map(NavRouteDto::getName).toList();
    }
}
