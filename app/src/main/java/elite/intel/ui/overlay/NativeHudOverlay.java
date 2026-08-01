package elite.intel.ui.overlay;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.ShipManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.NormalizedUserInputEvent;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AiResponseLogEvent;
import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Point;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Owns the native HUD overlay child process and feeds it.
 * <p>
 * The overlay renders in its own process because AWT cannot draw a per-pixel
 * translucent window without strobing on every repaint. The Java side stays a
 * pure producer: it projects app state into {@link HudObjective} cards and
 * conversation lines, writes them as protocol lines to the child's stdin, and
 * knows nothing about rendering.
 * <p>
 * Whole lines are sent, never characters - the overlay owns the typewriter
 * animation, so the effect never depends on pipe timing.
 */
public class NativeHudOverlay {

    private static final Logger log = LogManager.getLogger(NativeHudOverlay.class);
    private static final int OBJECTIVE_POLL_MS = 1000;

    private final List<HudObjectiveSource> sources = new ArrayList<>();
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final ShipManager shipManager = ShipManager.getInstance();

    private Process process;
    private BufferedWriter writer;
    private ScheduledExecutorService objectivePoll;
    private volatile HudObjective lastObjective;

    private final SystemSession systemSession = SystemSession.getInstance();

    private double backgroundAlpha;
    private double fontScale;
    private int width;
    /**
     * Last known screen position, or -1 for "wherever the overlay opens".
     */
    private volatile int windowX = -1;
    private volatile int windowY = -1;

    /**
     * Where the child binary lives. A seam, so tests can point it at nothing.
     */
    private final Supplier<Path> binaryLocator;

    public NativeHudOverlay() {
        this(AppPaths::getOverlayBinary);
    }

    /**
     * Seam for tests.
     */
    NativeHudOverlay(Supplier<Path> binaryLocator) {
        this.binaryLocator = binaryLocator;
        // Order is irrelevant - the highest priority wins each poll - but a
        // source that has nothing to say returns empty and never competes.
        sources.add(new MassacreObjectiveSource());
        sources.add(new MissionObjectiveSource());
        sources.add(new TradeRouteObjectiveSource());
        sources.add(new MonetizedRouteObjectiveSource());
        sources.add(new ExobiologyObjectiveSource());

        SystemSession.HudOverlayLayout stored = systemSession.getHudOverlayLayout();
        backgroundAlpha = stored.alpha();
        // A stored 0 means the commander never chose a size, so keep deriving it
        // from the screen: a 4K display should not open at a 1080p size just
        // because the column exists now.
        fontScale = stored.fontScale() > 0 ? stored.fontScale() : defaultFontScale();
        width = stored.width() > 0 ? stored.width() : 760;
        windowX = stored.x();
        windowY = stored.y();
    }

    public void addSource(HudObjectiveSource source) {
        sources.add(source);
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    // -- lifecycle -----------------------------------------------------------

    /**
     * Spawns the overlay. Returns false (and logs) when the binary is missing or
     * cannot start, so the caller can leave its toggle switched off rather than
     * pretending the overlay is up.
     */
    public synchronized boolean start() {
        if (isRunning()) return true;

        Path binary = binaryLocator.get();
        if (!Files.isRegularFile(binary)) {
            log.warn("HUD overlay binary not found: {}", binary);
            return false;
        }
        if (!ensureExecutable(binary)) return false;
        try {
            ProcessBuilder pb = new ProcessBuilder(binary.toString());
            pb.redirectErrorStream(false);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            process = pb.start();
            writer = new BufferedWriter(new OutputStreamWriter(
                    process.getOutputStream(), StandardCharsets.UTF_8));

            send(OverlayProtocol.handshake());
            send(OverlayProtocol.config(backgroundAlpha, fontScale, width));
            if (windowX >= 0 || windowY >= 0) send(OverlayProtocol.position(windowX, windowY));
            lastObjective = null;
            startPositionReader();

            // Two buses on purpose: commander speech (NormalizedUserInputEvent)
            // travels on the game bus, the AI's reply (AiResponseLogEvent) on the
            // UI bus. Registering on only one silently drops half the exchange.
            GameEventBus.register(this);
            UiBus.register(this);
            startObjectivePolling();
            log.info("HUD overlay started: {}", binary);
            return true;
        } catch (IOException e) {
            log.error("Failed to start HUD overlay: {}", e.getMessage());
            process = null;
            writer = null;
            return false;
        }
    }

    /**
     * Restores the executable bit if it was lost in packaging.
     * <p>
     * install4j copies distribution/ as data files, so the overlay arrives
     * mode 644 next to the models - and extracting the auto-update ZIP loses the
     * bit again on every update. Setting it here fixes both, and also repairs
     * installations that already went out without it. A failure is reported
     * rather than swallowed, because the alternative is a toggle that silently
     * does nothing.
     */
    private boolean ensureExecutable(Path binary) {
        if (Files.isExecutable(binary)) return true;
        try {
            Set<PosixFilePermission> perms = new HashSet<>(Files.getPosixFilePermissions(binary));
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.GROUP_EXECUTE);
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(binary, perms);
            log.info("Restored executable bit on HUD overlay binary: {}", binary);
            return true;
        } catch (UnsupportedOperationException e) {
            // Non-POSIX filesystem (Windows): isExecutable already decided.
            log.warn("HUD overlay binary is not executable and permissions cannot be set: {}", binary);
            return false;
        } catch (IOException e) {
            log.error("Cannot make HUD overlay binary executable ({}): {}", binary, e.getMessage());
            return false;
        }
    }

