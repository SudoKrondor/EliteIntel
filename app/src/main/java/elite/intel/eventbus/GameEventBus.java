package elite.intel.eventbus;

import com.google.common.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The GameEventBus class provides a centralized communication mechanism for publishing events
 * and managing subscribers using an underlying EventBus.
 * It acts as a utility for dispatching events across different components of an application.
 */
public class GameEventBus {
    // WHY: subscribers run on the publishing thread, but Guava queues reentrant posts until the current event
    // finishes. Some seams rely on same-thread dispatch; switching to AsyncEventBus would silently break those.
    private static final EventBus bus = new EventBus();
    private static final ThreadLocal<DispatchContext> dispatchContext =
            ThreadLocal.withInitial(DispatchContext::new);

    public static void publish(Object event) {
        DispatchContext context = dispatchContext.get();
        boolean outermost = context.depth == 0;
        context.depth++;
        try {
            bus.post(event);
        } finally {
            context.depth--;
            if (outermost) {
                List<Runnable> callbacks = List.copyOf(context.afterDispatch);
                context.afterDispatch.clear();
                dispatchContext.remove();
                callbacks.forEach(Runnable::run);
            }
        }
    }

    /**
     * Runs {@code callback} after the current outermost EventBus post has drained all reentrant events.
     * Outside an EventBus publication it runs immediately.
     * <p>
     * Guava's same-thread dispatcher deliberately queues a post made from a subscriber. Code that must inspect
     * the result of that nested event therefore cannot do so immediately after {@link #publish(Object)} returns
     * from inside the subscriber; this hook establishes the actual post-dispatch boundary.
     */
    public static void afterCurrentDispatch(Runnable callback) {
        Objects.requireNonNull(callback, "callback");
        DispatchContext context = dispatchContext.get();
        if (context.depth == 0) {
            dispatchContext.remove();
            callback.run();
        } else {
            context.afterDispatch.add(callback);
        }
    }

    public static void register(Object o) {
        bus.register(o);
    }

    public static void unregister(Object o) {
        bus.unregister(o);
    }

    private static final class DispatchContext {
        private int depth;
        private final List<Runnable> afterDispatch = new ArrayList<>();
    }
}
