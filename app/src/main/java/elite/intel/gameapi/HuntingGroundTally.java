package elite.intel.gameapi;

import com.google.gson.JsonObject;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.MissionCompletedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.PirateMassacreContract;
import elite.intel.gameapi.missions.ResourceSiteProfile;
import elite.intel.gameapi.signals.ResourceSiteGrade;
import elite.intel.gameapi.signals.ResourceSiteSweep;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * What one commander's journals, read oldest first, say about hunting grounds: every resource site they swept past
 * and every pirate massacre contract they took. Shared by the backfill of the journal folder
 * ({@link HuntingGroundJournalScanner}) and the import of journal backups.
 * <p>
 * Contracts are written as they are read, keyed on the game's {@code MissionID}, so reading a journal twice files
 * nothing twice. Sites are held until {@link #persistResourceSites()} and written once per system: a commander who
 * has flown through the same ring system fifty times would otherwise pay for fifty writes to learn the same thing.
 * <p>
 * Feed it one commander only. Where the commander is - which decides a contract's provider - is carried from one
 * line to the next, and two commanders' journals interleaved would hand one commander's contracts the other's
 * whereabouts.
 */
public final class HuntingGroundTally {

    private static final Logger log = LogManager.getLogger(HuntingGroundTally.class);

    private static final String EVENT_MARKER = "\"event\":\"";
    private static final Set<String> EVENTS_OF_INTEREST = Set.of(
            "FSDJump", "Location", "CarrierJump", "Docked", "Undocked",
            "FSSSignalDiscovered", "MissionAccepted", "MissionCompleted");

    private final HuntingGroundManager huntingGrounds;
    private final boolean recordContracts;

    private final Map<Long, String> systemNames = new HashMap<>();
    private final Map<Long, double[]> systemCoordinates = new HashMap<>();
    private final Map<Long, ResourceSiteProfile> sites = new LinkedHashMap<>();
    private final Map<Long, String> lastSeen = new HashMap<>();
    private final ResourceSiteSweep sweep = new ResourceSiteSweep();

    private String currentSystem;
    private double[] currentCoordinates;
    private String currentStation;

    /**
     * @param recordContracts false for journals whose commander is not known: their resource sites are facts about
     *                        the galaxy and belong to everyone, but a contract has to be filed under the commander
     *                        who took it
     */
    public HuntingGroundTally(HuntingGroundManager huntingGrounds, boolean recordContracts) {
        this.huntingGrounds = huntingGrounds;
        this.recordContracts = recordContracts;
    }

    /**
     * The event a journal line carries, read out of the raw text, or null when it carries none.
     * <p>
     * WHY not parse the line: a long archive is millions of lines and all but a few thousand are of no interest
     * here. One {@code indexOf} rejects them for the cost of a scan, where parsing each one to a JSON tree first
     * would dominate the run.
     */
    public static String eventNameOf(String line) {
        int marker = line.indexOf(EVENT_MARKER);
        if (marker < 0) return null;
        int start = marker + EVENT_MARKER.length();
        int end = line.indexOf('"', start);
        if (end < 0) return null;
        return line.substring(start, end);
    }

    public boolean wants(String eventName) {
        return EVENTS_OF_INTEREST.contains(eventName);
    }

    public void accept(String eventName, JsonObject json) {
        switch (eventName) {
            case "FSDJump", "Location", "CarrierJump" -> arrived(json);
            case "Docked" -> currentStation = text(json, "StationName");
            case "Undocked" -> currentStation = null;
            case "FSSSignalDiscovered" -> signal(json);
            case "MissionAccepted" -> recordContract(json);
            case "MissionCompleted" -> recordCompletion(json);
            default -> {
            }
        }
    }

    /**
     * Writes every system's best known resource site inventory. Counts only ever rise, so this never lowers what an
     * earlier run, the live subscriber or the EDDN relay already knew.
     */
    public void persistResourceSites() {
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

    private void arrived(JsonObject json) {
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

    /**
     * WHY the signal's own system address and not {@link #currentSystem}: signals do not always follow the arrival
     * that explains them, and crediting them to wherever the scan last thought it was invents resource sites in
     * systems that have none.
     */
    private void signal(JsonObject json) {
        ResourceSiteGrade grade = ResourceSiteGrade.fromSymbol(text(json, "SignalName"));
        if (grade == null || !json.has("SystemAddress")) return;

        long address = json.get("SystemAddress").getAsLong();
        String timestamp = text(json, "timestamp");
        ResourceSiteProfile sweepSoFar = sweep.add(address + "@" + timestamp, grade);
        sites.merge(address, sweepSoFar, ResourceSiteProfile::max);
        if (timestamp != null) lastSeen.put(address, timestamp);
    }

    private void recordContract(JsonObject json) {
        if (!recordContracts) return;
        MissionAcceptedEvent event = new MissionAcceptedEvent(json);
        if (!PirateMassacreContract.isOne(event.getName(), event.getTargetType())) return;

        Coordinates provider = currentLocation();
        if (provider == null) {
            log.debug("Hunting ground scan: contract {} has no known provider system, skipping", event.getMissionID());
            return;
        }
        huntingGrounds.recordContract(new MissionDto(event), provider, currentStation);
    }

    private void recordCompletion(JsonObject json) {
        if (!recordContracts) return;
        MissionCompletedEvent event = new MissionCompletedEvent(json);
        if (!PirateMassacreContract.isOne(event.getName(), event.getTargetType())) return;
        huntingGrounds.recordContractCompleted(event.getMissionID(), event.getTimestamp());
    }

    private Coordinates currentLocation() {
        if (currentSystem == null || currentCoordinates == null) return null;
        return new Coordinates(currentSystem, currentCoordinates[0], currentCoordinates[1], currentCoordinates[2]);
    }

    private static String text(JsonObject json, String field) {
        return json.has(field) && !json.get(field).isJsonNull() ? json.get(field).getAsString() : null;
    }

    private static double[] coordinates(JsonObject json) {
        if (!json.has("StarPos") || !json.get("StarPos").isJsonArray()) return null;
        var array = json.getAsJsonArray("StarPos");
        if (array.size() < 3) return null;
        return new double[]{array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()};
    }
}
