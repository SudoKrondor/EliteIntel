package elite.intel.db.managers;

import elite.intel.db.dao.ConstructionSiteDao;
import elite.intel.db.dao.ConstructionSiteDao.Requirement;
import elite.intel.db.dao.ConstructionSiteDao.Site;
import elite.intel.db.util.Database;

import java.util.List;

/**
 * The colonisation shopping lists, one per construction site the commander has visited.
 * <p>
 * A site's manifest is replaced wholesale on every visit rather than adjusted. The journal republishes
 * it in full and it is the only authority on what is still wanted: other commanders haul to the same
 * depot, so a list we maintained by subtracting our own deliveries would drift further from the truth
 * with every trip somebody else made.
 */
public final class ConstructionSiteManager {

    private static final ConstructionSiteManager INSTANCE = new ConstructionSiteManager();

    private ConstructionSiteManager() {
    }

    public static ConstructionSiteManager getInstance() {
        return INSTANCE;
    }

    /**
     * Records a site and replaces its manifest.
     */
    public void save(Site site, List<Requirement> requirements) {
        if (site == null) return;
        Database.withDao(ConstructionSiteDao.class, dao -> {
            dao.replaceManifest(site, requirements);
            return null;
        });
    }

    /**
     * The build the commander is working on, or null when none is. Landing at a depot makes that one
     * current; saying they are done leaves none, and the HUD empty, until they land at one again.
     */
    public Site currentSite() {
        return Database.withDao(ConstructionSiteDao.class, ConstructionSiteDao::currentSite);
    }

    /**
     * The ship is standing on this depot's pad: it is the current build, and its manifest has just been read
     * again. Does nothing for a site we have never stored, whose first manifest {@link #save} is about to
     * write anyway.
     */
    public void arrivedAt(long marketId, String visitedAt) {
        if (marketId == 0) return;
        Database.withDao(ConstructionSiteDao.class, dao -> {
            if (dao.findSite(marketId) == null) return null;
            dao.arrivedAt(marketId, visitedAt);
            return null;
        });
    }

    public Site findSite(long marketId) {
        return Database.withDao(ConstructionSiteDao.class, dao -> dao.findSite(marketId));
    }

    public List<Site> listSites() {
        return Database.withDao(ConstructionSiteDao.class, ConstructionSiteDao::listSites);
    }

    public List<Requirement> requirements(long marketId) {
        return Database.withDao(ConstructionSiteDao.class, dao -> dao.listRequirements(marketId));
    }

    /**
     * True when this market is a construction site that still wants something. False for a market that is
     * not a construction site at all, which is the answer the shopping command's visibility needs.
     */
    public boolean hasOutstandingRequirements(long marketId) {
        if (marketId == 0) return false;
        return Database.withDao(ConstructionSiteDao.class, dao -> dao.countOutstanding(marketId)) > 0;
    }

    /**
     * The commander is done with the build they were on: nothing is current any more.
     * <p>
     * Nothing is deleted. The manifests of every site they have visited stay exactly where they are - this
     * only stops one being volunteered, and landing at any depot makes that one current again.
     */
    public void dismissCurrent() {
        Database.withDao(ConstructionSiteDao.class, dao -> {
            dao.clearCurrent();
            return null;
        });
    }

    /**
     * How many builds the commander is tracking. Worth saying out loud when it is more than one, since the
     * spoken answers and the card are all about the site they were last standing on.
     */
    public int siteCount() {
        return listSites().size();
    }

    public void clear() {
        Database.withDao(ConstructionSiteDao.class, dao -> {
            dao.clearAllRequirements();
            dao.clearSites();
            return null;
        });
    }
}
