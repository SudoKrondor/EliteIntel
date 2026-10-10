package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.FssSurveyManager;
import elite.intel.gameapi.journal.events.FSSAllBodiesFoundEvent;
import elite.intel.gameapi.journal.events.FSSDiscoveryScanEvent;

/**
 * Files the discovery scanner's body count and the FSS's "all bodies found" for the explorer card.
 * <p>
 * Silent on purpose: this fills a ledger the HUD reads back, and says nothing. The two events can arrive
 * in either order - a one-body system logs FSSAllBodiesFound before the honk that counted it - and the
 * writes are built so that order does not matter.
 */
public class FssSurveySubscriber {

    private final FssSurveyManager surveys = FssSurveyManager.getInstance();

    @Subscribe
    public void onDiscoveryScan(FSSDiscoveryScanEvent event) {
        Thread.ofVirtual().start(() -> surveys.recordBodyCount(event.getSystemAddress(), event.getBodyCount()));
    }

    @Subscribe
    public void onAllBodiesFound(FSSAllBodiesFoundEvent event) {
        Thread.ofVirtual().start(() -> surveys.markAllBodiesFound(event.getSystemAddress(), event.getCount()));
    }
}
