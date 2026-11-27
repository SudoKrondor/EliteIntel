package elite.intel.integration;

import elite.intel.ui.controller.ManagedService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.server.WebSocketServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The local WebSocket other programs on this PC integrate through, on {@code ws://127.0.0.1:7497}. It carries
 * typed integration messages only - today FrameShift Command's ({@link FrameShiftLink}); it publishes nothing on
 * its own. Protocol in {@code docs/integration/FrameShiftIntegration.md}.
 * <p>
 * It listens on loopback alone, so nothing on the network can reach it, and it refuses a handshake from a web page
 * served by any host but this one: a browser lets any site open a socket to localhost, and the {@code Origin}
 * header is how such a page shows itself. A desktop program sends no origin, or a local one.
 */
public class IntegrationSocket implements Runnable, ManagedService {

    private static final Logger log = LogManager.getLogger(IntegrationSocket.class);
    static final int PORT = 7497;
    private static final Pattern SCHEME = Pattern.compile("([A-Za-z][A-Za-z0-9+.-]*):");
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]", "::1");
    private static final IntegrationSocket INSTANCE = new IntegrationSocket();

    private final FrameShiftLink frameShift = FrameShiftLink.getInstance();
    private final Map<WebSocket, FrameShiftLink.Peer> peers = new ConcurrentHashMap<>();
    private volatile WebSocketServer server;
    private Thread thread;

    private IntegrationSocket() {
    }

    public static IntegrationSocket getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized void start() {
        if (thread != null && thread.isAlive()) return;
        // Run off the main thread to avoid a JDK 17-21 ClassCircularityError in
        // WeakPairMap triggered when the WebSocket library initialises during JVM startup.
        thread = new Thread(this, "integration-socket");
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
                log.warn("Error stopping integration socket: {}", e.getMessage());
            }
            server = null;
        }
        thread = null;
    }

    /**
     * Whether a handshake's {@code Origin} may connect: none at all (a desktop program), the opaque {@code null}
     * origin, a non-web scheme such as {@code file:} or an app's own, or a web page served from this PC.
     */
    static boolean isAllowedOrigin(String origin) {
        if (origin == null || origin.isBlank() || origin.strip().equals("null")) return true;
        String value = origin.strip();
        Matcher scheme = SCHEME.matcher(value);
        if (!scheme.lookingAt()) return false;
        String name = scheme.group(1).toLowerCase(Locale.ROOT);
        if (!name.equals("http") && !name.equals("https")) return true;
        try {
            String host = URI.create(value).getHost();
            return host != null && LOCAL_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void run() {
        try {
            WebSocketServer srv = new WebSocketServer(new InetSocketAddress(InetAddress.getLoopbackAddress(), PORT)) {
                @Override
                public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket conn, Draft draft,
                                                                                   ClientHandshake request) throws InvalidDataException {
                    String origin = request.hasFieldValue("Origin") ? request.getFieldValue("Origin") : null;
                    if (!isAllowedOrigin(origin)) {
                        log.warn("Integration socket refused a connection from origin {}", origin);
                        throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Origin not allowed");
                    }
                    return super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);
                }

                @Override
                public void onOpen(WebSocket conn, ClientHandshake handshake) {
                    peers.put(conn, conn::send);
                    log.info("Integration client connected: {}", conn.getRemoteSocketAddress());
                }

                @Override
                public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                    FrameShiftLink.Peer peer = peers.remove(conn);
                    if (peer != null) frameShift.onClose(peer, remote);
                    log.info("Integration client disconnected: {}", conn.getRemoteSocketAddress());
                }

                @Override
                public void onMessage(WebSocket conn, String message) {
                    FrameShiftLink.Peer peer = peers.get(conn);
                    if (peer != null) frameShift.onMessage(peer, message);
                }

                @Override
                public void onError(WebSocket conn, Exception ex) {
                    log.error("Integration socket error on {}: {}", conn != null ? conn.getRemoteSocketAddress() : "server", ex.getMessage());
                }

                @Override
                public void onStart() {
                    log.info("Integration socket started on ws://127.0.0.1:{}", PORT);
                }
            };
            srv.setReuseAddr(true);
            server = srv;
            srv.run();
        } catch (Throwable e) {
            log.error("Integration socket failed - integrations disabled: {}", e.getMessage());
            server = null;
        }
    }
}
