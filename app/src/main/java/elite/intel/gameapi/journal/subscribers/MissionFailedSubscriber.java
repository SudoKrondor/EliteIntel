package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.MissionTitle;
import elite.intel.gameapi.journal.events.MissionFailedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;

/**
 * A mission the commander has lost: abandoned by the game rather than by them, or - far more often - simply
 * run out of time.
 *
 * <p>WHY this exists: {@code MissionFailed} was parsed and registered but nothing listened to it, so an
 * expired mission stayed in the database for good. Courier and delivery runs are all timed, which is how an
 * overlay ends up "stuck in some old courier jobs" while the game's own mission log has been empty for days.
 * {@link MissionsEventSubscriber} sweeps those up at the next game load; this catches them as they happen.
 *
 * <p>Deliberately shaped like {@link MissionAbandonedSubscriber}: same lookup, same silence when we never
 * held the mission, same one-line notification. The two are the same event to a commander.
 */
@SuppressWarnings("unused")
public class MissionFailedSubscriber {

    private final MissionManager missionManager = MissionManager.getInstance();

    @Subscribe
    public void onMissionFailedEvent(MissionFailedEvent event) {
        Thread.ofVirtual().start(() -> {
            MissionDto mission = missionManager.getMission(event.getMissionID());
            if (mission == null) return;

            missionManager.remove(event.getMissionID());
            // The title, never the raw key: a mission we stored before the journal gave us a localised name
            // would otherwise be announced as "Mission_Collect_RankEmp".
            String title = MissionTitle.of(event.getName(), event.getLocalisedName());
            CompanionRuntime.narrator().narrate(
                    "Notify: mission failed: " + title + ". Destination was " + mission.getDestinationSystem()
                            + ". Reward lost: " + mission.getReward() + " credits.",
                    "Tell the commander this mission has been lost, most likely by running out of time. "
                            + "One short sentence naming the mission. Do not offer to do anything about it.");
        });
    }
}
