package elite.intel.ai.brain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.SystemSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BaseAiClient {
    private static final Logger log = LogManager.getLogger(BaseAiClient.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private volatile Thread currentRequestThread = null;
    private final SystemSession systemSession = SystemSession.getInstance();

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
        CompletableFuture<HttpResponse<String>> exchange = null;
        try {
            // Keep the provider-facing API synchronous, but retain the physical exchange future so interrupting
            // a companion gateway task cancels the socket-level request instead of only abandoning its result.
            exchange = sendAsync(request);
            HttpResponse<String> response = exchange.get();
            int code = response.statusCode();
            if (code < 200 || code >= 300) {
                String body = response.body();
                log.error("HTTP {} – response: {}", code, body);
                return AiTransportResult.failure(httpFailureKind(code), code, "HTTP " + code);
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
