package elite.intel.ai.hands;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.managers.BindingConflictManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.DataDirectoryValidator;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.BindingsUpdatedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static elite.intel.util.StringUtls.humanizeBindingName;
import static elite.intel.util.StringUtls.localizedSpeech;

/**
 * The BindingsMonitor class is responsible for monitoring changes to
 * key bindings files in the specified directory and updating the internal
 * bindings map accordingly. It continuously monitors the target directory
 * for file events and processes changes to ensure the bindings remain up to
 * date.
 * <p>
 * This class relies on the {@link KeyBindingsParser} to parse the contents of
 * key
 * bindings files and determine the mapping of actions to key bindings.
 * <p>
 * Features:
 * - Monitors a directory for changes to "*.binds" files.
 * - Automatically reloads and parses bindings when a new or modified file is
 * detected.
 * - Provides access to the current bindings map.
 * <p>
 * Thread Safety:
 * - This class uses synchronization to ensure thread-safe access to start and
 * stop
 * monitoring operations.
 * <p>
 * Logging:
 * - Uses SLF4J for logging to provide information on status, errors, and events
 * during monitoring.
 * <p>
 * Exceptions:
 * - Captures and logs IOExceptions, InterruptedExceptions, and other unexpected
 * errors
 * during the monitoring process.
 */
public class BindingsMonitor {
    private static final Logger log = LogManager.getLogger(BindingsMonitor.class);
    private static volatile BindingsMonitor instance;
    private final KeyBindingsParser parser;
    private final BindingsLoader bindingsLoader = new BindingsLoader();
    private final BindingConflictManager conflictManager = BindingConflictManager.getInstance();
    private Path bindingsDir;
    // Written by the monitor thread's initial parse and read by callers on other threads
    // (KeyBindCheck at startup, command execution), so publication must be visible.
    private volatile Map<String, KeyBindingsParser.KeyBinding> bindings;
    private File currentBindsFile;
    private Thread processingThread;
    private volatile boolean running;
    /**
     * Identity of the file contents behind the current {@link #bindings}, so the watch loop can tell a
     * second notification about a write it already read from a genuinely new one. Elite writes the
     * .binds file more than once per save and each write arrives as its own ENTRY_MODIFY, which without
     * this re-parses an identical file, republishes {@link BindingsUpdatedEvent} and prints a second
     * "Key bindings updated" line - noise in the middle of the one activity that generates these events,
     * a commander sitting in the controls menu rebinding.
     */
    private String parsedFileFingerprint;

    /**
     * The Elite controls a built-in command presses - {@link Bindings.GameCommand#isDrivenByApp()}.
     * The single source for both things this class reports on: which unbound controls are worth naming
     * at startup, and which conflicts are worth interrupting the commander about.
     * <p>
     * Deliberately NOT the whole {@code GameCommand} list. That list is Elite's entire control set, carried
     * so the bindings editor can show every control and a custom command step can name any of them; measured
     * against a commander's own binds file it calls well over a hundred controls missing - emotes, vanity
     * cameras, Galnet audio, turret pitch - and flags every vanilla-vs-vanilla overlap between them. A
     * commander rebinding their controls then heard "a hundred and forty-two required bindings unassigned",
     * and read a wall of names that buried the seventeen actually stopping a command from working.
     * <p>
     * A binding a <em>custom</em> command taps is not included: the commander is told about that one by name
     * at the moment the sequence runs (see {@code InputSequenceExecutor#handleNoKeyBindingFound}), which is
     * both later and more precise than a startup list, and reaching the custom command registry from here
     * would point {@code ai.hands} back at {@code ai.brain}. The bindings panel still shows every control and
     * every conflict, which is where a commander goes looking for the full picture.
     */
    private static final Set<String> APP_DRIVEN_ACTIONS = appDrivenActions();

    private static Set<String> appDrivenActions() {
        Set<String> actions = new LinkedHashSet<>();
        for (Bindings.GameCommand cmd : Bindings.GameCommand.values()) {
            if (cmd.isDrivenByApp()) {
                actions.add(cmd.getGameBinding());
            }
        }
        return actions;
    }

    private BindingsMonitor() {
        this.parser = KeyBindingsParser.getInstance();

    }

