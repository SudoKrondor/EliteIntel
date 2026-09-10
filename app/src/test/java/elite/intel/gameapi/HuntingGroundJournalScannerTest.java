package elite.intel.gameapi;

import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.missions.MassacrePair;
import elite.intel.gameapi.missions.ResourceSiteProfile;
import elite.intel.util.Cypher;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Learning years of flying back out of the journal archive.
 * <p>
 * The scan exists because the pairing has to come from somewhere on the day the feature ships. A
 * commander who has been playing for years has already visited every hunting ground they use and
 * taken every contract that pairs with one - it is all on disk, and reading it is the difference
 * between a feature that works this evening and one that works in three months.
 */
class HuntingGroundJournalScannerTest {

    private static final String HAZARDOUS = "$MULTIPLAYER_SCENARIO79_TITLE;";
    private static final String HIGH = "$MULTIPLAYER_SCENARIO78_TITLE;";
    private static final String LOW = "$MULTIPLAYER_SCENARIO77_TITLE;";

    private final HuntingGroundManager ledger = HuntingGroundManager.getInstance();

    @TempDir
    Path journals;

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @BeforeEach
    void forgetWhereTheLastScanStopped() {
        try (Handle handle = Database.init()) {
            handle.execute("UPDATE hunting_ground_scan SET lastJournal = NULL WHERE id = 1");
        }
    }

    @Test
    void theArchiveTeachesGroundsPairsAndTheStationsTheContractsCameFrom() throws IOException {
        writeArchive();

        HuntingGroundJournalScanner.Result result = new HuntingGroundJournalScanner().scan(journals);

        assertEquals(2, result.journalsRead());
        assertEquals(new ResourceSiteProfile(0, 3, 1, 2), ledger.sitesIn("Scan Ground One"));

        MassacrePair pair = onlyPairFrom("Scan Provider One");
        assertEquals("Scan Ground One", pair.targetSystem());
        assertEquals("One Cartel", pair.targetFaction());
        assertEquals(List.of("Scan Defence", "Scan Union"), pair.providerFactions().stream().sorted().toList(),
                "a state-flavoured massacre contract sends the commander to the same ring as a plain one, "
                        + "so it teaches the same pairing - while Scan Couriers, whose contract in the same "
                        + "journal is a collection run against the same system, teaches nothing here");
        assertEquals(List.of("Scan Dock Alpha"), pair.stations());
        assertEquals(1, pair.missionsCompleted(),
                "one of the two contracts has a MissionCompleted in the archive, and a pairing the "
                        + "commander finished work on outranks one they only ever tried");
    }

    @Test
    void aSignalIsCreditedToTheSystemThatReportedItAndNotToWhereTheScanThoughtItWas() throws IOException {
        writeArchive();

        new HuntingGroundJournalScanner().scan(journals);

        assertNull(ledger.sitesIn("Scan Barren One"),
                "the low site announced while the ship sat in Scan Barren One carries Scan Ground One's "
                        + "system address, and reading the address is what stops the app inventing rings "
                        + "in a system that has none");
        assertEquals(3, ledger.sitesIn("Scan Ground One").low(),
                "it belongs to Scan Ground One, and as a sweep of its own it does not add to the "
                        + "three that arrival already counted");
    }

    @Test
    void aContractAgainstASystemWithNoRingsIsRecordedButNeverOffered() throws IOException {
        writeArchive();

        new HuntingGroundJournalScanner().scan(journals);

        assertTrue(ledger.bestPairs(at("Scan Barren One", 500), 1).isEmpty());
    }

    @Test
    void aSecondRunReadsOnlyWhatIsNewAndLearnsNothingTwice() throws IOException {
        writeArchive();
        new HuntingGroundJournalScanner().scan(journals);

        HuntingGroundJournalScanner.Result again = new HuntingGroundJournalScanner().scan(journals);

        assertEquals(1, again.journalsRead(),
                "the bookmarked journal is re-read because the game may still have been writing it, "
                        + "and everything before it is skipped");
        assertEquals(0, again.huntingGroundsLearned());
        assertEquals(0, again.contractsLearned());
        assertEquals(List.of("Scan Defence", "Scan Union"),
                onlyPairFrom("Scan Provider One").providerFactions().stream().sorted().toList(),
                "MissionID is the key, so a contract read again is the same contract");
    }

    // ---------------------------------------------------------------- fixtures

