package elite.intel.gameapi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.PlayerSession;
import elite.intel.ui.controller.ManagedService;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AuxiliaryFilesMonitor is responsible for monitoring specific auxiliary files in the Elite Dangerous game directory.
 * It observes changes in the monitored files, reads their contents, parses them into corresponding event objects,
 * and publishes those events for further processing.
 *
 * This class implements the Runnable interface and can run as a separate thread to continuously monitor file changes.
 */
public class AuxiliaryFilesMonitor implements Runnable, ManagedService {
    private static final Logger log = LogManager.getLogger(AuxiliaryFilesMonitor.class);
    private static final Gson GSON = GsonFactory.getGson();

    // List of files to monitor
    private static final List<String> MONITORED_FILES = Arrays.asList(
            "Cargo.json",
            "ModulesInfo.json",
            "Status.json",
            "Backpack.json",
            "NavRoute.json",
            "FCMaterials.json",
            "Outfitting.json",
            "Shipyard.json",
            "ShipLocker.json",
            "Market.json"
    );

    // Map file names to their corresponding event classes
    private static final Map<String, Class<?>> FILE_TO_EVENT_CLASS = new HashMap<>();

    static {
        FILE_TO_EVENT_CLASS.put("Cargo.json", GameEvents.CargoEvent.class);
        FILE_TO_EVENT_CLASS.put("ModulesInfo.json", GameEvents.ModulesInfoEvent.class);
        FILE_TO_EVENT_CLASS.put("Status.json", GameEvents.StatusEvent.class);
        FILE_TO_EVENT_CLASS.put("Backpack.json", GameEvents.BackpackEvent.class);
        FILE_TO_EVENT_CLASS.put("NavRoute.json", GameEvents.NavRouteEvent.class);
        FILE_TO_EVENT_CLASS.put("FCMaterials.json", GameEvents.FCMaterialsEvent.class);
        FILE_TO_EVENT_CLASS.put("Outfitting.json", GameEvents.OutfittingEvent.class);
        FILE_TO_EVENT_CLASS.put("Shipyard.json", GameEvents.ShipyardEvent.class);
        FILE_TO_EVENT_CLASS.put("ShipLocker.json", GameEvents.ShipLockerEvent.class);
        FILE_TO_EVENT_CLASS.put("Market.json", GameEvents.MarketEvent.class);
    }

    private  Path directory;
    private Thread processingThread;
    private volatile boolean isRunning;

    /**
     * The stamp - last-modified plus size - of the newest content we actually PARSED, per file.
     * <p>
     * A file whose stamp has moved is read again; one that fails to parse leaves its stamp unrecorded and
     * is therefore read again on the next cycle. That retry is the point: the game writes these files while
     * we may be halfway through reading them, and a 112KB {@code Market.json} is caught mid-write often
     * enough to matter. Before this, such a read was logged and thrown away, and the market it described
     * stayed unknown to the app until the commander next opened that screen - which is how a card that
     * lists what a station sells came to show nothing at a station selling six of the things it wanted.
     */
    private final Map<String, String> parsed = new HashMap<>();

    public AuxiliaryFilesMonitor() {
    }

    /**
     * Seam for tests: the folder to read, which in the app comes from the commander's settings.
     */
    AuxiliaryFilesMonitor(Path directory) {
        this.directory = directory;
    }

    public synchronized void start() {
        this.directory = PlayerSession.getInstance().getJournalPath();
        if (processingThread != null && processingThread.isAlive()) {
            log.warn("AuxiliaryFilesMonitor is already running");
            return;
        }
        isRunning = true;
        processingThread = new Thread(this, "AuxiliaryFilesMonitorThread");
        processingThread.start();
        log.info("AuxiliaryFilesMonitor started");
    }

    public synchronized void stop() {
        if (processingThread == null || !processingThread.isAlive()) {
            log.warn("AuxiliaryFilesMonitor is not running");
            return;
        }
        isRunning = false;
        processingThread.interrupt();
        try {
            processingThread.join(5000); // Wait up to 5 seconds for clean shutdown
            log.info("AuxiliaryFilesMonitor stopped");
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for AuxiliaryFilesMonitor to stop", e);
            Thread.currentThread().interrupt(); // Restore interrupted status
        }
        processingThread = null;
    }

    @Override
    public void run() {
        try {
            readAndPublishInitialFiles();
            monitorFiles();
        } catch (InterruptedException e) {
            log.info("AuxiliaryFilesMonitor interrupted, shutting down");
            Thread.currentThread().interrupt(); // Restore interrupted status
        } catch (Exception e) {
            log.error("Unexpected error in AuxiliaryFilesMonitor", e);
            UiBus.publish(new AppLogEvent("Check Journal directory settings."));
        }
    }

