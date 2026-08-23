package elite.intel.gameapi.missions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.MissionsEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game's mission log is the truth about what the commander is holding.
 *
 * <p>This is the reconciliation that was missing: a mission handed in, abandoned or expired while the app was
 * closed produces no event we ever see, so it sat in the database and kept its place on the HUD card. Taken
 * from a real diagnostics bundle: the journal read {@code "Active":[], "Failed":[], "Complete":[]} while the
 * overlay was still showing courier jobs from days earlier.
 */
class MissionReconciliationTest {

    private static final Instant SNAPSHOT = Instant.parse("2026-08-23T12:01:16Z");

    @Test
    void aMissionTheGameNoLongerListsIsDropped() {
        Map<Long, MissionDto> stored = stored(mission(900_001L, SNAPSHOT.minusSeconds(3600)));

        assertEquals(Set.of(900_001L), MissionReconciliation.stale(stored, snapshot()));
    }

    @Test
    void aMissionTheGameStillListsIsKept() {
        Map<Long, MissionDto> stored = stored(mission(900_001L, SNAPSHOT.minusSeconds(3600)));

        assertTrue(MissionReconciliation.stale(stored, snapshot(900_001L)).isEmpty());
    }

    /**
     * The whole point of the change: the game reports no missions at all, so nothing we hold is real.
     * Under the old rule, which read only Complete and Failed, this cleared nothing.
     */
    @Test
    void anEmptyMissionLogClearsEverythingWeHold() {
        Map<Long, MissionDto> stored = stored(
                mission(900_001L, SNAPSHOT.minusSeconds(3600)),
                mission(900_002L, SNAPSHOT.minusSeconds(60)));

        assertEquals(Set.of(900_001L, 900_002L), MissionReconciliation.stale(stored, snapshot()));
    }

    /**
     * The startup replay hands us a snapshot from game load together with events from long after it, and the
     * two arrive on different threads. A mission accepted after the snapshot is missing from it because it
     * did not exist yet, and deleting it would lose a mission the commander is actually flying.
     */
    @Test
    void aMissionAcceptedAfterTheSnapshotSurvivesIt() {
        Map<Long, MissionDto> stored = stored(mission(900_003L, SNAPSHOT.plusSeconds(1)));

        assertTrue(MissionReconciliation.stale(stored, snapshot()).isEmpty());
    }

    /**
     * A record from before accept times were captured is old by definition, which is the same reading
     * {@link MassacreProgress} takes of one.
     */
    @Test
    void aMissionWithNoAcceptTimeCountsAsOld() {
        Map<Long, MissionDto> stored = stored(mission(900_004L, null));

        assertEquals(Set.of(900_004L), MissionReconciliation.stale(stored, snapshot()));
    }

    @Test
    void nothingIsDroppedOnASnapshotWeCannotDate() {
        Map<Long, MissionDto> stored = stored(mission(900_005L, SNAPSHOT.minusSeconds(3600)));

        assertTrue(MissionReconciliation.stale(stored, undatedSnapshot()).isEmpty());
    }

    @Test
    void anEmptyDatabaseIsNotAnError() {
        assertTrue(MissionReconciliation.stale(Map.of(), snapshot()).isEmpty());
        assertTrue(MissionReconciliation.stale(null, snapshot()).isEmpty());
        assertTrue(MissionReconciliation.stale(stored(mission(1L, SNAPSHOT)), null).isEmpty());
    }

    private static Map<Long, MissionDto> stored(MissionDto... missions) {
        Map<Long, MissionDto> map = new LinkedHashMap<>();
        for (MissionDto mission : missions) {
            map.put(mission.getMissionId(), mission);
        }
        return map;
    }

    private static MissionDto mission(long missionId, Instant acceptedAt) {
        MissionDto mission = new MissionDto(null);
        mission.setMissionId(missionId);
        mission.setAcceptedAt(acceptedAt == null ? null : acceptedAt.toString());
        return mission;
    }

    private static MissionsEvent snapshot(long... activeIds) {
        return missionsEvent(SNAPSHOT.toString(), activeIds);
    }

    /**
     * A timestamp Instant cannot read. Contrived, but it is the one input that decides between deleting and
     * doing nothing, so the choice is worth pinning.
     */
    private static MissionsEvent undatedSnapshot() {
        return missionsEvent("not a timestamp");
    }

    private static MissionsEvent missionsEvent(String timestamp, long... activeIds) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", timestamp);
        json.addProperty("event", "Missions");
        JsonArray active = new JsonArray();
        for (long id : activeIds) {
            JsonObject entry = new JsonObject();
            entry.addProperty("MissionID", id);
            entry.addProperty("Name", "Mission_Delivery");
            active.add(entry);
        }
        json.add("Active", active);
        json.add("Failed", new JsonArray());
        json.add("Complete", new JsonArray());
        return new MissionsEvent(json);
    }
}
