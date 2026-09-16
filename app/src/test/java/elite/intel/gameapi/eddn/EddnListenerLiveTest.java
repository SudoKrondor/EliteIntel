package elite.intel.gameapi.eddn;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The listener against the real relay: connects, inflates, and sees the schema this app lives on.
 * <p>
 * Tagged out of the default run because it needs the network and the relay's traffic, which is
 * other commanders playing. It writes nothing - the handler counts - so it is safe against the
 * commander's own database, which is what the local-integration task points at.
 * <p>
 * Run with {@code ./gradlew localIntegrationTest --tests '*EddnListenerLiveTest'}.
 */
@Tag("local-integration")
class EddnListenerLiveTest {

    private static final long LISTEN_MS = 60_000;
    private static final String SCHEMA_FSS_SIGNALS = "https://eddn.edcd.io/schemas/fsssignaldiscovered/1";

    @Test
    void theRelayDeliversSweepsWithResourceSitesAndConflictZones() throws InterruptedException {
        AtomicInteger sweeps = new AtomicInteger();
        AtomicReference<String> resourceSite = new AtomicReference<>();
        AtomicReference<String> conflictZone = new AtomicReference<>();

        EddnListener listener = new EddnListener(envelope -> {
            if (!envelope.contains(SCHEMA_FSS_SIGNALS)) return false;
            sweeps.incrementAndGet();
            if (envelope.contains("$MULTIPLAYER_SCENARIO")) resourceSite.compareAndSet(null, envelope);
            if (envelope.contains("$Warzone_")) conflictZone.compareAndSet(null, envelope);
            return true;
        });

        long started = System.currentTimeMillis();
        listener.start();
        while (System.currentTimeMillis() - started < LISTEN_MS && (resourceSite.get() == null || conflictZone.get() == null)) {
            Thread.sleep(500);
        }
        listener.stop();

        assertTrue(listener.received() > 0, "nothing came off the relay - is the network up?");
        assertTrue(sweeps.get() > 0, "no FSS sweep in a minute is not the relay at any hour");
        assertNotNull(resourceSite.get(), "a minute of the relay carries several resource-site sweeps");
        assertNotNull(conflictZone.get(), "and several conflict-zone sweeps");
    }
}
