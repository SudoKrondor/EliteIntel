package elite.intel.ui.overlay;

import elite.intel.db.managers.BioSamplesManager;
import elite.intel.db.managers.FssSurveyManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.GenusDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The explorer's card through a survey, from arrival to sampling, against real stored state: the primary
 * star's discovery flag, the body rows the scans wrote, the honk's count and the samples taken. Each stage
 * is what the journal leaves behind at that point, written the way the subscribers write it.
 */
class DiscoveryObjectiveLifecycleTest {

    private static final long STAR_BODY = 0L;
    private static final long LIFE_BODY = 12L;
    /**
     * One shared test database with no delete-by-key, so each test gets its own system and names.
     */
    private static final AtomicInteger RUN = new AtomicInteger();

    private final PlayerSession session = PlayerSession.getInstance();
    private final LocationManager locations = LocationManager.getInstance();
    private final FssSurveyManager surveys = FssSurveyManager.getInstance();
    private final BioSamplesManager bioSamples = BioSamplesManager.getInstance();
    private final AtomicBoolean inOrbit = new AtomicBoolean(false);
    private final DiscoveryObjectiveSource source = new DiscoveryObjectiveSource(session, locations, surveys,
            new ExobiologyObjectiveSource(), inOrbit::get);

    private long system;
    private String star;
    private LocationData<Long, Long> previousLocation;
    private Boolean previousDiscoveryToggle;

