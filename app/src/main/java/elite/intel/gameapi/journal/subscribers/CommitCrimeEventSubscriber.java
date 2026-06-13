package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.CommitCrimeEvent;

import static elite.intel.util.StringUtls.localizedEvent;

public class CommitCrimeEventSubscriber {

    @Subscribe
    public void onCommitCrimeEvent(CommitCrimeEvent event) {
        Thread.ofVirtual().start(() -> {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(
                    localizedEvent("event.crime.bountyIssued",
                            event.getFaction(), event.getBounty(), event.getCrimeType(), event.getVictimLocalised())
            ));
        });
    }
}
