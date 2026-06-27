package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.EventNarrator;
import elite.intel.gameapi.journal.events.CommitCrimeEvent;

import static elite.intel.util.StringUtls.localizedEvent;

public class CommitCrimeEventSubscriber {

    @Subscribe
    public void onCommitCrimeEvent(CommitCrimeEvent event) {
        Thread.ofVirtual().start(() -> {
            EventNarrator.critical(
                    localizedEvent("event.crime.bountyIssued",
                            event.getFaction(), event.getBounty(), event.getCrimeType(), event.getVictimLocalised())
            );
        });
    }
}
