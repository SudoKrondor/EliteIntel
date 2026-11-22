package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * With no leg ahead the command used to return nothing: the commander heard "aye-aye", nothing was typed, and he
 * could not tell that the carrier was already at its final destination.
 */
class EnterFleetCarrierDestinationCommandTest {

    private final FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();
    private final PlayerSession session = PlayerSession.getInstance();

    @BeforeEach
    void clearRoute() {
        route.clear();
    }

    @AfterEach
    void resetSession() {
        route.clear();
        session.setLastKnownCarrierLocation(null);
    }

    @Test
    void withNoRoutePlottedItSaysThereIsNoLegToFollow() {
        session.setLastKnownCarrierLocation("Deciat");

        assertEquals(noLegsAhead(), new EnterFleetCarrierDestinationCommand().execute(new JsonObject(), null));
    }

    @Test
    void atTheFinalDestinationItSaysThereIsNoLegToFollow() {
        session.setLastKnownCarrierLocation("Maia");
        route.setFleetCarrierRoute(Map.of(1, leg("Maia"), 2, leg("Merope")));

        // The carrier arrives at the last leg.
        session.setLastKnownCarrierLocation("Merope");

        assertEquals(noLegsAhead(), new EnterFleetCarrierDestinationCommand().execute(new JsonObject(), null));
    }

    private static String noLegsAhead() {
        return StringUtls.localizedResponse("handler.fleetCarrierRoute.noLegsAhead");
    }

    private static CarrierJump leg(String system) {
        CarrierJump jump = new CarrierJump();
        jump.setSystemName(system);
        return jump;
    }
}
