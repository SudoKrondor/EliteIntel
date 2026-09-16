package elite.intel.gameapi.eddn;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.InflaterInputStream;

/**
 * Listens to the EDDN relay - the public feed of journal events every commander running EDMC,
 * EDDiscovery and the like uploads - and hands the messages this app can learn from to
 * {@link EddnSightings}.
 * <p>
 * <b>Subscribe only.</b> Nothing of ours goes out: the socket is a ZeroMQ SUB and the relay has no
 * way to hear it. What comes in is other commanders' journal data with the personal fields already
 * stripped by the gateway, and this app reads it the same way it reads its own journal.
 * <p>
 * <b>Silent and disposable.</b> One daemon thread, no voice, no UI. It reconnects on its own when
 * the relay goes away, and if it dies it dies quietly - the app is whole without it, it just learns
 * more slowly. The counters it keeps are for the log, so a support bundle can say whether it was
 * connected and how much it has taken in.
 * <p>
 * WHY the whole firehose is received and filtered here rather than subscribed to by topic: the
 * relay publishes every message on the empty topic, and the body is zlib-compressed, so nothing
 * can be told apart until it is inflated. The schema check is a substring search on the inflated
 * text before any JSON is parsed - about a tenth of the traffic survives it.
 */
public final class EddnListener {

    private static final Logger log = LogManager.getLogger(EddnListener.class);

    static final String RELAY = "tcp://eddn.edcd.io:9500";

    /**
     * How long one receive waits before the loop checks whether it should still be running.
     */
    private static final int RECEIVE_TIMEOUT_MS = 30_000;

    /**
     * How long without a single message before the connection is assumed dead and rebuilt. The
     * relay carries several messages a second at the quietest hour, so this is only ever a fault.
     */
    private static final long SILENCE_BEFORE_RECONNECT_MS = 5 * 60_000L;

    private static final long BACKOFF_INITIAL_MS = 5_000L;
    private static final long BACKOFF_MAX_MS = 5 * 60_000L;

    /**
     * Inflated messages larger than this are dropped unread. The biggest legitimate message (a
     * shipyard or outfitting list) is a few tens of kilobytes.
     */
    private static final int MAX_MESSAGE_BYTES = 4 * 1024 * 1024;

    private static final long STATS_INTERVAL_MS = 60 * 60_000L;

    private static volatile EddnListener instance;

    /**
     * What is done with each inflated envelope. {@link EddnSightings} in the app; a counter in the
     * live test, which must not write to the commander's ledgers.
     */
    interface EnvelopeHandler {
        /**
         * @return true when the envelope was one this app learns from
         */
        boolean accept(String envelope);
    }

    private final EnvelopeHandler sightings;
    private final AtomicBoolean running = new AtomicBoolean();

    private volatile long received;
    private volatile long handled;
    private long lastStatsAt;

    EddnListener(EnvelopeHandler sightings) {
        this.sightings = sightings;
    }

    public static EddnListener getInstance() {
        if (instance == null) {
            synchronized (EddnListener.class) {
                if (instance == null) {
                    instance = new EddnListener(new EddnSightings());
                }
            }
        }
        return instance;
    }

    public synchronized void start() {
        if (!running.compareAndSet(false, true)) return;
        Thread thread = new Thread(this::run, "eddn-listener");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Stops the loop. The socket is closed by the loop's own thread once its current receive times
     * out, because a ZeroMQ socket must only ever be touched by the thread that made it.
     */
    public synchronized void stop() {
        running.set(false);
    }

    /**
     * How many envelopes have come off the relay and how many were ones this app learns from,
     * since the listener started.
     */
    long received() {
        return received;
    }

    long handled() {
        return handled;
    }

    private void run() {
        long backoff = BACKOFF_INITIAL_MS;
        lastStatsAt = System.currentTimeMillis();
        while (running.get()) {
            try {
                listen();
                backoff = BACKOFF_INITIAL_MS;
            } catch (Exception e) {
                log.info("EDDN listener lost the relay ({}), reconnecting in {}s", e.getMessage(), backoff / 1000);
            }
            if (!running.get()) break;
            try {
                Thread.sleep(backoff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            backoff = Math.min(backoff * 2, BACKOFF_MAX_MS);
        }
        log.info("EDDN listener stopped after {} messages, {} used", received, handled);
    }

    /**
     * One connection's lifetime: until the loop is told to stop, the relay falls silent, or the
     * socket throws. Returning normally or throwing both lead back to the reconnect loop.
     */
    private void listen() {
        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(SocketType.SUB);
            socket.setReceiveTimeOut(RECEIVE_TIMEOUT_MS);
            socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
            socket.connect(RELAY);
            log.info("EDDN listener connected to {}", RELAY);

            long lastMessageAt = System.currentTimeMillis();
            while (running.get()) {
                byte[] compressed = socket.recv(0);
                long now = System.currentTimeMillis();
                if (compressed == null) {
                    if (now - lastMessageAt > SILENCE_BEFORE_RECONNECT_MS) {
                        throw new IllegalStateException("no message for " + (now - lastMessageAt) / 1000 + "s");
                    }
                    continue;
                }
                lastMessageAt = now;
                received++;
                handle(compressed);
                logStats(now);
            }
        }
    }

    /**
     * Inflates and dispatches one message. Faults in a single message are logged at debug and
     * dropped: the feed is other people's data, and one bad upload must not cost the connection.
     */
    void handle(byte[] compressed) {
        try {
            byte[] inflated = inflate(compressed);
            if (inflated == null) return;
            String envelope = new String(inflated, StandardCharsets.UTF_8);
            if (sightings.accept(envelope)) handled++;
        } catch (Exception e) {
            log.debug("EDDN message dropped: {}", e.getMessage());
        }
    }

    private static byte[] inflate(byte[] compressed) throws IOException {
        try (InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(compressed))) {
            byte[] inflated = in.readNBytes(MAX_MESSAGE_BYTES + 1);
            return inflated.length > MAX_MESSAGE_BYTES ? null : inflated;
        }
    }

    private void logStats(long now) {
        if (now - lastStatsAt < STATS_INTERVAL_MS) return;
        lastStatsAt = now;
        log.info("EDDN listener: {} messages received, {} used so far", received, handled);
    }
}
