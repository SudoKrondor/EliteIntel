package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.CommodityCatalogue;
import elite.intel.gameapi.journal.events.MiningRefinedEvent;

@SuppressWarnings("unused")
public class MiningEventSubscriber {

    @Subscribe
    public void onMiningRefined(MiningRefinedEvent dto) {
        // let's not auto-add materials that were refined to mining targets. that seem to be confusing to the users.
        // The refinery does name what it just produced, though, and an ore this build has never heard of
        // is learned from that so the next prospector call and the hold can name it.
        CommodityCatalogue.getInstance().learn(dto.getType(), dto.getTypeLocalised());
    }
}