    /**
     * Reads what has changed, every cycle.
     * <p>
     * WHY this polls rather than waiting on a {@link java.nio.file.WatchService}: notifications for files
     * written by another process arrive late, arrive coalesced, or do not arrive at all - the same class of
     * problem that already forced {@code Status.json} onto a direct read. A missed notification here is not
     * a delayed update but a permanently missed one, because nothing asks again until the game writes that
     * file afresh. Ten stamps a cycle costs less than the one file we already read in full eight times a
     * second.
     */
    private void monitorFiles() throws InterruptedException {
        if (directory == null || !Files.isDirectory(directory)) {
            // Said out loud rather than logged: nothing the app reads from the game works without this
            // folder, and the commander is the only one who can point it somewhere real.
            log.error("Journal directory is not readable: {}", directory);
            UiBus.publish(new AppLogEvent("Check Journal directory settings. Stopping services."));
            return;
        }
        log.info("Auxiliary files monitor started, watching directory: {}", directory);

        while (isRunning) {
            Thread.sleep(120);

            if (Thread.currentThread().isInterrupted() || !isRunning) {
                log.info("Shutting down AuxiliaryFilesMonitor due to interruption or stop signal");
                return;
            }

            for (String fileName : MONITORED_FILES) {
                // Status.json is read every cycle whatever its stamp says: SelectFireGroupByNatoHandler
                // sleeps only 300ms between key presses and needs the current fire group inside that window.
                if (fileName.equals("Status.json")) continue;
                publishIfChanged(fileName);
            }

            Path statusPath = directory.resolve("Status.json");
            if (Files.exists(statusPath)) {
                Object statusEvent = readAndParseFile(statusPath, "Status.json");
                if (statusEvent != null) {
                    GameEventBus.publish(statusEvent);
                }
            }
        }
    }

    /**
     * Publishes one file if its content has moved since the last time we parsed it.
     *
     * @return true when this call published the file
     */
    boolean publishIfChanged(String fileName) {
        Path filePath = directory.resolve(fileName);
        String stamp = stampOf(filePath);
        if (stamp == null || stamp.equals(parsed.get(fileName))) return false;

        Object eventObject = readAndParseFile(filePath, fileName);
        // A failed read leaves the stamp unrecorded, so the next cycle tries the same file again - see the
        // note on the parsed map.
        if (eventObject == null) return false;

        parsed.put(fileName, stamp);
        GameEventBus.publish(eventObject);
        log.info("Published update for file: {}", fileName);
        return true;
    }

    /**
     * Last-modified and size together, or null for a file that is not there.
     * <p>
     * WHY the full {@link java.time.Instant} rather than {@code toMillis()}: truncating to milliseconds
     * discards precision the filesystem already recorded, and two different contents of the SAME SIZE
     * written inside one millisecond then look identical - a market swapped for another market whose
     * MarketID has as many digits reads as "nothing has moved" and is never published. Measured on ext4:
     * back-to-back rewrites share a millisecond 197 times in 200, and a nanosecond never.
     * <p>
     * Size stays in the stamp for filesystems that keep coarser times than that (FAT's two-second
     * granularity being the worst of them), where it is the only thing left to tell two writes apart.
     */
    private static String stampOf(Path filePath) {
        try {
            if (!Files.exists(filePath)) return null;
            return Files.getLastModifiedTime(filePath).toInstant() + ":" + Files.size(filePath);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Reads and publishes all monitored files that exist at startup.
     */
    private void readAndPublishInitialFiles() {
        for (String fileName : MONITORED_FILES) {
            Path filePath = directory.resolve(fileName);
            if (Files.exists(filePath)) {
                Object eventObject = readAndParseFile(filePath, fileName);
                if (eventObject != null) {
                    parsed.put(fileName, stampOf(filePath));
                    GameEventBus.publish(eventObject);
                    log.info("Published initial event for file: {}", fileName);
                }
            }
        }
    }

    /**
     * Reads the file, parses it as a JsonObject, and deserializes it into the appropriate Event DTO.
     * Returns null if there's an error reading or parsing.
     */
    private Object readAndParseFile(Path filePath, String fileName) {
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
            if (jsonObject != null) {
                Class<?> eventClass = FILE_TO_EVENT_CLASS.get(fileName);
                if (eventClass != null) {
                    return GSON.fromJson(jsonObject, eventClass);
                } else {
                    log.warn("No event class mapped for file: {}", fileName);
                }
            }
        } catch (IOException e) {
            log.error("Failed to read file: {}", filePath, e);
        } catch (JsonParseException e) {
            // Not fatal and not final: the stamp stays unrecorded, so the next cycle reads it again once the
            // game has finished writing.
            log.warn("Caught {} mid-write, will re-read", filePath.getFileName());
        }
        return null;
    }
}