    public synchronized void stop() {
        if (objectivePoll != null) {
            // Not awaited: a poll already in flight may be blocked on this very
            // monitor inside send(), and waiting for it here would deadlock until
            // the timeout. It writes into a writer this method is about to clear,
            // and send() re-checks that under the same lock, so a late poll is a
            // no-op rather than a write to a dead pipe.
            objectivePoll.shutdownNow();
            objectivePoll = null;
        }
        unregisterBuses();
        if (process != null) {
            send(OverlayProtocol.quit());
            closeWriter();
            // The child exits on QUIT or on EOF; destroy() is the backstop for a
            // wedged process so a stale overlay can never outlive the app.
            if (!waitForExit()) process.destroy();
            process = null;
        }
        writer = null;
        lastObjective = null;
    }

    /**
     * Unregistering is idempotent, so a double stop() is harmless.
     */
    private void unregisterBuses() {
        unregisterFromGameBus(this);
        try {
            UiBus.unregister(this);
        } catch (RuntimeException e) {
            log.debug("Overlay already unregistered from UI bus: {}", e.getMessage());
        }
    }

    private static void unregisterFromGameBus(Object listener) {
        try {
            GameEventBus.unregister(listener);
        } catch (RuntimeException e) {
            log.debug("Overlay listener already unregistered from game bus: {}", e.getMessage());
        }
    }

