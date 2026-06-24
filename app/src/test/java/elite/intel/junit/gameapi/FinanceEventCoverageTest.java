package elite.intel.junit.gameapi;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.FinancePreScanAccumulator;
import elite.intel.gameapi.journal.subscribers.FinanceSubscriber;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Enforces the otherwise-implicit contract that the live credit handler
 * ({@link FinanceSubscriber}) and the startup reconstruction
 * ({@link FinancePreScanAccumulator}) react to the SAME set of journal events.
 * If a new money event is wired into one but not the other, the reconstructed
 * startup balance would silently drift. This test fails instead of drifting.
 */
class FinanceEventCoverageTest {

    @Test
    void liveAndPreScanHandleTheSameEventTypes() {
        Set<Class<?>> live = subscribedEventTypes(FinanceSubscriber.class);
        Set<Class<?>> preScan = subscribedEventTypes(FinancePreScanAccumulator.class);

        assertEquals(live, preScan,
                "FinanceSubscriber and FinancePreScanAccumulator must subscribe to the same event types. "
                        + "When adding a money event, wire it into both.");
    }

    private static Set<Class<?>> subscribedEventTypes(Class<?> subscriber) {
        return Arrays.stream(subscriber.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Subscribe.class))
                .map((Method m) -> m.getParameterTypes()[0])
                .collect(Collectors.toSet());
    }
}
