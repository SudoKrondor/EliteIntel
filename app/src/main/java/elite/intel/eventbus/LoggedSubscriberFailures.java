package elite.intel.eventbus;

import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiConsumer;

/**
 * Puts a subscriber that threw into the app log.
 * <p>
 * WHY this exists: Guava's default handler reports a failing subscriber through {@code java.util.logging},
 * which this app never bridges into log4j - so the report went to stderr, and a Windows install has no
 * stderr. A subscriber that threw on every event it was given, session after session, left nothing in a
 * support bundle at all. Measured on a construction-site manifest that had not been refreshed in sixteen
 * dockings: the bundle showed the journal was clean and the code path worked on that data, and nothing
 * else, because the one line that would have named the failure was never written down.
 * <p>
 * Error level, because a subscriber that throws has dropped an event it was registered to handle. The
 * logger is the bus's own, so the line is kept whatever level the subscriber's package is pinned to.
 */
public final class LoggedSubscriberFailures implements SubscriberExceptionHandler {

    private static final Logger log = LogManager.getLogger(LoggedSubscriberFailures.class);

    private final String busName;
    private final BiConsumer<String, Throwable> sink;

    public LoggedSubscriberFailures(String busName) {
        this(busName, log::error);
    }

    /**
     * Seam for tests.
     *
     * @param sink receives the composed message and the subscriber's exception
     */
    LoggedSubscriberFailures(String busName, BiConsumer<String, Throwable> sink) {
        this.busName = busName;
        this.sink = sink;
    }

    @Override
    public void handleException(Throwable exception, SubscriberExceptionContext context) {
        String message = busName + " bus: " + context.getSubscriber().getClass().getSimpleName() + "."
                + context.getSubscriberMethod().getName() + " threw on " + context.getEvent().getClass().getSimpleName()
                + " - the event was dropped by that subscriber";
        sink.accept(message, exception);
    }
}
