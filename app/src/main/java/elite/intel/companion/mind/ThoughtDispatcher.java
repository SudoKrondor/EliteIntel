package elite.intel.companion.mind;

import elite.intel.companion.input.EventInputFormatter;
import elite.intel.companion.input.EventTopicMap;
import elite.intel.companion.input.SensorInputFormatter;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.ui.controller.ManagedService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The accounting/scheduling node of the consciousness. Owns one serialized {@link ThoughtLane} per
 * {@link ThoughtSource}, so at most one COMMANDER, one EVENT, and one NARRATION thought are live at a time
 * (they may run concurrently); a lane's deque is that source's thought queue. It assigns urgency at thought
 * birth and drives preemption, but does not interpret meaning or know a thought's internal state (§2.3).
 * <p>
 * The lanes are held in one source-keyed map, published as a single volatile reference: cross-cutting
 * operations (start/stop, interrupt, watchdog, idle) iterate the lanes, while a submit targets the lane of
 * its source. A separate NARRATION lane keeps the slow LLM narration round off the EVENT lane, so the
 * memory-only event "knowing" channel records without queuing behind it.
 * <p>
 * Urgency (§1.1.4/§1.7.29): a normal thought queues at its lane's tail; an urgent thought interrupts every
 * live thought (regardless of its origin) and jumps to its lane's head. The urgent-phrase /
 * urgent-event-type matchers are a tunable concern (§7.1); by the default policy nothing is urgent yet,
 * except subscriber narration, which is born urgent.
 * <p>
 * A watchdog periodically force-interrupts a thought that overruns the timeout (§2.3). Barge-in reaches
 * the live thoughts via {@link #interruptLiveThoughts()}.
 */
public final class ThoughtDispatcher implements ManagedService {

    private static final Logger log = LogManager.getLogger(ThoughtDispatcher.class);

    /**
     * Max commander thoughts live at once: a long synchronous command/query occupies a worker, so several
     * lets new commander input run meanwhile instead of blocking; the rest queue (§1.2). EVENT/NARRATION
     * stay single-worker (no slow handlers there).
     */
    private static final int MAX_LIVE_COMMANDER_THOUGHTS = 5;
    /** Grace period for a lane to drain on stop before its live thoughts are force-interrupted. */
    private static final long SHUTDOWN_WAIT_MILLIS = 5000;
    /** A thought running longer than this is force-interrupted by the watchdog (§2.3 / §7.2 setting). */
    private static final long WATCHDOG_TIMEOUT_MILLIS = 60_000;
    /** How often the watchdog checks the live thoughts. */
    private static final long WATCHDOG_INTERVAL_MILLIS = 5_000;

    private final ThoughtContext ctx;
    private final UrgencyPolicy urgencyPolicy;
    private final long watchdogTimeoutMillis;
    private final long watchdogIntervalMillis;

    /** One serialized lane per source; null until {@link #start()}, published as a single volatile reference. */
    private volatile Map<ThoughtSource, ThoughtLane> lanes;
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
        enqueue(ThoughtSource.COMMANDER, Thought.commander(urgency, input, ctx), urgency);
    }

    /**
     * Accepts a filtered game event, creates an EVENT thought, and queues it on the event lane. The event's
     * {@link BaseEvent#importance()} is forwarded to the thought (a forwarded property, not an interpretation
     * of meaning): the EVENT thought is memory-only - a {@code HIGH} event is recorded, a {@code NORMAL} one
     * is dropped, and the LLM is never engaged (see {@code EventThought}).
     */
    public void submitEvent(BaseEvent event) {
        if (event == null) {
            return;
        }
        Urgency urgency = urgencyPolicy.forEvent(event);
        enqueue(ThoughtSource.EVENT,
                Thought.event(urgency, summarize(event), EventTopicMap.topicFor(event), event.importance(), ctx),
                urgency);
    }

    /**
     * Accepts subscriber-prepared sensor narration, creates a NARRATION thought, and queues it on the
     * narration lane. SensorDataEvent is trusted output from gameplay subscribers: they already applied
     * settings, filtering, and calculations, so the thought is born as urgent narration under the topic
     * provided by that layer.
     */
    public void submitSensorData(SensorDataEvent event) {
        if (event == null) {
            return;
        }
        Urgency urgency = Urgency.URGENT;
        enqueue(ThoughtSource.NARRATION,
                Thought.sensorNarration(urgency, SensorInputFormatter.format(event), sensorTopic(event), ctx),
                urgency);
    }

    /**
     * Accepts a curated announcement that already carries finished text (mining/discovery/route/radar/
     * navigation), creates a verbatim NARRATION thought, and queues it on the narration lane. The line is
     * remembered and voiced verbatim in the companion's voice - no LLM phrasing.
     */
    public void submitVerbatimNarration(String text, ConversationTopic topic) {
        if (text == null || text.isBlank()) {
            return;
        }
        Urgency urgency = Urgency.URGENT;
        enqueue(ThoughtSource.NARRATION, Thought.verbatimNarration(urgency, text, topic, ctx), urgency);
    }

    @Override
    public void start() {
        if (lanes == null) {
            Map<ThoughtSource, ThoughtLane> built = new EnumMap<>(ThoughtSource.class);
            built.put(ThoughtSource.COMMANDER, new ThoughtLane("companion-commander", MAX_LIVE_COMMANDER_THOUGHTS));
            built.put(ThoughtSource.EVENT, new ThoughtLane("companion-event", 1));
            built.put(ThoughtSource.NARRATION, new ThoughtLane("companion-narration", 1));
            lanes = built; // single volatile publish of the fully-built lane set
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
        Map<ThoughtSource, ThoughtLane> current = lanes;
        lanes = null; // stop accepting new work before draining
        if (current != null) {
            current.values().forEach(lane -> lane.shutdown(SHUTDOWN_WAIT_MILLIS));
        }
    }

    /** Interrupts every live thought on barge-in (§2.15); the dispatcher owns the thought lifecycle, not speech. */
    public void interruptLiveThoughts() {
        interruptLive();
    }

    /** Whether all lanes are idle (no live thought, empty queues) - a turn-boundary signal for harnesses. */
    public boolean isIdle() {
        Map<ThoughtSource, ThoughtLane> snapshot = lanes;
        return snapshot == null || snapshot.values().stream().allMatch(ThoughtLane::isIdle);
    }

    /** Queues a thought on its source's lane; an urgent one interrupts every live thought and jumps its head. */
    private void enqueue(ThoughtSource source, Thought thought, Urgency urgency) {
        Map<ThoughtSource, ThoughtLane> snapshot = lanes;
        ThoughtLane lane = snapshot == null ? null : snapshot.get(source);
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

    /** Interrupts every live thought, regardless of the urgent thought's origin. */
    private void interruptLive() {
        Map<ThoughtSource, ThoughtLane> snapshot = lanes;
        if (snapshot != null) {
            snapshot.values().forEach(ThoughtLane::interruptLive);
        }
    }

    /** Watchdog tick: force-interrupt any thought that has been running past the timeout (§2.3). */
    private void checkWatchdog() {
        try {
            Map<ThoughtSource, ThoughtLane> snapshot = lanes;
            if (snapshot != null) {
                snapshot.values().forEach(this::interruptIfStuck);
            }
        } catch (RuntimeException unexpected) {
            // Never let a tick failure cancel the periodic schedule (scheduleAtFixedRate stops on throw).
            log.error("Companion watchdog tick failed", unexpected);
        }
    }

    private void interruptIfStuck(ThoughtLane lane) {
        lane.interruptStuck(watchdogTimeoutMillis);
    }

    /** The current input text for an EVENT thought is the shared prompt/memory event envelope. */
    private static String summarize(BaseEvent event) {
        return EventInputFormatter.format(event);
    }

    private static ConversationTopic sensorTopic(SensorDataEvent event) {
        try {
            return ConversationTopic.valueOf(event.getTopic().trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException invalidTopic) {
            return ConversationTopic.SYSTEM;
        }
    }
}
