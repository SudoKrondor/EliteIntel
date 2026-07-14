package elite.intel.companion.clarify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runtime-owned single-slot rendezvous between a completed {@code request_input} turn and the next commander
 * reply. Claiming is atomic: one input either owns the continuation or observes no pending interaction, so a
 * stale action can never be resumed by two queued turns.
 */
public final class ClarificationCoordinator {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    private final AtomicReference<PendingClarification> pending = new AtomicReference<>();
    private final Clock clock;
    private final Duration timeout;

    /** Production coordinator with a short voice-conversation timeout. */
    public ClarificationCoordinator() {
        this(Clock.systemUTC(), DEFAULT_TIMEOUT);
    }

    /** Test seam for deterministic expiry checks. */
    ClarificationCoordinator(Clock clock, Duration timeout) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }

    /**
     * Opens or replaces the one pending clarification. Replacement is intentional: a later validated
     * {@code request_input} is the current dialogue state and an older request must not remain callable.
     */
    public PendingClarification open(
            String actionId,
            String parameterName,
            String originalInput,
            String question
    ) {
        Instant now = clock.instant();
        PendingClarification next = new PendingClarification(
                actionId, parameterName, originalInput, question, now.plus(timeout));
        pending.set(next);
        return next;
    }

    /**
     * Atomically transfers a non-expired clarification to one commander turn. The slot is cleared even when
     * expired, so unrelated future speech can never revive stale action context.
     */
    public Optional<PendingClarification> claim() {
        PendingClarification claimed = pending.getAndSet(null);
        return claimed == null || claimed.isExpiredAt(clock.instant())
                ? Optional.empty()
                : Optional.of(claimed);
    }

    /** Returns the live pending state without consuming it; intended for diagnostics and tests. */
    public Optional<PendingClarification> peek() {
        PendingClarification current = pending.get();
        if (current == null) {
            return Optional.empty();
        }
        if (current.isExpiredAt(clock.instant())) {
            pending.compareAndSet(current, null);
            return Optional.empty();
        }
        return Optional.of(current);
    }

    /** Clears any pending interaction during cancellation or runtime shutdown. */
    public void cancel() {
        pending.set(null);
    }
}
