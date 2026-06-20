package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.MissionManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.MissionType;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.session.PlayerSession;

import static elite.intel.gameapi.MissionType.MISSION_PIRATE_MASSACRE;
import static elite.intel.gameapi.MissionType.MISSION_PIRATE_MASSACRE_WING;

@SuppressWarnings("unused")
public class MissionAcceptedSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final MissionManager missionManager = MissionManager.getInstance();

    private static void genericMission(MissionAcceptedEvent event, MissionManager missionManager) {
        if (event != null) {
            missionManager.save(new MissionDto(event));
            String instructions = """
                        Provide key mission parameters as a summary.
                        Ignore unimportant fields such as timestamps, timeToLive, missionID etc.
                    """;
            GameEventBus.publish(
                    new SensorDataEvent(
                            "Mission Accepted: " + event.toYaml(),
                            instructions
                    )
            );
        }
    }

    private static void processPirateMission(MissionAcceptedEvent event, PlayerSession playerSession) {
        HuntingGroundManager huntingGroundManager = HuntingGroundManager.getInstance();

        String destinationSystem = event.getDestinationSystem();
        String targetFaction = event.getTargetFaction();

        String providerStarSystem = playerSession.getPrimaryStarName();
        String missionProviderFaction = event.getFaction();

        int factionId = huntingGroundManager.updateTargetFaction(destinationSystem, targetFaction);
        huntingGroundManager.updateProviderFaction(providerStarSystem, factionId, missionProviderFaction, destinationSystem);

        playerSession.addMission(new MissionDto(event));
        String instructions = """
                    Summarize key mission parameters, destination, reward.
                        - IF relevant to the mission type kill count and or the target name.
                        - Ignore unimportant fields such as timestamps, timeToLive, missionID etc.
                """;
        GameEventBus.publish(new SensorDataEvent("Mission Accepted: " + event.toYaml(), instructions));
    }

    @Subscribe
    public void onMissionAcceptedEvent(MissionAcceptedEvent event) {
        MissionType missionType = missionManager.getMissionType(event.getName());

        if (MISSION_PIRATE_MASSACRE.equals(missionType) || MISSION_PIRATE_MASSACRE_WING.equals(missionType)) {
            processPirateMission(event, playerSession);
        } else {
            genericMission(event, missionManager);
        }
    }
}