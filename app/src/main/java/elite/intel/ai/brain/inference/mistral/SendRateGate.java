package elite.intel.ai.brain.inference.mistral;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Holds sends to a provider apart by a fixed minimum interval. Mistral's free tier allows one request per second
 * and the companion trips that without sending anything unreasonable: a commander turn can put a companion round
 * and a query analysis back to back, and the event lane narrates alongside the commander lane, so two short calls
 * land inside one second and the second is refused with a 429.
 * <p>
 * Spacing is measured from send to send - the same thing a rate limiter counts - so a slow exchange never pays
 * twice, and it is taken from a monotonic clock so a wall-clock correction cannot stall or open the gate. Every
 * wait is interruptible: a cancelled thought leaves immediately, and leaves the slot for whoever is still waiting.
 */
final class SendRateGate {

    private final long minIntervalNanos;

    /**
     * Fair, so a queued send keeps its place rather than being overtaken by a later one.
     */
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition slotFree = lock.newCondition();

    /**
     * Monotonic instant the next send may start. Guarded by {@link #lock}.
     */
    private long nextSendAtNanos = System.nanoTime();

    SendRateGate(long minInterval, TimeUnit unit) {
        this.minIntervalNanos = unit.toNanos(minInterval);
        if (minIntervalNanos < 0) {
            throw new IllegalArgumentException("minInterval must not be negative");
        }
    }

    /**
     * Blocks until the next send is allowed, then claims that slot.
     *
     * @return true when the slot is claimed and the caller must send; false when the wait was interrupted, in
     * which case the caller must abandon the send rather than let a cancelled turn spend the slot.
     */
    boolean awaitSlot() {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException cancelledBeforeQueueing) {
            Thread.currentThread().interrupt();
            return false;
        }
        try {
            long remainingNanos = nextSendAtNanos - System.nanoTime();
            while (remainingNanos > 0) {
                remainingNanos = slotFree.awaitNanos(remainingNanos);
            }
            nextSendAtNanos = System.nanoTime() + minIntervalNanos;
            return true;
        } catch (InterruptedException cancelledWhileWaiting) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            lock.unlock();
        }
    }
}
