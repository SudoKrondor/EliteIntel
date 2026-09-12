package elite.intel.gameapi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.MissionCompletedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.PirateMassacreContract;
import elite.intel.gameapi.missions.ResourceSiteProfile;
import elite.intel.gameapi.signals.ResourceSiteGrade;
import elite.intel.gameapi.signals.ResourceSiteSweep;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Reads the commander's journal archive and learns every hunting ground and every pirate massacre
 * contract in it.
 * <p>
 * WHY this is not part of {@link JournalPreScanner}: that reads the last two journals at every start
 * to restore session state, and it publishes real events onto a bus of real subscribers. This reads
 * years of flying, once, on demand, and writes nothing but two tables. A commander with a long
 * archive should not pay for it on every launch, which is why it is a command rather than a startup
 * step.
 * <p>
 * Re-running it is safe and cheap. Contracts are keyed on the game's own {@code MissionID} so the
 * same one lands on the same row however many times it is read, resource site counts only ever rise,
 * and the scan remembers which journal it last finished so a second run reads only what is new.
 */
public class HuntingGroundJournalScanner {

    private static final Logger log = LogManager.getLogger(HuntingGroundJournalScanner.class);

    private static final String EVENT_MARKER = "\"event\":\"";
    private static final Set<String> EVENTS_OF_INTEREST = Set.of(
            "FSDJump", "Location", "CarrierJump", "Docked", "Undocked",
            "FSSSignalDiscovered", "MissionAccepted", "MissionCompleted");

    private final HuntingGroundManager huntingGrounds;

    public HuntingGroundJournalScanner() {
        this(HuntingGroundManager.getInstance());
    }

    /**
     * Seam for tests.
     */
    public HuntingGroundJournalScanner(HuntingGroundManager huntingGrounds) {
        this.huntingGrounds = huntingGrounds;
    }

    /**
     * Reads every journal not already read and records what it finds.
     */
    public Result scan(Path journalDir) {
        HuntingGroundManager.Knowledge before = huntingGrounds.knowledge();

        List<Path> journals = journalsToRead(journalDir);
        if (journals.isEmpty()) {
            return new Result(0, before, before);
        }

        Tally tally = new Tally();
        for (Path journal : journals) {
            readJournal(journal, tally);
        }
        tally.persistResourceSites(huntingGrounds);
        rememberProgress(journals.getLast());

        HuntingGroundManager.Knowledge after = huntingGrounds.knowledge();
        log.info("Hunting ground scan: {} journal(s) read, now know {} ground(s) and {} contract(s)",
                journals.size(), after.huntingGrounds(), after.contracts());
        return new Result(journals.size(), before, after);
    }

    // ---------------------------------------------------------------- which files

