package elite.intel.ai.mouth.edge;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/** Native Java HTTP/WebSocket transport for Microsoft Edge's consumer Read Aloud protocol. */
final class EdgeReadAloudClient implements EdgeSynthesisClient {
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration RECEIVE_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient;
    private final EdgeProtocolAuth auth;
    private final Duration httpTimeout;
    private final Duration connectTimeout;
    private final Duration receiveTimeout;
    private final ConcurrentMap<String, CompletableFuture<WebSocket>> connecting = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WebSocket> active = new ConcurrentHashMap<>();
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final Set<String> cancelled = ConcurrentHashMap.newKeySet();

    EdgeReadAloudClient() {
        this(HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(), new EdgeProtocolAuth(),
                HTTP_TIMEOUT, CONNECT_TIMEOUT, RECEIVE_TIMEOUT);
    }

    EdgeReadAloudClient(HttpClient httpClient, EdgeProtocolAuth auth) {
        this(httpClient, auth, HTTP_TIMEOUT, CONNECT_TIMEOUT, RECEIVE_TIMEOUT);
    }

    EdgeReadAloudClient(
            HttpClient httpClient,
            EdgeProtocolAuth auth,
            Duration httpTimeout,
            Duration connectTimeout,
            Duration receiveTimeout
    ) {
        this.httpClient = httpClient;
        this.auth = auth;
        this.httpTimeout = httpTimeout;
        this.connectTimeout = connectTimeout;
        this.receiveTimeout = receiveTimeout;
    }

    @Override
    public List<EdgeVoice> listVoices() throws IOException, InterruptedException {
        HttpResponse<String> response = sendVoiceListRequest();
        if (response.statusCode() == 403) {
            adjustClock(response.headers());
            response = sendVoiceListRequest();
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new EdgeProtocolException("Edge voice-list request failed with HTTP " + response.statusCode());
        }
        return EdgeVoiceListParser.parse(response.body());
    }

    @Override
    public byte[] synthesize(EdgeSynthesisRequest request) throws IOException, InterruptedException {
        inFlight.add(request.requestId());
        try {
            checkCancelled(request.requestId());
            try {
                return synthesizeOnce(request);
            } catch (HandshakeFailure failure) {
                if (failure.statusCode() != 403) {
                    throw new EdgeProtocolException(
                            "Edge WebSocket handshake failed with HTTP " + failure.statusCode(), failure);
                }
                checkCancelled(request.requestId());
                adjustClock(failure.headers());
                checkCancelled(request.requestId());
                try {
                    return synthesizeOnce(request);
                } catch (HandshakeFailure retryFailure) {
                    throw new EdgeProtocolException(
                            "Edge WebSocket handshake failed after clock-skew retry with HTTP "
                                    + retryFailure.statusCode(), retryFailure);
                }
            }
        } finally {
            inFlight.remove(request.requestId());
            cancelled.remove(request.requestId());
        }
    }

    @Override
    public void cancel(String requestId) {
        cancelled.add(requestId);
        CompletableFuture<WebSocket> pending = connecting.remove(requestId);
        if (pending != null) {
            abortPending(pending);
        }
        WebSocket socket = active.remove(requestId);
        if (socket != null) {
            socket.abort();
        }
    }

    @Override
    public void cancelAll() {
        new ArrayList<>(inFlight).forEach(this::cancel);
        connecting.keySet().forEach(this::cancel);
        active.keySet().forEach(this::cancel);
    }

