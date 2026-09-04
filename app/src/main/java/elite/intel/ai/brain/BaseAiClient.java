package elite.intel.ai.brain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.ResponseTextProvider;
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
    private static final String SERVICE_UNREACHABLE_KEY = "handler.common.aiServiceUnreachable";
    private static final String SERVICE_REJECTED_KEY = "handler.common.aiServiceRejected";
    private static final String UNUSABLE_RESPONSE_KEY = "handler.common.cantDoNow";
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
        return createErrorResponse(transportFailurePhrase((AiTransportResult.Failure) outcome));
    }

    /**
     * Sends one JSON HTTP request without choosing any user-facing narration. Callers receive a typed transport
     * outcome and own their retry and speech policies; the legacy {@link #sendJsonRequest(HttpRequest)} wrapper
     * folds a failure into an error object carrying the phrase its callers should speak.
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

    /**
     * Returns what the commander hears for a transport failure that never produced a usable response, as a
     * localized phrase describing OUR side of the exchange rather than the provider's status code.
     * <p>
     * A provider is free to answer a status code that means something else entirely: Mistral answered 429
     * throughout the free-tier outage of 2026-09-04, a condition with nothing to do with request volume.
     * Repeating "too many requests" therefore blames the commander for an outage and sends them hunting for
     * a quota that does not exist. Only the failures the commander can actually act on - a rejected key or a
     * request shape the provider will never accept - earn a phrase of their own; the status code and the
     * provider's own body stay in the ERROR log, where a diagnosis belongs.
     * <p>
     * A cancelled request says nothing at all: the commander interrupted it themselves.
     */
    private String transportFailurePhrase(AiTransportResult.Failure failure) {
        return switch (failure.kind()) {
            case CANCELLED -> "";
            case MALFORMED_RESPONSE -> servicePhrase(UNUSABLE_RESPONSE_KEY);
            case TRANSIENT, PERMANENT -> servicePhrase(
                    isCommanderActionable(failure.statusCode()) ? SERVICE_REJECTED_KEY : SERVICE_UNREACHABLE_KEY);
        };
    }

    /**
     * True for the refusals a commander can fix from the settings tab: 401/403 is always the API key, and a
     * cloud 400 is either the key or a request body that provider will never accept. A local host answering
     * 400 is our own request shape and nothing the commander configured, so it stays a service failure.
     */
    private boolean isCommanderActionable(Integer statusCode) {
        if (statusCode == null) {
            return false;
        }
        return statusCode == 401 || statusCode == 403
                || (statusCode == 400 && !systemSession.useLocalCommandLlm());
    }

    private static String servicePhrase(String key) {
        return ResponseTextProvider.getText(
                AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance()), key);
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
