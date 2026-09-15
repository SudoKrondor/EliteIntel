package elite.intel.gameapi;


import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.MissionCompletedEvent;
import elite.intel.gameapi.journal.events.MissionFailedEvent;
import elite.intel.session.PlayerSession;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class HistoricalMissionScanner {
    private static final Logger log = LogManager.getLogger(HistoricalMissionScanner.class);
    private static final HistoricalMissionScanner INSTANCE = new HistoricalMissionScanner();

    private HistoricalMissionScanner() {
    }

    public static HistoricalMissionScanner getInstance() {
        return INSTANCE;
    }

    /**
     * Scans the player's journal files for pending MissionAcceptedEvents matching the specified target mission IDs.
     * Pending events are those accepted but neither completed nor failed, determined by processing events
     * chronologically from the oldest journal files. (last 4 file limit)
     *
     * @param targetIDs the set of mission IDs to scan for pending accepted events
     * @return a list of pending MissionAcceptedEvents for the target IDs, or an empty list if no files found,
     *         no matching events, or an error occurs
     */
    public List<MissionAcceptedEvent> scanForPendingAcceptedEvents(Set<Long> targetIDs) {
        if (targetIDs.isEmpty()) {
            return Collections.emptyList();
        }

        Path journalDir = PlayerSession.getInstance().getJournalPath();
        List<Path> journalFiles;
        try {
            // The last four by the stamp in their names, oldest first - the mtime sort this replaced took
            // the four OLDEST files in the folder, which on a long-lived install never held a live mission.
            journalFiles = JournalFiles.newest(journalDir, 4);
        } catch (IOException e) {
            log.error("Failed to list journal files", e);
            return Collections.emptyList();
        }

        Map<Long, MissionAcceptedEvent> pendingAccepted = new HashMap<>();
        Set<Long> completedOrFailedIDs = new HashSet<>();

        for (Path file : journalFiles) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {
                    /// skip trash
                    if (isFirstLine) {
                        if (!line.isEmpty() && line.charAt(0) == '\uFEFF') { // BOM (thanks windows)
                            line = line.substring(1);
                        }
                        isFirstLine = false;
                    }

                    if (line.isBlank()) continue;

                    String sanitizedLine = line.replaceAll("[\\p{Cntrl}\\p{Cc}\\p{Cf}]", "").trim(); // In case used edited files with Ctrl+C/Ctrl+V on windows

                    if (!sanitizedLine.startsWith("{") || !sanitizedLine.endsWith("}")) continue; // only process complete json lines.

                    /// this better be JSON. if not add more trash removal above.
                    JsonObject eventJson = GsonFactory.getGson().fromJson(sanitizedLine, JsonObject.class);
                    if (!eventJson.has("event")) continue;

                    /// process old journal events, ignore TTL
                    String eventType = eventJson.get("event").getAsString();
                    switch (eventType) {
                        case "MissionAccepted" -> {
                            MissionAcceptedEvent event = new MissionAcceptedEvent(eventJson);
                            long id = event.getMissionID();
                            if (targetIDs.contains(id) && !completedOrFailedIDs.contains(id)) {
                                pendingAccepted.put(id, event);
                            }
                        }
                        case "MissionCompleted" -> {
                            MissionCompletedEvent event = new MissionCompletedEvent(eventJson);
                            long id = event.getMissionID();
                            completedOrFailedIDs.add(id);
                            pendingAccepted.remove(id);
                        }
                        case "MissionFailed" -> {
                            MissionFailedEvent event = new MissionFailedEvent(eventJson);
                            long id = event.getMissionID();
                            completedOrFailedIDs.add(id);
                            pendingAccepted.remove(id);
                        }
                        case null, default -> {
                            // we are not interested in this event type
                        }
                    }
                }
            } catch (IOException e) {
                log.error("IO Error reading file: {}", file, e);
            } catch (Exception e) {
                log.error("Error parsing event in file: {}", file, e);
            }
        }
        return new ArrayList<>(pendingAccepted.values());
    }
}