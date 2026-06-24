package elite.intel.ai.hands;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.dao.KeyBindingDao.KeyBinding;
import elite.intel.db.managers.BindingConflictManager;
import elite.intel.db.managers.KeyBindingManager;
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
    private final KeyBindingManager keyBindingManager = KeyBindingManager.getInstance();
    private final BindingConflictManager conflictManager = BindingConflictManager.getInstance();
    private Path bindingsDir;
    private Map<String, KeyBindingsParser.KeyBinding> bindings;
    private File currentBindsFile;
    private Thread processingThread;
    private volatile boolean running;

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
                            File activeFile = new BindingsLoader().getLatestBindsFile();
                            boolean activeFileWasModified = activeFile.getName().equals(changed.toString());
                            boolean activeFileChanged = !activeFile.equals(currentBindsFile);
                            if (activeFileWasModified || activeFileChanged) {
                                currentBindsFile = activeFile;
                                Thread.sleep(300); // wait for the game to finish writing
                                parseAndUpdateBindings();
                                log.info("Reloaded bindings from: {}", currentBindsFile.getName());
                            }
                        }
                    }
                }
                checkForMissingBindingsAndPersist();
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
            currentBindsFile = new BindingsLoader().getLatestBindsFile();
            bindings = parser.parseBindings(currentBindsFile);
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

    public Map<String, KeyBindingsParser.KeyBinding> getBindings() {
        return bindings;
    }

    public File getCurrentBindsFile() {
        return currentBindsFile;
    }

    /**
     * Detects binding conflicts among GameCommand bindings and persists them.
     * Returns descriptions of newly detected conflicts only - empty list means
     * nothing changed.
     */
    public List<String> checkForConflictsAndPersist() {
        List<String> newDescriptions = new ArrayList<>();
        Map<String, KeyBindingsParser.KeyBinding> currentBindings = getBindings();
        if (currentBindings == null || currentBindings.isEmpty())
            return newDescriptions;

        // Invert: keyCombo → all action names sharing it
        Map<String, List<String>> byCombo = new HashMap<>();
        for (Map.Entry<String, KeyBindingsParser.KeyBinding> e : currentBindings.entrySet()) {
            String combo = normalizeCombo(e.getValue());
            if (!combo.isEmpty())
                byCombo.computeIfAbsent(combo, k -> new ArrayList<>()).add(e.getKey());
        }

        // Find conflicts: for each distinct GameCommand binding, check what else shares
        // its key
        Set<String> currentConflictKeys = new HashSet<>();
        Map<String, String> currentConflictDescriptions = new LinkedHashMap<>();
        Set<String> processedCombos = new HashSet<>();

        for (Bindings.GameCommand cmd : Bindings.GameCommand.values()) {
            String gameBinding = cmd.getGameBinding();
            KeyBindingsParser.KeyBinding kb = currentBindings.get(gameBinding);
            if (kb == null)
                continue;

            String combo = normalizeCombo(kb);
            if (!processedCombos.add(combo))
                continue; // same combo already checked via another GameCommand

            List<String> sharingActions = byCombo.getOrDefault(combo, List.of());
            for (String other : sharingActions) {
                if (other.equals(gameBinding))
                    continue;
                if (BindingConflictRules.isSafeOverlap(gameBinding, other))
                    continue;

                String conflictKey = BindingConflictRules.makeKey(gameBinding, other);
                if (currentConflictKeys.add(conflictKey)) {
                    currentConflictDescriptions.put(conflictKey, BindingConflictRules.describe(gameBinding, other));
                }
            }
        }

        // Diff against persisted state
        Set<String> persistedKeys = new HashSet<>(
                conflictManager.getConflicts().stream()
                        .map(r -> r.getConflictKey())
                        .toList());

        for (Map.Entry<String, String> entry : currentConflictDescriptions.entrySet()) {
            if (!persistedKeys.contains(entry.getKey())) {
                conflictManager.save(entry.getKey(), entry.getValue());
                newDescriptions.add(entry.getValue());
            }
        }

        for (String persisted : persistedKeys) {
            if (!currentConflictKeys.contains(persisted)) {
                conflictManager.remove(persisted);
            }
        }

        return newDescriptions;
    }

    private String normalizeCombo(KeyBindingsParser.KeyBinding kb) {
        if (kb == null || kb.key == null || kb.key.isBlank() || kb.key.equals("Key_"))
            return "";
        if (kb.modifiers == null || kb.modifiers.length == 0)
            return kb.key;
        String[] sorted = kb.modifiers.clone();
        Arrays.sort(sorted);
        return kb.key + "|" + String.join("|", sorted);
    }

    /**
     * Checks for missing bindings by iterating over all game commands and
     * determines if a corresponding
     * key binding exists. If a binding is missing, it adds a new binding through
     * the key binding manager
     * and records the newly added binding names.
     *
     * @return a list of names of key bindings that were missing and subsequently
     *         added.
     */
    public List<String> checkForMissingBindingsAndPersist() {
        List<String> result = new ArrayList<>();
        Map<String, KeyBindingsParser.KeyBinding> currentBindings = getBindings();
        if (currentBindings == null) {
            log.warn("Bindings not yet loaded, skipping missing binding check");
            return result;
        }

        List<String> oldMissingBindings = keyBindingManager
                .getMissingBindings()
                .stream()
                .map(KeyBinding::getKeyBinding)
                .toList();

        for (String gameBinding : findMissingGameBindings(currentBindings)) {
            String bindingName = humanizeBindingName(gameBinding);
            keyBindingManager.addBinding(bindingName);
            result.add(bindingName);
        }

        for (String gameBinding : findFoundGameBindings(currentBindings)) {
            String bindingName = humanizeBindingName(gameBinding);
            if (oldMissingBindings.contains(bindingName))
                keyBindingManager.removeBinding(bindingName);
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

    private List<String> requiredGameBindings() {
        Set<String> checkedGameBindings = new LinkedHashSet<>();
        for (Bindings.GameCommand command : Bindings.GameCommand.values()) {
            checkedGameBindings.add(command.getGameBinding());
        }
        return new ArrayList<>(checkedGameBindings);
    }
}
