package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.db.dao.ConstructionSiteDao.Requirement;
import elite.intel.db.dao.ConstructionSiteDao.Site;
import elite.intel.gameapi.journal.events.ColonisationConstructionDepotEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.DockedMarket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The manifest cache, and the reason it is not written on every event.
 * <p>
 * The fixtures are the real journal lines from Orbital Construction Site: Divis Gateway, trimmed to a
 * handful of the seventeen commodities. That site belongs to another architect, which is the ordinary
 * case: anyone can haul to anyone's depot.
 */
class ColonisationDepotSubscriberTest {

    private static final long MARKET_ID = 3967232514L;

    private final List<Site> sitesWritten = new ArrayList<>();
    private final List<List<Requirement>> manifestsWritten = new ArrayList<>();
    /**
     * Every "we are standing on this pad" the subscriber recorded, as {@code marketId@timestamp}.
     */
    private final List<String> padsRecorded = new ArrayList<>();

    private ColonisationDepotSubscriber subscriberSeeing(LocationDto location) {
        return new ColonisationDepotSubscriber(
                (site, manifest) -> {
                    sitesWritten.add(site);
                    manifestsWritten.add(manifest);
                },
                (visitedAt, marketId) -> padsRecorded.add(marketId + "@" + visitedAt),
                marketId -> location);
    }

    private static ColonisationConstructionDepotEvent depot(String timestamp, double progress, int membraneProvided) {
        String json = """
                { "timestamp":"%s", "event":"ColonisationConstructionDepot", "MarketID":%d,
                  "ConstructionProgress":%s, "ConstructionComplete":false, "ConstructionFailed":false,
                  "ResourcesRequired":[
                    { "Name":"$steel_name;", "Name_Localised":"Steel", "RequiredAmount":2542, "ProvidedAmount":0, "Payment":5057 },
                    { "Name":"$insulatingmembrane_name;", "Name_Localised":"Insulating Membrane", "RequiredAmount":106, "ProvidedAmount":%d, "Payment":11788 },
                    { "Name":"$computercomponents_name;", "Name_Localised":"Computer Components", "RequiredAmount":22, "ProvidedAmount":22, "Payment":1112 }
                  ] }
                """.formatted(timestamp, MARKET_ID, progress, membraneProvided);
        return new ColonisationConstructionDepotEvent(JsonParser.parseString(json).getAsJsonObject());
    }

    private static LocationDto divisGateway() {
        LocationDto location = new LocationDto(0L, 5070074422609L);
        location.setStationName("Orbital Construction Site: Divis Gateway");
        location.setStarName("Hyades Sector NR-V b2-2");
        return location;
    }

    @BeforeEach
    @AfterEach
    void clearDockedMarker() {
        DockedMarket.getInstance().departed();
    }

    /**
     * The repair for a restart made while sitting on the pad: the {@code Docked} that would have recorded
     * the port is in an already-read journal, so without this the shopping command stays unavailable for the
     * rest of the visit. The manifest repeats every 15-30 seconds and only ever on that pad, so it is a
     * standing answer to "where am I".
     */
    @Test
    void aManifestProvesWeAreOnThatDepotsPad() {
        subscriberSeeing(divisGateway()).onConstructionDepot(depot("2026-08-23T19:11:54Z", 0.010415, 0));

        assertEquals(MARKET_ID, DockedMarket.getInstance().marketId());
    }

    @Test
    void theFirstManifestOfAVisitIsStored() {
        subscriberSeeing(divisGateway()).onConstructionDepot(depot("2026-08-23T19:11:54Z", 0.010415, 0));

        assertEquals(1, sitesWritten.size());
        Site site = sitesWritten.getFirst();
        assertEquals(MARKET_ID, site.getMarketId());
        assertEquals("Orbital Construction Site: Divis Gateway", site.getStationName());
        assertEquals("Hyades Sector NR-V b2-2", site.getStarSystem());
        assertEquals(5070074422609L, site.getSystemAddress());
        assertEquals("2026-08-23T19:11:54Z", site.getVisitedAt());
        assertEquals(0.010415, site.getProgress(), 1e-9);
    }

