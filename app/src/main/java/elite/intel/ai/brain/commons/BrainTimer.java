package elite.intel.ai.brain.commons;

import org.apache.logging.log4j.Logger;

/**
 * Per-thread stopwatch that measures how long a single user command spends inside
 * the brain package - from the moment a {@code UserInputEvent} enters a command
 * endpoint until {@link ResponseRouter} resolves the action returned by the LLM.
 * <p>
 * Command endpoints submit user input to a single-thread executor and then invoke
 * {@code ResponseRouter.processAiResponse(...)} synchronously on that same executor
 * thread. The entry timestamp is therefore recorded on that thread via a
 * {@link ThreadLocal}, so it travels the whole call chain without having to be
 * threaded through method signatures.
 * <p>
 * Only user-originated commands are timed; sensor-driven LLM calls are ignored by
 * the caller (see {@link ResponseRouter}).
 */
public final class BrainTimer {

    private static final ThreadLocal<Long> START_NANOS = new ThreadLocal<>();

    private BrainTimer() {
    }

    /**
     * Records the entry timestamp (in nanoseconds) for the command currently being
     * processed on this thread. Re-invoking overwrites any previous value, so a
     * command that never reached the router cannot leave a stale start behind.
     */
    public static void start(long entryNanos) {
        START_NANOS.set(entryNanos);
    }

    /**
     * Logs, at INFO level, the elapsed time since {@link #start(long)} and clears the
     * timer for this thread. No-op when no start was recorded on this thread.
     *
     * @param log    logger of the calling class
     * @param action the action resolved by the LLM (used as a label)
     */
    public static void stopAndLog(Logger log, String action) {
        Long start = START_NANOS.get();
        if (start == null) return;
        START_NANOS.remove();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        String label = (action == null || action.isEmpty()) ? "chat/no-action" : action;
        log.info("Brain processing time: {} ms (action: {})", elapsedMs, label);
    }
}