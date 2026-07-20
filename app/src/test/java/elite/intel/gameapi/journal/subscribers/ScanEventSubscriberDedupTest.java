package elite.intel.gameapi.journal.subscribers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the per-body announcement guard. The game emits an {@code AutoScan} on arrival and a
 * {@code Detailed} scan when a body is honked or targeted, both carrying {@code WasDiscovered:false},
 * which announced "New System discovered!" twice seconds apart.
 */
class ScanEventSubscriberDedupTest {

    private static final long SYSTEM = 1234567890L;
    private static final long OTHER_SYSTEM = 987654321L;

    private final ScanEventSubscriber subscriber = new ScanEventSubscriber();

    @Test
    void theSecondScanOfTheSameBodyDoesNotAnnounce() {
        assertTrue(subscriber.isFirstAnnouncementForBody(SYSTEM, 0L), "AutoScan should announce");
        assertFalse(subscriber.isFirstAnnouncementForBody(SYSTEM, 0L), "Detailed scan of the same body must not");
    }

    @Test
    void eachBodyInASystemAnnouncesOnItsOwn() {
        assertTrue(subscriber.isFirstAnnouncementForBody(SYSTEM, 0L));
        assertTrue(subscriber.isFirstAnnouncementForBody(SYSTEM, 1L));
        assertTrue(subscriber.isFirstAnnouncementForBody(SYSTEM, 2L));
        assertFalse(subscriber.isFirstAnnouncementForBody(SYSTEM, 1L));
    }

    @Test
    void leavingTheSystemClearsTheGuardSoTheSetStaysBounded() {
        assertTrue(subscriber.isFirstAnnouncementForBody(SYSTEM, 0L));
        assertTrue(subscriber.isFirstAnnouncementForBody(OTHER_SYSTEM, 0L), "same body id, different system");
        assertTrue(subscriber.isFirstAnnouncementForBody(SYSTEM, 0L), "returning re-arms the guard");
    }

    /**
     * BodyID is absent often enough in this journal API that folding those onto one shared key would
     * silence every unidentified body after the first. Announcing twice beats never announcing.
     */
    @Test
    void aBodyWithNoIdAlwaysAnnounces() {
        assertTrue(subscriber.isFirstAnnouncementForBody(SYSTEM, null));
        assertTrue(subscriber.isFirstAnnouncementForBody(SYSTEM, null));
    }

    @Test
    void aBodyWithNoIdDoesNotSuppressIdentifiedBodies() {
        assertTrue(subscriber.isFirstAnnouncementForBody(SYSTEM, null));
        assertTrue(subscriber.isFirstAnnouncementForBody(SYSTEM, 0L));
        assertFalse(subscriber.isFirstAnnouncementForBody(SYSTEM, 0L));
    }
}