    /**
     * The game republishes the whole manifest every 15 to 30 seconds for as long as the ship is on the
     * pad - fifty copies over the visit this fixture came from. Writing each one would mean deleting and
     * reinserting seventeen rows every twenty seconds for no change at all.
     */
    @Test
    void anUnchangedRepublishIsNotWrittenAgain() {
        ColonisationDepotSubscriber subscriber = subscriberSeeing(divisGateway());

        subscriber.onConstructionDepot(depot("2026-08-23T19:11:54Z", 0.010415, 0));
        subscriber.onConstructionDepot(depot("2026-08-23T19:12:10Z", 0.010415, 0));
        subscriber.onConstructionDepot(depot("2026-08-23T19:12:25Z", 0.010415, 0));

        assertEquals(1, sitesWritten.size(), "only the manifest that said something new should be stored");
    }

    /**
     * The delivery that moved this build from 1.04% to 2.62%: 106 tonnes of insulating membrane.
     */
    @Test
    void aDeliveryIsWrittenBecauseTheManifestChanged() {
        ColonisationDepotSubscriber subscriber = subscriberSeeing(divisGateway());

        subscriber.onConstructionDepot(depot("2026-08-23T19:39:08Z", 0.010415, 0));
        subscriber.onConstructionDepot(depot("2026-08-23T19:39:43Z", 0.026187, 106));

        assertEquals(2, sitesWritten.size());
        assertEquals(0.026187, sitesWritten.getLast().getProgress(), 1e-9);
        assertEquals(106, membrane(manifestsWritten.getLast()).getProvidedAmount());
    }

    /**
     * Somebody else's delivery looks exactly like ours from here, and has to be stored just the same.
     */
    @Test
    void aDeliveryByAnotherCommanderIsStoredToo() {
        ColonisationDepotSubscriber subscriber = subscriberSeeing(divisGateway());

        subscriber.onConstructionDepot(depot("2026-08-23T19:11:54Z", 0.010415, 0));
        subscriber.onConstructionDepot(depot("2026-08-23T20:15:00Z", 0.019534, 60));

        assertEquals(2, sitesWritten.size());
        assertEquals(60, membrane(manifestsWritten.getLast()).getProvidedAmount());
    }

    /**
     * The manifest writes {@code $insulatingmembrane_name;}; the hold and the commodities table both
     * spell it bare and lower-cased. A symbol left decorated joins with neither.
     */
    @Test
    void symbolsAreNormalisedSoTheyJoinWithTheHold() {
        subscriberSeeing(divisGateway()).onConstructionDepot(depot("2026-08-23T19:11:54Z", 0.010415, 0));

        assertEquals(List.of("computercomponents", "insulatingmembrane", "steel"),
                manifestsWritten.getFirst().stream()
                        .map(Requirement::getSymbol)
                        .sorted()
                        .collect(Collectors.toList()));
    }

    /**
     * The depot event carries only a MarketID. After a restart on the pad the {@code Docked} that named
     * the place is in an already-read journal, so the manifest is all there is - and the manifest is the
     * part the shopping commands need.
     */
    @Test
    void aSiteWithNoNameYetIsStillStored() {
        subscriberSeeing(null).onConstructionDepot(depot("2026-08-23T19:11:54Z", 0.010415, 0));

        assertEquals(1, sitesWritten.size());
        assertNull(sitesWritten.getFirst().getStationName());
        assertEquals(3, manifestsWritten.getFirst().size());
    }

    @Test
    void anEventWithoutAMarketIdIsIgnored() {
        String json = """
                { "timestamp":"2026-08-23T19:11:54Z", "event":"ColonisationConstructionDepot",
                  "ConstructionProgress":0.5, "ConstructionComplete":false, "ConstructionFailed":false,
                  "ResourcesRequired":[] }
                """;
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();

        subscriberSeeing(divisGateway()).onConstructionDepot(new ColonisationConstructionDepotEvent(object));

        assertTrue(sitesWritten.isEmpty());
    }