    /**
     * Two journals: a night spent finding a ring system and taking work against it, and a later one
     * that visits a system with no rings at all - and, in the middle of that visit, reports one more
     * of the first system's sites.
     */
    private void writeArchive() throws IOException {
        writeJournal("Journal.2026-09-01T100000.01.log", 1_000_000L, List.of(
                arrival("2026-09-01T10:00:00Z", "Scan Ground One", 111, 100),
                signal("2026-09-01T10:00:05Z", 111, HAZARDOUS),
                signal("2026-09-01T10:00:05Z", 111, HAZARDOUS),
                signal("2026-09-01T10:00:05Z", 111, HIGH),
                signal("2026-09-01T10:00:05Z", 111, LOW),
                signal("2026-09-01T10:00:05Z", 111, LOW),
                signal("2026-09-01T10:00:05Z", 111, LOW),
                arrival("2026-09-01T11:00:00Z", "Scan Provider One", 222, 101),
                docked("2026-09-01T11:05:00Z", "Scan Dock Alpha"),
                accepted("2026-09-01T11:06:00Z", "Scan Union", "Mission_Massacre",
                        "$MissionUtil_FactionTag_Pirate;", "One Cartel", "Scan Ground One", 7001),
                accepted("2026-09-01T11:07:00Z", "Scan Defence", "Mission_Massacre_Legal_Military",
                        "$MissionUtil_FactionTag_Pirate;", "One Cartel", "Scan Ground One", 7002),
                accepted("2026-09-01T11:08:00Z", "Scan Couriers", "Mission_Collect",
                        "", "One Cartel", "Scan Ground One", 7003)
        ));

        writeJournal("Journal.2026-09-02T100000.01.log", 2_000_000L, List.of(
                arrival("2026-09-02T10:00:00Z", "Scan Barren One", 333, 500),
                signal("2026-09-02T10:01:00Z", 111, LOW),
                accepted("2026-09-02T10:05:00Z", "Barren Union", "Mission_Massacre",
                        "$MissionUtil_FactionTag_Pirate;", "Barren Cartel", "Scan Barren One", 7004),
                completed("2026-09-02T11:00:00Z", 7001)
        ));
    }

    private void writeJournal(String name, long lastModified, List<String> lines) throws IOException {
        Path file = journals.resolve(name);
        Files.write(file, String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        assertTrue(file.toFile().setLastModified(lastModified), "the scan reads journals oldest first");
    }

    private static String arrival(String timestamp, String starSystem, long address, double x) {
        return ("{ \"timestamp\":\"%s\", \"event\":\"FSDJump\", \"StarSystem\":\"%s\", "
                + "\"SystemAddress\":%d, \"StarPos\":[%s,0.0,0.0] }").formatted(timestamp, starSystem, address, x);
    }

    private static String signal(String timestamp, long address, String signalName) {
        return ("{ \"timestamp\":\"%s\", \"event\":\"FSSSignalDiscovered\", \"SystemAddress\":%d, "
                + "\"SignalName\":\"%s\", \"SignalType\":\"ResourceExtraction\" }")
                .formatted(timestamp, address, signalName);
    }

    private static String docked(String timestamp, String stationName) {
        return "{ \"timestamp\":\"%s\", \"event\":\"Docked\", \"StationName\":\"%s\", \"MarketID\":1 }"
                .formatted(timestamp, stationName);
    }

    private static String accepted(String timestamp, String faction, String name, String targetType,
                                   String targetFaction, String destination, long missionId) {
        return ("{ \"timestamp\":\"%s\", \"event\":\"MissionAccepted\", \"Faction\":\"%s\", \"Name\":\"%s\", "
                + "\"TargetType\":\"%s\", \"TargetFaction\":\"%s\", \"KillCount\":4, "
                + "\"DestinationSystem\":\"%s\", \"Wing\":false, \"Reward\":1000000, \"MissionID\":%d }")
                .formatted(timestamp, faction, name, targetType, targetFaction, destination, missionId);
    }

    private static String completed(String timestamp, long missionId) {
        return ("{ \"timestamp\":\"%s\", \"event\":\"MissionCompleted\", \"Faction\":\"Scan Union\", "
                + "\"Name\":\"Mission_Massacre_name\", \"TargetType\":\"$MissionUtil_FactionTag_Pirate;\", "
                + "\"TargetFaction\":\"One Cartel\", \"MissionID\":%d, \"Reward\":1000000 }")
                .formatted(timestamp, missionId);
    }

    private static Coordinates at(String starSystem, double x) {
        return new Coordinates(starSystem, x, 0, 0);
    }

    private MassacrePair onlyPairFrom(String providerSystem) {
        List<MassacrePair> pairs = ledger.bestPairs(at(providerSystem, 101), 1);
        assertEquals(1, pairs.size(), "one provider and one target faction is one pairing");
        return pairs.getFirst();
    }
}
