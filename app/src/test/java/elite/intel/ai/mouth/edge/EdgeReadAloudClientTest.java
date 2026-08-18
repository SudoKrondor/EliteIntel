package elite.intel.ai.mouth.edge;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeReadAloudClientTest {
    private static final Instant NOW = Instant.parse("2024-01-02T03:04:05Z");
    private static final String VOICES = """
            [{"Name":"Full Emma","ShortName":"en-US-EmmaMultilingualNeural",
              "Gender":"Female","Locale":"en-US",
              "SuggestedCodec":"audio-24khz-48kbitrate-mono-mp3"}]
            """;

    @Test
    void voiceListUsesCurrentHeadersAndRetriesOne403AfterClockCorrection() throws Exception {
        StubHttpClient http = new StubHttpClient();
        http.responses.add(response(403, "", Map.of("Date", List.of("Tue, 2 Jan 2024 03:10:05 GMT"))));
        http.responses.add(response(200, VOICES, Map.of()));
        EdgeProtocolAuth auth = authWithDistinctMuids();
        EdgeReadAloudClient client = client(http, auth);

        List<EdgeVoice> voices = client.listVoices();

        assertEquals("en-US-EmmaMultilingualNeural", voices.getFirst().shortName());
        assertEquals(2, http.requests.size());
        HttpRequest first = http.requests.getFirst();
        HttpRequest second = http.requests.get(1);
        assertTrue(first.uri().toString().contains("trustedclienttoken="
                + EdgeProtocolConstants.TRUSTED_CLIENT_TOKEN));
        assertTrue(first.uri().toString().contains("Sec-MS-GEC-Version=1-143.0.3650.75"));
        assertNotEquals(query(first.uri(), "Sec-MS-GEC"), query(second.uri(), "Sec-MS-GEC"));
        assertNotEquals(first.headers().firstValue("Cookie"), second.headers().firstValue("Cookie"));
        assertEquals("*/*", first.headers().firstValue("Accept").orElseThrow());
        assertEquals(Duration.ofMillis(100), first.timeout().orElseThrow());
        assertTrue(first.headers().firstValue("User-Agent").orElseThrow().contains("Edg/143.0.0.0"));
        assertTrue(first.headers().firstValue("Sec-CH-UA").orElseThrow().contains("Chromium\";v=\"143"));
    }

    @Test
    void synthesisSendsFramedMessagesAndAssemblesAudioInOrder() throws Exception {
        StubHttpClient http = new StubHttpClient();
        http.webSocketPlans.add(WebSocketPlan.success(new byte[]{1, 2}, new byte[]{3, 4}));
        EdgeReadAloudClient client = client(http, authWithDistinctMuids());

        byte[] audio = client.synthesize(request("request-one"));

        assertArrayEquals(new byte[]{1, 2, 3, 4}, audio);
        StubWebSocketBuilder builder = http.builders.getFirst();
        assertTrue(builder.uri.toString().contains("ConnectionId="));
        assertTrue(builder.uri.toString().contains("TrustedClientToken="
                + EdgeProtocolConstants.TRUSTED_CLIENT_TOKEN));
        assertEquals("no-cache", builder.headers.get("Cache-Control"));
        assertTrue(builder.headers.get("Cookie").startsWith("muid="));
        assertEquals(2, builder.socket.sentText.size());
        assertTrue(builder.socket.sentText.getFirst().contains("Path:speech.config"));
        assertTrue(builder.socket.sentText.getFirst().contains(
                "\"sentenceBoundaryEnabled\":\"true\""));
        assertTrue(builder.socket.sentText.get(1).contains("Path:ssml\r\n\r\n"));
        assertTrue(builder.socket.sentText.get(1).contains("X-Timestamp:")
                && builder.socket.sentText.get(1).contains(")Z\r\n"));
        assertTrue(builder.socket.aborted);
    }

    @Test
    void websocket403RetriesOnceAndMalformedOrSilentResponsesFailCleanly() throws Exception {
        StubHttpClient retrying = new StubHttpClient();
        retrying.webSocketPlans.add(WebSocketPlan.handshake403());
        retrying.webSocketPlans.add(WebSocketPlan.success(new byte[]{7}));
        assertArrayEquals(new byte[]{7}, client(retrying, authWithDistinctMuids()).synthesize(request("retry")));
        assertEquals(2, retrying.builders.size());
        assertNotEquals(query(retrying.builders.getFirst().uri, "Sec-MS-GEC"),
                query(retrying.builders.get(1).uri, "Sec-MS-GEC"));

        StubHttpClient malformed = new StubHttpClient();
        malformed.webSocketPlans.add(WebSocketPlan.malformed());
        EdgeProtocolException badFrame = assertThrows(EdgeProtocolException.class,
                () -> client(malformed, authWithDistinctMuids()).synthesize(request("bad-frame")));
        assertTrue(badFrame.getMessage().contains("header length"));

        StubHttpClient silent = new StubHttpClient();
        silent.webSocketPlans.add(WebSocketPlan.silent());
        EdgeProtocolException timeout = assertThrows(EdgeProtocolException.class,
                () -> client(silent, authWithDistinctMuids()).synthesize(request("silent")));
        assertTrue(timeout.getMessage().contains("Timed out receiving"));
        assertTrue(silent.builders.getFirst().socket.aborted);
    }

    @Test
    void cancellationAbortsAnInProgressConnectionPromptly() throws Exception {
        StubHttpClient http = new StubHttpClient();
        WebSocketPlan connecting = WebSocketPlan.connecting();
        http.webSocketPlans.add(connecting);
        EdgeReadAloudClient client = client(http, authWithDistinctMuids());
        CompletableFuture<Throwable> outcome = CompletableFuture.supplyAsync(() -> {
            try {
                client.synthesize(request("cancel-me"));
                return new AssertionError("synthesis unexpectedly succeeded");
            } catch (Throwable failure) {
                return failure;
            }
        });

        assertTrue(connecting.started.get(1, TimeUnit.SECONDS));
        client.cancel("cancel-me");

        Throwable failure = outcome.get(1, TimeUnit.SECONDS);
        assertTrue(failure instanceof EdgeProtocolException);
        assertTrue(failure.getMessage().contains("cancelled"));
        assertTrue(connecting.connection.isCancelled());
    }

    @Test
    void connectionTimeoutCancelsThePendingHandshake() {
        StubHttpClient http = new StubHttpClient();
        WebSocketPlan connecting = WebSocketPlan.connecting();
        http.webSocketPlans.add(connecting);

        EdgeProtocolException failure = assertThrows(EdgeProtocolException.class,
                () -> client(http, authWithDistinctMuids()).synthesize(request("connect-timeout")));

        assertTrue(failure.getMessage().contains("Timed out connecting"));
        assertTrue(connecting.connection.isCancelled());
    }

    private static EdgeReadAloudClient client(StubHttpClient http, EdgeProtocolAuth auth) {
        return new EdgeReadAloudClient(
                http, auth, Duration.ofMillis(100), Duration.ofMillis(100), Duration.ofMillis(30));
    }

    private static EdgeProtocolAuth authWithDistinctMuids() {
        AtomicInteger sequence = new AtomicInteger();
        return new EdgeProtocolAuth(Clock.fixed(NOW, ZoneOffset.UTC), () -> {
            byte[] muid = new byte[16];
            muid[15] = (byte) sequence.getAndIncrement();
            return muid;
        });
    }

    private static EdgeSynthesisRequest request(String id) {
        return new EdgeSynthesisRequest(
                id,
                "Elite Intel test.",
                new EdgeVoice(null, "en-US-EmmaMultilingualNeural", "Female", "en-US",
                        EdgeProtocolConstants.OUTPUT_FORMAT),
                "+0%", "+0%", "+0Hz");
    }

    private static String query(URI uri, String name) {
        for (String pair : uri.getRawQuery().split("&")) {
            int equals = pair.indexOf('=');
            if (equals > 0 && pair.substring(0, equals).equals(name)) {
                return pair.substring(equals + 1);
            }
        }
        throw new AssertionError("Missing query parameter " + name);
    }

    private static HttpResponse<String> response(int status, String body, Map<String, List<String>> headers) {
        return new StubResponse<>(status, body, HttpHeaders.of(headers, (name, value) -> true), null);
    }

    private static byte[] frame(String headers, byte[] body) {
        byte[] encoded = headers.getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(2 + encoded.length + body.length)
                .putShort((short) encoded.length)
                .put(encoded)
                .put(body)
                .array();
    }

    private static final class StubHttpClient extends HttpClient {
        private final Queue<HttpResponse<String>> responses = new ArrayDeque<>();
        private final Queue<WebSocketPlan> webSocketPlans = new ArrayDeque<>();
        private final List<HttpRequest> requests = new ArrayList<>();
        private final List<StubWebSocketBuilder> builders = new ArrayList<>();

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler)
                throws IOException {
            requests.add(request);
            HttpResponse<String> response = responses.poll();
            if (response == null) {
                throw new IOException("No stub HTTP response");
            }
            HttpResponse.ResponseInfo responseInfo = new HttpResponse.ResponseInfo() {
                @Override public int statusCode() { return response.statusCode(); }
                @Override public HttpHeaders headers() { return response.headers(); }
                @Override public Version version() { return Version.HTTP_2; }
            };
            HttpResponse.BodySubscriber<T> subscriber = handler.apply(responseInfo);
            subscriber.onSubscribe(new java.util.concurrent.Flow.Subscription() {
                @Override public void request(long count) { }
                @Override public void cancel() { }
            });
            if (!response.body().isEmpty()) {
                subscriber.onNext(List.of(ByteBuffer.wrap(response.body().getBytes(StandardCharsets.UTF_8))));
            }
            subscriber.onComplete();
            T body = subscriber.getBody().toCompletableFuture().join();
            return new StubResponse<>(response.statusCode(), body, response.headers(), request);
        }

        @Override
        public WebSocket.Builder newWebSocketBuilder() {
            StubWebSocketBuilder builder = new StubWebSocketBuilder(webSocketPlans.remove());
            builders.add(builder);
            return builder;
        }

        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() { return null; }
        @Override public SSLParameters sslParameters() { return new SSLParameters(); }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_2; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, HttpResponse.BodyHandler<T> handler) { throw new UnsupportedOperationException(); }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, HttpResponse.BodyHandler<T> handler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) { throw new UnsupportedOperationException(); }
    }

    private static final class StubWebSocketBuilder implements WebSocket.Builder {
        private final WebSocketPlan plan;
        private final Map<String, String> headers = new java.util.LinkedHashMap<>();
        private URI uri;
        private StubWebSocket socket;

        private StubWebSocketBuilder(WebSocketPlan plan) {
            this.plan = plan;
        }

        @Override
        public WebSocket.Builder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        @Override public WebSocket.Builder connectTimeout(Duration timeout) { return this; }
        @Override public WebSocket.Builder subprotocols(String mostPreferred, String... lesserPreferred) {
            return this;
        }

        @Override
        public CompletableFuture<WebSocket> buildAsync(URI uri, WebSocket.Listener listener) {
            this.uri = uri;
            return plan.connect(uri, listener, this);
        }
    }

    private static final class WebSocketPlan {
        private enum Mode { SUCCESS, HANDSHAKE_403, MALFORMED, SILENT, CONNECTING }

        private final Mode mode;
        private final byte[][] audio;
        private final CompletableFuture<Boolean> started = new CompletableFuture<>();
        private final CompletableFuture<WebSocket> connection = new CompletableFuture<>();

        private WebSocketPlan(Mode mode, byte[][] audio) {
            this.mode = mode;
            this.audio = audio;
        }

        static WebSocketPlan success(byte[]... audio) { return new WebSocketPlan(Mode.SUCCESS, audio); }
        static WebSocketPlan handshake403() { return new WebSocketPlan(Mode.HANDSHAKE_403, new byte[0][]); }
        static WebSocketPlan malformed() { return new WebSocketPlan(Mode.MALFORMED, new byte[0][]); }
        static WebSocketPlan silent() { return new WebSocketPlan(Mode.SILENT, new byte[0][]); }
        static WebSocketPlan connecting() { return new WebSocketPlan(Mode.CONNECTING, new byte[0][]); }

        private CompletableFuture<WebSocket> connect(
                URI uri, WebSocket.Listener listener, StubWebSocketBuilder builder) {
            started.complete(true);
            if (mode == Mode.CONNECTING) {
                return connection;
            }
            if (mode == Mode.HANDSHAKE_403) {
                HttpResponse<Void> response = new StubResponse<>(403, null, HttpHeaders.of(
                        Map.of("Date", List.of("Tue, 2 Jan 2024 03:10:05 GMT")), (name, value) -> true), null);
                return CompletableFuture.failedFuture(new WebSocketHandshakeException(response));
            }
            StubWebSocket socket = new StubWebSocket(listener, this);
            builder.socket = socket;
            listener.onOpen(socket);
            return CompletableFuture.completedFuture(socket);
        }
    }

    private static final class StubWebSocket implements WebSocket {
        private final WebSocket.Listener listener;
        private final WebSocketPlan plan;
        private final List<String> sentText = new ArrayList<>();
        private boolean aborted;

        private StubWebSocket(WebSocket.Listener listener, WebSocketPlan plan) {
            this.listener = listener;
            this.plan = plan;
        }

        @Override
        public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
            sentText.add(data.toString());
            if (sentText.size() == 2) {
                if (plan.mode == WebSocketPlan.Mode.MALFORMED) {
                    listener.onBinary(this, ByteBuffer.wrap(new byte[]{0}), true);
                } else if (plan.mode == WebSocketPlan.Mode.SUCCESS) {
                    listener.onText(this, "Path:response\r\n\r\n{}", true);
                    listener.onText(this, "Path:turn.start\r\n\r\n{}", true);
                    listener.onText(this, "Path:audio.metadata\r\n\r\n{}", true);
                    for (byte[] audio : plan.audio) {
                        listener.onBinary(this, ByteBuffer.wrap(frame(
                                "Path:audio\r\nContent-Type:audio/mpeg\r\n", audio)), true);
                    }
                    listener.onBinary(this, ByteBuffer.wrap(frame("Path:audio\r\n", new byte[0])), true);
                    listener.onText(this, "Path:turn.end\r\n\r\n{}", true);
                }
            }
            return CompletableFuture.completedFuture(this);
        }

        @Override public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
            return CompletableFuture.completedFuture(this);
        }
        @Override public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }
        @Override public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }
        @Override public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
            return CompletableFuture.completedFuture(this);
        }
        @Override public void request(long n) { }
        @Override public String getSubprotocol() { return ""; }
        @Override public boolean isOutputClosed() { return aborted; }
        @Override public boolean isInputClosed() { return aborted; }
        @Override public void abort() { aborted = true; }
    }

    private record StubResponse<T>(
            int statusCode,
            T body,
            HttpHeaders headers,
            HttpRequest request
    ) implements HttpResponse<T> {
        @Override public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return request == null ? URI.create("https://example.test") : request.uri(); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_2; }
    }
}
