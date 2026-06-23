package elite.intel.companion.mind;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A single serialized execution lane for one thought source: a worker thread draining a deque so at most
 * one thought is live at a time. Normal thoughts queue at the tail and urgent thoughts jump to the head
 * (§1.7.28/§1.7.29), and the live thought is exposed so it can be interrupted for preemption or shutdown.
 * <p>
 * Shutdown is graceful: a poison pill queued at the tail lets the worker finish the live thought and drain
 * the queue before it exits; only if the worker does not finish within the join window is the live thought
 * forced to interrupt (cooperative safe-flush).
 */
final class ThoughtLane {

    /** Sentinel that ends the worker loop; offered at the tail so queued thoughts drain before it. */
    private static final Runnable POISON = () -> {};

    private final BlockingDeque<Runnable> queue = new LinkedBlockingDeque<>();
    private final Thread worker;
    private volatile Thought live;
    /** When the live thought started (epoch millis), or 0 when the lane is idle; read by the watchdog. */
    private volatile long liveStartMillis;

    ThoughtLane(String name) {
        worker = new Thread(this::drain, name);
        worker.setDaemon(true);
        worker.start();
    }

    /** Queues a normal thought at the tail. */
    void submit(Thought thought) {
        queue.offerLast(wrap(thought));
    }

    /** Queues an urgent thought at the head, ahead of any already-queued normal thought. */
    void submitFirst(Thought thought) {
        queue.offerFirst(wrap(thought));
    }

    /** Interrupts the currently live thought, if any (cooperative; it safe-flushes and dies). */
    void interruptLive() {
        Thought current = live;
        if (current != null) {
            current.interrupt();
        }
    }

    /** Whether the live thought has been running longer than the given duration (watchdog check, §2.3). */
    boolean liveLongerThan(long millis) {
        long start = liveStartMillis;
        return start != 0 && System.currentTimeMillis() - start > millis;
    }

    /** Graceful stop: drain queued thoughts and finish the live one, forcing interrupt only if it hangs. */
    void shutdown(long timeoutMillis) {
        queue.offerLast(POISON);
        join(timeoutMillis);
        if (worker.isAlive()) {
            interruptLive(); // live thought stuck (e.g. slow LLM): make it safe-flush and die
            worker.interrupt();
            join(timeoutMillis);
        }
    }

    private Runnable wrap(Thought thought) {
        return () -> {
            live = thought;
            liveStartMillis = System.currentTimeMillis();
            try {
                thought.run();
            } finally {
                live = null;
                liveStartMillis = 0;
            }
        };
    }

    private void drain() {
        try {
            Runnable task;
            while ((task = queue.take()) != POISON) {
                task.run();
            }
        } catch (InterruptedException stopping) {
            Thread.currentThread().interrupt();
        }
    }

    private void join(long timeoutMillis) {
        try {
            worker.join(timeoutMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
