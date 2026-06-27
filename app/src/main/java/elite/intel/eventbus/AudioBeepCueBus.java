package elite.intel.eventbus;

import com.google.common.eventbus.AsyncEventBus;

import java.util.concurrent.Executors;

/**
 * Separate async event bus for fire-and-forget audio cues (notification beeps).
 * <p>
 * Uses Guava AsyncEventBus backed by a single daemon thread. Beeps must be
 * delivered on a thread of their own rather than on the synchronous
 * {@link GameEventBus}: long-running searches (spansh / EDSM) emit progress
 * beeps from inside a {@code GameEventBus} dispatch, and Guava defers any event
 * re-posted onto a bus that is already dispatching on the current thread until
 * that dispatch unwinds. Routing cues here guarantees they play immediately,
 * never blocking or being queued behind the in-flight game/UI event.
 */
public class AudioBeepCueBus {

    private static final AsyncEventBus bus = new AsyncEventBus(
            "audio-cue",
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "Audio-Cue-Bus");
                t.setDaemon(true);
                return t;
            })
    );

    public static void publish(Object event) {
        bus.post(event);
    }

    public static void register(Object subscriber) {
        bus.register(subscriber);
    }

    public static void unregister(Object subscriber) {
        bus.unregister(subscriber);
    }
}
