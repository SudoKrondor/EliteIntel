package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.MissionManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.MissionType;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.MissionCompletedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.session.PlayerSession;

import static elite.intel.gameapi.MissionType.MISSION_PIRATE_MASSACRE;
import static elite.intel.gameapi.MissionType.MISSION_PIRATE_MASSACRE_WING;
import static elite.intel.util.StringUtls.removeNameEnding;

@SuppressWarnings("unused")
public class MissionCompletedSubscriber {
    private MissionManager missionManager = MissionManager.getInstance();
    private PlayerSession playerSession = PlayerSession.getInstance();

    @Subscribe
    public void onMissionCompletedEvent(MissionCompletedEvent event) {
        Thread.ofVirtual().start(() -> {
            MissionDto mission = playerSession.getMission(event.getMissionID());
            if (mission == null) {
                return; // no mission in session storage. just exit silently - nothing to do.
            }

            MissionType missionType = missionManager.getMissionType(removeNameEnding(event.getName()));
            if (MISSION_PIRATE_MASSACRE.equals(missionType) || MISSION_PIRATE_MASSACRE_WING.equals(missionType)) {
                playerSession.removeMission(event.getMissionID());
                String targetFaction = event.getTargetFaction();
                GameEventBus.publish(new SensorDataEvent("Notify: Mission against Faction \"" + targetFaction + "\" Completed: " + event,
                        "Notify user of a successful mission completion, provide detailed summary from the data received."));
            } else {
                missionManager.remove(event.getMissionID());
                String missionDetails = event.getLocalisedName();
                GameEventBus.publish(new SensorDataEvent("Notify: Mission \"" + missionDetails + "\" Completed: " + event,
                        "Summarize key mission parameters, destination, reward, and fields relevant to the missiontype. Ignore unimportant fields such as timestamps, timeToLive, missionID etc"));
            }
        });
    }
}