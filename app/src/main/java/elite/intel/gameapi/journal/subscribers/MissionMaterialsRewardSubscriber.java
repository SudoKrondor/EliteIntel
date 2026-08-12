package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.MaterialManager;
import elite.intel.gameapi.journal.events.MissionCompletedEvent;
import elite.intel.gameapi.search.edsm.dto.MaterialsType;

import java.util.List;

/**
 * Credits engineering materials paid out by a completed mission.
 * <p>
 * Separate from {@link MissionCompletedSubscriber} because that one returns early when the mission is
 * not in session storage — correct for narration, since there is nothing to summarise, but the
 * materials land in the hold either way and must be counted regardless.
 */
public class MissionMaterialsRewardSubscriber {

    private final MaterialManager materialManager = MaterialManager.getInstance();

    @Subscribe
    public void onMissionCompleted(MissionCompletedEvent event) {
        Thread.ofVirtual().start(() -> {
            List<MissionCompletedEvent.MaterialReward> rewards = event.getMaterialsReward();
            if (rewards == null) return;
            for (MissionCompletedEvent.MaterialReward reward : rewards) {
                // The manager lower-cases the mixed-case Name this event reports, and
                // fromJournalCategory unwraps its $MICRORESOURCE_CATEGORY_...; token.
                materialManager.collect(
                        reward.getName(),
                        MaterialsType.fromJournalCategory(reward.getCategory()),
                        reward.getCount(),
                        reward.getNameLocalised());
            }
        });
    }
}
