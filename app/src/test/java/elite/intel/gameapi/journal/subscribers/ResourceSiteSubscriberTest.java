package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.RedeemVoucherEvent;
import elite.intel.gameapi.journal.events.SupercruiseDestinationDropEvent;
import elite.intel.gameapi.signals.ResourceSiteGrade;
import elite.intel.session.PlayerSession;
import elite.intel.session.ResourceSite;
import elite.intel.util.Cypher;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Knowing the ship has dropped into a resource site, and knowing when the hunt is over.
 * <p>
 * The drop event is the one place the game says outright what the commander is about to fight in,
 * and it says it as a symbol rather than a name - the same symbol {@code FSSSignalDiscovered} uses
 * for the sites a system holds, so one enum reads both.
 */
class ResourceSiteSubscriberTest {

    private static final long ODOMAZOTZ = 5306666980066L;

    /**
     * The symbols spelled out here rather than read back off the enum: asking the enum for its own
     * answer and comparing it to itself would pass however wrong the mapping was.
     */
    private static final Map<ResourceSiteGrade, String> SYMBOLS = Map.of(
            ResourceSiteGrade.STANDARD, "$MULTIPLAYER_SCENARIO14_TITLE;",
            ResourceSiteGrade.LOW, "$MULTIPLAYER_SCENARIO77_TITLE;",
            ResourceSiteGrade.HIGH, "$MULTIPLAYER_SCENARIO78_TITLE;",
            ResourceSiteGrade.HAZARDOUS, "$MULTIPLAYER_SCENARIO79_TITLE;");

    private final ResourceSite resourceSite = ResourceSite.getInstance();
    private final ResourceSiteSubscriber subscriber = new ResourceSiteSubscriber();

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @BeforeEach
    void inOdomazotz() {
        PlayerSession.getInstance().setCurrentLocationId(1, ODOMAZOTZ);
        resourceSite.cashedIn();
    }

    @Test
    void everyGradeOfResourceSiteStartsAHunt() {
        for (ResourceSiteGrade grade : ResourceSiteGrade.values()) {
            resourceSite.cashedIn();
            subscriber.onSuperCruiseDrop(drop(grade.name(), SYMBOLS.get(grade), 4));

            ResourceSite.Hunt hunt = resourceSite.in(ODOMAZOTZ);
            assertNotNull(hunt, "dropping into a " + grade + " site is a hunt");
            assertEquals(grade, hunt.grade());
            assertEquals(4, hunt.threat());
        }
    }

    @Test
    void aNavBeaconIsNotAResourceSite() {
        subscriber.onSuperCruiseDrop(drop("Nav Beacon", "$MULTIPLAYER_SCENARIO42_TITLE;", 2));

        assertNull(resourceSite.in(ODOMAZOTZ),
                "the nav beacon shares the scenario numbering and is not a place to hunt");
    }

    /**
     * Flying in to cash the vouchers means dropping at a station, and that drop must not be read as
     * leaving the hunt: the tally is exactly what the commander is flying there to collect on.
     */
    @Test
    void droppingAtAStationLeavesTheHuntRunning() {
        subscriber.onSuperCruiseDrop(drop("Hazardous", SYMBOLS.get(ResourceSiteGrade.HAZARDOUS), 4));
        subscriber.onSuperCruiseDrop(drop("Cummings Depot", "Cummings Depot", 0));

        assertNotNull(resourceSite.in(ODOMAZOTZ));
        assertEquals(ResourceSiteGrade.HAZARDOUS, resourceSite.in(ODOMAZOTZ).grade(),
                "and the station drop does not overwrite the site being fought in");
    }

    @Test
    void cashingTheVouchersInEndsTheHunt() {
        subscriber.onSuperCruiseDrop(drop("High", SYMBOLS.get(ResourceSiteGrade.HIGH), 3));

        subscriber.onRedeemVoucher(new RedeemVoucherEvent(GsonFactory.getGson().fromJson("""
                {"timestamp":"2026-09-10T12:00:00Z","event":"RedeemVoucher","Type":"bounty","Amount":681500}
                """, JsonObject.class)));

        assertNull(resourceSite.in(ODOMAZOTZ));
    }

    /**
     * The drop event names no system of its own, so the hunt is filed against the one the ship is in.
     */
    @Test
    void aHuntBelongsToTheSystemItStartedIn() {
        subscriber.onSuperCruiseDrop(drop("Hazardous", SYMBOLS.get(ResourceSiteGrade.HAZARDOUS), 4));

        assertNotNull(resourceSite.in(ODOMAZOTZ));
        assertNull(resourceSite.in(5031789073122L));
    }

    // -- the event itself ------------------------------------------------------

    /**
     * {@code StationName.display} spells a bare symbol out when there is nothing after the semicolon,
     * which is right for a station panel and wrong for a scenario whose whole name IS the symbol. It
     * put "MULTIPLAYER SCENARIO79 TITLE" in front of the LLM every time the commander dropped into a
     * hazardous site.
     */
    @Test
    void theDropReadsAsWordsAndNotAsAScenarioNumber() {
        SupercruiseDestinationDropEvent event =
                drop("Resource Extraction Site [Hazardous]", SYMBOLS.get(ResourceSiteGrade.HAZARDOUS), 4);

        assertEquals("Resource Extraction Site [Hazardous]", event.getType());
        assertEquals("$MULTIPLAYER_SCENARIO79_TITLE;", event.getTypeSymbol());
    }

    @Test
    void theSymbolNeverReachesTheNarrationPayloadOrTheStoredEvent() {
        SupercruiseDestinationDropEvent event =
                drop("Resource Extraction Site [High]", SYMBOLS.get(ResourceSiteGrade.HIGH), 3);

        assertFalse(event.toYaml().contains("MULTIPLAYER_SCENARIO"),
                "anything in the narration payload can come back out of the speaker");
        assertFalse(event.toJson().contains("MULTIPLAYER_SCENARIO"));
    }

    @Test
    void anOrdinaryDestinationStillReadsAsItsOwnName() {
        SupercruiseDestinationDropEvent event = drop(null, "Cummings Depot", 0);

        assertEquals("Cummings Depot", event.getType());
    }

    // -- fixtures --------------------------------------------------------------

    private static SupercruiseDestinationDropEvent drop(String localised, String type, int threat) {
        String json = localised == null
                ? """
                {"timestamp":"2026-09-10T12:00:00Z","event":"SupercruiseDestinationDrop",
                 "Type":"%s","Threat":%d}
                """.formatted(type, threat)
                : """
                {"timestamp":"2026-09-10T12:00:00Z","event":"SupercruiseDestinationDrop",
                 "Type":"%s","Type_Localised":"%s","Threat":%d}
                """.formatted(type, localised, threat);
        return new SupercruiseDestinationDropEvent(GsonFactory.getGson().fromJson(json, JsonObject.class));
    }
}
