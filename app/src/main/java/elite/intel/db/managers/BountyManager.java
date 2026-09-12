package elite.intel.db.managers;

import elite.intel.db.dao.BountyDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.dto.BountyDto;
import elite.intel.util.json.GsonFactory;

import java.util.Set;

public class BountyManager {

    private static final BountyManager INSTANCE = new BountyManager();

    private BountyManager() {
    }

    public static BountyManager getInstance() {
        return INSTANCE;
    }


    public void add(BountyDto bounty) {
        Database.withDao(BountyDao.class, dao ->{
            BountyDao.Bounty data = new BountyDao.Bounty();
            data.setBounty(bounty.toJson());
            data.setKey(bounty.getKey());
            dao.upsert(data);
            return null;
        });
    }

    public void remove(BountyDto bounty) {
        Database.withDao(BountyDao.class, dao ->{
            dao.delete(bounty.getKey());
            return null;
        });
    }

    public Set<BountyDto> getAll() {
        return Database.withDao(BountyDao.class, dao -> {
            Set<BountyDto> result = new java.util.HashSet<>();
            BountyDao.Bounty[] data = dao.listAll();
            for(BountyDao.Bounty bounty : data) {
                result.add(GsonFactory.getGson().fromJson(bounty.getBounty(), BountyDto.class));
            }
            return result;
        });
    }

    /**
     * The vouchers the commander is carrying, and what they are worth. Both zero once they are
     * cashed in, which is how the bounty-hunting card knows the hunt paid out.
     */
    public BountyDao.Pending pending() {
        return Database.withDao(BountyDao.class, BountyDao::pending);
    }

    public void markAllCashedIn() {
        Database.withDao(BountyDao.class, dao -> {
            BountyDao.Bounty[] data = dao.listAll();
            for (BountyDao.Bounty row : data) {
                BountyDto bounty = GsonFactory.getGson().fromJson(row.getBounty(), BountyDto.class);
                bounty.setCashedIn(true);
                row.setBounty(bounty.toJson());
                dao.upsert(row);
            }
            return null;
        });
    }

    public void clear() {
        Database.withDao(BountyDao.class, dao -> {dao.clear(); return null;});
    }
}
