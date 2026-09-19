package elite.intel.gameapi.gamestate.subscribers;

import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.gamestate.status_events.PlayerMovedEvent;
import elite.intel.gameapi.gamestate.subscribers.BioSampleTrackingSubscriber.Separation;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The separation tracker runs on every Status.json tick while the commander is on a surface. It used
 * to load the body, stamp the flags and save the whole row back each time, outside the body's write
 * lock - so a tick that loaded the row just before a completed genus's partials were deleted wrote
 * them straight back, and the commander kept hearing "minimum separation reached" all the way to the
 * ship after being told the survey was complete.
 */
class BioSampleTrackingSubscriberTest {

    private static final AtomicInteger RUN = new AtomicInteger();
    private static final long BODY = 31;
    /**
     * Large enough that a hundredth of a degree is well inside a 500 m colony range and a twentieth well outside.
     */
    private static final double RADIUS = 1_000_000;
    private static final double BACTERIUM_LAT = 0, BACTERIUM_LON = 0;
    private static final double NEAR_LAT = 0.01;   // ~175 m from the colony
    private static final double FAR_LAT = 0.05;    // ~870 m from the colony

    private final PlayerSession session = PlayerSession.getInstance();
    private final LocationManager locations = LocationManager.getInstance();
    private final BioSampleTrackingSubscriber tracker = new BioSampleTrackingSubscriber();

    private long system;
    private String star;
    private LocationData<Long, Long> previousLocation;

    @BeforeEach
    void setUp() {
        int run = RUN.incrementAndGet();
        star = String.format("Separation Test %03d", run);
        system = 591167680000L + run;

        LocationDto body = new LocationDto(BODY, system);
        body.setStarName(star);
        body.setPlanetName(star + " 6 g");
        locations.save(body);

        previousLocation = session.getLocationData();
        session.setCurrentLocationId(BODY, system);
    }

    @AfterEach
    void restoreSession() {
        if (previousLocation != null && previousLocation.getSystemAddress() != null) {
            session.setCurrentLocationId(
                    previousLocation.getInGameId() == null ? 0 : previousLocation.getInGameId(),
                    previousLocation.getSystemAddress());
        }
    }

    @Test
    void nothingTrackedMeansNothingToMeasure() {
        assertNull(BioSampleTrackingSubscriber.refresh(List.of(), at(FAR_LAT)));
        assertNull(BioSampleTrackingSubscriber.refresh(null, at(FAR_LAT)));
    }

    @Test
    void walkingClearOfEveryPartialFlipsTheAggregateAndStampsEachSample() {
        List<BioSampleDto> partials = List.of(bacterium(1, false), bacterium(2, false));

        Separation separation = BioSampleTrackingSubscriber.refresh(partials, at(FAR_LAT));

        assertEquals(new Separation(false, true), separation);
        assertTrue(separation.flipped());
        assertTrue(partials.stream().allMatch(BioSampleDto::isPlayerFarEnough));
    }

    @Test
    void steppingBackInsideRangeOfOnePartialFlipsItBack() {
        List<BioSampleDto> partials = List.of(bacterium(1, true), bacterium(2, true));

        Separation separation = BioSampleTrackingSubscriber.refresh(partials, at(NEAR_LAT));

        assertEquals(new Separation(true, false), separation);
    }

    @Test
    void aTickThatChangesNothingDoesNotTouchTheRow() {
        locations.updateBody(system, BODY, body -> body.addBioScan(bacterium(1, false)));

        tracker.onPlayerMovedEvent(at(NEAR_LAT));

        BioSampleDto stored = locations.findBySystemAddress(system, BODY).getPartialBioSamples().getFirst();
        assertFalse(stored.isPlayerFarEnough());
    }

    @Test
    void aTickThatFlipsTheAggregateIsPersisted() {
        locations.updateBody(system, BODY, body -> body.addBioScan(bacterium(1, false)));

        tracker.onPlayerMovedEvent(at(FAR_LAT));

        BioSampleDto stored = locations.findBySystemAddress(system, BODY).getPartialBioSamples().getFirst();
        assertTrue(stored.isPlayerFarEnough());
    }

    /**
     * The support-bundle case, forced: the commander is walking back to the SRV (ticks landing every
     * moment, each one flipping the aggregate so each one writes) while the Analyse scan deletes the
     * genus's partials. Whichever way the two interleave, the partials must stay deleted.
     */
    @Test
    void aCompletedGenusStaysDeletedWhileMovementTicksRaceTheCompletion() throws Exception {
        locations.updateBody(system, BODY, body -> {
            body.addBioScan(bacterium(1, false));
            body.addBioScan(bacterium(2, false));
            body.addBioScan(bacterium(3, false));
        });

        int ticks = 40;
        AtomicInteger ticked = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        Thread walker = Thread.ofVirtual().start(() -> {
            try {
                start.await();
                for (int i = 0; i < ticks; i++) {
                    tracker.onPlayerMovedEvent(at(i % 2 == 0 ? FAR_LAT : NEAR_LAT));
                    ticked.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        });
        Thread analyser = Thread.ofVirtual().start(() -> {
            try {
                start.await();
                locations.updateBody(system, BODY,
                        body -> body.deletePartialBioSamplesFor("Bacterial", "Bacterium"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        });

        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "walker or analyser did not finish");
        walker.join();
        analyser.join();
        assertEquals(ticks, ticked.get(), "a tick threw part-way, so the race was never exercised");

        assertTrue(locations.findBySystemAddress(system, BODY).getPartialBioSamples().isEmpty(),
                "a movement tick wrote the completed genus's partials back");
    }

    private static PlayerMovedEvent at(double latitude) {
        return new PlayerMovedEvent(latitude, BACTERIUM_LON, RADIUS, 0);
    }

    private BioSampleDto bacterium(int scanXof3, boolean farEnough) {
        BioSampleDto sample = new BioSampleDto();
        sample.setPrimaryStar(star);
        sample.setPlanetName(star + " 6 g");
        sample.setBodyId(BODY);
        sample.setGenus("Bacterium");
        sample.setGenusSymbol("Bacterial");
        sample.setSpecies("Cerbrus");
        sample.setSpeciesSymbol("Bacterial_12");
        sample.setScanLatitude(BACTERIUM_LAT);
        sample.setScanLongitude(BACTERIUM_LON);
        sample.setScanXof3(scanXof3);
        sample.setPlayerFarEnough(farEnough);
        return sample;
    }
}