    private HttpResponse<String> sendVoiceListRequest() throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(EdgeProtocolConstants.voiceListUri(auth))
                .timeout(httpTimeout)
                .GET();
        EdgeProtocolConstants.voiceHeaders(auth).forEach(request::header);
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private byte[] synthesizeOnce(EdgeSynthesisRequest request) throws IOException, InterruptedException,
            HandshakeFailure {
        checkCancelled(request.requestId());
        String connectionId = EdgeSsml.requestId();
        SynthesisListener listener = new SynthesisListener();
        WebSocket.Builder builder = httpClient.newWebSocketBuilder().connectTimeout(connectTimeout);
        EdgeProtocolConstants.webSocketHeaders(auth).forEach(builder::header);
        CompletableFuture<WebSocket> pending = builder.buildAsync(
                EdgeProtocolConstants.synthesisUri(auth, connectionId), listener);
        connecting.put(request.requestId(), pending);
        if (cancelled.contains(request.requestId())) {
            cancel(request.requestId());
            throw new EdgeProtocolException("Edge Read Aloud synthesis was cancelled");
        }

        WebSocket socket;
        try {
            socket = pending.get(connectTimeout.toMillis(), TimeUnit.MILLISECONDS);
            active.put(request.requestId(), socket);
        } catch (ExecutionException e) {
            throw handshakeOrProtocolFailure(e.getCause());
        } catch (TimeoutException e) {
            abortPending(pending);
            throw new EdgeProtocolException("Timed out connecting to Edge Read Aloud", e);
        } catch (CancellationException e) {
            throw new EdgeProtocolException("Edge Read Aloud synthesis was cancelled", e);
        } finally {
            connecting.remove(request.requestId(), pending);
        }

        try {
            checkCancelled(request.requestId());
            String ssml = EdgeSsml.build(request.text(), request.voice().protocolName(), request.rate());
            send(socket, EdgeSsml.speechConfig(Instant.now()));
            send(socket, EdgeSsml.ssmlMessage(ssml, Instant.now()));
            byte[] audio = listener.result().get(receiveTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (audio.length == 0) {
                throw new EdgeProtocolException("Edge Read Aloud returned no audio");
            }
            return audio;
        } catch (ExecutionException e) {
            Throwable cause = unwrap(e.getCause());
            if (cause instanceof IOException io) {
                throw io;
            }
            throw new EdgeProtocolException("Edge Read Aloud synthesis failed", cause);
        } catch (TimeoutException e) {
            throw new EdgeProtocolException("Timed out receiving Edge Read Aloud audio", e);
        } catch (CancellationException e) {
            throw new EdgeProtocolException("Edge Read Aloud synthesis was cancelled", e);
        } finally {
            active.remove(request.requestId(), socket);
            socket.abort();
        }
    }

    private static void send(WebSocket socket, String message) throws EdgeProtocolException {
        try {
            socket.sendText(message, true).join();
        } catch (CompletionException e) {
            throw new EdgeProtocolException("Could not send Edge Read Aloud protocol message", unwrap(e));
        }
    }

    private void adjustClock(HttpHeaders headers) throws EdgeProtocolException {
        auth.adjustToServerDate(headers.firstValue("Date").orElse(null));
    }

    private void checkCancelled(String requestId) throws EdgeProtocolException {
        if (cancelled.contains(requestId)) {
            throw new EdgeProtocolException("Edge Read Aloud synthesis was cancelled");
        }
    }

    private static void abortPending(CompletableFuture<WebSocket> pending) {
        pending.cancel(true);
        try {
            WebSocket completed = pending.getNow(null);
            if (completed != null) {
                completed.abort();
            }
        } catch (CancellationException | CompletionException ignored) {
            // The failed/cancelled handshake has no socket to abort.
        }
    }

    private static HandshakeFailure handshakeOrProtocolFailure(Throwable failure) throws EdgeProtocolException {
        Throwable cause = unwrap(failure);
        if (cause instanceof WebSocketHandshakeException handshake) {
            return new HandshakeFailure(
                    handshake.getResponse().statusCode(), handshake.getResponse().headers(), handshake);
        }
        throw new EdgeProtocolException("Could not connect to Edge Read Aloud", cause);
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static final class HandshakeFailure extends Exception {
        private final int statusCode;
        private final HttpHeaders headers;

        private HandshakeFailure(int statusCode, HttpHeaders headers, Throwable cause) {
            super(cause);
            this.statusCode = statusCode;
            this.headers = headers;
        }

        int statusCode() {
            return statusCode;
        }

        HttpHeaders headers() {
            return headers;
        }
    }

    private static final class SynthesisListener implements WebSocket.Listener {
        private final EdgeAudioStreamAssembler assembler = new EdgeAudioStreamAssembler();
        private final ByteArrayOutputStream binaryFragments = new ByteArrayOutputStream();
        private final StringBuilder textFragments = new StringBuilder();
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletableFuture<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textFragments.append(data);
            if (last) {
                handleText(webSocket, textFragments.toString());
                textFragments.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletableFuture<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            byte[] fragment = new byte[data.remaining()];
            data.get(fragment);
            binaryFragments.writeBytes(fragment);
            if (last) {
                handleBinary(binaryFragments.toByteArray());
                binaryFragments.reset();
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletableFuture<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (!result.isDone()) {
                result.completeExceptionally(new EdgeProtocolException(
                        "Edge Read Aloud closed before turn.end (status " + statusCode + ")"));
            }
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            result.completeExceptionally(new EdgeProtocolException("Edge Read Aloud WebSocket error", error));
        }

        CompletableFuture<byte[]> result() {
            return result;
        }

        private void handleText(WebSocket socket, String message) {
            try {
                if (assembler.acceptText(message)) {
                    result.complete(assembler.audio());
                    socket.sendClose(WebSocket.NORMAL_CLOSURE, "complete");
                }
            } catch (EdgeProtocolException e) {
                result.completeExceptionally(e);
            }
        }

        private void handleBinary(byte[] frame) {
            try {
                assembler.acceptBinary(frame);
            } catch (EdgeProtocolException e) {
                result.completeExceptionally(e);
            }
        }
    }
}
