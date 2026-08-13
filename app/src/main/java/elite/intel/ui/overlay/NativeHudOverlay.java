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

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Owns the native HUD overlay child processes and feeds them.
 * <p>
 * The overlay renders in its own process because AWT cannot draw a per-pixel
 * translucent window without strobing on every repaint. The Java side stays a
 * pure producer: it projects app state into {@link HudObjective} cards and
 * conversation lines, writes them as protocol lines to the children's stdin, and
 * knows nothing about rendering.
 * <p>
 * Whole lines are sent, never characters - the overlay owns the typewriter
 * animation, so the effect never depends on pipe timing.
 * <p>
 * There can be more than one child: {@link HudDisplayMode#BOTH} runs a desktop
 * overlay and a VR overlay side by side. They are separate processes rather than
 * one process drawing twice, which keeps them crash-isolated and costs only a
 * second write of each line - both render from identical data, so they cannot
 * drift apart.
 */
public class NativeHudOverlay {

    private static final Logger log = LogManager.getLogger(NativeHudOverlay.class);
    private static final int OBJECTIVE_POLL_MS = 1000;

    private final List<HudObjectiveSource> sources = new ArrayList<>();
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final ShipManager shipManager = ShipManager.getInstance();

    private final List<Child> children = new ArrayList<>();
    private ScheduledExecutorService objectivePoll;
    private volatile HudObjective lastObjective;
    private HudDisplayMode displayMode = HudDisplayMode.DESKTOP;

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
     * Where the card hangs in the headset. Ignored by a desktop child, which is
     * placed by dragging it.
     */
    private HudVrPosition vrPosition = HudVrPosition.DEFAULT;

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
        // The highest priority wins each poll, and a source with nothing to say
        // returns empty and never competes - so order only decides ties. Mining
        // is listed ahead of exobiology because both are ambient: a commander in
        // a ring is working the ring, not the sampling list of the system it
        // happens to sit in.
        sources.addAll(defaultSources());

        SystemSession.HudOverlayLayout stored = systemSession.getHudOverlayLayout();
        backgroundAlpha = stored.alpha();
        // A stored 0 means the commander never chose a size, so keep deriving it
        // from the screen: a 4K display should not open at a 1080p size just
        // because the column exists now.
        fontScale = stored.fontScale() > 0 ? stored.fontScale() : defaultFontScale();
        width = stored.width() > 0 ? stored.width() : 760;
        windowX = stored.x();
        windowY = stored.y();
        displayMode = parseDisplayMode(stored.displayMode());
        vrPosition = HudVrPosition.parse(stored.vrPosition());
    }

    /**
     * Reads the stored mode leniently: anything unrecognised, including null from
     * a row written before the column existed, means the desktop overlay.
     * <p>
     * A bad value here must not cost the commander their overlay, and falling
     * back to DESKTOP is the one answer that always leaves something on screen.
     */
    static HudDisplayMode parseDisplayMode(String stored) {
        if (stored == null) return HudDisplayMode.DESKTOP;
        try {
            return HudDisplayMode.valueOf(stored.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("Unknown HUD overlay display mode {}, using DESKTOP", stored);
            return HudDisplayMode.DESKTOP;
        }
    }

    public void addSource(HudObjectiveSource source) {
        sources.add(source);
    }

    public synchronized boolean isRunning() {
        return children.stream().anyMatch(child -> child.process().isAlive());
    }

    /**
     * One overlay process and the pipe into it.
     * <p>
     * The role is for the log only - it is what tells a commander reading a log
     * which of two overlays reported something, or which one went away.
     */
    private record Child(String role, Process process, BufferedWriter writer) {
    }

    /**
     * What to spawn for a display mode. Package-private and pure so the mapping
     * can be tested without starting a process.
     */
    record ChildSpec(String role, List<String> command) {
    }

    /**
     * The VR child of {@link HudDisplayMode#BOTH} gets {@code --vr=only}, which
     * exits rather than falling back to a window: two desktop overlays would
     * stack exactly on top of each other and the commander would drag one while
     * the other stayed put. A lone VR child instead gets {@code --vr=on}, where
     * falling back to a window is the friendly answer - some overlay beats none.
     */
    static List<ChildSpec> childSpecs(Path binary, HudDisplayMode mode) {
        String bin = binary.toString();
        return switch (mode) {
            case DESKTOP -> List.of(new ChildSpec("desktop", List.of(bin)));
            case VR -> List.of(new ChildSpec("vr", List.of(bin, "--vr=on")));
            case BOTH -> List.of(
                    new ChildSpec("desktop", List.of(bin)),
                    new ChildSpec("vr", List.of(bin, "--vr=only")));
            // No --vr at all: this child never probes SteamVR, because the
            // headset is the capture tool's problem and not ours.
            case CAPTURE_WINDOW -> List.of(new ChildSpec("capture", List.of(bin, "--capture")));
        };
    }

    // -- lifecycle -----------------------------------------------------------

    /**
     * Spawns the overlay. Returns false (and logs) when the binary is missing or
     * no child could be started, so the caller can leave its toggle switched off
     * rather than pretending the overlay is up.
     * <p>
     * In {@link HudDisplayMode#BOTH} one child starting is enough to report
     * success: on a machine with no SteamVR the VR child exits immediately and
     * the desktop overlay is the whole overlay, which is exactly the intent.
     */
    public synchronized boolean start() {
        if (isRunning()) return true;

        Path binary = binaryLocator.get();
        if (!Files.isRegularFile(binary)) {
            log.warn("HUD overlay binary not found: {}", binary);
            return false;
        }
        if (!ensureExecutable(binary)) return false;

        children.clear();
        for (ChildSpec spec : childSpecs(binary, displayMode)) {
            spawn(spec).ifPresent(children::add);
        }
        if (children.isEmpty()) return false;

        send(OverlayProtocol.handshake());
        send(OverlayProtocol.config(backgroundAlpha, fontScale, width));
        send(OverlayProtocol.vrPosition(vrPosition));
        if (windowX >= 0 || windowY >= 0) send(OverlayProtocol.position(windowX, windowY));
        lastObjective = null;
        children.forEach(this::startOutputReader);
        children.forEach(this::startErrorReader);

        // Two buses on purpose: commander speech (NormalizedUserInputEvent)
        // travels on the game bus, the AI's reply (AiResponseLogEvent) on the
        // UI bus. Registering on only one silently drops half the exchange.
        GameEventBus.register(this);
        UiBus.register(this);
        startObjectivePolling();
        log.info("HUD overlay started: {} ({})", binary, displayMode);
        return true;
    }

    private Optional<Child> spawn(ChildSpec spec) {
        try {
            ProcessBuilder pb = new ProcessBuilder(spec.command());
            pb.redirectErrorStream(false);
            // PIPE, not DISCARD. The overlay writes everything it knows about a
            // failing SteamVR to stderr - which init error, which interface
            // version is missing, which EVROverlayError the compositor refused a
            // frame with - and discarding it meant a commander reporting "the VR
            // HUD flickers" could send us a log with nothing in it but the mode
            // line. See startErrorReader for the obligation that comes with this.
            pb.redirectError(ProcessBuilder.Redirect.PIPE);
            Process process = pb.start();
            return Optional.of(new Child(spec.role(), process, new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))));
        } catch (IOException e) {
            log.error("Failed to start {} HUD overlay: {}", spec.role(), e.getMessage());
            return Optional.empty();
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
        if (!children.isEmpty()) {
            send(OverlayProtocol.quit());
            for (Child child : children) {
                closeWriter(child);
                // A child exits on QUIT or on EOF; destroy() is the backstop for
                // a wedged one, so a stale overlay can never outlive the app.
                if (!waitForExit(child)) child.process().destroy();
            }
            children.clear();
        }
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

    private boolean waitForExit(Child child) {
        try {
            return child.process().waitFor(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void closeWriter(Child child) {
        try {
            child.writer().close();
        } catch (IOException e) {
            log.debug("Closing {} overlay stdin failed: {}", child.role(), e.getMessage());
        }
    }

    /**
     * Reads a child's stdout so a dragged window is remembered.
     * <p>
     * The overlay is dragged with the mouse, so the app cannot know where it ended
     * up unless the overlay says so. It reports its position when a drag finishes;
     * this thread is also what keeps that pipe drained, which matters whether or
     * not anything is listening - an unread stdout pipe eventually fills and
     * blocks the child mid-draw. Every child gets a reader for that reason, even
     * the VR one, which has no window to drag and so never reports a position.
     * <p>
     * Unknown lines are ignored, so an older binary that reports nothing, or a
     * newer one that reports more, both behave.
     */
    private void startOutputReader(Child child) {
        Thread reader = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(child.process().getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    Optional<Point> position = OverlayProtocol.parsePosition(line);
                    if (position.isPresent()) rememberPosition(position.get());
                    else if (line.startsWith("MODE\t")) logMode(child, line);
                }
            } catch (IOException e) {
                log.debug("HUD overlay ({}) output closed: {}", child.role(), e.getMessage());
            }
        }, "hud-overlay-reader-" + child.role());
        reader.setDaemon(true);
        reader.start();
    }

    /**
     * Copies the child's stderr into the app log, one line at a time.
     * <p>
     * This thread is not optional once stderr is a pipe rather than DISCARD: a
     * pipe nobody reads fills its buffer and then blocks the child inside
     * fprintf, which for the VR shell means it stops drawing and stops draining
     * its own stdin - the overlay would freeze on whatever frame it had, for the
     * one reason hardest to guess from the outside. The same rule already applies
     * to stdout, and for the same reason; see {@link #startOutputReader}.
     * <p>
     * Logged at info, at the same level as the mode line, because this channel
     * carries both - which SteamVR interface we attached to as readily as which
     * error a frame was refused with. Every line is edge-triggered, written once
     * per occurrence and never per frame, and every one is prefixed "overlay: "
     * by the child, so the whole VR story greps out of a commander's log in one
     * pass whether it ends in a failure or not.
     */
    private void startErrorReader(Child child) {
        Thread reader = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(child.process().getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (!line.isBlank()) log.info("HUD overlay ({}): {}", child.role(), line);
                }
            } catch (IOException e) {
                log.debug("HUD overlay ({}) stderr closed: {}", child.role(), e.getMessage());
            }
        }, "hud-overlay-stderr-" + child.role());
        reader.setDaemon(true);
        reader.start();
    }

    /**
     * Records which shell the child actually came up in, and why, when VR was
     * asked for and could not be had.
     * <p>
     * This is the one line that says what a commander is actually looking at, so
     * it is what somebody reporting "VR does nothing" can be asked to send. The
     * detail behind it now arrives on stderr as well; see
     * {@link #startErrorReader}.
     */
    private void logMode(Child child, String line) {
        log.info("HUD overlay ({}): {}", child.role(), line.replace('\t', ' '));
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
        systemSession.setHudOverlayLayout(new SystemSession.HudOverlayLayout(
                backgroundAlpha, fontScale, width, windowX, windowY,
                displayMode.name(), vrPosition.name()));
    }

    public double getFontScale() {
        return fontScale;
    }

    public HudDisplayMode getDisplayMode() {
        return displayMode;
    }

    public HudVrPosition getVrPosition() {
        return vrPosition;
    }

    /**
     * Moves the card in the headset. Applied live, unlike the display mode:
     * placement is a line on the wire, not a decision made when a child is
     * spawned, so the commander can try directions against the cockpit they are
     * flying instead of restarting the overlay for each one.
     */
    public synchronized void setVrPosition(HudVrPosition position) {
        if (position == null || position == vrPosition) return;
        vrPosition = position;
        send(OverlayProtocol.vrPosition(vrPosition));
        saveLayout();
    }

    /**
     * Changes where the HUD is drawn, restarting the overlay if it is up.
     * <p>
     * A restart is unavoidable: which shell a child runs is fixed when it is
     * spawned. It is also the whole cost - only the overlay children go down and
     * come back, never the speech, LLM or journal services.
     */
    public synchronized void setDisplayMode(HudDisplayMode mode) {
        if (mode == null || mode == displayMode) return;
        displayMode = mode;
        saveLayout();
        if (!isRunning()) return;
        stop();
        start();
    }

    /**
     * 1.0 is calibrated for 1440p, so 1080p lands near 0.75 and 4K near 1.5.
     * <p>
     * With no display there is no height to scale from, and asking anyway throws:
     * a headless build server constructs this class to test the child-process
     * lifecycle, which has nothing to do with screens. 1.0 is the calibration
     * point, so it is the honest answer when the question cannot be asked.
     */
    private static double defaultFontScale() {
        if (GraphicsEnvironment.isHeadless()) return 1.0;
        int height = Toolkit.getDefaultToolkit().getScreenSize().height;
        return Math.max(0.75, Math.min(2.0, height / 1440d));
    }

    // -- feed ----------------------------------------------------------------

    @Subscribe
    public void onUserInput(NormalizedUserInputEvent event) {
        if (event.getText() == null || event.getText().isBlank()) return;
        send(OverlayProtocol.say(playerSession.getPlayerName(), event.getText(),
                OverlayProtocol.Speaker.COMMANDER));
    }

    @Subscribe
    public void onAiResponse(AiResponseLogEvent event) {
        if (event.getData() == null || event.getData().isBlank()) return;
        // A named speaker is a radio transmission: the AI is the only voice that speaks unattributed.
        OverlayProtocol.Speaker kind = isRadio(event)
                ? OverlayProtocol.Speaker.RADIO
                : OverlayProtocol.Speaker.AI;
        send(OverlayProtocol.say(speakerOf(event), event.getData(), kind));
    }

    private static boolean isRadio(AiResponseLogEvent event) {
        return event.getSpeaker() != null && !event.getSpeaker().isBlank();
    }

    /**
     * The ship speaks for the AI, but a radio transmission is somebody else on the
     * channel - a station, a carrier, another commander - so it is named after its
     * own source. Attributing it to the ship put words in the AI's mouth that it
     * never said.
     */
    private String speakerOf(AiResponseLogEvent event) {
        if (isRadio(event)) return event.getSpeaker();
        return shipManager.getShip() == null ? "AI" : shipManager.getShip().getShipName();
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
    /**
     * The sources the overlay runs, in the order they compete. Priority decides first; this order is only the
     * tie-break, which is what separates the equally-ranked ambient cards from each other. Visible so a test
     * can assert the ladder against the real order instead of a copy that can drift from it.
     */
    static List<HudObjectiveSource> defaultSources() {
        return List.of(
                new MassacreObjectiveSource(),
                new MissionObjectiveSource(),
                new TradeRouteObjectiveSource(),
                new MonetizedRouteObjectiveSource(),
                new MiningObjectiveSource(),
                new ExobiologyObjectiveSource(),
                // Last and weakest: where the commander is pointed, which is worth showing when nothing
                // else is and worth nothing next to what they are actually doing. A destination the app
                // worked out (material trader, broker, factors) enriches this card rather than competing
                // with it, so a stale errand can never claim the screen on its own.
                new ShipRouteObjectiveSource());
    }

    static Optional<HudObjective> highestPriority(List<HudObjectiveSource> sources) {
        return sources.stream()
                .map(HudObjectiveSource::currentObjective)
                .flatMap(Optional::stream)
                .max(Comparator.comparingInt(HudObjective::priority));
    }

    // -- transport -----------------------------------------------------------

    private synchronized void send(String line) {
        if (children.isEmpty()) return;

        List<Child> lost = new ArrayList<>();
        for (Child child : children) {
            try {
                child.writer().write(line);
                child.writer().newLine();
                child.writer().flush();
            } catch (IOException e) {
                // A broken pipe means that child died. In BOTH mode this is the
                // ordinary outcome for the VR child on a machine with no
                // SteamVR, so it costs the desktop overlay nothing: only the
                // dead child is dropped, and the survivors keep being fed.
                log.info("HUD overlay ({}) pipe closed, dropping it: {}", child.role(), e.getMessage());
                lost.add(child);
            }
        }
        if (lost.isEmpty()) return;

        children.removeAll(lost);
        if (children.isEmpty()) shutdownAfterLastChild();
    }

    /**
     * Tears down the feed once no child is left to read it, so the toggle can
     * start a fresh overlay rather than writing into dead processes.
     */
    private void shutdownAfterLastChild() {
        log.warn("No HUD overlay process left; shutting the feed down");
        // shutdown(), not shutdownNow(): this can be reached from the poll
        // thread itself, which must be allowed to finish rather than interrupt
        // itself mid-teardown.
        if (objectivePoll != null) objectivePoll.shutdown();
        unregisterBuses();
    }
}