    /**
     * The journals still to read, oldest first.
     * <p>
     * The bookmarked file is read again rather than skipped: it may have been the one the game was
     * still writing when the scan ran, and re-reading it costs one file and cannot double anything.
     */
    private List<Path> journalsToRead(Path journalDir) {
        List<Path> all;
        try (var files = Files.list(journalDir)) {
            all = files.filter(path -> path.getFileName().toString().endsWith(".log"))
                    .sorted(Comparator.comparingLong(path -> path.toFile().lastModified()))
                    .toList();
        } catch (IOException e) {
            log.warn("Hunting ground scan: cannot list {}: {}", journalDir, e.getMessage());
            return List.of();
        }

        String bookmark = huntingGrounds.lastScannedJournal();
        if (bookmark == null || bookmark.isBlank()) return all;

        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getFileName().toString().equals(bookmark)) {
                return all.subList(i, all.size());
            }
        }
        // WHY: the bookmarked file is gone - archived or deleted - so there is no telling what was
        // read. Reading everything again is the only honest answer, and it costs nothing but time.
        return all;
    }

    private void rememberProgress(Path lastRead) {
        huntingGrounds.rememberScannedJournal(lastRead.getFileName().toString(), Instant.now().toString());
    }

    // ---------------------------------------------------------------- reading

    private void readJournal(Path journal, Tally tally) {
        try (BufferedReader reader = Files.newBufferedReader(journal, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String eventName = eventNameOf(line);
                if (eventName == null) continue;
                handle(eventName, line, tally);
            }
        } catch (IOException e) {
            log.warn("Hunting ground scan: cannot read {}: {}", journal.getFileName(), e.getMessage());
        }
    }

    /**
     * The event a journal line carries, or null when the line is not one this scan cares about.
     * <p>
     * WHY read the name out of the raw text rather than parse the line: a long archive is millions of
     * lines and all but a few thousand are of no interest here. One {@code indexOf} rejects them for
     * the cost of a scan, where parsing each one to a JSON tree first would dominate the run.
     */
    private String eventNameOf(String line) {
        int marker = line.indexOf(EVENT_MARKER);
        if (marker < 0) return null;
        int start = marker + EVENT_MARKER.length();
        int end = line.indexOf('"', start);
        if (end < 0) return null;
        String name = line.substring(start, end);
        return EVENTS_OF_INTEREST.contains(name) ? name : null;
    }

    private void handle(String eventName, String line, Tally tally) {
        JsonObject json = parse(line);
        if (json == null) return;

        switch (eventName) {
            case "FSDJump", "Location", "CarrierJump" -> tally.arrived(json);
            case "Docked" -> tally.docked(text(json, "StationName"));
            case "Undocked" -> tally.docked(null);
            case "FSSSignalDiscovered" -> tally.signal(json);
            case "MissionAccepted" -> recordContract(json, tally);
            case "MissionCompleted" -> recordCompletion(json);
        }
    }

    private JsonObject parse(String line) {
        String sanitized = line.replaceAll("[\\p{Cntrl}\\p{Cc}\\p{Cf}]", "").trim();
        if (!sanitized.startsWith("{") || !sanitized.endsWith("}")) return null;
        try {
            return GsonFactory.getGson().fromJson(sanitized, JsonObject.class);
        } catch (JsonParseException e) {
            log.debug("Hunting ground scan: skipping malformed line: {}", e.getMessage());
            return null;
        }
    }

    private void recordContract(JsonObject json, Tally tally) {
        MissionAcceptedEvent event = new MissionAcceptedEvent(json);
        if (!PirateMassacreContract.isOne(event.getName(), event.getTargetType())) return;

        Coordinates provider = tally.currentLocation();
        if (provider == null) {
            log.debug("Hunting ground scan: contract {} has no known provider system, skipping",
                    event.getMissionID());
            return;
        }
        huntingGrounds.recordContract(new MissionDto(event), provider, tally.currentStation());
    }

    private void recordCompletion(JsonObject json) {
        MissionCompletedEvent event = new MissionCompletedEvent(json);
        if (!PirateMassacreContract.isOne(event.getName(), event.getTargetType())) return;
        huntingGrounds.recordContractCompleted(event.getMissionID(), event.getTimestamp());
    }

    private static String text(JsonObject json, String field) {
        return json.has(field) && !json.get(field).isJsonNull() ? json.get(field).getAsString() : null;
    }

    // ---------------------------------------------------------------- what the scan is holding

    /**
     * Where the commander was, and what the journal has said about resource sites so far.
     * <p>
     * Sites are held in memory until the whole scan is done and then written once per system. A
     * commander who has flown through the same ring system fifty times would otherwise pay for fifty
     * writes to learn the same thing.
     */
    private static final class Tally {

        private final Map<Long, String> systemNames = new HashMap<>();
        private final Map<Long, double[]> systemCoordinates = new HashMap<>();
        private final Map<Long, ResourceSiteProfile> sites = new LinkedHashMap<>();
        private final Map<Long, String> lastSeen = new HashMap<>();
        private final ResourceSiteSweep sweep = new ResourceSiteSweep();

        private String currentSystem;
        private double[] currentCoordinates;
        private String currentStation;

        void arrived(JsonObject json) {
            String system = text(json, "StarSystem");
            if (system == null) return;

            currentSystem = system;
            currentCoordinates = coordinates(json);
            currentStation = text(json, "StationName");

            if (!json.has("SystemAddress")) return;
            long address = json.get("SystemAddress").getAsLong();
            systemNames.put(address, system);
            if (currentCoordinates != null) systemCoordinates.put(address, currentCoordinates);
        }

        void docked(String stationName) {
            currentStation = stationName;
        }

        /**
         * WHY the signal's own system address and not {@link #currentSystem}: signals do not always
         * follow the arrival that explains them, and crediting them to wherever the scan last thought
         * it was invents resource sites in systems that have none.
         */
        void signal(JsonObject json) {
            ResourceSiteGrade grade = ResourceSiteGrade.fromSymbol(text(json, "SignalName"));
            if (grade == null || !json.has("SystemAddress")) return;

            long address = json.get("SystemAddress").getAsLong();
            String timestamp = text(json, "timestamp");
            ResourceSiteProfile sweepSoFar = sweep.add(address + "@" + timestamp, grade);
            sites.merge(address, sweepSoFar, ResourceSiteProfile::max);
            if (timestamp != null) lastSeen.put(address, timestamp);
        }

        Coordinates currentLocation() {
            if (currentSystem == null || currentCoordinates == null) return null;
            return new Coordinates(currentSystem, currentCoordinates[0], currentCoordinates[1], currentCoordinates[2]);
        }

        String currentStation() {
            return currentStation;
        }

        void persistResourceSites(HuntingGroundManager huntingGrounds) {
            for (Map.Entry<Long, ResourceSiteProfile> entry : sites.entrySet()) {
                long address = entry.getKey();
                String system = systemNames.get(address);
                if (system == null) continue;

                double[] position = systemCoordinates.get(address);
                Coordinates coordinates = position == null
                        ? null
                        : new Coordinates(system, position[0], position[1], position[2]);
                huntingGrounds.recordResourceSites(system, address, coordinates, entry.getValue(), lastSeen.get(address));
            }
        }

        private static double[] coordinates(JsonObject json) {
            if (!json.has("StarPos") || !json.get("StarPos").isJsonArray()) return null;
            var array = json.getAsJsonArray("StarPos");
            if (array.size() < 3) return null;
            return new double[]{array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()};
        }
    }

    /**
     * What one run of the scan did.
     *
     * @param before what was known when it started, so the caller can say what was new
     */
    public record Result(int journalsRead,
                         HuntingGroundManager.Knowledge before,
                         HuntingGroundManager.Knowledge after) {

        public int huntingGroundsLearned() {
            return after.huntingGrounds() - before.huntingGrounds();
        }

        public int contractsLearned() {
            return after.contracts() - before.contracts();
        }
    }
}
