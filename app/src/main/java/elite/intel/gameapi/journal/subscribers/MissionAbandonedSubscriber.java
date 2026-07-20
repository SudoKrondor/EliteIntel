package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.MissionAbandonedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;

@SuppressWarnings("unused")
public class MissionAbandonedSubscriber {

    private final MissionManager missionManager = MissionManager.getInstance();

    @Subscribe
    public void onMissionAbandonedEvent(MissionAbandonedEvent event) {
        Thread.ofVirtual().start(() -> {
            MissionDto mission = missionManager.getMission(event.getMissionID());
            if (mission != null) {
                missionManager.remove(event.getMissionID());
                String missionDetails = mission.getMissionDescription();
                CompanionRuntime.narrator().narrate("Notify: Mission \"" + missionDetails + "\" Abandoned: " + mission, "Notify user of mission abandonment, provide short summary from the data received.");
            }
        });
    }
}