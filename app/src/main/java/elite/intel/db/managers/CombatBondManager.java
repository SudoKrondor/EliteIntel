package elite.intel.db.managers;

import elite.intel.db.dao.CombatBondDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.missions.MassacreKill;

import java.util.List;

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
     * Every bond on record, cashed in or not, as the kill it paid for - what a war massacre contract counts.
     */
    public List<MassacreKill> kills() {
        return Database.withDao(CombatBondDao.class, CombatBondDao::kills);
    }

    /**
     * Bonds redeemed while a massacre contract is open: paid, so gone from the held total, but kept as its
     * kills.
     */
    public void markAllCashedIn() {
        Database.withDao(CombatBondDao.class, dao -> {
            dao.markAllCashedIn();
            return Void.TYPE;
        });
    }

    /**
     * Bonds redeemed with no massacre contract to count them: the tally has been paid and starts again
     * from nothing.
     */
    public void clear() {
        Database.withDao(CombatBondDao.class, dao -> {
            dao.clear();
            return Void.TYPE;
        });
    }
}
