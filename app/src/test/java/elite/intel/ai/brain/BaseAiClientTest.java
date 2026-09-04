package elite.intel.ai.brain;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
        assertEquals("LLM Call Failed",
                response.get().get(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE).getAsString());
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

    /**
     * A rate-limited provider that names its own cooldown is worth more than a blind ladder, so the header is
     * carried on the failure in both of its legal forms - and an unusable one advises nothing rather than zero.
     */
    @Test
    void transientFailureCarriesTheProvidersRetryAfterAdvice() {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost/unused")).build();

        assertEquals(2_000L, retryAfterOf(request, Map.of("Retry-After", List.of("2"))));
        assertNull(retryAfterOf(request, Map.of()));
        assertNull(retryAfterOf(request, Map.of("Retry-After", List.of("soon"))));
        assertNull(retryAfterOf(request, Map.of("Retry-After", List.of("-5"))));

        String httpDate = DateTimeFormatter.RFC_1123_DATE_TIME.format(
                ZonedDateTime.ofInstant(Instant.now().plusSeconds(30), ZoneOffset.UTC));
        Long fromDate = retryAfterOf(request, Map.of("Retry-After", List.of(httpDate)));
        assertNotNull(fromDate);
        assertTrue(fromDate > 20_000 && fromDate <= 30_000, "HTTP-date advice was " + fromDate + " ms");

        String past = DateTimeFormatter.RFC_1123_DATE_TIME.format(
                ZonedDateTime.ofInstant(Instant.now().minusSeconds(30), ZoneOffset.UTC));
        assertEquals(0L, retryAfterOf(request, Map.of("Retry-After", List.of(past))));
    }

    private static Long retryAfterOf(HttpRequest request, Map<String, List<String>> headers) {
        ControlledClient client = new ControlledClient();
        client.exchange.complete(httpResponse(429, "rate limited", headers));

        AiTransportResult.Failure failure = assertInstanceOf(AiTransportResult.Failure.class,
                client.sendOutcome(request));
        assertEquals(AiTransportResult.FailureKind.TRANSIENT, failure.kind());
        return failure.retryAfterMillis();
    }

    private static HttpResponse<String> httpResponse(int statusCode, String body) {
        return httpResponse(statusCode, body, Map.of());
    }

    private static HttpResponse<String> httpResponse(
            int statusCode,
            String body,
            Map<String, List<String>> headers
    ) {
        return new HttpResponse<>() {
            @Override public int statusCode() { return statusCode; }
            @Override public HttpRequest request() { return null; }
            @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(headers, (name, value) -> true);
            }
            @Override public String body() { return body; }
            @Override public Optional<javax.net.ssl.SSLSession> sslSession() { return Optional.empty(); }
            @Override public URI uri() { return URI.create("http://localhost/unused"); }
            @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
        };
    }

    /**
     * A 429 is only interpretable next to the traffic that earned it and the provider's own limit headers, so
     * both travel with the line. Read back through the failure the caller sees rather than through the log.
     */
    @Test
    void rateLimitHeadersAreReportedVerbatim() {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost/unused")).build();
        ControlledClient client = new ControlledClient();
        client.exchange.complete(httpResponse(429, "rate limited", Map.of(
                "ratelimitbysize-remaining", List.of("0"),
                "Retry-After", List.of("7"),
                "content-type", List.of("application/json"))));

        AiTransportResult.Failure failure = assertInstanceOf(AiTransportResult.Failure.class,
                client.sendOutcome(request));

        assertEquals(7_000L, failure.retryAfterMillis());
        assertEquals(429, failure.statusCode());
    }
}
