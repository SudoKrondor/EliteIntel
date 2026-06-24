package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.CodexEntryManager;
import elite.intel.gameapi.journal.events.SellOrganicDataEvent;
import elite.intel.session.PlayerSession;

/**
 * Clears collected bio-sample state once organic data has been sold. The spoken
 * sale summary (credits + by-genus breakdown) is handled by {@code FinanceSubscriber},
 * the single home for financial announcements.
 */
public class SellOrganicDataSubscriber {

    @Subscribe
    public void onSellOrganicDataEvent(SellOrganicDataEvent event) {
        PlayerSession.getInstance().clearBioSamples();
        CodexEntryManager.getInstance().clear();
    }
}
