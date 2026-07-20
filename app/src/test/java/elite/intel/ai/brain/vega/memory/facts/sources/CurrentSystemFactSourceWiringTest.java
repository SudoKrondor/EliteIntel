package elite.intel.ai.brain.vega.memory.facts.sources;

import elite.intel.ai.brain.vega.memory.facts.MemoryFactContext;
import elite.intel.ai.brain.vega.memory.facts.sources.CurrentSystemFactSource;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Wiring test (no model): seeds a known current system and situation into the live singletons (backed by the default
 * in-memory DB), then asserts {@link CurrentSystemFactSource} reads the right fields and shrinks by situation. It
 * complements the pure {@code format} tests by exercising the {@code factsFor} path (field reads plus the
 * situation-to-brief decision) end to end.
 */
class CurrentSystemFactSourceWiringTest {

    @BeforeAll
    static void initDb() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @Test
    void writesTheFullSystemLineWhileTravelling() {
        seedSol();
        seedSituation(PlayerSituation.IN_SHIP_SUPERCRUISE);

        assertEquals(
                List.of("current system Sol: allegiance Federation, security High, economy Refinery, population 22.7B, controlled by Zachary Hudson"),
                new CurrentSystemFactSource().factsFor(MemoryFactContext.forCommanderInput("")));
    }

    @Test
    void shrinksToGroundingLineWhenAtABody() {
        seedSol();
        seedSituation(PlayerSituation.IN_SHIP_LANDED);

        assertEquals(
                List.of("current system Sol: allegiance Federation, security High"),
                new CurrentSystemFactSource().factsFor(MemoryFactContext.forCommanderInput("")));
    }

    private static void seedSol() {
        PlayerSession.getInstance().setCurrentPrimaryStarName("Sol");
        LocationDto sol = new LocationDto(1L);
        sol.setStarName("Sol");
        sol.setPlanetName("Sol");
        sol.setLocationType(LocationDto.LocationType.PRIMARY_STAR);
        sol.setSystemAddress(9999L);
        sol.setAllegiance("Federation");
        sol.setSecurity("High Security");
        sol.setEconomy("Refinery");
        sol.setPopulation(22_700_000_000L);
        sol.setControllingPower("Zachary Hudson");
        LocationManager.getInstance().save(sol);
    }

    private static void seedSituation(PlayerSituation situation) {
        long[] flags = StatusFlags.flagsForSituation(situation);
        GameEvents.StatusEvent event = new GameEvents.StatusEvent();
        event.setFlags(flags[0]);
        event.setFlags2(flags[1]);
        Status.getInstance().setStatus(event);
    }
}
