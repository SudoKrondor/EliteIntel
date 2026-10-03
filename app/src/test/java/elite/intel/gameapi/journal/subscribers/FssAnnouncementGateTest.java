package elite.intel.gameapi.journal.subscribers;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static elite.intel.gameapi.journal.subscribers.FssAnnouncementGate.SIGNAL_REPEAT;
import static elite.intel.gameapi.journal.subscribers.FssAnnouncementGate.SYSTEM_FEATURE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * How often VEGA repeats an FSS signal announcement.
 * <p>
 * The reported case, from the journal: a jump into Colonia logged three identical
 * {@code $Fixed_Event_Life_Cloud;} signals at 20:58:54, and "Discovered Notable Stellar Phenomena" was said
 * three times.
 */
class FssAnnouncementGateTest {

    private static final String COLONIA_LIFE_CLOUD = "3238296097059|$Fixed_Event_Life_Cloud;";
    private static final String ELSEWHERE_LIFE_CLOUD = "1234567890|$Fixed_Event_Life_Cloud;";
    private static final String SALVAGE_14_MINUTES = "3238296097059|Degraded emissions: 14 minutes remaining";
    private static final String SALVAGE_9_MINUTES = "3238296097059|Degraded emissions: 9 minutes remaining";
    private static final Instant ARRIVAL = Instant.parse("2026-10-01T20:58:54Z");

    @Test
    void theArrivalBurstIsAnnouncedOnce() {
        FssAnnouncementGate gate = new FssAnnouncementGate();
        assertTrue(gate.shouldAnnounce(COLONIA_LIFE_CLOUD, ARRIVAL, SYSTEM_FEATURE));
        assertFalse(gate.shouldAnnounce(COLONIA_LIFE_CLOUD, ARRIVAL, SYSTEM_FEATURE));
        assertFalse(gate.shouldAnnounce(COLONIA_LIFE_CLOUD, ARRIVAL, SYSTEM_FEATURE));
    }

    @Test
    void anotherSystemIsItsOwnDiscovery() {
        FssAnnouncementGate gate = new FssAnnouncementGate();
        assertTrue(gate.shouldAnnounce(COLONIA_LIFE_CLOUD, ARRIVAL, SYSTEM_FEATURE));
        assertTrue(gate.shouldAnnounce(ELSEWHERE_LIFE_CLOUD, ARRIVAL.plusSeconds(40), SYSTEM_FEATURE));
    }

    @Test
    void stayingInTheSystemStaysQuietAndALaterReturnIsToldAgain() {
        FssAnnouncementGate gate = new FssAnnouncementGate();
        assertTrue(gate.shouldAnnounce(COLONIA_LIFE_CLOUD, ARRIVAL, SYSTEM_FEATURE));
        assertFalse(gate.shouldAnnounce(COLONIA_LIFE_CLOUD, ARRIVAL.plus(SYSTEM_FEATURE).minusSeconds(1), SYSTEM_FEATURE));
        assertTrue(gate.shouldAnnounce(COLONIA_LIFE_CLOUD, ARRIVAL.plus(SYSTEM_FEATURE), SYSTEM_FEATURE));
    }

    @Test
    void identicalSalvageInABurstIsSaidOnceButADifferentOneStillIs() {
        FssAnnouncementGate gate = new FssAnnouncementGate();
        assertTrue(gate.shouldAnnounce(SALVAGE_14_MINUTES, ARRIVAL, SIGNAL_REPEAT));
        assertFalse(gate.shouldAnnounce(SALVAGE_14_MINUTES, ARRIVAL, SIGNAL_REPEAT));
        assertTrue(gate.shouldAnnounce(SALVAGE_9_MINUTES, ARRIVAL, SIGNAL_REPEAT),
                "a second source with another timer is a real signal");
    }

    @Test
    void theSameSalvageSentenceCanBeSaidAgainOnceTheShortQuietIsOver() {
        FssAnnouncementGate gate = new FssAnnouncementGate();
        assertTrue(gate.shouldAnnounce(SALVAGE_14_MINUTES, ARRIVAL, SIGNAL_REPEAT));
        assertTrue(gate.shouldAnnounce(SALVAGE_14_MINUTES, ARRIVAL.plus(SIGNAL_REPEAT), SIGNAL_REPEAT));
    }

    @Test
    void aBurstHandledOnParallelThreadsIsStillAnnouncedOnce() throws Exception {
        // The subscriber handles each signal on its own virtual thread, so the burst reaches the gate at once.
        FssAnnouncementGate gate = new FssAnnouncementGate();
        AtomicInteger announced = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        List<CompletableFuture<Void>> signals = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            signals.add(CompletableFuture.runAsync(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (gate.shouldAnnounce(COLONIA_LIFE_CLOUD, ARRIVAL, SYSTEM_FEATURE)) announced.incrementAndGet();
            }));
        }
        start.countDown();
        CompletableFuture.allOf(signals.toArray(CompletableFuture[]::new)).get(10, TimeUnit.SECONDS);
        assertEquals(1, announced.get());
    }
}
