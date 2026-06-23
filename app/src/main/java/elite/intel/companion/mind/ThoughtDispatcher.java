package elite.intel.companion.mind;

import elite.intel.companion.input.EventTopicMap;
import elite.intel.companion.model.Urgency;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.ui.controller.ManagedService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * The accounting/scheduling node of the consciousness. Owns one serialized lane per source so at most
 * one COMMANDER and one EVENT thought are live at a time (they may run concurrently); a lane's queue is
 * the source's thought queue. It does not interpret meaning and does not know a thought's internal state
 * (see COMPANION_ARCHITECTURE.md §2.3).
 * <p>
 * Urgency is assigned here at thought birth. Urgency-driven preemption (an urgent thought jumping its
 * queue head and interrupting live thoughts) and the urgent-phrase / urgent-event-type matchers depend
 * on the {@code Thought.interrupt} machinery and land in a later phase; for now every thought is
 * {@link Urgency#NORMAL} and lanes are plain FIFO.
 */
public final class ThoughtDispatcher implements ManagedService {

    /** Bounded grace period for in-flight thoughts to finish on stop before lanes are force-cancelled. */
    private static final long SHUTDOWN_WAIT_SECONDS = 5;

    private final ThoughtContext ctx;

    private volatile ExecutorService commanderLane;
    private volatile ExecutorService eventLane;

    public ThoughtDispatcher(ThoughtContext ctx) {
        this.ctx = ctx;
    }

    /** Accepts a commander reply, creates a COMMANDER thought, and queues it on the commander lane. */
    public void submitCommanderInput(String input) {
        if (input == null || input.isBlank()) {
            return;
        }
        run(commanderLane, Thought.commander(Urgency.NORMAL, input, ctx));
    }

    /** Accepts a filtered game event, creates an EVENT thought, and queues it on the event lane. */
    public void submitEvent(BaseEvent event) {
        if (event == null) {
            return;
        }
        run(eventLane, Thought.event(Urgency.NORMAL, summarize(event), EventTopicMap.topicFor(event), ctx));
    }

    @Override
    public void start() {
        if (commanderLane == null) {
            commanderLane = Executors.newSingleThreadExecutor(daemon("companion-commander"));
        }
        if (eventLane == null) {
            eventLane = Executors.newSingleThreadExecutor(daemon("companion-event"));
        }
    }

    @Override
    public void stop() {
        shutdown(commanderLane);
        shutdown(eventLane);
        commanderLane = null;
        eventLane = null;
    }

    /** Queues the thought on its lane; a not-running or just-shut-down lane silently drops it. */
    private static void run(ExecutorService lane, Thought thought) {
        if (lane == null) {
            return; // subsystem not running (input racing lifecycle)
        }
        try {
            lane.execute(thought::run);
        } catch (RejectedExecutionException shuttingDown) {
            // Lane shut down between the null-check and submit; drop the thought (watchdog/diagnostics is a later phase).
        }
    }

    /** The current input text for an EVENT thought is the event's own JSON (non-mutating, unlike toYaml). */
    private static String summarize(BaseEvent event) {
        return event.toJson();
    }

    private static void shutdown(ExecutorService lane) {
        if (lane == null) {
            return;
        }
        lane.shutdown();
        try {
            if (!lane.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                lane.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            lane.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static ThreadFactory daemon(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }
}
