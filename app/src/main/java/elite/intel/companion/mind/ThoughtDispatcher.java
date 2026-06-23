package elite.intel.companion.mind;

import elite.intel.companion.input.EventTopicMap;
import elite.intel.companion.model.Urgency;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.ui.controller.ManagedService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The accounting/scheduling node of the consciousness. Owns one serialized {@link ThoughtLane} per source
 * so at most one COMMANDER and one EVENT thought are live at a time (they may run concurrently); a lane's
 * deque is the source's thought queue. It assigns urgency at thought birth and drives preemption, but does
 * not interpret meaning or know a thought's internal state (§2.3).
 * <p>
 * Urgency (§1.1.4/§1.7.29): a normal thought queues at its lane's tail; an urgent thought interrupts both
 * live thoughts (regardless of its origin) and jumps to its lane's head. The urgent-phrase /
 * urgent-event-type matchers are a tunable concern (§7.1); by the default policy nothing is urgent yet.
 * <p>
 * A watchdog periodically force-interrupts a thought that overruns the timeout (§2.3). Barge-in reaches
 * the live thoughts via {@link #interruptLiveThoughts()}.
 */
public final class ThoughtDispatcher implements ManagedService {

    private static final Logger log = LogManager.getLogger(ThoughtDispatcher.class);

    /** Grace period for a lane to drain on stop before its live thought is force-interrupted. */
    private static final long SHUTDOWN_WAIT_MILLIS = 5000;
    /** A thought running longer than this is force-interrupted by the watchdog (§2.3 / §7.2 setting). */
    private static final long WATCHDOG_TIMEOUT_MILLIS = 60_000;
    /** How often the watchdog checks the live thoughts. */
    private static final long WATCHDOG_INTERVAL_MILLIS = 5_000;

    private final ThoughtContext ctx;
    private final UrgencyPolicy urgencyPolicy;
    private final long watchdogTimeoutMillis;
    private final long watchdogIntervalMillis;

    private volatile ThoughtLane commanderLane;
    private volatile ThoughtLane eventLane;
    private volatile ScheduledExecutorService watchdog;

    public ThoughtDispatcher(ThoughtContext ctx) {
        this(ctx, UrgencyPolicy.normalOnly(), WATCHDOG_TIMEOUT_MILLIS, WATCHDOG_INTERVAL_MILLIS);
    }

    /** Test seam: inject the urgency policy to exercise preemption. */
    ThoughtDispatcher(ThoughtContext ctx, UrgencyPolicy urgencyPolicy) {
        this(ctx, urgencyPolicy, WATCHDOG_TIMEOUT_MILLIS, WATCHDOG_INTERVAL_MILLIS);
    }

    /** Test seam: inject the urgency policy and watchdog timing. */
    ThoughtDispatcher(ThoughtContext ctx, UrgencyPolicy urgencyPolicy,
                      long watchdogTimeoutMillis, long watchdogIntervalMillis) {
        this.ctx = ctx;
        this.urgencyPolicy = urgencyPolicy;
        this.watchdogTimeoutMillis = watchdogTimeoutMillis;
        this.watchdogIntervalMillis = watchdogIntervalMillis;
    }

    /** Accepts a commander reply, creates a COMMANDER thought, and queues it on the commander lane. */
    public void submitCommanderInput(String input) {
        if (input == null || input.isBlank()) {
            return;
        }
        Urgency urgency = urgencyPolicy.forCommander(input);
        enqueue(commanderLane, Thought.commander(urgency, input, ctx), urgency);
    }

    /** Accepts a filtered game event, creates an EVENT thought, and queues it on the event lane. */
    public void submitEvent(BaseEvent event) {
        if (event == null) {
            return;
        }
        Urgency urgency = urgencyPolicy.forEvent(event);
        enqueue(eventLane, Thought.event(urgency, summarize(event), EventTopicMap.topicFor(event), ctx), urgency);
    }

    @Override
    public void start() {
        if (commanderLane == null) {
            commanderLane = new ThoughtLane("companion-commander");
        }
        if (eventLane == null) {
            eventLane = new ThoughtLane("companion-event");
        }
        if (watchdog == null) {
            watchdog = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "companion-watchdog");
                thread.setDaemon(true);
                return thread;
            });
            watchdog.scheduleAtFixedRate(this::checkWatchdog,
                    watchdogIntervalMillis, watchdogIntervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop() {
        ScheduledExecutorService currentWatchdog = watchdog;
        watchdog = null;
        if (currentWatchdog != null) {
            currentWatchdog.shutdownNow();
        }
        ThoughtLane commander = commanderLane;
        ThoughtLane event = eventLane;
        commanderLane = null; // stop accepting new work before draining
        eventLane = null;
        if (commander != null) {
            commander.shutdown(SHUTDOWN_WAIT_MILLIS);
        }
        if (event != null) {
            event.shutdown(SHUTDOWN_WAIT_MILLIS);
        }
    }

    /** Interrupts both live thoughts on barge-in (§2.15); the dispatcher owns the thought lifecycle, not speech. */
    public void interruptLiveThoughts() {
        interruptLive();
    }

    /** Queues a thought; an urgent one interrupts both live thoughts and jumps its queue head (§1.7.29). */
    private void enqueue(ThoughtLane lane, Thought thought, Urgency urgency) {
        if (lane == null) {
            return; // subsystem not running (input racing lifecycle)
        }
        if (urgency == Urgency.URGENT) {
            interruptLive();
            lane.submitFirst(thought);
        } else {
            lane.submit(thought);
        }
    }

    /** Interrupts both live thoughts, regardless of the urgent thought's origin. */
    private void interruptLive() {
        ThoughtLane commander = commanderLane;
        ThoughtLane event = eventLane;
        if (commander != null) {
            commander.interruptLive();
        }
        if (event != null) {
            event.interruptLive();
        }
    }

    /** Watchdog tick: force-interrupt a thought that has been running past the timeout (§2.3). */
    private void checkWatchdog() {
        try {
            interruptIfStuck(commanderLane);
            interruptIfStuck(eventLane);
        } catch (RuntimeException unexpected) {
            // Never let a tick failure cancel the periodic schedule (scheduleAtFixedRate stops on throw).
            log.error("Companion watchdog tick failed", unexpected);
        }
    }

    private void interruptIfStuck(ThoughtLane lane) {
        if (lane != null && lane.liveLongerThan(watchdogTimeoutMillis)) {
            lane.interruptLive();
        }
    }

    /** The current input text for an EVENT thought is the event's own JSON (non-mutating, unlike toYaml). */
    private static String summarize(BaseEvent event) {
        return event.toJson();
    }
}
