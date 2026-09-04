package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.gameapi.journal.events.SupercruiseExitEvent;
import org.junit.jupiter.api.Test;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Every BodyType a supercruise exit reported across two months of journals (2016 drops), each line verbatim.
 * The subscriber used to run these through {@code LocationDto.determineType}, which answers {@code null} for
 * "Planet" - so the most common drop of all recorded nothing - and it never saved the record anyway.
 */
class SupercruiseExitClassificationTest {

    private static final String PLANET_DROP = """
            { "timestamp":"2026-07-09T03:18:06Z", "event":"SupercruiseExit", "Taxi":false, "Multicrew":false,
              "StarSystem":"Phylurn IB-O b22-0", "SystemAddress":724374595777,
              "Body":"Phylurn IB-O b22-0 2 b", "BodyID":23, "BodyType":"Planet" }
            """;

    private static final String COMPANION_STAR_DROP = """
            { "timestamp":"2026-07-09T03:51:23Z", "event":"SupercruiseExit", "Taxi":false, "Multicrew":false,
              "StarSystem":"Phylurn YC-H c11-2", "SystemAddress":639984079970,
              "Body":"Phylurn YC-H c11-2 A", "BodyID":2, "BodyType":"Star" }
            """;

    private static final String PRIMARY_STAR_DROP = """
            { "timestamp":"2026-07-11T21:32:52Z", "event":"SupercruiseExit", "Taxi":false, "Multicrew":false,
              "StarSystem":"IC 1287 Sector BG-X c1-9", "SystemAddress":2557082637194,
              "Body":"IC 1287 Sector BG-X c1-9", "BodyID":0, "BodyType":"Star" }
            """;

    private static final String STATION_DROP = """
            { "timestamp":"2026-07-10T21:01:59Z", "event":"SupercruiseExit", "Taxi":false, "Multicrew":false,
              "StarSystem":"Gnowee", "SystemAddress":358797546202,
              "Body":"Abasheli City", "BodyID":46, "BodyType":"Station" }
            """;

    private static final String RING_DROP = """
            { "timestamp":"2026-07-10T20:37:26Z", "event":"SupercruiseExit", "Taxi":false, "Multicrew":false,
              "StarSystem":"Gnowee", "SystemAddress":358797546202,
              "Body":"Gnowee 3 B Ring", "BodyID":24, "BodyType":"PlanetaryRing" }
            """;

    private static final String BELT_DROP = """
            { "timestamp":"2026-08-15T09:12:03Z", "event":"SupercruiseExit", "Taxi":false, "Multicrew":false,
              "StarSystem":"Col 285 Sector CV-M b21-0", "SystemAddress":673369957841,
              "Body":"Col 285 Sector CV-M b21-0 A A Belt", "BodyID":3, "BodyType":"StellarRing" }
            """;

    private static final String CLUSTER_DROP = """
            { "timestamp":"2026-07-25T03:09:16Z", "event":"SupercruiseExit", "Taxi":false, "Multicrew":false,
              "StarSystem":"Eol Prou MI-O c8-333", "SystemAddress":91602290418250,
              "Body":"Eol Prou MI-O c8-333 A A Belt Cluster 1", "BodyID":6, "BodyType":"AsteroidCluster" }
            """;

    /**
     * A barycentre: the point two stars orbit. BodyID 7 here, and 0 on two of the thirteen seen.
     */
    private static final String BARYCENTRE_DROP = """
            { "timestamp":"2026-08-05T03:46:54Z", "event":"SupercruiseExit", "Taxi":false, "Multicrew":false,
              "StarSystem":"Prua Hypa FN-K b53-0", "SystemAddress":551767323593,
              "Body":"Prua Hypa FN-K b53-0 A 1+2", "BodyID":7, "BodyType":"Null" }
            """;

    @Test
    void everyBodyTypeTheJournalReportsIsClassified() {
        assertEquals(PLANET, classify(PLANET_DROP));
        assertEquals(STAR, classify(COMPANION_STAR_DROP));
        assertEquals(STATION, classify(STATION_DROP));
        assertEquals(PLANETARY_RING, classify(RING_DROP));
        assertEquals(PLANETARY_RING, classify(BELT_DROP));
        assertEquals(BELT_CLUSTER, classify(CLUSTER_DROP));
    }

    @Test
    void onlyTheStarASystemIsNamedAfterIsItsPrimary() {
        assertEquals(PRIMARY_STAR, classify(PRIMARY_STAR_DROP));
        assertEquals(STAR, classify(COMPANION_STAR_DROP));
    }

    @Test
    void aBarycentreIsNotRecorded() {
        // Nothing is there, and its BodyID can be 0 - the primary star's id.
        assertNull(classify(BARYCENTRE_DROP));
        assertNull(classify(PLANET_DROP.replace("\"Planet\"", "\"\"")));
    }

    @Test
    void anUnknownBodyTypeIsStillRecorded() {
        // Frontier adds body types; the drop is worth keeping under its name for a later scan to classify.
        assertEquals(UNCLASSIFIED, classify(PLANET_DROP.replace("\"Planet\"", "\"Dyson Sphere\"")));
    }

    private static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType classify(String journalLine) {
        JsonObject json = JsonParser.parseString(journalLine).getAsJsonObject();
        return SupercruiseExitedSubscriber.classify(new SupercruiseExitEvent(json));
    }
}
