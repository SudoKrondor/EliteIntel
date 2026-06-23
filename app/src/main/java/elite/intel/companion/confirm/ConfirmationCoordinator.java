package elite.intel.companion.confirm;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The rendezvous between a {@code Thought} that froze a dangerous action and the commander's confirmation
 * delivered on the confirmation bus (§2.13). At most one confirmation may be pending at a time - no
 * overlapping dangerous confirmations (§1.6.25) - so the {@code ThoughtDispatcher} never needs to know a
 * thought's {@code awaiting_confirmation} state (§1.6.26): the waiting thought blocks on the future this
 * coordinator hands it, and the bus subscriber completes that future.
 */
public final class ConfirmationCoordinator {

    private final AtomicReference<CompletableFuture<Boolean>> pending = new AtomicReference<>();

    /**
     * Opens a confirmation wait. Returns the future to await (completes {@code true} on confirm,
     * {@code false} on cancel), or {@code null} when a confirmation is already pending so the caller must
     * not open another (overlapping confirmations are refused).
     */
    public CompletableFuture<Boolean> open() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        return pending.compareAndSet(null, future) ? future : null;
    }

    /** Closes the wait, clearing the slot for the next confirmation; pass the future returned by {@link #open}. */
    public void close(CompletableFuture<Boolean> future) {
        pending.compareAndSet(future, null);
    }

    /** Confirms the pending wait (no-op if none is open). Called by the confirmation-bus subscriber. */
    public void confirm() {
        CompletableFuture<Boolean> future = pending.get();
        if (future != null) {
            future.complete(true);
        }
    }

    /** Cancels the pending wait (no-op if none is open). Called on an explicit commander cancel. */
    public void cancel() {
        CompletableFuture<Boolean> future = pending.get();
        if (future != null) {
            future.complete(false);
        }
    }
}
