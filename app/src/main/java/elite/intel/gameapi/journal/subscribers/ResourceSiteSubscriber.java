package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.journal.events.RedeemVoucherEvent;
import elite.intel.gameapi.journal.events.SupercruiseDestinationDropEvent;
import elite.intel.gameapi.signals.ResourceSiteGrade;
import elite.intel.session.PlayerSession;
import elite.intel.session.ResourceSite;

/**
 * Keeps {@link ResourceSite} level with the journal: which system the commander is bounty hunting
 * in, and when they have stopped.
 * <p>
 * Deliberately separate from {@code SuperCruiseDropSubscriber}, which narrates the drop and repairs
 * the location record on a virtual thread. This is a single assignment that has to have happened by
 * the time the overlay polls, so it runs on the bus thread and owns nothing else - the same split as
 * {@code DockedMarketSubscriber} against {@code DockedSubscriber}.
 * <p>
 * A drop that is not into a resource site is ignored rather than treated as leaving one. Flying in
 * to cash in means dropping at a station, and that must not throw away the tally the commander is
 * flying there to collect on.
 */
public class ResourceSiteSubscriber {

    private final ResourceSite resourceSite = ResourceSite.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    /**
     * WHY the system comes from the session rather than the event: a supercruise drop names the
     * destination and its threat, and no system at all. The ship cannot drop into a site in a system
     * it is not in, so where we are is the answer.
     */
    @Subscribe
    public void onSuperCruiseDrop(SupercruiseDestinationDropEvent event) {
        if (event == null) return;
        ResourceSiteGrade grade = ResourceSiteGrade.fromSymbol(event.getTypeSymbol());
        if (grade == null) return;

        resourceSite.droppedIn(playerSession.getLocationData().getSystemAddress(), grade, event.getThreat());
    }

    @Subscribe
    public void onRedeemVoucher(RedeemVoucherEvent event) {
        if (event == null) return;
        resourceSite.cashedIn();
    }
}