    public static BindingsMonitor getInstance() {
        if (instance == null) {
            synchronized (BindingsMonitor.class) {
                if (instance == null) {
                    instance = new BindingsMonitor();
                }
            }
        }
        return instance;
    }

    public synchronized void startMonitoring() throws IOException {
        this.bindingsDir = PlayerSession.getInstance().getBindingsDir();
        if (processingThread != null && processingThread.isAlive()) {
            log.warn("BindingsMonitor is already running");
            return;
        }
        running = true;
        processingThread = new Thread(this::monitorBindings, "BindingsMonitorThread");
        processingThread.start();
        log.info("BindingsMonitor started");
    }

    public synchronized void stopMonitoring() {
        if (processingThread == null || !processingThread.isAlive()) {
            log.warn("BindingsMonitor is not running");
            return;
        }
        running = false;
        processingThread.interrupt();
        try {
            processingThread.join(5000); // Wait up to 5 seconds for clean shutdown
            log.info("BindingsMonitor stopped");
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for BindingsMonitor to stop", e);
            Thread.currentThread().interrupt(); // Restore interrupted status
        }
        processingThread = null;
    }

    private void monitorBindings() {
        DataDirectoryValidator.validateAndWarn(bindingsDir, DataDirectoryValidator.DirectoryKind.BINDINGS);
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            bindingsDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE);
            log.info("Monitoring key bindings in directory: {}", bindingsDir);

            // Initial parse of bindings
            parseAndUpdateBindings();

