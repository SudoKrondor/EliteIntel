package elite.intel.db.managers;

import elite.intel.db.dao.BrainTreesDao;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.search.spansh.braintrees.BrainTreeClient;
import elite.intel.gameapi.search.spansh.braintrees.BrainTreeCriteria;
import elite.intel.gameapi.search.spansh.stellarobjects.StellarObjectSearchResultDto;
import elite.intel.util.json.GsonFactory;

import java.util.Arrays;
import java.util.List;

public class BrainTreeManager {

    private static volatile BrainTreeManager instance;

    private BrainTreeManager() {
    }

    public static BrainTreeManager getInstance() {
        if (instance == null) {
            synchronized (BrainTreeManager.class) {
                if (instance == null) {
                    instance = new BrainTreeManager();
                }
            }
        }
        return instance;
    }


    public void addBrainTreeLocation(StellarObjectSearchResultDto.Result data) {
        Database.withDao(BrainTreesDao.class, dao -> {
            BrainTreesDao.BrainTreeLocation entity = new BrainTreesDao.BrainTreeLocation();
            entity.setJson(data.toJson());
            entity.setStarSystem(data.getSystemName());
            entity.setX(data.getX());
            entity.setY(data.getY());
            entity.setZ(data.getZ());
            dao.save(entity);
            return Void.TYPE;
        });
    }

    public StellarObjectSearchResultDto.Result findForStarSystem(String starSystem) {
        return Database.withDao(BrainTreesDao.class, dao -> {
            BrainTreesDao.BrainTreeLocation entity = dao.findForStarSystem(starSystem);
            if(entity == null) return null;
            return GsonFactory.getGson().fromJson(entity.getJson(), StellarObjectSearchResultDto.Result.class);
        });
    }

    public StellarObjectSearchResultDto.Result findNearestForCoordinates(double x, double y, double z){
        return Database.withDao(BrainTreesDao.class, dao -> {
            BrainTreesDao.BrainTreeLocation entity = dao.findNearest(x, y, z);
            if(entity == null) return null;
            return GsonFactory.getGson().fromJson(entity.getJson(), StellarObjectSearchResultDto.Result.class);
        });
    }

    public StellarObjectSearchResultDto.Result findNearestWithMaterial(String material, double x, double y, double z){
        return Database.withDao(BrainTreesDao.class, dao ->{
            List<BrainTreesDao.BrainTreeLocation> entity = dao.findByMaterialNearest(material, x, y, z, 1);
            if(entity.isEmpty()) return null;
            return GsonFactory.getGson().fromJson(entity.get(0).getJson(), StellarObjectSearchResultDto.Result.class);
        });
    }

    public int getCount() {
        return Database.withDao(BrainTreesDao.class, dao-> dao.count());
    }

    public void retrieveFromSpansh(){
        BrainTreeClient client = BrainTreeClient.getInstance();

        BrainTreeCriteria criteria = new BrainTreeCriteria();
        criteria.setPage(1);
        criteria.setSize(50);
        BrainTreeCriteria.Filters filters = new BrainTreeCriteria.Filters();

        BrainTreeCriteria.Distance distance = new BrainTreeCriteria.Distance();
        distance.setMin(0);
        distance.setMax(Integer.MAX_VALUE);
        filters.setDistance(distance);

        BrainTreeCriteria.Genuses genuses = new BrainTreeCriteria.Genuses();
        genuses.setGenuses(Arrays.asList("Brain Trees"));
        filters.setGenuses(genuses);

        LocationManager locationManager = LocationManager.getInstance();
        LocationDao.Coordinates galacticCoordinates = locationManager.getGalacticCoordinates();
        BrainTreeCriteria.ReferenceCoords coords = new BrainTreeCriteria.ReferenceCoords();
        coords.setX(galacticCoordinates.x());
        coords.setY(galacticCoordinates.y());
        coords.setZ(galacticCoordinates.z());
        criteria.setReferenceCoords(coords);
        criteria.setFilters(filters);

        StellarObjectSearchResultDto result = client.search(criteria);
        if (result == null) return;
        result.getResults().forEach(this::addBrainTreeLocation);
    }
}
