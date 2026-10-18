package elite.intel.junit.gameapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.MissionsPreScanAccumulator;
import elite.intel.gameapi.journal.events.*;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The live parser skips everything written before app start, and the game lists held missions only at game load,
 * so a mission accepted between the two was never stored. The pre-scan rebuilds the list from the journal.
 */
class MissionsPreScanAccumulatorTest {

    /**
     * Comfortably before app start, so every fixture counts as a replay.
     */
    private static final String LOAD = "2026-01-01T14:35:17Z";
    private static final String T1 = "2026-01-01T14:38:33Z";
    private static final String T2 = "2026-01-01T14:39:47Z";
    private static final String T3 = "2026-01-01T15:03:28Z";

    private final MissionManager missions = MissionManager.getInstance();

    @BeforeEach
    @AfterEach
    void clearMissions() {
        missions.getMissions().keySet().forEach(missions::remove);
    }

    /**
     * The support-bundle case: an empty mission log at game load, then two accepts before the app was started.
     */
    @Test
    void missionsAcceptedAfterTheGameLoadedAreRestored() {
        MissionsPreScanAccumulator acc = new MissionsPreScanAccumulator();

        acc.onMissions(snapshot(LOAD));
        acc.onMissionAccepted(accepted(T1, 1068775564L, "Firdaus"));
        acc.onMissionAccepted(accepted(T2, 1068775999L, "Miphifa"));
        acc.persist();

        assertEquals(Set.of(1068775564L, 1068775999L), missions.getMissions().keySet());
        assertEquals("Miphifa", missions.getMission(1068775999L).getDestinationSystem());
    }

    /**
     * An accept in the journal is not a mission held: one handed in, failed or abandoned before the app started is
     * gone, and bringing it back would put a finished job on the HUD.
     */
    @Test
    void aMissionThatEndedBeforeLaunchIsNotRestored() {
        MissionsPreScanAccumulator acc = new MissionsPreScanAccumulator();

        acc.onMissions(snapshot(LOAD));
        acc.onMissionAccepted(accepted(T1, 1L, "Firdaus"));
        acc.onMissionAccepted(accepted(T1, 2L, "Firdaus"));
        acc.onMissionAccepted(accepted(T1, 3L, "Firdaus"));
        acc.onMissionAccepted(accepted(T1, 4L, "Firdaus"));
        acc.onMissionCompleted(new MissionCompletedEvent(ended("MissionCompleted", T3, 1L)));
        acc.onMissionFailed(new MissionFailedEvent(ended("MissionFailed", T3, 2L)));
        acc.onMissionAbandoned(new MissionAbandonedEvent(ended("MissionAbandoned", T3, 3L)));
        acc.persist();

        assertEquals(Set.of(4L), missions.getMissions().keySet());
    }

    /**
     * Ending a mission while the app was closed must also clear it if an earlier run had already stored it.
     */
    @Test
    void aStoredMissionThatEndedWhileTheAppWasClosedIsRemoved() {
        missions.save(new MissionDto(accepted(T1, 7L, "Firdaus")));
        MissionsPreScanAccumulator acc = new MissionsPreScanAccumulator();

        acc.onMissionCompleted(new MissionCompletedEvent(ended("MissionCompleted", T3, 7L)));
        acc.persist();

        assertNull(missions.getMission(7L));
    }

    /**
     * The snapshot is the game's own list: what it holds, from an accept in the previous session, is restored,
     * and what we stored that it does not hold has ended.
     */
    @Test
    void theNewestSnapshotDecidesWhatIsHeld() {
        missions.save(new MissionDto(accepted("2025-12-31T10:00:00Z", 99L, "Sol")));
        MissionsPreScanAccumulator acc = new MissionsPreScanAccumulator();

        acc.onMissionAccepted(accepted("2025-12-31T11:00:00Z", 5L, "Deciat"));
        acc.onMissions(snapshot(LOAD, 5L));
        acc.persist();

        assertEquals(Set.of(5L), missions.getMissions().keySet());
    }

    @Test
    void aReplayedRedirectMovesTheHandIn() {
        MissionsPreScanAccumulator acc = new MissionsPreScanAccumulator();

        acc.onMissions(snapshot(LOAD));
        acc.onMissionAccepted(accepted(T1, 8L, "Firdaus"));
        JsonObject j = event("MissionRedirected", T3);
        j.addProperty("MissionID", 8L);
        j.addProperty("NewDestinationSystem", "Cemiess");
        j.addProperty("NewDestinationStation", "Mackenzie Relay");
        acc.onMissionRedirected(new MissionRedirectedEvent(j));
        acc.persist();

        MissionDto stored = missions.getMission(8L);
        assertEquals("Cemiess", stored.getDestinationSystem());
        assertEquals(T3, stored.getRedirectedAt());
    }

    /**
     * Events at or after app start belong to the live subscribers: handling them here too would race them.
     */
    @Test
    void liveEventsAreLeftToTheLiveSubscribers() {
        MissionsPreScanAccumulator acc = new MissionsPreScanAccumulator();

        acc.onMissionAccepted(accepted(Instant.now().plusSeconds(60).toString(), 9L, "Firdaus"));
        acc.persist();

        assertNull(missions.getMission(9L));
    }

    @Test
    void replayingTwiceGivesTheSameAnswer() {
        for (int run = 0; run < 2; run++) {
            MissionsPreScanAccumulator acc = new MissionsPreScanAccumulator();
            acc.onMissions(snapshot(LOAD));
            acc.onMissionAccepted(accepted(T1, 10L, "Firdaus"));
            acc.persist();

            assertEquals(Set.of(10L), missions.getMissions().keySet(), "run " + run);
        }
    }

    private static MissionsEvent snapshot(String timestamp, long... activeIds) {
        JsonObject j = event("Missions", timestamp);
        JsonArray active = new JsonArray();
        for (long id : activeIds) {
            JsonObject mission = new JsonObject();
            mission.addProperty("MissionID", id);
            mission.addProperty("Name", "Mission_PassengerBulk_name");
            mission.addProperty("PassengerMission", true);
            mission.addProperty("Expires", 3600);
            active.add(mission);
        }
        j.add("Active", active);
        j.add("Failed", new JsonArray());
        j.add("Complete", new JsonArray());
        return new MissionsEvent(j);
    }

    private static MissionAcceptedEvent accepted(String timestamp, long id, String destination) {
        JsonObject j = event("MissionAccepted", timestamp);
        j.addProperty("Faction", "Cemiess Commodities");
        j.addProperty("Name", "Mission_PassengerBulk");
        j.addProperty("LocalisedName", "10 Touristen suchen nach Transportmöglichkeit");
        j.addProperty("DestinationSystem", destination);
        j.addProperty("DestinationStation", "Kludze Lab");
        j.addProperty("Expiry", "2099-01-01T00:00:00Z");
        j.addProperty("Wing", false);
        j.addProperty("Reward", 100000);
        j.addProperty("PassengerCount", 10);
        j.addProperty("MissionID", id);
        return new MissionAcceptedEvent(j);
    }

    private static JsonObject ended(String eventName, String timestamp, long id) {
        JsonObject j = event(eventName, timestamp);
        j.addProperty("Name", "Mission_PassengerBulk_name");
        j.addProperty("MissionID", id);
        return j;
    }

    private static JsonObject event(String name, String timestamp) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", timestamp);
        j.addProperty("event", name);
        return j;
    }
}
