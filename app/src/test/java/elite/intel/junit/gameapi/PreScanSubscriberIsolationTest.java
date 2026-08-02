package elite.intel.junit.gameapi;

import elite.intel.gameapi.PreScanOnly;
import elite.intel.gameapi.SubscriberRegistration;
import elite.intel.gameapi.journal.subscribers.CarrierLocationSubscriber;
import elite.intel.gameapi.journal.subscribers.SilentPersistenceSubscriber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pre-scan subscribers belong to the JournalPreScanner private bus alone, but they live in a
 * package the live registration scans, so only their marker keeps them off the live bus. There they
 * do real damage: they mirror the real subscribers' writes while doing deliberately less, and they
 * write synchronously where the real subscribers hand off to a virtual thread, so the narrower write
 * lands first. A carrier arrival then had its "did the carrier move?" comparison answered by the
 * silent write, read it as "no", and left the carrier holding the coordinates of the system it had
 * just left.
 */
class PreScanSubscriberIsolationTest {

    @Test
    @DisplayName("the set registered on the live bus excludes the pre-scan subscriber")
    void preScanSubscriberIsMarkedAndExcludedFromTheLiveBus() {
        Set<Class<?>> live = SubscriberRegistration.liveSubscriberClasses();

        assertTrue(SilentPersistenceSubscriber.class.isAnnotationPresent(PreScanOnly.class),
                "a pre-scan-only subscriber in a scanned package must carry @PreScanOnly");
        assertTrue(live.contains(CarrierLocationSubscriber.class),
                "sanity: the scan has to be finding real subscribers, or the exclusion below proves nothing");
        assertFalse(live.contains(SilentPersistenceSubscriber.class),
                "the pre-scan subscriber must never reach the live bus");
    }
}
