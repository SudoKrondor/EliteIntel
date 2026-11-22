package elite.intel.ui.screen.bindings;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One refresh at a time, and none asked for lost: a request made while a refresh runs is remembered, and one more
 * refresh follows when it finishes - however many requests came in meanwhile.
 * <p>
 * <strong>WHY not simply drop a request while busy</strong> (C5 review, 2026-10-11, Alan). The Alias Designer screen
 * did, and once the device-file monitor polled every two seconds a dropped request stopped being rare: a poll lands
 * mid-APPLY and starts a refresh, APPLY's own refresh is dropped, and the screen keeps showing what APPLY already
 * changed. A request that only costs one extra refresh - a ship-profile change, say - is the cheap side of that trade.
 */
final class RefreshCoalescer {

    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean again = new AtomicBoolean();

    /**
     * @return true when the caller should start a refresh now; false when one is already running, in which case
     * {@link #finish()} will say to run again
     */
    boolean tryStart() {
        if (running.compareAndSet(false, true)) return true;
        again.set(true);
        return false;
    }

    /**
     * Marks the running refresh done.
     * <p>
     * WHY in this order: {@code running} is cleared first, so a request arriving from here on starts a refresh of its
     * own, and one that arrived before is still in {@code again}. Clearing {@code again} first would leave a window
     * where a request is neither run nor remembered. The cost of this order is, at worst, one refresh more than needed.
     *
     * @return true when requests came in while it ran, and the caller should refresh again
     */
    boolean finish() {
        running.set(false);
        return again.getAndSet(false);
    }
}
