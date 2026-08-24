package elite.intel.gameapi.colonisation;

import elite.intel.db.dao.ConstructionSiteDao.Site;

/**
 * Whether a stored construction site is still worth putting in front of the commander.
 * <p>
 * <b>The problem this answers.</b> Hauling to a build is a job with no end event. A commander does a few
 * runs, then goes trading or pirate hunting, and nothing in the journal ever says they are finished with
 * that site - so a card derived purely from "is there a manifest" would sit on screen for weeks.
 * <p>
 * <b>Why staleness is the rule and not a timer for its own sake.</b> The manifest is a record of the last
 * visit, and other commanders haul to the same depot. That is already why the card warns
 * {@code AS OF / LAST VISIT} after an hour. Past {@link #FORGOTTEN_AFTER_DAYS} days the tonnages have had
 * long enough to be wrong that showing them is worse than showing nothing - so the same reasoning that adds
 * the caveat eventually withdraws the card. Docking at the depot writes a fresh manifest and brings it
 * straight back, which is the only signal that actually means "still working on this".
 * <p>
 * The commander can also say so outright - see {@code DismissConstructionSiteCommand} - for a build they
 * abandoned today rather than three days ago.
 */
public final class ActiveConstructionSite {

    /**
     * How long a manifest goes unrefreshed before the site stops being volunteered.
     * <p>
     * Deliberately generous: a commander who plays at the weekend should find their build where they left
     * it, and the cost of being wrong in this direction is one stale card rather than a forgotten job.
     */
    public static final int FORGOTTEN_AFTER_DAYS = 3;

    private ActiveConstructionSite() {
    }

    /**
     * True when this site is a job the commander is plausibly still on: it exists, it is neither finished
     * nor failed, and its manifest is recent enough to be worth believing.
     * <p>
     * Used by the things that VOLUNTEER the site - the HUD card, and whether to offer the route back. A
     * commander who asks about the build directly is asking a different question and gets an answer whatever
     * its age, because "we have not visited a construction site" is a wrong answer to a question about one
     * they clearly remember.
     */
    public static boolean isLive(Site site) {
        if (site == null || site.isComplete() || site.isFailed()) return false;
        return ManifestAge.hoursSince(site.getVisitedAt()) < FORGOTTEN_AFTER_DAYS * 24L;
    }
}