    /**
     * The figure the card draws and the voice speaks. Confirmed against the real manifest: 70 delivered
     * tonnes of 6721 required reads as 0.010415, so it is a flat tonnage ratio rather than one weighted
     * by what the goods are worth. Worth pinning, because a card that derived it any other way would
     * disagree with the game's own panel.
     */
    @Test
    void constructionProgressIsDeliveredTonnesOverRequiredTonnes() {
        // Divis Gateway's seventeen lines add up to 6721 required tonnes; Computer Components, Food
        // Cartridges, Power Generators and Water Purifiers were the only ones delivered at that point.
        int requiredTonnes = 6721;
        int providedTonnes = 22 + 26 + 9 + 13;

        assertEquals(0.010415, providedTonnes / (double) requiredTonnes, 1e-6,
                "the figure the journal reported before the membrane run");
        assertEquals(0.026187, (providedTonnes + 106) / (double) requiredTonnes, 1e-6,
                "and after it - so progress is not weighted by what the goods are worth");
    }

    private static Requirement membrane(List<Requirement> manifest) {
        return manifest.stream()
                .filter(line -> line.getSymbol().equals("insulatingmembrane"))
                .findFirst()
                .orElseThrow();
    }

    /**
     * The bug this closes, from a live session: land at Divis Gateway, land at Johri Horizons, fly back to
     * Divis Gateway - and the overlay went on showing Johri Horizons. The manifest the commander returned to
     * had not moved while they were away, so the fingerprint skipped the write, and the write was the only
     * thing that said which build was current.
     */
    @Test
    void returningToASiteWhoseManifestHasNotMovedStillSaysWeAreThere() {
        ColonisationDepotSubscriber subscriber = subscriberSeeing(null);

        subscriber.onConstructionDepot(depot("2026-08-24T17:13:20Z", 0.978426, 106));
        subscriber.onConstructionDepot(depot("2026-08-24T17:22:49Z", 0.978426, 106));

        assertEquals(1, sitesWritten.size(), "an unchanged manifest is still not rewritten");
        assertEquals(List.of(MARKET_ID + "@2026-08-24T17:22:49Z"), padsRecorded,
                "but standing on the pad is recorded every time, which is what makes this build the current one");
    }

    /**
     * The timestamp travels with it. An identical republish is a fresh reading of the site's own panel, so a
     * manifest that has not moved in hours must not read AS OF LAST VISIT while the commander is standing in
     * front of it.
     */
    @Test
    void anUnchangedRepublishStillCountsAsHavingSeenTheManifest() {
        ColonisationDepotSubscriber subscriber = subscriberSeeing(null);

        subscriber.onConstructionDepot(depot("2026-08-24T17:13:20Z", 0.978426, 106));
        subscriber.onConstructionDepot(depot("2026-08-24T17:13:35Z", 0.978426, 106));
        subscriber.onConstructionDepot(depot("2026-08-24T17:13:50Z", 0.978426, 106));

        assertEquals(List.of(MARKET_ID + "@2026-08-24T17:13:35Z", MARKET_ID + "@2026-08-24T17:13:50Z"),
                padsRecorded);
    }

    /**
     * A manifest that HAS moved goes down the write path, which makes the site current itself - recording it
     * twice would be a second transaction saying what the first already said.
     */
    @Test
    void aChangedManifestNeedsNoSeparateArrival() {
        ColonisationDepotSubscriber subscriber = subscriberSeeing(null);

        subscriber.onConstructionDepot(depot("2026-08-24T17:13:20Z", 0.883202, 0));
        subscriber.onConstructionDepot(depot("2026-08-24T17:13:35Z", 0.978426, 106));

        assertEquals(2, sitesWritten.size());
        assertTrue(padsRecorded.isEmpty(), padsRecorded.toString());
    }
}