            while (running) {
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                if (key == null) {
                    if (Thread.currentThread().isInterrupted() || !running) {
                        log.info("Shutting down BindingsMonitor due to interruption or stop signal");
                        return;
                    }
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.ENTRY_MODIFY || kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path changed = (Path) event.context();
                        if (changed.toString().endsWith(".binds")) {
                            File activeFile = bindingsLoader.getLatestBindsFile();
                            boolean activeFileWasModified = activeFile.getName().equals(changed.toString());
                            boolean activeFileChanged = !activeFile.equals(currentBindsFile);
                            if (activeFileWasModified || activeFileChanged) {
                                currentBindsFile = activeFile;
                                Thread.sleep(300); // wait for the game to finish writing
                                if (fingerprintOf(activeFile).equals(parsedFileFingerprint)) {
                                    log.debug("Ignoring repeat change event for unchanged file: {}",
                                            activeFile.getName());
                                } else {
                                    parseAndUpdateBindings();
                                    log.info("Reloaded bindings from: {}", currentBindsFile.getName());
                                }
                            }
                        }
                    }
                }
                checkForConflictsAndPersist();
                boolean valid = key.reset();
                if (!valid) {
                    log.error("Watch key no longer valid; directory may be inaccessible");
                    GameEventBus.publish(new AiVoxResponseEvent(localizedSpeech("speech.warning.bindingsDirectoryInaccessible")));
                    break;
                }

            }
        } catch (IOException e) {
            log.error("IOException in BindingsMonitor", e);
            UiBus.publish(new AppLogEvent("Please check the bindings directory. Stopping services."));
        } catch (InterruptedException e) {
            log.info("BindingsMonitor interrupted, shutting down");
            Thread.currentThread().interrupt(); // Restore interrupted status
        } catch (Exception e) {
            log.error("Unexpected error in BindingsMonitor", e);
        }
    }

    private void parseAndUpdateBindings() {
        try {
            currentBindsFile = bindingsLoader.getLatestBindsFile();
            bindings = parser.parseBindings(currentBindsFile);
            parsedFileFingerprint = fingerprintOf(currentBindsFile);
            GameEventBus.publish(
                    new AppLogEvent("SYSTEM: Key bindings updated from file " + currentBindsFile.getAbsolutePath()));
            UiBus.publish(new BindingsUpdatedEvent());
            log.info("Key bindings updated from: {}", currentBindsFile.getName());
        } catch (Exception e) {
            log.error("Failed to parse key bindings from: {}",
                    currentBindsFile != null ? currentBindsFile.getName() : "null", e);
            GameEventBus.publish(
                    new AiVoxResponseEvent(localizedSpeech("speech.warning.bindingsUpdateFailed")));
        }
    }

    /**
     * Last-modified time (to the finest resolution the filesystem reports) plus size - enough to tell
     * two writes apart without reading the file, and cheap enough to run on every change event.
     * An unreadable file yields a value that matches nothing, so the caller parses and reports the
     * real failure rather than silently skipping.
     */
    private String fingerprintOf(File file) {
        try {
            Path path = file.toPath();
            return Files.getLastModifiedTime(path) + ":" + Files.size(path);
        } catch (IOException e) {
            log.debug("Could not fingerprint bindings file {}; treating it as changed", file.getName(), e);
            return "unreadable:" + System.nanoTime();
        }
    }

    public Map<String, KeyBindingsParser.KeyBinding> getBindings() {
        return bindings;
    }

    /**
     * Parses the active binds file now if the monitor's own initial parse has not landed yet.
     * <p>
     * {@link #startMonitoring()} does that first parse on the monitor thread, after registering a
     * WatchService, so a caller that runs immediately after service start - {@link KeyBindCheck} -
     * would otherwise read a null map, skip silently, and never tell the commander which bindings
     * are missing. Deliberately unsynchronized: {@code stopMonitoring()} joins the monitor thread
     * while holding the instance lock, so taking that lock on a parse path could deadlock. A rare
     * duplicate parse is harmless - both produce the same map.
     */
    public void ensureBindingsLoaded() {
        if (bindings == null) {
            log.info("Bindings not parsed yet; parsing on demand before the missing-binding check");
            parseAndUpdateBindings();
        }
    }

    public File getCurrentBindsFile() {
        return currentBindsFile;
    }

    /**
     * Returns the file currently being monitored, falling back to a fresh
     * {@link BindingsLoader#getLatestBindsFile()} lookup if monitoring hasn't started or hasn't
     * found one yet. Shared by anything that needs "the active game binds file" outside the
     * monitoring loop itself (e.g. {@code BindingProfilePanel}, restore-to-live).
     */
    public File resolveActiveBindsFile() throws IOException {
        return currentBindsFile != null ? currentBindsFile : bindingsLoader.getLatestBindsFile();
    }

    /**
     * Every <b>blocking</b> conflict currently in the binds file - the overlaps that stop EliteIntel
     * driving the game outright (see {@link BindingConflictRules#isBlocking}). Returned whole, chord
     * included, because the warning names the keys the commander actually has to move.
     * <p>
     * A pure read with no persistence diff, deliberately: {@link #checkForConflictsAndPersist()} tells the
     * commander about a conflict exactly once and is then silent forever, which is right for "this control
     * may misbehave" and wrong for "route plotting cannot work". A commander who heard the line once, months
     * ago, keeps a permanently broken setup and no longer has any way to find out - which is how a field
     * report of "route plotting does not work for me" reached us with all four map/UI overlaps sitting in the
     * file and not one word about them in the diagnostics bundle.
     */
    public List<BindingConflictScanner.Conflict> blockingConflicts() {
        return detectConflicts().stream()
                .filter(BindingConflictScanner.Conflict::blocking)
                .toList();
    }

    /**
     * UI direction keys that a focused Elite text field would swallow as text - see
     * {@link UiNavigationTextTrap}.
     * <p>
     * A pure read announced on every start, for the same reason as {@link #blockingConflicts()}: it stops
     * route plotting outright rather than degrading it, and the commander cannot discover it by playing
     * because by hand they click the search result with the mouse.
     */
    public List<UiNavigationTextTrap.TrappedBinding> textTrappedUiNavigation() {
        return UiNavigationTextTrap.scan(getBindings());
    }

    /**
     * Controls already bound to a key or chord that must never be assigned - the key the commander has
     * on the game menu, or an OS combination like Alt+F4. See {@link ReservedKeyChords}.
     * <p>
     * A pure read announced on every start, for the same reason as {@link #blockingConflicts()}: it is a
     * permanent property of the file that playing the game does not reveal, and the chord stays reserved
     * for as long as it sits there. EliteIntel refuses to assign one, but Elite's own controls screen
     * does not, so the file has to be read as well.
     */
    public List<ReservedKeyChords.ReservedBinding> reservedChordBindings() {
        return ReservedKeyChords.scan(getBindings());
    }

    /**
     * Detects binding conflicts among GameCommand bindings and persists them.
     * Returns the newly detected conflicts only - empty list means nothing changed. Whole conflicts
     * rather than their descriptions, so the caller can name the two actions itself and report a run of
     * them compactly. Blocking conflicts are excluded from both the persisted set and the returned list:
     * {@link #blockingConflicts()} owns announcing those, on every start rather than once, so an
     * "already told you" row for one would only ever silence it.
     */
    public List<BindingConflictScanner.Conflict> checkForConflictsAndPersist() {
        List<BindingConflictScanner.Conflict> newConflicts = new ArrayList<>();

        Set<String> currentConflictKeys = new HashSet<>();
        Map<String, BindingConflictScanner.Conflict> currentConflictDescriptions = new LinkedHashMap<>();
        for (BindingConflictScanner.Conflict c : detectConflicts()) {
            // Announce only conflicts that touch a control EliteIntel presses, so voice alerts stay
            // meaningful and do not flood on unrelated vanilla-vs-vanilla overlaps. The UI surfaces
            // the full set live. Rows persisted for pairs that no longer pass this filter are swept
            // out by the stale-row pass below, so the store follows the scope on its own.
            if (!APP_DRIVEN_ACTIONS.contains(c.actionA())
                    && !APP_DRIVEN_ACTIONS.contains(c.actionB()))
                continue;
            String conflictKey = BindingConflictRules.makeKey(c.actionA(), c.actionB());
            if (currentConflictKeys.add(conflictKey) && !c.blocking()) {
                // Still tracked in currentConflictKeys above so the stale-row sweep below stays correct;
                // only kept out of the announce-once list.
                currentConflictDescriptions.put(conflictKey, c);
            }
        }

        // Diff against persisted state
        Set<String> persistedKeys = new HashSet<>(
                conflictManager.getConflicts().stream()
                        .map(r -> r.getConflictKey())
                        .toList());

        for (Map.Entry<String, BindingConflictScanner.Conflict> entry : currentConflictDescriptions.entrySet()) {
            if (!persistedKeys.contains(entry.getKey())) {
                conflictManager.save(entry.getKey(), entry.getValue().description());
                newConflicts.add(entry.getValue());
            }
        }

        for (String persisted : persistedKeys) {
            if (!currentConflictKeys.contains(persisted)) {
                conflictManager.remove(persisted);
            }
        }

        return newConflicts;
    }

    /**
     * Detects all keyboard binding conflicts in the current file using ED's exact-chord model
     * (see {@link BindingConflictScanner}): two bindings conflict only when they share an
     * identical chord within the same context.
     */
    private List<BindingConflictScanner.Conflict> detectConflicts() {
        return BindingConflictScanner.scan(getBindings());
    }

    /**
     * Computes the humanized names of the controls EliteIntel presses that are currently
     * missing from the active binds file - see {@link #requiredGameBindings()} for what counts.
     * Pure read over the freshly parsed bindings - no persistence (the legacy DB-backed
     * missing-binding store was removed; the binds editor UI and startup notification both
     * work off the live parse).
     *
     * @return a list of humanized names of the app-driven key bindings that are missing.
     */
    public List<String> checkForMissingBindings() {
        Map<String, KeyBindingsParser.KeyBinding> currentBindings = getBindings();
        if (currentBindings == null) {
            log.warn("Bindings not yet loaded, skipping missing binding check");
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (String gameBinding : findMissingGameBindings(currentBindings)) {
            result.add(humanizeBindingName(gameBinding));
        }
        return result;
    }

    public List<String> findMissingGameBindings(Map<String, KeyBindingsParser.KeyBinding> currentBindings) {
        if (currentBindings == null)
            return List.of();
        return requiredGameBindings().stream()
                .filter(gameBinding -> currentBindings.get(gameBinding) == null)
                .toList();
    }

    public List<String> findFoundGameBindings(Map<String, KeyBindingsParser.KeyBinding> currentBindings) {
        if (currentBindings == null)
            return List.of();
        return requiredGameBindings().stream()
                .filter(gameBinding -> currentBindings.get(gameBinding) != null)
                .toList();
    }

    /**
     * The controls the startup missing-binding check measures the binds file against - see
     * {@link #APP_DRIVEN_ACTIONS}.
     */
    private List<String> requiredGameBindings() {
        return new ArrayList<>(APP_DRIVEN_ACTIONS);
    }
}
