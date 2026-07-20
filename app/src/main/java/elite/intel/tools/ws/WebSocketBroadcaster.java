package elite.intel.tools.ws;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import elite.intel.ui.controller.ManagedService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.http.HttpRequest;

public class WebSocketBroadcaster implements Runnable, ManagedService {

    private static final Logger log = LogManager.getLogger(WebSocketBroadcaster.class);
    private static final int PORT = 7497;
    private static final WebSocketBroadcaster INSTANCE = new WebSocketBroadcaster();

    private final Gson gson = new Gson();
    private volatile WebSocketServer server;
    private Thread thread;

    private WebSocketBroadcaster() {
    }

    public static WebSocketBroadcaster getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized void start() {
        if (thread != null && thread.isAlive()) return;
        // Run off the main thread to avoid a JDK 17-21 ClassCircularityError in
        // WeakPairMap triggered when the WebSocket library initialises during JVM startup.
        thread = new Thread(this, "ws-broadcaster");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public synchronized void stop() {
        WebSocketServer srv = server;
        if (srv != null) {
            try {
                srv.stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Error stopping WS server: {}", e.getMessage());
            }
            server = null;
        }
        thread = null;
        log.info("LlmActionBroadcaster stopped");
    }

    public void broadcast(JsonObject json) {
        broadcast(gson.toJson(json));
    }

    public void broadcast(String text) {
        WebSocketServer srv = server;
        if (srv == null) return;
        try {
            srv.broadcast(text);
        } catch (Exception e) {
            log.debug("WS broadcast failed: {}", e.getMessage());
        }
    }

    public void broadcast(HttpRequest request) {
        JsonObject o = new JsonObject();
        o.addProperty("URL", request.uri().toASCIIString());
        o.addProperty("method", request.method());
        o.addProperty("headers", request.headers().toString());
        broadcast(gson.toJson(o));
    }

    @Override
    public void run() {
        try {
            WebSocketServer srv = new WebSocketServer(new InetSocketAddress(PORT)) {
                @Override
                public void onOpen(WebSocket conn, ClientHandshake handshake) {
                    log.info("WS client connected: {}", conn.getRemoteSocketAddress());
                }

                @Override
                public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                    log.info("WS client disconnected: {}", conn.getRemoteSocketAddress());
                }

                @Override
                public void onMessage(WebSocket conn, String message) {
                }

                @Override
                public void onError(WebSocket conn, Exception ex) {
                    log.error("WS error on {}: {}", conn != null ? conn.getRemoteSocketAddress() : "server", ex.getMessage());
                }

                @Override
                public void onStart() {
                    log.info("LLM action WebSocket server started on ws://localhost:{}", PORT);
                }
            };
            srv.setReuseAddr(true);
            server = srv;
            srv.run();
        } catch (Throwable e) {
            log.error("LLM action WebSocket server failed - integration disabled: {}", e.getMessage());
            server = null;
        }
    }
}