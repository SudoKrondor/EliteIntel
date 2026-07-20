package elite.intel.db.managers;

import elite.intel.db.dao.MaterialsDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.data.EDMaterialCaps;
import elite.intel.gameapi.search.edsm.dto.MaterialsType;
import elite.intel.util.StringUtls;

public class MaterialManager {
    private static MaterialManager instance;

    private MaterialManager() {
    }

    public static MaterialManager getInstance() {
        if (instance == null) {
            instance = new MaterialManager();
        }
        return instance;
    }

    public void save(String materialName, MaterialsType type, int amount) {
        Database.withDao(MaterialsDao.class, dao -> {
            String name = StringUtls.capitalizeWords(materialName);
            MaterialsDao.Material material = dao.findByExactName(name);
            dao.upsert(
                    StringUtls.capitalizeWords(name.trim()),
                    type.getType(),
                    amount + (material == null ? 0 : material.getAmount()),
                    EDMaterialCaps.getMax(name)
            );
            return null;
        });
    }

    public MaterialsDao.Material find(String materialName) {
        return Database.withDao(MaterialsDao.class, dao -> dao.findByExactName(materialName));
    }

    public void clear() {
        Database.withDao(MaterialsDao.class, dao -> {
            dao.clear();
            return null;
        });
    }

    public void substract(String materialName, int amountUsed) {
        Database.withDao(MaterialsDao.class, dao -> {
            dao.updateAmount(materialName, amountUsed);
            return Void.class;
        });
    }
}
