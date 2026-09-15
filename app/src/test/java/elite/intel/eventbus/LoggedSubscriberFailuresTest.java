package elite.intel.eventbus;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A subscriber that throws must leave a line in OUR log, naming the subscriber, the method and the event.
 * Guava's default handler wrote to java.util.logging, which never reached the app log - see the class.
 */
class LoggedSubscriberFailuresTest {

    private final List<String> messages = new ArrayList<>();
    private final List<Throwable> causes = new ArrayList<>();
    private final EventBus bus = new EventBus(new LoggedSubscriberFailures("test", (message, cause) -> {
        messages.add(message);
        causes.add(cause);
    }));

    @Test
    void aThrowingSubscriberIsNamedInTheLogWithItsEventAndCause() {
        bus.register(new BrokenSubscriber());

        bus.post(new ManifestArrived());

        assertEquals(1, messages.size(), messages.toString());
        String line = messages.getFirst();
        assertTrue(line.contains("test bus"), line);
        assertTrue(line.contains("BrokenSubscriber.onManifest"), line);
        assertTrue(line.contains("ManifestArrived"), line);
        assertInstanceOf(IllegalStateException.class, causes.getFirst());
        assertEquals("database is locked", causes.getFirst().getMessage());
    }

    @Test
    void theOtherSubscribersStillReceiveTheEvent() {
        HealthySubscriber healthy = new HealthySubscriber();
        bus.register(new BrokenSubscriber());
        bus.register(healthy);

        bus.post(new ManifestArrived());

        assertEquals(1, healthy.received);
        assertEquals(1, messages.size());
    }

    private static final class ManifestArrived {
    }

    private static final class BrokenSubscriber {
        @Subscribe
        public void onManifest(ManifestArrived event) {
            throw new IllegalStateException("database is locked");
        }
    }

    private static final class HealthySubscriber {
        int received;

        @Subscribe
        public void onManifest(ManifestArrived event) {
            received++;
        }
    }
}
