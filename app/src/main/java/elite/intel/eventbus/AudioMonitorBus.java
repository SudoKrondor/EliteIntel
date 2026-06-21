package elite.intel.eventbus;

import com.google.common.eventbus.AsyncEventBus;

import java.util.concurrent.Executors;

/**
 * Separate async event bus for real-time audio monitoring.
 * <p>
 * Uses Guava AsyncEventBus backed by a single daemon thread so that publishing
 * from Parakeet's hot capture loop never blocks the main GameEventBus
 * (which is synchronous on the caller's thread).
 */
public class AudioMonitorBus {

    private static final AsyncEventBus bus = new AsyncEventBus(
            "audio-monitor",
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "Audio-Monitor-Bus");
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
