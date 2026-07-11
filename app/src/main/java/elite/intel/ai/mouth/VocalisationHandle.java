package elite.intel.ai.mouth;

import elite.intel.ai.ears.IsSpeakingEvent;
import elite.intel.eventbus.GameEventBus;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Request-scoped ownership and completion handle for one vocalisation accepted by an active Mouth.
 * <p>
 * A producer creates the handle, but exactly one eligible Mouth claims it before queueing work. Claiming joins
 * the process-wide playback count; completion, cancellation, or failure leaves that count exactly once. The
 * resulting {@link IsSpeakingEvent} stream changes only on zero-to-one and one-to-zero transitions, so one
 * utterance cannot report silence while another accepted utterance is still pending or playing.
 */
public final class VocalisationHandle {

    private static final AtomicInteger ACTIVE_REQUESTS = new AtomicInteger();

    private final String requestId;
    private final boolean interruptible;
    private final CompletableFuture<Void> completion;
    private final AtomicBoolean handled = new AtomicBoolean();
    private final AtomicBoolean counted = new AtomicBoolean();

    /** Creates an unclaimed handle, reusing {@code completion} when a caller supplied one. */
    public VocalisationHandle(
            String requestId,
            boolean interruptible,
            CompletableFuture<Void> completion
    ) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("Vocalisation request id must not be blank");
        }
        this.requestId = requestId;
        this.interruptible = interruptible;
        this.completion = completion == null ? new CompletableFuture<>() : completion;
    }

    /**
     * Claims this request for the active Mouth. A second backend, or an already-settled request, cannot claim it.
     */
    public boolean claimForPlayback() {
        if (completion.isDone() || !handled.compareAndSet(false, true)) {
            return false;
        }
        counted.set(true);
        if (ACTIVE_REQUESTS.getAndIncrement() == 0) {
            GameEventBus.publish(new IsSpeakingEvent(true));
        }
        completion.whenComplete((ignored, failure) -> leaveSpeakingState());
        return true;
    }

    /**
     * Rejects a request that no Mouth accepted. Used after synchronous EventBus publication to avoid a future
     * that can never finish when TTS is unavailable.
     */
    public boolean rejectIfUnclaimed(Throwable failure) {
        Objects.requireNonNull(failure, "failure");
        if (!handled.compareAndSet(false, true)) {
            return false;
        }
        completion.completeExceptionally(failure);
        return true;
    }

    /** Completes the accepted request after playback or intentional interruption. */
    public void complete() {
        completion.complete(null);
    }

    /** Completes the accepted request exceptionally after a synthesis, device, or playback failure. */
    public void fail(Throwable failure) {
        completion.completeExceptionally(Objects.requireNonNull(failure, "failure"));
    }

    public String requestId() {
        return requestId;
    }

    public boolean interruptible() {
        return interruptible;
    }

    public CompletableFuture<Void> completion() {
        return completion;
    }

    public boolean isHandled() {
        return handled.get();
    }

    public boolean isDone() {
        return completion.isDone();
    }

    private void leaveSpeakingState() {
        if (!counted.compareAndSet(true, false)) {
            return;
        }
        int remaining = ACTIVE_REQUESTS.decrementAndGet();
        if (remaining == 0) {
            GameEventBus.publish(new IsSpeakingEvent(false));
        } else if (remaining < 0) {
            // Defensive recovery: exactly-once completion should make this unreachable, but never publish a
            // permanently corrupt negative speaking count if a future implementation violates the contract.
            ACTIVE_REQUESTS.compareAndSet(remaining, 0);
        }
    }
}
