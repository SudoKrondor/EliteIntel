package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.journal.events.MiningRefinedEvent;
import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("unused")
public class MiningEventSubscriber {

    private static final Logger log = LogManager.getLogger(MiningEventSubscriber.class);
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Subscribe
    public void onMiningRefined(MiningRefinedEvent dto) {
        // let's not auto-add materials that were refined to mining targets. that seem to be confusing to the users.
    }
}
