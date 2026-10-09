package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.CodexEntryManager;
import elite.intel.gameapi.data.BioFormsValueAudit;
import elite.intel.gameapi.journal.events.DiedEvent;
import elite.intel.gameapi.journal.events.SellOrganicDataEvent;
import elite.intel.session.PlayerSession;

/**
 * Clears collected bio-sample state once organic data has been sold, or lost with the commander. The spoken
 * sale summary (credits + by-genus breakdown) is handled by {@code FinanceSubscriber},
 * the single home for financial announcements.
 *
 * <p>The sale is also the one moment the game states what an organism is actually worth, so the
 * payout table is checked against it on the way past - see {@link BioFormsValueAudit}. That runs
 * BEFORE the clear, though it reads neither of the things being cleared: the order is what a reader
 * expects, and a future audit that does want the samples would otherwise find them gone.
 * <p>
 * Live bus only, never the pre-scan's: samples are not rebuilt from the journal, so a death replayed at
 * startup would wipe the ones collected after it.
 */
public class SellOrganicDataSubscriber {

    @Subscribe
    public void onSellOrganicDataEvent(SellOrganicDataEvent event) {
        BioFormsValueAudit.audit(event);
        PlayerSession.getInstance().clearBioSamples();
        CodexEntryManager.getInstance().clear();
    }

    /**
     * A dead commander's samples die with them. Codex vouchers are left alone: whether a death costs those
     * too has not been seen in a journal, and wiping them on a guess would be worse than keeping them.
     */
    @Subscribe
    public void onDied(DiedEvent event) {
        PlayerSession.getInstance().clearBioSamples();
    }
}
