package elite.intel.db.managers;

import elite.intel.db.dao.ExoMasteryDao.Body;
import elite.intel.db.dao.ExoMasteryDao.Site;
import elite.intel.db.dao.ExoMasteryDao.Stats;
import elite.intel.db.util.Database;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.exomastery.ExoMasteryCatalog;
import elite.intel.gameapi.exomastery.ExoMasteryCatalog.ExoBody;
import elite.intel.gameapi.exomastery.ExoMasteryCatalog.ExoSpecies;
import elite.intel.gameapi.exomastery.ExoMasteryCatalog.ExoSystem;
import elite.intel.gameapi.gamestate.subscribers.ExoMasterySubscriber;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The ledger of where the exobiology money is and what the commander has already collected.
 * <p>
 * The cases are about the one thing that must never be lost - a body's completed flag - and the one
 * thing the route command needs right: the richest system by what is <em>left</em>. Each case owns
 * its own system addresses in a band no other test uses, and reads the ranking filtered to them, so
 * the tests share the database without seeing each other's systems.
 */
class ExoMasteryLedgerTest {

    private static final long BAND = 9_100_000_000L;

    private final ExoMasteryManager ledger = ExoMasteryManager.getInstance();
    private static final ExoMasterySubscriber SUBSCRIBER = new ExoMasterySubscriber();

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        // Registered by hand: the package scan that puts it on the bus in the app is not run here.
        GameEventBus.register(SUBSCRIBER);
    }

    @AfterAll
    static void unregister() {
        GameEventBus.unregister(SUBSCRIBER);
    }

    @Test
    void theRichestSystemIsTheOneWithMostLeftToSample() {
        long rich = BAND + 10, poor = BAND + 11;
        load(system("Exo Rich A", rich, body("Exo Rich A 1", 1, 50_000_000, "Stratum_07"), body("Exo Rich A 2", 2, 30_000_000, "Stratum_07")),
                system("Exo Poor A", poor, body("Exo Poor A 1", 1, 60_000_000, "Stratum_07")));

        assertEquals(List.of("Exo Rich A", "Exo Poor A"), ranking(rich, poor), "eighty million on offer beats sixty");

        ledger.setBodyCompleted(rich, 1, true);

        assertEquals(List.of("Exo Poor A", "Exo Rich A"), ranking(rich, poor),
                "with the fifty-million body done, thirty million is what Rich still offers - and sixty beats it");
        assertEquals(30_000_000, siteOf(rich).remainingValue());
    }

    @Test
    void aSystemSampledOutDropsOffTheList() {
        long done = BAND + 20, open = BAND + 21;
        load(system("Exo Done B", done, body("Exo Done B 1", 1, 90_000_000, "Stratum_07")),
                system("Exo Open B", open, body("Exo Open B 1", 1, 10_000_000, "Stratum_07")));

        ledger.setBodyCompleted(done, 1, true);

        assertEquals(List.of("Exo Open B"), ranking(done, open));
        assertFalse(ledger.hasRemaining(done));
        assertTrue(ledger.hasRemaining(open));
    }

    @Test
    void theThirdScanOfTheLastListedSpeciesCompletesTheBody() {
        long address = BAND + 30;
        load(system("Exo Scan C", address, body("Exo Scan C 1", 1, 35_000_000, "Stratum_07", "Tussocks_14")));

        assertFalse(ledger.recordSampled(address, 1, "Stratum_07"), "one of two species sampled: the body is still open");
        assertTrue(ledger.hasRemaining(address));
        assertFalse(ledger.recordSampled(address, 1, "Osseus_02"), "a species the catalogue never listed here changes nothing");
        assertTrue(ledger.recordSampled(address, 1, "Tussocks_14"), "the last listed species: sampled out");
        assertFalse(ledger.hasRemaining(address));
        assertTrue(ledger.bodiesIn(address).get(0).completed());
    }

    @Test
    void disablingKeepsTheCompletedBodiesAndReEnablingDoesNotReopenThem() {
        long address = BAND + 40;
        ExoSystem system = system("Exo Purge D", address,
                body("Exo Purge D 1", 1, 20_000_000, "Stratum_07"), body("Exo Purge D 2", 2, 15_000_000, "Stratum_07"));
        load(system);
        ledger.setBodyCompleted(address, 2, true);

        ledger.purge();

        assertFalse(ledger.isEnabled(), "the catalogue is gone, so the feature is off");
        List<Body> kept = ledger.bodiesIn(address);
        assertEquals(1, kept.size(), "the uncompleted body went with the catalogue, the completed one stayed");
        assertEquals("Exo Purge D 2", kept.get(0).bodyName());
        assertTrue(kept.get(0).completed());
        Stats stats = ledger.stats();
        assertEquals(0, stats.systems());
        assertTrue(stats.harvestedValue() >= 15_000_000, "what was collected still counts");

        load(system);

        assertTrue(ledger.isEnabled());
        List<Body> reloaded = ledger.bodiesIn(address);
        assertEquals(2, reloaded.size());
        assertEquals(List.of(false, true), reloaded.stream().map(Body::completed).toList(),
                "the re-import brought the open body back and left the completed one completed");
        assertEquals(20_000_000, siteOf(address).remainingValue());
    }

    @Test
    void writingOffASystemClosesEveryOpenBodyAndNamesThem() {
        long address = BAND + 50;
        load(system("Exo Write E", address,
                body("Exo Write E 1", 1, 20_000_000, "Stratum_07"),
                body("Exo Write E 2 a", 2, 15_000_000, "Stratum_07"),
                body("Exo Write E 3", 3, 12_000_000, "Stratum_07")));
        ledger.setBodyCompleted(address, 3, true);

        List<Body> written = ledger.completeSystem(address);

        assertEquals(List.of("1", "2 a"), written.stream().map(ExoMasteryManager::shortBodyName).toList(),
                "the two still open, richest first, named as a commander would");
        assertFalse(ledger.hasRemaining(address));
        assertTrue(ledger.completeSystem(address).isEmpty(), "nothing left to write off the second time");
    }

    @Test
    void aBodyAlreadySampledOutOnTheLocationTableIsCompletedOnImport() {
        long address = BAND + 60;
        LocationDto known = new LocationDto(4L, address);
        known.setStarName("Exo Known F");
        known.setPlanetName("Exo Known F 4");
        known.markBioScansCompleted();
        LocationManager.getInstance().save(known);

        load(system("Exo Known F", address, body("Exo Known F 4", 4, 20_000_000, "Stratum_07"), body("Exo Known F 5", 5, 20_000_000, "Stratum_07")));

        List<Body> bodies = ledger.bodiesIn(address);
        assertEquals(List.of("Exo Known F 5", "Exo Known F 4"), bodies.stream().map(Body::bodyName).toList(), "open first");
        assertTrue(bodies.get(1).completed(), "the location table already knew this one was done");
        assertFalse(bodies.get(0).completed());
    }

    @Test
    void theSurveyCompleteLatchFlippingOnALocationCompletesTheBodyThroughTheBus() {
        long address = BAND + 70;
        load(system("Exo Latch G", address, body("Exo Latch G 2", 2, 20_000_000, "Stratum_07")));
        LocationManager locations = LocationManager.getInstance();
        locations.updateBody(address, 2, location -> {
            location.setStarName("Exo Latch G");
            location.setPlanetName("Exo Latch G 2");
        });
        assertTrue(ledger.hasRemaining(address), "a body saved without the latch stays open");

        locations.updateBody(address, 2, LocationDto::markBioScansCompleted);
        assertFalse(ledger.hasRemaining(address), "the game's own word that the body is sampled out");

        locations.updateBody(address, 2, location -> location.setBioScansCompleted(false));
        assertTrue(ledger.hasRemaining(address), "and the commander taking it back reopens it");
    }

    @Test
    void theCatalogueFileParsesAndANewerLayoutIsRefused() throws IOException {
        String json = """
                {"format":1,"generated":"2026-09-16","source":"test","minColonies":3,"systems":[
                  {"name":"76 Leonis","systemAddress":358999069386,"x":159.96875,"y":245.21875,"z":-30.5,"value":27248600,
                   "bodies":[{"name":"76 Leonis 6 g","bodyId":31,"type":"Planet","value":27248600,
                     "species":[{"name":"Tussock Virgam","symbol":"Tussocks_14","count":15,"value":14313700},
                                {"name":"Osseus Discus","symbol":"Osseus_02","count":23,"value":12934900}]}]}]}
                """;
        ExoMasteryCatalog catalog = ExoMasteryCatalog.parse(json.getBytes(StandardCharsets.UTF_8));
        ExoBody body = catalog.systems().get(0).bodies().get(0);
        assertEquals(358999069386L, catalog.systems().get(0).systemAddress());
        assertEquals(31, body.bodyId());
        assertEquals("Tussocks_14", body.species().get(0).symbol());

        IOException refused = assertThrows(IOException.class, () ->
                ExoMasteryCatalog.parse("{\"format\":2,\"systems\":[]}".getBytes(StandardCharsets.UTF_8)));
        assertTrue(refused.getMessage().contains("format 2"));
    }

    @Test
    void shortBodyNameDropsTheSystemAndNothingElse() {
        assertEquals("6 g", ExoMasteryManager.shortBodyName(new Body(1, 1, "76 Leonis", "76 Leonis 6 g", "Planet", 1, false)));
        assertEquals("Elsewhere B", ExoMasteryManager.shortBodyName(new Body(1, 1, "76 Leonis", "Elsewhere B", "Planet", 1, false)));
        assertEquals("76 Leonis", ExoMasteryManager.shortBodyName(new Body(1, 1, "76 Leonis", "76 Leonis", "Planet", 1, false)));
    }

    // ---------------------------------------------------------------- helpers

    private void load(ExoSystem... systems) {
        ledger.importCatalog(new ExoMasteryCatalog(1, "2026-09-16", "test", 3, List.of(systems)), percent -> {
        });
    }

    private List<String> ranking(long... mine) {
        Set<Long> own = Set.of(java.util.Arrays.stream(mine).boxed().toArray(Long[]::new));
        return ledger.richestRemaining(1000).stream()
                .filter(site -> own.contains(site.systemAddress()))
                .map(Site::starSystem)
                .toList();
    }

    private Site siteOf(long address) {
        return ledger.richestRemaining(1000).stream().filter(site -> site.systemAddress() == address).findFirst().orElseThrow();
    }

    private static ExoSystem system(String name, long address, ExoBody... bodies) {
        return new ExoSystem(name, address, address % 1000, 0, 0,
                java.util.Arrays.stream(bodies).mapToLong(ExoBody::value).sum(), List.of(bodies));
    }

    private static ExoBody body(String name, long bodyId, long value, String... species) {
        List<ExoSpecies> list = java.util.Arrays.stream(species)
                .map(symbol -> new ExoSpecies(symbol, symbol, 5, value / species.length)).toList();
        return new ExoBody(name, bodyId, "Planet", value, list);
    }
}
