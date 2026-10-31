package elite.intel.gameapi.journalimport;

import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.LocationFixtures;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.util.Cypher;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static elite.intel.gameapi.journalimport.TestArchives.*;
import static elite.intel.gameapi.journalimport.TestJournals.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * One backup, several commanders, years of flying: each commander's contracts, samples and discoveries land in their
 * own file, what belongs to everyone lands in the shared one, and nothing the app already knows about the present is
 * rolled back to the past.
 */
class JournalImporterTest {

    private static final String ALPHA = "F9100001";
    private static final String BRAVO = "F9100002";

    private static final long PROVIDER = 7_710_000_001L;
    private static final long GROUND = 7_710_000_002L;
    private static final long LONELY_GROUND = 7_710_000_003L;

    private static final String HIGH = "$MULTIPLAYER_SCENARIO78_TITLE;";

    /**
     * Each commander's file is a named in-memory database under test, gone once its last connection closes. These
     * keep them alive past the import's own connections, and are how the test reads what landed there.
     */
    private static Handle alphaFile;
    private static Handle bravoFile;

    @TempDir
    Path work;

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        alphaFile = Jdbi.create("jdbc:sqlite:file:testcmdr_" + ALPHA + "?mode=memory&cache=shared").open();
        bravoFile = Jdbi.create("jdbc:sqlite:file:testcmdr_" + BRAVO + "?mode=memory&cache=shared").open();
    }

    @AfterAll
    static void release() {
        alphaFile.close();
        bravoFile.close();
    }

    @Test
    void everyCommanderInTheBackupIsImportedIntoTheirOwnFile() throws IOException {
        seedThePresent();
        String openCommander = Database.currentCommander();
        Path backup = writeBackup();

        ImportReport report = JournalImporter.run(List.of(backup), (phase, done, total) -> {
        }, () -> false, work.resolve("staging"));

        assertEquals(List.of("Bravo", "Alpha"), report.commanders(), "the commander flying most recently first");
        assertEquals(4, report.journalsRead());
        assertEquals(1, report.unownedJournals());
        assertTrue(report.notJournals() >= 2, "the notes, and the file that only has a journal's name");
        assertEquals(openCommander, Database.currentCommander(), "the app's own file never moved");

        assertEquals(List.of(910_001L, 910_002L), contracts(alphaFile),
                "the continuation part's contract is Alpha's too, though that part never names Alpha");
        assertEquals(List.of(910_003L), contracts(bravoFile));
        assertEquals(3, report.contractsLearned());

        HuntingGroundManager grounds = HuntingGroundManager.getInstance();
        assertTrue(grounds.sitesIn("Archive Import Ground").high() > 0);
        assertTrue(grounds.sitesIn("Archive Import Lonely Ground").high() > 0,
                "a journal no commander can be told for still teaches where the resource sites are");

        assertEquals(1, alphaFile.createQuery("""
                        SELECT COUNT(*) FROM exo_mastery_sample
                         WHERE systemAddress = :system AND bodyId = 5 AND speciesSymbol = 'Stratum_07'""")
                .bind("system", GROUND).mapTo(Integer.class).one(), "the sample is filed with or without a catalogue");

        assertEquals(1, discoveryFlag(alphaFile, "Archive Import Ground A 1"),
                "a body Alpha found first is Alpha's discovery");
        assertEquals(1, discoveryFlag(alphaFile, "Archive Import Ground Known 2"),
                "an old scan can still say a body the app already knew was Alpha's discovery");
        assertNull(discoveryFlag(bravoFile, "Archive Import Ground A 1"), "and it is not Bravo's");

        LocationManager locations = LocationManager.getInstance();
        assertEquals(5000, locations.findSystemRecord(PROVIDER).getPopulation(),
                "a 2019 jump says the provider was empty - the record the live journal left says otherwise, and wins");
        assertEquals("Seeded class", locations.findBySystemAddress(GROUND, 2L).getPlanetClass(),
                "an old scan adds to a known body's flags and changes nothing else on it");

        try (Handle shared = Database.init()) {
            assertEquals("Archivetestium", shared.createQuery(
                    "SELECT commodity FROM commodities WHERE symbol = 'archivetestium'").mapTo(String.class).one());
            assertEquals("Archivtestin", shared.createQuery(
                            "SELECT commodity_de FROM commodities WHERE symbol = 'archivtestin'").mapTo(String.class).one(),
                    "Bravo flew a German client, so the name goes under German whatever client is installed today");
        }
        assertFalse(Files.exists(work.resolve("staging")), "the staging folder is cleared away");
    }

    @Test
    void importingTheSameBackupAgainLearnsNothingTwice() throws IOException {
        seedThePresent();
        Path backup = writeBackup();
        JournalImporter.run(List.of(backup), (phase, done, total) -> {
        }, () -> false, work.resolve("staging"));

        ImportReport again = JournalImporter.run(List.of(backup), (phase, done, total) -> {
        }, () -> false, work.resolve("staging"));

        assertEquals(0, again.contractsLearned());
        assertEquals(0, again.huntingGroundsLearned());
        assertEquals(List.of(910_001L, 910_002L), contracts(alphaFile));
    }

    @Test
    void cancellingStopsTheImportAndClearsTheStagingFolder() throws IOException {
        Path backup = writeBackup();

        assertThrows(ImportCancelledException.class, () -> JournalImporter.run(List.of(backup),
                (phase, done, total) -> {
                }, () -> true, work.resolve("staging")));
        assertFalse(Files.exists(work.resolve("staging")));
    }

    // ---------------------------------------------------------------- the backup

    private Path writeBackup() throws IOException {
        List<String> alphaFirst = new ArrayList<>(List.of(
                header("2019-04-01T10:00:00Z", "English/UK"),
                commander("2019-04-01T10:00:01Z", ALPHA, "Alpha"),
                jump("2019-04-01T10:05:00Z", "Archive Import Provider", PROVIDER, 10.0, 0),
                massacre("2019-04-01T10:10:00Z", 910_001L, "Provider Union", "Archive Import Ground"),
                jump("2019-04-01T10:20:00Z", "Archive Import Ground", GROUND, 12.0, 0),
                signal("2019-04-01T10:20:05Z", GROUND, HIGH),
                scan("2019-04-01T10:25:00Z", GROUND, "Archive Import Ground", "Archive Import Ground A 1", 5, false),
                scan("2019-04-01T10:26:00Z", GROUND, "Archive Import Ground", "Archive Import Ground Known 2", 2, false),
                finalOrganicScan("2019-04-01T11:00:00Z", GROUND, 5, "Stratum_07"),
                refined("2019-04-01T11:30:00Z", "archivetestium", "Archivetestium")));
        alphaFirst.addAll(garbage());
        String alphaContinued = journal(
                header("2019-04-01T16:00:00Z", "English/UK"),
                jump("2019-04-01T16:05:00Z", "Archive Import Provider", PROVIDER, 10.0, 0),
                massacre("2019-04-01T16:10:00Z", 910_002L, "Provider Union", "Archive Import Ground"));
        String bravo = journal(
                header("2020-06-01T10:00:00Z", "German/DE"),
                commander("2020-06-01T10:00:01Z", BRAVO, "Bravo"),
                jump("2020-06-01T10:05:00Z", "Archive Import Provider", PROVIDER, 10.0, 0),
                massacre("2020-06-01T10:10:00Z", 910_003L, "Provider Union", "Archive Import Ground"),
                refined("2020-06-01T11:30:00Z", "archivtestin", "Archivtestin"));
        String nobody = journal(
                header("2017-01-01T10:00:00Z", "English/UK"),
                legacyLoadGame("2017-01-01T10:00:01Z", "Never Named Again"),
                jump("2017-01-01T10:05:00Z", "Archive Import Lonely Ground", LONELY_GROUND, 30.0, 0),
                signal("2017-01-01T10:05:05Z", LONELY_GROUND, HIGH),
                massacre("2017-01-01T10:10:00Z", 910_004L, "Lonely Union", "Archive Import Lonely Ground"));

        return write(work.resolve("backup.zip"), zip(entries(
                "2019/Journal.2019-04-01T100000.01.log", journal(alphaFirst.toArray(String[]::new)),
                "2019/Journal.2019-04-01T160000.02.log", alphaContinued,
                "2020/Journal.2020-06-01T100000.01.log", bravo,
                "2017/Journal.170101100000.01.log", nobody,
                "2022/Journal.2022-01-01T000000.01.log", "this was never a journal",
                "notes.txt", "remember to back up")));
    }

    /**
     * What the app already knows when the import runs: the provider's population as the live journal left it, and a
     * body in the hunting ground with facts of its own on file.
     */
    private static void seedThePresent() {
        LocationDto provider = new LocationDto(0L, PROVIDER);
        provider.setStarName("Archive Import Provider");
        provider.setPlanetName("Archive Import Provider");
        provider.setLocationType(LocationDto.LocationType.PRIMARY_STAR);
        provider.setPopulation(5000);
        provider.setSecurity("Low Security");
        LocationFixtures.store(provider);

        LocationDto known = new LocationDto(2L, GROUND);
        known.setStarName("Archive Import Ground");
        known.setPlanetName("Archive Import Ground Known 2");
        known.setPlanetClass("Seeded class");
        LocationFixtures.store(known);
    }

    private static List<Long> contracts(Handle commanderFile) {
        return commanderFile.createQuery("SELECT missionId FROM massacre_mission WHERE missionId BETWEEN 910000 AND 910999 ORDER BY missionId")
                .mapTo(Long.class).list();
    }

    /**
     * The commander's discovery flag on a location: SQLite reads the stored JSON true back as 1. Null when the
     * commander has no visit row there at all.
     */
    private static Integer discoveryFlag(Handle commanderFile, String locationName) {
        return commanderFile.createQuery("SELECT flags ->> '$.ourDiscovery' FROM location_visit WHERE locationName = :name")
                .bind("name", locationName).mapTo(Integer.class).findOne().orElse(null);
    }
}