    @BeforeEach
    void setUp() {
        int run = RUN.incrementAndGet();
        star = String.format("Discovery Test %03d", run);
        system = 481167690000L + run * 2L;

        previousLocation = session.getLocationData();
        previousDiscoveryToggle = session.isDiscoveryAnnouncementOn();
        session.setDiscoveryAnnouncementOn(true);
        savePrimaryStar(true);
        session.setCurrentLocationId(STAR_BODY, system);
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
    void beforeTheHonkTheCardIsTheBannerAndTheSystemName() {
        HudObjective card = source.currentObjective().orElseThrow();

        assertEquals("NEW DISCOVERY", card.title());
        assertEquals(star.toUpperCase(), card.subtitle());
        assertTrue(card.rows().isEmpty());
    }

    @Test
    void aSystemSomeoneElseDiscoveredHasNoCard() {
        savePrimaryStar(false);

        assertTrue(source.currentObjective().isEmpty());
    }

    @Test
    void discoveryAnnouncementsOffHidesTheCard() {
        session.setDiscoveryAnnouncementOn(false);

        assertTrue(source.currentObjective().isEmpty());
    }

    @Test
    void theHonkStartsTheScanProgress() {
        surveys.recordBodyCount(system, 15);

        HudRow fss = source.currentObjective().orElseThrow().rows().getFirst();
        assertEquals("FSS", fss.label());
        assertEquals(1, fss.current(), "the arrival star is the one body found so far");
        assertEquals(15, fss.max());
    }

    @Test
    void theFssFillsInTheFindsAndTheLife() {
        surveys.recordBodyCount(system, 15);
        saveBody(21, "2", "Sudarsky class III gas giant", 0);
        saveBody(24, "2 a", "Icy body", 0);
        saveBody(25, "2 b", "Icy body", 0);
        LocationDto geo = saveBody(11, "1 a", "High metal content body", 0);
        geo.setGeoSignals(2);
        locations.save(geo);
        saveBody(LIFE_BODY, "1 b", "Rocky body", 4);

        HudObjective card = source.currentObjective().orElseThrow();

        assertEquals(List.of("FSS", "ICY BODIES", "HIGH METAL", "GAS GIANTS", "GEO SIGNALS", "LIFE 1 B"),
                card.rows().stream().map(HudRow::label).toList());
        HudRow fss = card.rows().getFirst();
        assertEquals(6, fss.current());
        HudRow life = card.rows().getLast();
        assertEquals(0, life.current());
        assertEquals(4, life.max(), "before a DSS the FSS signal count is the total");
        assertEquals(HudRow.State.GOOD, life.state());
    }

    @Test
    void allBodiesFoundFillsTheBarEvenWithAScanTheAppNeverWrote() {
        surveys.recordBodyCount(system, 3);
        surveys.markAllBodiesFound(system, 3);

        HudRow fss = source.currentObjective().orElseThrow().rows().getFirst();
        assertEquals(3, fss.current());
        assertEquals(HudRow.State.GOOD, fss.state());
    }

    @Test
    void sampledGenusesAdvanceTheBodysLine() {
        surveys.recordBodyCount(system, 2);
        saveSurveyedLifeBody("Bacterium", "Stratum", "Tubus", "Tussock");
        bioSamples.add(completedSample("Tubus"));

        HudRow life = source.currentObjective().orElseThrow().rows().getLast();
        assertEquals("LIFE 1 B", life.label());
        assertEquals(1, life.current());
        assertEquals(4, life.max());
    }

    @Test
    void atABodyWithSamplingToDoTheExobiologyCardTakesOver() {
        surveys.recordBodyCount(system, 2);
        saveSurveyedLifeBody("Bacterium", "Tussock");
        session.setCurrentLocationId(LIFE_BODY, system);

        inOrbit.set(true);
        assertTrue(source.currentObjective().isEmpty(), "in orbit of the life body");

        inOrbit.set(false);
        assertTrue(source.currentObjective().isPresent(), "back in supercruise");
    }

    @Test
    void inOrbitOfABodyWithNoLifeTheDiscoveryCardStays() {
        surveys.recordBodyCount(system, 2);
        saveSurveyedLifeBody("Bacterium");
        saveBody(24, "2 a", "Icy body", 0);
        session.setCurrentLocationId(24L, system);
        inOrbit.set(true);

        assertTrue(source.currentObjective().isPresent());
    }

    /**
     * The card is the system's: jumping out clears it, so a plotted route - ranked below - is what shows.
     */
    @Test
    void leavingTheSystemClearsTheCardForThePlottedRoute() {
        surveys.recordBodyCount(system, 15);
        assertTrue(source.currentObjective().isPresent());

        session.setCurrentLocationId(0L, system + 1);

        assertTrue(source.currentObjective().isEmpty());
    }

    @Test
    void theCardSurvivesARestart() {
        surveys.recordBodyCount(system, 15);
        saveBody(LIFE_BODY, "1 b", "Rocky body", 4);

        DiscoveryObjectiveSource afterRestart = new DiscoveryObjectiveSource(session, locations, surveys,
                new ExobiologyObjectiveSource(), inOrbit::get);

        assertEquals(source.currentObjective(), afterRestart.currentObjective());
    }

    // -- fixtures --------------------------------------------------------------

    private void savePrimaryStar(boolean ourDiscovery) {
        LocationDto primary = new LocationDto(STAR_BODY, system);
        primary.setStarName(star);
        primary.setPlanetName(star);
        primary.setLocationType(LocationDto.LocationType.PRIMARY_STAR);
        primary.setOurDiscovery(ourDiscovery);
        locations.save(primary);
    }

    private LocationDto saveBody(long bodyId, String shortName, String planetClass, int bioSignals) {
        LocationDto body = new LocationDto(bodyId, system);
        body.setStarName(star);
        body.setPlanetName(star + " " + shortName);
        body.setPlanetShortName(shortName);
        body.setLocationType(shortName.contains(" ") ? LocationDto.LocationType.MOON : LocationDto.LocationType.PLANET);
        body.setPlanetClass(planetClass);
        body.setBioSignals(bioSignals);
        locations.save(body);
        return body;
    }

    private void saveSurveyedLifeBody(String... genusNames) {
        LocationDto body = saveBody(LIFE_BODY, "1 b", "Rocky body", genusNames.length);
        body.setGenus(Arrays.stream(genusNames).map(name -> {
            GenusDto genus = new GenusDto();
            genus.setGenusLocalised(name);
            genus.setGenusSymbol("$Codex_Ent_" + name + "_Genus_Name;");
            genus.setPlanetName(body.getPlanetName());
            return genus;
        }).toList());
        locations.save(body);
    }

    private BioSampleDto completedSample(String genusName) {
        BioSampleDto sample = new BioSampleDto();
        sample.setPrimaryStar(star);
        sample.setPlanetName(star + " 1 b");
        sample.setGenus(genusName);
        sample.setGenusSymbol("$Codex_Ent_" + genusName + "_Genus_Name;");
        sample.setSpecies(genusName + " Acies");
        sample.setSpeciesSymbol("$Codex_Ent_" + genusName + "_01_Name;");
        sample.setScanXof3(3);
        sample.setBioSampleCompleted(true);
        sample.setBodyId(LIFE_BODY);
        return sample;
    }
}
