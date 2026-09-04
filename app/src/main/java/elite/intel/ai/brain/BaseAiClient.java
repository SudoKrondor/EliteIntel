package elite.intel.ai.brain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.SystemSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class BaseAiClient {
    private static final Logger log = LogManager.getLogger(BaseAiClient.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    /**
     * How long a send is remembered for the cadence note below.
     */
    private static final long CADENCE_WINDOW_NANOS = 60L * 1_000_000_000L;

    private volatile Thread currentRequestThread = null;
    private final SystemSession systemSession = SystemSession.getInstance();

    /**
     * Start times of this client's recent sends, newest first. A rate-limit refusal is only interpretable next
     * to the traffic that earned it: a burst and a first-send-of-the-session refusal mean opposite things, and
     * the log cannot otherwise tell them apart because a successful send writes nothing.
     */
    private final Deque<Long> recentSendNanos = new ArrayDeque<>();

    public BaseAiClient() {
    }

    protected static double wallClockTps(long elapsedNs, int completionTokens) {
        return elapsedNs > 0 ? completionTokens * 1_000_000_000.0 / elapsedNs : 0.0;
    }

    public JsonObject createErrorResponse(String message) {
        JsonObject err = new JsonObject();
        err.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, message);
        return err;
    }

    public void cancelCurrentRequest() {
        Thread t = currentRequestThread;
        if (t != null) {
            t.interrupt();
        }
    }

    public JsonObject sendJsonRequest(HttpRequest request) {
        AiTransportResult outcome = sendTransportRequest(request);
        if (outcome instanceof AiTransportResult.Success success) {
            return success.response();
        }
        AiTransportResult.Failure failure = (AiTransportResult.Failure) outcome;
        announceLegacyHttpFailure(failure);
        return createErrorResponse(legacyErrorMessage(failure));
    }

    /**
     * Sends one JSON HTTP request without choosing any user-facing narration. Callers receive a typed transport
     * outcome and own their retry and speech policies; the legacy {@link #sendJsonRequest(HttpRequest)} wrapper
     * retains its existing error-object behavior for older callers.
     */
    protected AiTransportResult sendTransportRequest(HttpRequest request) {
        currentRequestThread = Thread.currentThread();
        String cadence = recordSendAndDescribeCadence();
        CompletableFuture<HttpResponse<String>> exchange = null;
        try {
            // Keep the provider-facing API synchronous, but retain the physical exchange future so interrupting
            // a companion gateway task cancels the socket-level request instead of only abandoning its result.
            exchange = sendAsync(request);
            HttpResponse<String> response = exchange.get();
            int code = response.statusCode();
            if (code < 200 || code >= 300) {
                String body = response.body();
                AiTransportResult.FailureKind kind = httpFailureKind(code);
                Long retryAfterMillis = parseRetryAfterMillis(response.headers());
                if (kind == AiTransportResult.FailureKind.TRANSIENT) {
                    // A rate-limited or overloaded provider is resent along the caller's backoff ladder, so this
                    // is a blip the turn usually survives; logging it at ERROR made a recovered send look fatal.
                    // The cadence and the provider's own limit headers travel with it: without them a 429 cannot
                    // be read as either "we sent too fast" or "this account/tier is out of allowance".
                    log.warn("HTTP {} (transient, retryable; {}) – limits: [{}] – response: {}",
                            code, cadence, rateLimitHeaders(response.headers()), body);
                } else {
                    log.error("HTTP {} – response: {}", code, body);
                }
                return AiTransportResult.failure(kind, code, "HTTP " + code, retryAfterMillis);
            }
            try {
                return AiTransportResult.success(JsonParser.parseString(response.body()).getAsJsonObject());
            } catch (RuntimeException malformed) {
                log.error("HTTP {} returned a non-object JSON response", code, malformed);
                return AiTransportResult.failure(AiTransportResult.FailureKind.MALFORMED_RESPONSE, code,
                        "Response body is not a JSON object");
            }
        } catch (InterruptedException e) {
            if (exchange != null) {
                exchange.cancel(true);
            }
            Thread.currentThread().interrupt();
            return AiTransportResult.failure(AiTransportResult.FailureKind.CANCELLED, null, "Request interrupted");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            String message = cause != null ? cause.getMessage() : e.getMessage();
            return AiTransportResult.failure(AiTransportResult.FailureKind.TRANSIENT, null,
                    "Request failed: " + message);
        } catch (CancellationException e) {
            return AiTransportResult.failure(AiTransportResult.FailureKind.CANCELLED, null, "Request cancelled");
        } finally {
            currentRequestThread = null;
        }
    }

    private void announceLegacyHttpFailure(AiTransportResult.Failure failure) {
        Integer code = failure.statusCode();
        if (code == null) {
            return;
        }
        if (code == 400 && !systemSession.useLocalCommandLlm()) {
            GameEventBus.publish(new AiVoxResponseEvent("Bad Request. Unsupported request format or invalid API key"));
        } else if (code == 429) {
            GameEventBus.publish(new AiVoxResponseEvent("Too Many Requests. Please try again later."));
        } else if (code == 401) {
            GameEventBus.publish(new AiVoxResponseEvent("Invalid API Key. Please check your API Key and try again."));
        } else if (code == 500) {
            GameEventBus.publish(new AiVoxResponseEvent("Internal Server Error. Please try again later."));
        }
    }

    private static String legacyErrorMessage(AiTransportResult.Failure failure) {
        if (failure.statusCode() != null) {
            return "HTTP " + failure.statusCode();
        }
        return switch (failure.kind()) {
            case CANCELLED -> "LLM Call Failed";
            case MALFORMED_RESPONSE -> "LLM response is malformed";
            case TRANSIENT, PERMANENT -> failure.diagnostic();
        };
    }

    /**
     * Records this send and describes the cadence it belongs to: how long since the previous send from this
     * client, and how many it makes in the last minute. Cheap - one small deque, pruned as it is read.
     */
    private String recordSendAndDescribeCadence() {
        long now = System.nanoTime();
        long sincePreviousMillis;
        int sendsInWindow;
        synchronized (recentSendNanos) {
            Long previous = recentSendNanos.peekFirst();
            sincePreviousMillis = previous == null ? -1 : (now - previous) / 1_000_000;
            while (!recentSendNanos.isEmpty() && now - recentSendNanos.peekLast() > CADENCE_WINDOW_NANOS) {
                recentSendNanos.pollLast();
            }
            recentSendNanos.addFirst(now);
            sendsInWindow = recentSendNanos.size();
        }
        String since = sincePreviousMillis < 0
                ? "first send of this session"
                : "previous send " + sincePreviousMillis + " ms ago";
        return since + ", " + sendsInWindow + " in the last minute";
    }

    /**
     * The provider's own limit headers, verbatim. Vendors spell these differently and change the spelling, so
     * everything mentioning a rate limit is passed through rather than mapped onto names we guessed in advance.
     * Response headers only - nothing here can carry the request's credentials.
     */
    private static String rateLimitHeaders(HttpHeaders headers) {
        if (headers == null) {
            return "none reported";
        }
        StringBuilder reported = new StringBuilder();
        for (Map.Entry<String, List<String>> header : headers.map().entrySet()) {
            String name = header.getKey().toLowerCase(Locale.ROOT);
            if (!name.contains("ratelimit") && !name.contains("rate-limit") && !name.equals("retry-after")) {
                continue;
            }
            if (!reported.isEmpty()) {
                reported.append(", ");
            }
            reported.append(header.getKey()).append('=').append(String.join("/", header.getValue()));
        }
        return reported.isEmpty() ? "none reported" : reported.toString();
    }

    /**
     * Reads the standard {@code Retry-After} header, in either of its two forms - delta-seconds, or an HTTP
     * date - and returns the advised wait in milliseconds, or null when the provider sent no usable advice.
     * The header is only advice: what to do with it (honour, clamp, ignore) belongs to the caller's retry policy.
     */
    static Long parseRetryAfterMillis(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        Optional<String> header = headers.firstValue("Retry-After");
        if (header.isEmpty()) {
            return null;
        }
        String advice = header.get().trim();
        if (advice.isEmpty()) {
            return null;
        }
        try {
            return TimeUnit.SECONDS.toMillis(Long.parseLong(advice));
        } catch (NumberFormatException notDeltaSeconds) {
            // Fall through: the other legal form is an HTTP date.
        }
        try {
            Instant retryAt = ZonedDateTime.parse(advice, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
            return Math.max(0L, Duration.between(Instant.now(), retryAt).toMillis());
        } catch (DateTimeParseException unparseable) {
            return null;
        }
    }

    private static AiTransportResult.FailureKind httpFailureKind(int statusCode) {
        return statusCode == 408 || statusCode == 425 || statusCode == 429 || statusCode >= 500
                ? AiTransportResult.FailureKind.TRANSIENT
                : AiTransportResult.FailureKind.PERMANENT;
    }

    /** Starts the physical HTTP exchange; protected so cancellation can be verified without real network I/O. */
    protected CompletableFuture<HttpResponse<String>> sendAsync(HttpRequest request) {
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