    private boolean waitForExit() {
        try {
            return process.waitFor(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void closeWriter() {
        if (writer == null) return;
        try {
            writer.close();
        } catch (IOException e) {
            log.debug("Closing overlay stdin failed: {}", e.getMessage());
        }
    }

    /**
     * Reads the overlay's stdout so a dragged window is remembered.
     * <p>
     * The overlay is dragged with the mouse, so the app cannot know where it ended
     * up unless the overlay says so. It reports its position when a drag finishes;
     * this thread is also what keeps that pipe drained, which matters whether or
     * not anything is listening - an unread stdout pipe eventually fills and
     * blocks the child mid-draw.
     * <p>
     * Unknown lines are ignored, so an older binary that reports nothing, or a
     * newer one that reports more, both behave.
     */
    private void startPositionReader() {
        Process current = process;
        Thread reader = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(current.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    OverlayProtocol.parsePosition(line).ifPresent(this::rememberPosition);
                }
            } catch (IOException e) {
                log.debug("HUD overlay output closed: {}", e.getMessage());
            }
        }, "hud-overlay-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void rememberPosition(Point position) {
        windowX = position.x;
        windowY = position.y;
        saveLayout();
    }

    // -- settings ------------------------------------------------------------

    public void setBackgroundAlpha(double alpha) {
        this.backgroundAlpha = Math.max(0, Math.min(1, alpha));
        send(OverlayProtocol.config(backgroundAlpha, fontScale, width));
        saveLayout();
    }

    public double getBackgroundAlpha() {
        return backgroundAlpha;
    }

    public void setFontScale(double scale) {
        this.fontScale = Math.max(0.5, Math.min(3, scale));
        send(OverlayProtocol.config(backgroundAlpha, fontScale, width));
        saveLayout();
    }

    /**
     * Stores the whole layout, on every change rather than at shutdown: the app
     * can be killed with the game, and a setting the commander adjusted and then
     * lost is worse than a spare row write.
     */
    private void saveLayout() {
        systemSession.setHudOverlayLayout(
                new SystemSession.HudOverlayLayout(backgroundAlpha, fontScale, width, windowX, windowY));
    }

    public double getFontScale() {
        return fontScale;
    }

    /**
     * 1.0 is calibrated for 1440p, so 1080p lands near 0.75 and 4K near 1.5.
     */
    private static double defaultFontScale() {
        int height = Toolkit.getDefaultToolkit().getScreenSize().height;
        return Math.max(0.75, Math.min(2.0, height / 1440d));
    }

    // -- feed ----------------------------------------------------------------

    @Subscribe
    public void onUserInput(NormalizedUserInputEvent event) {
        if (event.getText() == null || event.getText().isBlank()) return;
        send(OverlayProtocol.say(playerSession.getPlayerName(), event.getText(), false));
    }

    @Subscribe
    public void onAiResponse(AiResponseLogEvent event) {
        if (event.getData() == null || event.getData().isBlank()) return;
        String speaker = shipManager.getShip() == null ? "AI" : shipManager.getShip().getShipName();
        send(OverlayProtocol.say(speaker, event.getData(), true));
    }

    /**
     * Polls off the EDT, on one daemon thread of its own.
     * <p>
     * WHY not a Swing timer: every source reads the database - missions, bounties,
     * routes, scanned bodies, samples - so a poll is a handful of SQLite round
     * trips, some of them scans of tables that only grow with playtime. On the
     * EDT that cost lands on the commander's UI once a second, and grows
     * invisibly. Nothing here touches a Swing component, and {@link #send} is
     * synchronized, so the work has no business being on the EDT at all.
     */
    private void startObjectivePolling() {
        objectivePoll = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "hud-overlay-objectives");
            thread.setDaemon(true);
            return thread;
        });
        objectivePoll.scheduleWithFixedDelay(
                this::pushObjectiveIfChanged, 0, OBJECTIVE_POLL_MS, TimeUnit.MILLISECONDS);
    }

    private void pushObjectiveIfChanged() {
        // Deliberate catch-all boundary: a scheduled task that throws is silently
        // cancelled, so one bad row in one source would stop the card updating for
        // the rest of the session with nothing on screen to say why. Log it and
        // keep polling; the next poll re-derives from scratch anyway.
        try {
            pushObjective();
        } catch (RuntimeException e) {
            log.warn("HUD overlay objective poll failed, continuing: {}", e.toString());
        }
    }

    private void pushObjective() {
        HudObjective next = pollObjective().orElse(null);
        // Only write on change: an idle overlay costs nothing on the pipe.
        if (Objects.equals(next, lastObjective)) return;
        lastObjective = next;
        if (next == null) {
            send(OverlayProtocol.clearObjective());
        } else {
            OverlayProtocol.objective(next).forEach(this::send);
        }
    }

    private Optional<HudObjective> pollObjective() {
        return highestPriority(sources);
    }

    /**
     * The one card to show. Only one fits on screen, so this is where a
     * volunteered objective loses to work the commander actually took on - an
     * accepted mission beats an exobiology sampling list, whatever else is
     * happening. Ties keep the earlier source, so registration order is the
     * tie-break.
     */
    static Optional<HudObjective> highestPriority(List<HudObjectiveSource> sources) {
        return sources.stream()
                .map(HudObjectiveSource::currentObjective)
                .flatMap(Optional::stream)
                .max(Comparator.comparingInt(HudObjective::priority));
    }

    // -- transport -----------------------------------------------------------

    private synchronized void send(String line) {
        if (writer == null || !isRunning()) return;
        try {
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            // A broken pipe means the overlay died; tear down so the toggle can
            // start a fresh one rather than writing into a dead process.
            log.warn("HUD overlay pipe closed, shutting it down: {}", e.getMessage());
            writer = null;
            // shutdown(), not shutdownNow(): this can be reached from the poll
            // thread itself, which must be allowed to finish rather than
            // interrupt itself mid-teardown.
            if (objectivePoll != null) objectivePoll.shutdown();
            unregisterBuses();
        }
    }
}
