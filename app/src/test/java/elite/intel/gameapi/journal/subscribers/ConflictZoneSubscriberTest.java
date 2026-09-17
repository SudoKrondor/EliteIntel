package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.RedeemVoucherEvent;
import elite.intel.gameapi.journal.events.SupercruiseDestinationDropEvent;
import elite.intel.gameapi.signals.ConflictZoneIntensity;
import elite.intel.session.ConflictZone;
import elite.intel.session.PlayerSession;
import elite.intel.util.Cypher;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Knowing the ship has dropped into a conflict zone, and knowing when the fight is over. The twin of
 * {@link ResourceSiteSubscriberTest} for the other kind of fighting.
 * <p>
 * The drop event says which zone as a symbol - the same {@code $Warzone_...} family the FSS reports a
 * system's zones under - so one enum reads both, and only cashing the combat bonds ends the fight.
 */
class ConflictZoneSubscriberTest {

    private static final long CEOS = 2278152997195L;
    private static final long ELSEWHERE = 5031789073122L;

    private final ConflictZone conflictZone = ConflictZone.getInstance();
    private final ConflictZoneSubscriber subscriber = new ConflictZoneSubscriber();

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @BeforeEach
    void inCeos() {
        PlayerSession.getInstance().setCurrentLocationId(1, CEOS);
        conflictZone.cashedIn();
    }

    @Test
    void droppingIntoAZoneStartsAFightInTheSystemTheShipIsIn() {
        subscriber.onSuperCruiseDrop(drop("Conflict Zone [High]", "$Warzone_PointRace_High:#index=2;"));

        ConflictZone.Engagement fight = conflictZone.in(CEOS);
        assertNotNull(fight, "the drop names no system, so the fight belongs to the one the ship is in");
        assertEquals(ConflictZoneIntensity.HIGH, fight.intensity());
        assertNull(conflictZone.in(ELSEWHERE));
    }

    @Test
    void theLastZoneDroppedIntoIsTheOneBeingFoughtIn() {
        subscriber.onSuperCruiseDrop(drop("Conflict Zone [Low]", "$Warzone_PointRace_Low:#index=1;"));
        subscriber.onSuperCruiseDrop(drop("Conflict Zone [Medium]", "$Warzone_PointRace_Med:#index=1;"));

        assertEquals(ConflictZoneIntensity.MEDIUM, conflictZone.in(CEOS).intensity());
    }

    @Test
    void aDropThatIsNotAZoneLeavesTheFightRunning() {
        // Flying to the station to cash in means dropping at a station, and that must not throw away
        // the tally the commander is flying there to collect on.
        subscriber.onSuperCruiseDrop(drop("Conflict Zone [High]", "$Warzone_PointRace_High:#index=1;"));
        subscriber.onSuperCruiseDrop(drop("Cummings Depot", "Cummings Depot"));
        subscriber.onSuperCruiseDrop(drop("Resource Extraction Site [Hazardous]", "$MULTIPLAYER_SCENARIO79_TITLE;"));

        assertEquals(ConflictZoneIntensity.HIGH, conflictZone.in(CEOS).intensity());
    }

    @Test
    void cashingTheBondsInEndsTheFight() {
        subscriber.onSuperCruiseDrop(drop("Conflict Zone [High]", "$Warzone_PointRace_High:#index=1;"));

        subscriber.onRedeemVoucher(voucher("CombatBond"));

        assertNull(conflictZone.in(CEOS));
    }

    @Test
    void cashingBountiesAtTheSameCounterSaysNothingAboutTheWar() {
        subscriber.onSuperCruiseDrop(drop("Conflict Zone [High]", "$Warzone_PointRace_High:#index=1;"));

        subscriber.onRedeemVoucher(voucher("bounty"));
        subscriber.onRedeemVoucher(voucher("codex"));

        assertNotNull(conflictZone.in(CEOS));
    }

    private static SupercruiseDestinationDropEvent drop(String localised, String type) {
        String json = """
                {"timestamp":"2026-09-10T12:00:00Z","event":"SupercruiseDestinationDrop",
                 "Type":"%s","Type_Localised":"%s","Threat":0}
                """.formatted(type, localised);
        return new SupercruiseDestinationDropEvent(GsonFactory.getGson().fromJson(json, JsonObject.class));
    }

    private static RedeemVoucherEvent voucher(String type) {
        String json = """
                {"timestamp":"2026-09-10T12:00:00Z","event":"RedeemVoucher","Type":"%s","Amount":681500}
                """.formatted(type);
        return new RedeemVoucherEvent(GsonFactory.getGson().fromJson(json, JsonObject.class));
    }
}
