package elite.intel.ai.brain.vega;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Identity and liveness token for one installed companion runtime. Asynchronous work keeps this token so a
 * completion from an older runtime can detect that its owner has stopped before it writes memory or starts
 * speech. Generation identifiers are process-local and strictly increasing.
 */
public final class CompanionRuntimeGeneration {

    private static final AtomicLong NEXT_GENERATION_ID = new AtomicLong();

    private final long generationId = NEXT_GENERATION_ID.incrementAndGet();
    private final AtomicBoolean active = new AtomicBoolean(true);

    /** Creates the next active process-local companion generation. */
    public CompanionRuntimeGeneration() {
    }

    /** Returns the process-local identifier of this runtime generation. */
    public long generationId() {
        return generationId;
    }

    /** Whether work owned by this generation may still publish side effects. */
    public boolean isActive() {
        return active.get();
    }

    /**
     * Admits one publication side effect only while this generation is active. This check is deliberately
     * non-blocking: shutdown fences new work immediately and then cancels/drains already-admitted work through
     * the owning graph's resource contracts.
     *
     * @return {@code true} when the side effect ran, {@code false} when this generation had already stopped
     */
    public boolean runIfActive(Runnable sideEffect) {
        java.util.Objects.requireNonNull(sideEffect, "sideEffect");
        if (!active.get()) {
            return false;
        }
        sideEffect.run();
        return true;
    }

    /** Permanently fences this generation. Called once by its owning runtime graph during shutdown. */
    void deactivate() {
        active.set(false);
    }
}
