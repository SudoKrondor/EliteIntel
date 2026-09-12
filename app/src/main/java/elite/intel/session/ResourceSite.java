package elite.intel.session;

import elite.intel.gameapi.signals.ResourceSiteGrade;

import java.util.concurrent.atomic.AtomicReference;

/**
 * The bounty hunt the commander is currently on, if any.
 * <p>
 * A hunt starts the moment the ship drops into a resource extraction site and lasts until the
 * vouchers are cashed in or the ship leaves the system. It is deliberately NOT "the ship is inside a
 * site this second": the loop is drop in, clear the site, boost out to supercruise, drop into the
 * next one, and finally fly to a station to cash in. Ending the hunt at the first supercruise entry
 * would blink the card off and on through all of that and hide the tally exactly when the commander
 * is carrying the most.
 * <p>
 * So the marker is scoped to a system, and the two things that genuinely end a hunt end it:
 * {@code RedeemVoucher}, and leaving - which needs no event at all, because a caller compares the
 * system it holds against the one the ship is in.
 * <p>
 * <b>In memory only, like {@link DockedMarket}.</b> It describes what the ship is doing right now,
 * which is worth nothing after a restart taken somewhere else. A restart taken mid-hunt gets it back
 * anyway: {@code ResourceSiteSubscriber} is registered on the pre-scan bus, so replaying the last
 * journals rebuilds the marker before anything asks.
 */
public final class ResourceSite {

    private static final ResourceSite INSTANCE = new ResourceSite();

    private final AtomicReference<Hunt> hunt = new AtomicReference<>();

    private ResourceSite() {
    }

    public static ResourceSite getInstance() {
        return INSTANCE;
    }

    /**
     * Records a drop into a resource extraction site, starting a hunt or re-describing the one in
     * progress with the site just entered.
     */
    public void droppedIn(long systemAddress, ResourceSiteGrade grade, int threat) {
        if (systemAddress <= 0 || grade == null) return;
        hunt.set(new Hunt(systemAddress, grade, threat));
    }

    /**
     * Ends the hunt. Called when the vouchers are cashed in, which is the commander saying they are
     * done rather than merely between sites.
     */
    public void cashedIn() {
        hunt.set(null);
    }

    /**
     * The hunt in this system, or null when there is none - either because none was started or
     * because it was started somewhere the ship has since left.
     */
    public Hunt in(long systemAddress) {
        Hunt current = hunt.get();
        return current != null && current.systemAddress() == systemAddress ? current : null;
    }

    /**
     * @param grade  the last site dropped into, which is what the commander is fighting in now
     * @param threat as the game rated that site, 0 when it said nothing
     */
    public record Hunt(long systemAddress, ResourceSiteGrade grade, int threat) {
    }
}
