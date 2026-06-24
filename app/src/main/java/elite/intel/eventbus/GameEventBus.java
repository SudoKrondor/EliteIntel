package elite.intel.eventbus;

import com.google.common.eventbus.EventBus;

/**
 * The GameEventBus class provides a centralized communication mechanism for publishing events
 * and managing subscribers using an underlying EventBus.
 * It acts as a utility for dispatching events across different components of an application.
 */
public class GameEventBus {
    private static final EventBus bus = new EventBus();

    public static void publish(Object event) {
        bus.post(event);
    }

    public static void register(Object o) {
        bus.register(o);
    }

    public static void unregister(Object o) {
        bus.unregister(o);
    }
}
