package elite.intel.ai.brain.vega.mind;

import elite.intel.ai.brain.vega.diag.VegaDiagnostics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An execution lane for one thought source: worker threads drain one deque in admission order. Normal thoughts
 * queue at the tail and urgent thoughts jump to the head (§1.7.28/§1.7.29). A thought may return a detached
 * lifecycle completion; the worker then accepts the next thought while the detached one remains live and tracked
 * for preemption, watchdog, shutdown, pending accounting, and {@link #isIdle()}.
 * <p>
 * COMMANDER uses one worker so prompt selection and dialogue/query memory publication remain ordered; game handlers
 * detach after that stage. EVENT also uses one worker. The generic concurrency argument remains for lanes whose
 * cognitive stages are safe to parallelize.
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

    /** Interrupts every currently live thought (cooperative; incomplete turns publish no memory and then die). */
    void interruptLive() {
        live.keySet().forEach(Thought::interrupt);
    }

    /** Interrupts each live thought running longer than the given duration (watchdog check, §2.3). */
    void interruptStuck(long millis) {
        long now = System.currentTimeMillis();
        live.forEach((thought, start) -> {
            if (now - start > millis) {
                VegaDiagnostics.debug(thought.trace(), "watchdog",
                        "force-interrupt after " + (now - start) + " ms (>" + millis + ")");
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
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        workers.forEach(worker -> queue.offerLast(POISON));
        joinAllUntil(deadlineNanos);
        if (workers.stream().anyMatch(Thread::isAlive) || !live.isEmpty()) {
            // A detached handler keeps its thought live after the lane worker has accepted the next turn. Stop owns
            // that lifecycle too: cancel its result before the old runtime graph can speak or write into a restart.
            interruptLive();
            workers.forEach(Thread::interrupt);
            joinAllUntil(deadlineNanos);
            awaitPendingUntil(deadlineNanos);
        }
    }

    /**
     * Drops the queued thoughts that cannot run without the model, after a thought ended with the model not answering.
     * WHY: the one ahead already told the commander the AI service did not answer and to try again. A Windows tester's
     * question queued behind a timed-out turn waited 43 s, then was sent to the same model and failed again
     * (2026-11-10). A thought that needs no model - a reflex - still runs. A dropped thought gives back the
     * clarification it claimed ({@link Thought#dropped()}).
     */
    private void dropQueuedThoughtsNeedingTheModel() {
        for (Runnable task : queue.toArray(new Runnable[0])) {
            // WHY remove() decides: a worker may take the same task in between, and only one of the two may own it.
            if (task instanceof QueuedThought queued && queued.thought.needsTheModel() && queue.remove(task)) {
                pending.decrementAndGet();
                queued.thought.dropped();
                VegaDiagnostics.info(queued.thought.trace(), "drop",
                        "queued behind a turn the AI service did not answer; dropped");
            }
        }
    }

    /**
     * A queued thought and the task that runs it. WHY a class of its own: the queue holds tasks, and a drop needs to
     * know which thought each one runs. Identity equality, so {@link BlockingDeque#remove(Object)} takes this task.
     */
    private static final class QueuedThought implements Runnable {
        private final Thought thought;
        private final Runnable task;

        private QueuedThought(Thought thought, Runnable task) {
            this.thought = thought;
            this.task = task;
        }

        @Override
        public void run() {
            task.run();
        }
    }

    private Runnable wrap(Thought thought) {
        return new QueuedThought(thought, () -> {
            long startMillis = System.currentTimeMillis();
            long startNanos = System.nanoTime();
            String laneThread = Thread.currentThread().getName();
            live.put(thought, startMillis);
            log.info("Lane {}: {} ({}) thought started", laneThread, thought.source(), thought.urgency());
            VegaDiagnostics.debug(thought.trace(), "start", thought.urgency() + " on " + laneThread);
            // Bind this thought's trace to the lane thread so leaf components it calls (reducer, memory recall)
            // tag their own diagnostic lines with it (see VegaDiagnostics.enterThought).
            VegaDiagnostics.enterThought(thought.trace());
            CompletableFuture<Void> completion;
            try {
                completion = thought.startLifecycle();
            } finally {
                VegaDiagnostics.exitThought();
            }
            // WHY here: this worker takes the next queued thought as soon as this returns, so the drop must come first.
            if (thought.endedWithoutAnAnswer()) {
                dropQueuedThoughtsNeedingTheModel();
            }
            completion.whenComplete((ignored, failure) ->
                    finishThought(thought, laneThread, startNanos, failure));
        });
    }

    /** Completes lane ownership when both the cognitive stage and any detached handler work have settled. */
    private void finishThought(Thought thought, String laneThread, long startNanos, Throwable failure) {
        live.remove(thought);
        pending.decrementAndGet();
        long runMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        if (failure != null) {
            log.error("VEGA thought failed on lane {}", laneThread, failure);
        }
        log.info("Lane {}: {} thought finished in {} ms", laneThread, thought.source(), runMillis);
        VegaDiagnostics.debug(thought.trace(), "done",
                "run=" + runMillis + " ms turn=" + thought.elapsedSinceAcceptanceMillis() + " ms");
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
                log.error("VEGA thought failed on lane {}", Thread.currentThread().getName(), failure);
            }
        }
    }

    private void joinAllUntil(long deadlineNanos) {
        for (Thread worker : workers) {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                return;
            }
            try {
                long millis = TimeUnit.NANOSECONDS.toMillis(remainingNanos);
                int nanos = (int) (remainingNanos - TimeUnit.MILLISECONDS.toNanos(millis));
                worker.join(millis, nanos);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void awaitPendingUntil(long deadlineNanos) {
        while (pending.get() > 0 && System.nanoTime() < deadlineNanos) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
