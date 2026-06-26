package elite.intel.companion.mind;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An execution lane for one thought source: a bounded pool of worker threads draining one deque, so up to
 * {@code concurrency} thoughts of this source are live at a time (the others wait in the deque). Normal
 * thoughts queue at the tail and urgent thoughts jump to the head (§1.7.28/§1.7.29); every live thought is
 * tracked so it can be interrupted for preemption, the watchdog, or shutdown.
 * <p>
 * A single-worker lane ({@code concurrency == 1}) keeps the original "one live thought" behaviour for the
 * memory-only EVENT and the short NARRATION lanes; the COMMANDER lane runs several workers so a long
 * synchronous command/query (whose slow part is the handler, not the LLM round) does not block new commander
 * input - other commander thoughts run on the free workers meanwhile.
 * <p>
 * Shutdown is graceful: one poison pill per worker lets the workers finish their live thoughts and drain the
 * queue before they exit; only workers that do not finish within the join window are forced to interrupt.
 */
final class ThoughtLane {

    private static final Logger log = LogManager.getLogger(ThoughtLane.class);

    /** Sentinel that ends a worker loop; one is offered per worker so queued thoughts drain before them. */
    private static final Runnable POISON = () -> {};

    private final BlockingDeque<Runnable> queue = new LinkedBlockingDeque<>();
    private final List<Thread> workers = new ArrayList<>();
    /** Currently live (running) thoughts mapped to their start time (epoch millis); read by the watchdog. */
    private final Map<Thought, Long> live = new ConcurrentHashMap<>();
    /**
     * Submitted-but-not-finished thoughts (queued plus live). Incremented synchronously at submit, before a
     * worker can dequeue, and decremented only after the thought fully completes - so {@link #isIdle()} never
     * reports a transient idle in the {@code take()} -> running window.
     */
    private final AtomicInteger pending = new AtomicInteger();

    ThoughtLane(String name, int concurrency) {
        int workerCount = Math.max(1, concurrency);
        for (int i = 0; i < workerCount; i++) {
            Thread worker = new Thread(this::drain, workerCount == 1 ? name : name + "-" + i);
            worker.setDaemon(true);
            worker.start();
            workers.add(worker);
        }
    }

    /** Queues a normal thought at the tail. */
    void submit(Thought thought) {
        pending.incrementAndGet(); // count it busy before a worker can dequeue it
        queue.offerLast(wrap(thought));
    }

    /** Queues an urgent thought at the head, ahead of any already-queued normal thought. */
    void submitFirst(Thought thought) {
        pending.incrementAndGet(); // count it busy before a worker can dequeue it
        queue.offerFirst(wrap(thought));
    }

    /** Interrupts every currently live thought (cooperative; each safe-flushes and dies). */
    void interruptLive() {
        live.keySet().forEach(Thought::interrupt);
    }

    /** Interrupts each live thought running longer than the given duration (watchdog check, §2.3). */
    void interruptStuck(long millis) {
        long now = System.currentTimeMillis();
        live.forEach((thought, start) -> {
            if (now - start > millis) {
                thought.interrupt();
            }
        });
    }

    /** Whether the lane has no submitted work left (queued or live) - a race-free turn-boundary signal. */
    boolean isIdle() {
        return pending.get() == 0;
    }

    /** Graceful stop: drain queued thoughts and finish the live ones, forcing interrupt only if a worker hangs. */
    void shutdown(long timeoutMillis) {
        workers.forEach(worker -> queue.offerLast(POISON));
        joinAll(timeoutMillis);
        if (workers.stream().anyMatch(Thread::isAlive)) {
            interruptLive(); // some live thought is stuck (e.g. slow LLM): make it safe-flush and die
            workers.forEach(Thread::interrupt);
            joinAll(timeoutMillis);
        }
    }

    private Runnable wrap(Thought thought) {
        return () -> {
            long startMillis = System.currentTimeMillis();
            live.put(thought, startMillis);
            log.info("Lane {}: {} ({}) thought started", Thread.currentThread().getName(), thought.source(), thought.urgency());
            try {
                thought.run();
            } finally {
                live.remove(thought);
                pending.decrementAndGet(); // mark idle only after the thought has fully finished
                log.info("Lane {}: {} thought finished in {} ms",
                        Thread.currentThread().getName(), thought.source(), System.currentTimeMillis() - startMillis);
            }
        };
    }

    private void drain() {
        while (true) {
            Runnable task;
            try {
                task = queue.take();
            } catch (InterruptedException stopping) {
                Thread.currentThread().interrupt();
                return;
            }
            if (task == POISON) {
                return;
            }
            try {
                task.run();
            } catch (Throwable failure) {
                // A failing thought must never kill its worker - that would shrink the lane's capacity.
                log.error("Companion thought failed on lane {}", Thread.currentThread().getName(), failure);
            }
        }
    }

    private void joinAll(long timeoutMillis) {
        for (Thread worker : workers) {
            try {
                worker.join(timeoutMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
