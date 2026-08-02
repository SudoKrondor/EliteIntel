package elite.intel.ui.overlay;

import elite.intel.db.managers.BioSamplesManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.GenusDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which body the card is about, and when it goes away - the parts that run
 * against real stored state rather than a DTO handed in by a test.
 * <p>
 * Exercises the persisted path deliberately: the sampling list is read back from
 * the location row a DSS wrote and the bio-sample rows the scans wrote, so a
 * change to either shape would break the card silently on screen.
 */
class ExobiologyObjectiveLifecycleTest {

    private static final long BODY = 2L;
    private static final long OTHER_BODY = 5L;
    /**
     * Locations and bio samples live in one shared test database with no
     * delete-by-key, so each test gets its own system and body names rather than
     * cleaning up after itself - a leftover sample would otherwise finish the
     * next test's body before it started.
     */
    private static final AtomicInteger RUN = new AtomicInteger();

    private final PlayerSession session = PlayerSession.getInstance();
    private final LocationManager locations = LocationManager.getInstance();
    private final BioSamplesManager bioSamples = BioSamplesManager.getInstance();
    private final ExobiologyObjectiveSource source = new ExobiologyObjectiveSource();

    private long system;
    private long otherSystem;
    private String star;
    private String planet;
    private String otherPlanet;
    private LocationData<Long, Long> previousLocation;
    private Boolean previousDiscoveryToggle;

    @BeforeEach
    void setUp() {
        int run = RUN.incrementAndGet();
        star = String.format("Overlay Test %03d", run);
        system = 481167680000L + run * 2L;
        otherSystem = system + 1;
        planet = star + " " + BODY;
        otherPlanet = star + " " + OTHER_BODY;

        previousLocation = session.getLocationData();
        previousDiscoveryToggle = session.isDiscoveryAnnouncementOn();
        session.setDiscoveryAnnouncementOn(true);
        saveBodyWithGenuses(BODY, planet, "Bacterium", "Fonticulua");
        session.setCurrentLocationId(BODY, system);
    }

    @AfterEach
    void restoreSession() {
        session.setDiscoveryAnnouncementOn(previousDiscoveryToggle);
        if (previousLocation != null && previousLocation.getSystemAddress() != null) {
            session.setCurrentLocationId(
                    previousLocation.getInGameId() == null ? 0 : previousLocation.getInGameId(),
                    previousLocation.getSystemAddress());
        }
    }

    @Test
    void theBodyTheCommanderIsAtDrivesTheCard() {
        HudObjective card = source.currentObjective().orElseThrow();

        assertEquals("EXOBIOLOGY", card.title());
        assertEquals(List.of("GENUS", "BACTERIUM", "FONTICULUA"),
                card.rows().stream().map(HudRow::label).toList());
    }

    @Test
    void discoveryAnnouncementsOffHidesTheCard() {
        session.setDiscoveryAnnouncementOn(false);

        assertTrue(source.currentObjective().isEmpty());
    }

    /**
     * The commander's own position lags the scan: a body is surface-scanned from
     * orbit long before it is approached, and BodyID in the journal is unreliable
     * enough that the stored pointer is often some other body entirely. The card
     * has to come from the system, not from that pointer.
     */
    @Test
    void aScannedBodyShowsEvenWhenNothingPointsAtIt() {
        session.setCurrentLocationId(0L, system); // arrival star, no biology

        assertEquals("2", source.currentObjective().orElseThrow().subtitle());
    }

    /**
     * The whole point of deriving rather than remembering: closing the app with
     * sampling to do and reopening it has to bring the card back. A fresh source
     * is exactly what a restart produces.
     */
    @Test
    void theCardSurvivesARestart() {
        assertTrue(source.currentObjective().isPresent());

        ExobiologyObjectiveSource afterRestart = new ExobiologyObjectiveSource();

        assertEquals(source.currentObjective(), afterRestart.currentObjective());
    }

    @Test
    void leavingTheSystemClearsTheCard() {
        assertTrue(source.currentObjective().isPresent());

        session.setCurrentLocationId(0L, otherSystem);

        assertTrue(source.currentObjective().isEmpty());
    }

    @Test
    void returningToTheSystemBringsTheCardBack() {
        session.setCurrentLocationId(0L, otherSystem);
        assertTrue(source.currentObjective().isEmpty());

        session.setCurrentLocationId(0L, system);

        assertTrue(source.currentObjective().isPresent());
    }

    @Test
    void flyingToAnotherScannedBodySwitchesTheCard() {
        saveBodyWithGenuses(OTHER_BODY, otherPlanet, "Tussock");

        session.setCurrentLocationId(OTHER_BODY, system);

        HudObjective card = source.currentObjective().orElseThrow();
        assertEquals(List.of("GENUS", "TUSSOCK"), card.rows().stream().map(HudRow::label).toList());
    }

    /**
     * With no pointer to go on, body order keeps the choice from flickering.
     */
    @Test
    void aSecondScannedBodyDoesNotDisplaceTheFirst() {
        saveBodyWithGenuses(OTHER_BODY, otherPlanet, "Tussock");

        session.setCurrentLocationId(0L, system);

        assertEquals("2", source.currentObjective().orElseThrow().subtitle());
    }

    @Test
    void aSampledGenusIsGoneOnTheNextPollAndTheCardEndsWithTheBody() {
        bioSamples.add(completedSample("Bacterium"));

        Optional<HudObjective> afterFirst = source.currentObjective();
        assertEquals(List.of("GENUS", "FONTICULUA"),
                afterFirst.orElseThrow().rows().stream().map(HudRow::label).toList());

        bioSamples.add(completedSample("Fonticulua"));

        assertTrue(source.currentObjective().isEmpty(), "a finished body shows nothing");
    }

    // -- fixtures --------------------------------------------------------------

    private void saveBodyWithGenuses(long bodyId, String planetName, String... genusNames) {
        LocationDto body = new LocationDto(bodyId, system);
        body.setStarName(star);
        body.setPlanetName(planetName);
        body.setPlanetShortName(planetName.substring(planetName.lastIndexOf(' ') + 1));
        body.setBioSignals(genusNames.length);
        body.setGenus(java.util.Arrays.stream(genusNames).map(name -> {
            GenusDto genus = new GenusDto();
            genus.setGenusLocalised(name);
            genus.setGenusSymbol("$Codex_Ent_" + name + "_Genus_Name;");
            genus.setPlanetName(planetName);
            return genus;
        }).toList());
        locations.save(body);
    }

    private BioSampleDto completedSample(String genusName) {
        BioSampleDto sample = new BioSampleDto();
        sample.setPrimaryStar(star);
        sample.setPlanetName(planet);
        sample.setGenus(genusName);
        sample.setGenusSymbol("$Codex_Ent_" + genusName + "_Genus_Name;");
        sample.setSpecies(genusName + " Acies");
        sample.setSpeciesSymbol("$Codex_Ent_" + genusName + "_01_Name;");
        sample.setScanXof3(3);
        sample.setBioSampleCompleted(true);
        sample.setBodyId(BODY);
        return sample;
    }

}
