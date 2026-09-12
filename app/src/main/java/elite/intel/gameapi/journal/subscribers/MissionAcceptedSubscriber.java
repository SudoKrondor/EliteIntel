package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.SpokenAmounts;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.MissionType;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.PirateMassacreContract;
import elite.intel.session.PlayerSession;

import static elite.intel.gameapi.MissionType.MISSION_PIRATE_MASSACRE;
import static elite.intel.gameapi.MissionType.MISSION_PIRATE_MASSACRE_WING;

@SuppressWarnings("unused")
public class MissionAcceptedSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final MissionManager missionManager = MissionManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final HuntingGroundManager huntingGrounds = HuntingGroundManager.getInstance();

    @Subscribe
    public void onMissionAcceptedEvent(MissionAcceptedEvent event) {
        recordHuntingGroundPair(event);

        MissionType missionType = missionManager.getMissionType(event.getName());

        if (MISSION_PIRATE_MASSACRE.equals(missionType) || MISSION_PIRATE_MASSACRE_WING.equals(missionType)) {
            processPirateMission(event, playerSession);
        } else {
            genericMission(event, missionManager);
        }
    }

    /**
     * Files where this contract was issued and where it sends the commander, so the app learns the
     * pairing from the commander's own flying rather than from a service.
     * <p>
     * WHY this runs before the mission-type branch below and on a wider rule than it: the branch
     * decides what to say and what the kill bar counts, and it names only the two plain massacre
     * missions. The game issues the same work wrapped in a state or a rank as well, and those teach
     * the same lesson about the same hunting ground. See {@link PirateMassacreContract}.
     */
    private void recordHuntingGroundPair(MissionAcceptedEvent event) {
        if (!PirateMassacreContract.isOne(event.getName(), event.getTargetType())) return;

        LocationDto here = locationManager.findByLocationData(playerSession.getLocationData());
        if (here.getStarName() == null || here.getStarName().isBlank()) return;

        LocationDto station = locationManager.findCurrentStation();
        huntingGrounds.recordContract(
                new MissionDto(event),
                new Coordinates(here.getStarName(), here.getX(), here.getY(), here.getZ()),
                station == null ? null : station.getStationName()
        );
    }

    private static void processPirateMission(MissionAcceptedEvent event, PlayerSession playerSession) {
        playerSession.addMission(new MissionDto(event));
        String instructions = """
                    Summarize key mission parameters, destination, reward.
                        - IF relevant to the mission type kill count and or the target name.
                        - Ignore unimportant fields such as timestamps, timeToLive, missionID etc.
                """ + SpokenAmounts.RULE;
        VegaRuntime.narrator().narrate("Mission Accepted: " + withSpokenReward(event), instructions);
    }

    private static void genericMission(MissionAcceptedEvent event, MissionManager missionManager) {
        if (event != null) {
            missionManager.save(new MissionDto(event));
            String instructions = """
                        Provide key mission parameters as a summary.
                        Ignore unimportant fields such as timestamps, timeToLive, missionID etc.
                    """ + SpokenAmounts.RULE;
            VegaRuntime.narrator().narrate(
                    "Mission Accepted: " + withSpokenReward(event),
                    instructions
            );
        }
    }

    /**
     * The event with its reward also in the form it should be said: a twelve-million-credit reward read digit
     * by digit is the thing the commander complained about, and the narration prompt forbids the model to
     * round a figure itself.
     */
    private static String withSpokenReward(MissionAcceptedEvent event) {
        return event.toYaml() + SpokenAmounts.yamlLine("Reward", event.getReward());
    }
}
