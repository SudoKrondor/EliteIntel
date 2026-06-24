package elite.intel.eventbus;

import com.google.common.eventbus.EventBus;

/**
 * Dedicated synchronous event bus for UI-layer events (elite.intel.ui.event.*).
 * Separates UI signals (settings changes, service state, logging, PTT, etc.)
 * from game journal events on GameEventBus.
 */
public class UiBus {
    private static final EventBus bus = new EventBus("ui");

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
