package elite.intel.ai.brain;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.ResponseTextProvider;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class BaseAiClientTest {

    private static final class TrackingFuture extends CompletableFuture<HttpResponse<String>> {
        private final AtomicBoolean cancelCalled = new AtomicBoolean();

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelCalled.set(true);
            return super.cancel(mayInterruptIfRunning);
        }
    }

    private static final class ControlledClient extends BaseAiClient {
        private final CountDownLatch exchangeStarted = new CountDownLatch(1);
        private final TrackingFuture exchange = new TrackingFuture();

        @Override
        protected CompletableFuture<HttpResponse<String>> sendAsync(HttpRequest request) {
            exchangeStarted.countDown();
            return exchange;
        }

        private AiTransportResult sendOutcome(HttpRequest request) {
            return sendTransportRequest(request);
        }
    }

    @Test
    void interruptCancelsThePhysicalHttpFuture() throws Exception {
        ControlledClient client = new ControlledClient();
        AtomicReference<JsonObject> response = new AtomicReference<>();
        Thread worker = new Thread(() -> response.set(client.sendJsonRequest(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost/unused"))
                .build())), "base-ai-client-test");
        worker.setDaemon(true);

        worker.start();
        assertTrue(client.exchangeStarted.await(2, TimeUnit.SECONDS));
        client.cancelCurrentRequest();
        worker.join(2_000);

        assertFalse(worker.isAlive(), "interrupt must release the synchronous provider wrapper");
        assertTrue(client.exchange.cancelCalled.get(), "interrupt must cancel the retained HTTP future");
        // A cancelled request says nothing: the commander interrupted it themselves, so there is no failure
        // to announce over whatever they interrupted it with.
        assertEquals("", response.get().get(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE).getAsString());
    }

    @Test
    void typedOutcomeSeparatesPermanentAndTransientHttpFailures() {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost/unused")).build();
        assertHttpFailureKind(request, 400, AiTransportResult.FailureKind.PERMANENT);
        assertHttpFailureKind(request, 401, AiTransportResult.FailureKind.PERMANENT);
        assertHttpFailureKind(request, 403, AiTransportResult.FailureKind.PERMANENT);
        assertHttpFailureKind(request, 429, AiTransportResult.FailureKind.TRANSIENT);
        assertHttpFailureKind(request, 503, AiTransportResult.FailureKind.TRANSIENT);

        ControlledClient malformed = new ControlledClient();
        malformed.exchange.complete(httpResponse(200, "not-json"));

        AiTransportResult.Failure malformedFailure = assertInstanceOf(AiTransportResult.Failure.class,
                malformed.sendOutcome(request));
        assertEquals(AiTransportResult.FailureKind.MALFORMED_RESPONSE, malformedFailure.kind());
        assertEquals(200, malformedFailure.statusCode());

        ControlledClient offline = new ControlledClient();
        offline.exchange.completeExceptionally(new IOException("network unavailable"));

        AiTransportResult.Failure networkFailure = assertInstanceOf(AiTransportResult.Failure.class,
                offline.sendOutcome(request));
        assertEquals(AiTransportResult.FailureKind.TRANSIENT, networkFailure.kind());
        assertNull(networkFailure.statusCode());
    }

    /**
     * A provider's status code describes the provider's bookkeeping, not the commander's situation: Mistral
     * answered 429 for a disabled free tier, which is an outage and not a rate limit. So a failed exchange
     * must speak our own condition - unreachable, or refused and worth checking the key - and never repeat
     * the code or the wording that came back with it.
     */
    @Test
    void transportFailureSpeaksOurConditionNeverTheProvidersStatusCode() {
        String unreachable = spokenPhraseFor(429);

        assertEquals(localized("handler.common.aiServiceUnreachable"), unreachable);
        assertEquals(unreachable, spokenPhraseFor(503), "an outage answered 429 reads the same as one answered 503");
        assertFalse(unreachable.contains("429"), "the status code belongs in the log, not in the commander's ear");
        assertFalse(unreachable.toLowerCase().contains("too many"),
                "429 during an outage must not accuse the commander of sending too many requests");

        String rejected = spokenPhraseFor(401);
        assertEquals(localized("handler.common.aiServiceRejected"), rejected);
        assertNotEquals(unreachable, rejected, "a rejected key is actionable and must not sound like an outage");
        assertEquals(rejected, spokenPhraseFor(403));
    }

    private static String spokenPhraseFor(int statusCode) {
        ControlledClient client = new ControlledClient();
        client.exchange.complete(httpResponse(statusCode, "{\"message\":\"Requests rate limit exceeded\"}"));

        return client.sendJsonRequest(HttpRequest.newBuilder().uri(URI.create("http://localhost/unused")).build())
                .get(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE).getAsString();
    }

    private static String localized(String key) {
        return ResponseTextProvider.getText(
                AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance()), key);
    }

    private static void assertHttpFailureKind(
            HttpRequest request,
            int statusCode,
            AiTransportResult.FailureKind expectedKind
    ) {
        ControlledClient client = new ControlledClient();
        client.exchange.complete(httpResponse(statusCode, "response"));

        AiTransportResult.Failure failure = assertInstanceOf(AiTransportResult.Failure.class,
                client.sendOutcome(request));
        assertEquals(expectedKind, failure.kind());
        assertEquals(statusCode, failure.statusCode());
    }

    private static HttpResponse<String> httpResponse(int statusCode, String body) {
        return new HttpResponse<>() {
            @Override public int statusCode() { return statusCode; }
            @Override public HttpRequest request() { return null; }
            @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
            @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (name, value) -> true); }
            @Override public String body() { return body; }
            @Override public Optional<javax.net.ssl.SSLSession> sslSession() { return Optional.empty(); }
            @Override public URI uri() { return URI.create("http://localhost/unused"); }
            @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
        };
    }
}
