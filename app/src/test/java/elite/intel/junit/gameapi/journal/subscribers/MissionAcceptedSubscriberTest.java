package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.MissionType;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.journal.subscribers.MissionAcceptedSubscriber;
import elite.intel.session.PlayerSession;
import elite.intel.session.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MissionAcceptedSubscriberTest {

    private final MissionAcceptedSubscriber subscriber = new MissionAcceptedSubscriber();
    private final MissionManager missionManager = MissionManager.getInstance();

    @BeforeEach
    void clearMissions() {
        missionManager.clear();
        PlayerSession.getInstance().moveTo(new Position(10477373803L, "Sol", null));
    }

    @Test
    void genericMissionIsSavedAndRetrievableById() {
        subscriber.onMissionAcceptedEvent(event(100L, "Mission_Courier", "Deliver supplies", "GoodFaction", null, null, 50_000L));

        MissionDto saved = missionManager.getMission(100L);
        assertNotNull(saved);
        assertEquals(100L, saved.getMissionId());
        assertEquals(50_000L, saved.getReward());
        assertEquals("Deliver supplies", saved.getMissionDescription());
    }

    @Test
    void genericMissionTypeIsCorrectlyMappedFromEventName() {
        subscriber.onMissionAcceptedEvent(event(101L, "Mission_Courier", "Deliver supplies", "GoodFaction", null, null, 10_000L));

        MissionDto saved = missionManager.getMission(101L);
        assertEquals(MissionType.MISSION_COURIER, saved.getMissionType());
    }

    @Test
    void pirateMassacreMissionIsSavedViaPlayerSession() {
        subscriber.onMissionAcceptedEvent(event(200L, "Mission_Massacre", "Kill pirates", "EmpireFaction", "Deciat", "SpacePirates", 200_000L));

        MissionDto saved = PlayerSession.getInstance().getMission(200L);
        assertNotNull(saved);
        assertEquals(200L, saved.getMissionId());
        assertEquals("SpacePirates", saved.getMissionTargetFaction());
    }

    @Test
    void pirateMassacreWingMissionIsAlsoRoutedAsPirate() {
        subscriber.onMissionAcceptedEvent(event(201L, "Mission_MassacreWing", "Wing pirate hunt", "EmpireFaction", "Deciat", "SpacePirates", 150_000L));

        assertNotNull(PlayerSession.getInstance().getMission(201L));
    }

    /**
     * Mission_Massacre_Conflict_* contains Mission_Massacre, so a war massacre used to be filed as a pirate one
     * and its kill bar waited for bounties a conflict zone never pays.
     */
    @Test
    void aWarMassacreIsNotFiledAsAPirateOne() {
        subscriber.onMissionAcceptedEvent(event(300L, "Mission_Massacre_Conflict_CivilWar", "Kill Fire Lotus Coalition ships",
                "Gallant Investment Brokers", "Exphiay", "Fire Lotus Coalition", 4_769_216L));
        subscriber.onMissionAcceptedEvent(event(301L, "Mission_Massacre_Conflict_War", "Kill Other Side ships",
                "Gallant Investment Brokers", "Exphiay", "Other Side", 1_000_000L));

        assertEquals(MissionType.MISSION_MASSACRE_CONFLICT, missionManager.getMission(300L).getMissionType());
        assertEquals(MissionType.MISSION_MASSACRE_CONFLICT, missionManager.getMission(301L).getMissionType());
        assertEquals(MissionType.MISSION_PIRATE_MASSACRE, saveAndRead(302L, "Mission_Massacre"));
    }

    private MissionType saveAndRead(long id, String name) {
        subscriber.onMissionAcceptedEvent(event(id, name, "Kill pirates", "EmpireFaction", "Deciat", "SpacePirates", 1L));
        return missionManager.getMission(id).getMissionType();
    }

    @Test
    void unknownMissionNameIsSavedWithUnknownType() {
        subscriber.onMissionAcceptedEvent(event(102L, "Mission_Bizarre_Unknown_Type", "Do something weird", "SomeFaction", null, null, 1_000L));

        MissionDto saved = missionManager.getMission(102L);
        assertNotNull(saved);
        assertEquals(MissionType.UNKNOWN, saved.getMissionType());
    }

    private static MissionAcceptedEvent event(long missionId, String name, String localisedName,
                                              String faction, String destinationSystem,
                                              String targetFaction, long reward) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "MissionAccepted");
        j.addProperty("MissionID", missionId);
        j.addProperty("Name", name);
        j.addProperty("LocalisedName", localisedName);
        j.addProperty("Faction", faction);
        j.addProperty("Reward", reward);
        if (destinationSystem != null) j.addProperty("DestinationSystem", destinationSystem);
        if (targetFaction != null) j.addProperty("TargetFaction", targetFaction);
        j.addProperty("Expiry", Instant.now().plusSeconds(3600).toString());
        return new MissionAcceptedEvent(j);
    }
}
