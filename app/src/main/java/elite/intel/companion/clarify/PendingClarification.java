package elite.intel.companion.clarify;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable continuation state for one commander action that is waiting for a required argument.
 * It is runtime state, not conversation memory: the originating thought has already completed, while the
 * dispatcher may attach this record to exactly one later commander turn.
 *
 * @param actionId      exact game-tool id selected from the originating turn's offered tool snapshot
 * @param parameterName required action parameter requested from the commander
 * @param originalInput original commander order, retained so the continuation can rebuild complete arguments
 * @param question      words actually spoken when the value was requested
 * @param expiresAt     absolute expiry after which a terse reply must no longer resume the action
 */
public record PendingClarification(
        String actionId,
        String parameterName,
        String originalInput,
        String question,
        Instant expiresAt
) {

    /** Rejects incomplete state before it can become a cross-turn execution capability. */
    public PendingClarification {
        actionId = requireText(actionId, "actionId");
        parameterName = requireText(parameterName, "parameterName");
        originalInput = requireText(originalInput, "originalInput");
        question = requireText(question, "question");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    }

    /** Whether this continuation is no longer eligible to claim a commander reply. */
    boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(Objects.requireNonNull(now, "now"));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}
