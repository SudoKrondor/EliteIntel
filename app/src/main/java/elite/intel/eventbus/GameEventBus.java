package elite.intel.eventbus;

import com.google.common.eventbus.EventBus;

/**
 * The GameEventBus class provides a centralized communication mechanism for publishing events
 * and managing subscribers using an underlying EventBus.
 * It acts as a utility for dispatching events across different components of an application.
 */
public class GameEventBus {
    // WHY: a synchronous EventBus - subscribers run on the publishing thread. Some seams rely on this
    // same-thread dispatch, e.g. companion tool-result correlation reads a thread-scoped id
    // (elite.intel.companion.execution.ActiveToolCall) inside a subscriber. Switching to AsyncEventBus would
    // silently break those; audit them first.
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
