package elite.intel.db.managers;

import elite.intel.db.dao.CombatBondDao;
import elite.intel.db.util.Database;

/**
 * The combat bonds the commander is carrying - the conflict-zone twin of {@link BountyManager}.
 */
public class CombatBondManager {

    private static final CombatBondManager INSTANCE = new CombatBondManager();

    private CombatBondManager() {
    }

    public static CombatBondManager getInstance() {
        return INSTANCE;
    }

    public void record(Long systemAddress, String awardingFaction, String victimFaction, long reward, String earnedAt) {
        if (earnedAt == null) return;
        Database.withDao(CombatBondDao.class, dao -> {
            dao.record(systemAddress, awardingFaction, victimFaction, reward, earnedAt);
            return Void.TYPE;
        });
    }

    /**
     * The bonds the commander is carrying, what they are worth and which side paid them. All empty
     * once they are cashed in, which is how the conflict-zone card knows the fight paid out.
     */
    public CombatBondDao.Pending pending() {
        return Database.withDao(CombatBondDao.class, CombatBondDao::pending);
    }

    /**
     * Called when combat bonds are redeemed: the tally has been paid and starts again from nothing.
     */
    public void cashedIn() {
        Database.withDao(CombatBondDao.class, dao -> {
            dao.clear();
            return Void.TYPE;
        });
    }
}
