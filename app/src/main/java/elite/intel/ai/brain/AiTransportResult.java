package elite.intel.ai.brain;

import com.google.gson.JsonObject;

import java.util.Objects;

/**
 * The outcome of one HTTP JSON exchange before any provider-specific response parsing. Transport failures carry
 * a machine-readable retry category so callers can keep retry policy separate from response-schema repair.
 */
public sealed interface AiTransportResult permits AiTransportResult.Success, AiTransportResult.Failure {

    /** A successful HTTP exchange carrying a JSON object for the provider adapter to parse. */
    record Success(JsonObject response) implements AiTransportResult {
        public Success {
            Objects.requireNonNull(response, "response");
        }
    }

    /**
     * A failed or unusable HTTP exchange, with no provider response available for normal parsing.
     * {@code retryAfterMillis} carries the provider's own advice from a {@code Retry-After} header when it sent
     * one, and is null otherwise - a rate limiter that names its own cooldown knows better than a blind ladder.
     */
    record Failure(FailureKind kind, Integer statusCode, String diagnostic, Long retryAfterMillis)
            implements AiTransportResult {

        public Failure {
            Objects.requireNonNull(kind, "kind");
            diagnostic = diagnostic == null ? "" : diagnostic;
            if (retryAfterMillis != null && retryAfterMillis < 0) {
                retryAfterMillis = null; // a past date or a negative delta advises nothing
            }
        }

        /**
         * Creates a failure the provider gave no retry advice for.
         */
        public Failure(FailureKind kind, Integer statusCode, String diagnostic) {
            this(kind, statusCode, diagnostic, null);
        }
    }

    /** Retry policy category for a transport-level failure. */
    enum FailureKind {
        /** Connectivity, rate limiting, or a 5xx response may succeed on one delayed retry. */
        TRANSIENT,
        /** Authentication, authorization, and request-shape failures must not be retried. */
        PERMANENT,
        /** A 2xx exchange returned a body that is not a JSON object; protocol repair may retry it. */
        MALFORMED_RESPONSE,
        /** The owning request was cancelled and must not create another physical attempt. */
        CANCELLED
    }

    /** Creates a successful outcome from a provider response object. */
    static Success success(JsonObject response) {
        return new Success(response);
    }

    /** Creates a failed outcome without exposing a raw response body to callers. */
    static Failure failure(FailureKind kind, Integer statusCode, String diagnostic) {
        return new Failure(kind, statusCode, diagnostic, null);
    }

    /**
     * Creates a failed outcome carrying the provider's own {@code Retry-After} advice, in milliseconds.
     */
    static Failure failure(FailureKind kind, Integer statusCode, String diagnostic, Long retryAfterMillis) {
        return new Failure(kind, statusCode, diagnostic, retryAfterMillis);
    }
}
