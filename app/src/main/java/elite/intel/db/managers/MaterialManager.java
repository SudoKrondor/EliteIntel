package elite.intel.db.managers;

import elite.intel.db.dao.MaterialNameDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.search.edsm.dto.MaterialsType;
import elite.intel.util.StringUtls;

/**
 * Engineering material inventory.
 * <p>
 * Every method takes the journal's non-localized {@code Name} (the FDev symbol, e.g.
 * {@code focuscrystals}), never {@code Name_Localised}. Frontier omits {@code Name_Localised}
 * whenever it would equal the raw name, and changes it with the client language, so it is a display
 * string and not an identifier.
 */
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

    /**
     * Records an absolute count from the Materials event, which is a full inventory snapshot.
     *
     * @param symbol      journal {@code Name}, e.g. {@code basicconductors}
     * @param displayName journal {@code Name_Localised}, used only to name a material this build
     *                    does not yet know about
     */
    public void snapshot(String symbol, MaterialsType type, int amount, String displayName) {
        Database.withDao(MaterialNameDao.class, dao -> {
            ensureKnown(dao, symbol, type, displayName);
            dao.setAmount(symbol, amount);
            return null;
        });
    }

    /**
     * Adds a pickup reported by MaterialCollected, which carries a delta rather than a total.
     */
    public void collect(String symbol, MaterialsType type, int count, String displayName) {
        Database.withDao(MaterialNameDao.class, dao -> {
            ensureKnown(dao, symbol, type, displayName);
            dao.addAmount(symbol, count);
            return null;
        });
    }

    /**
     * Deducts material spent on an engineering roll or synthesis.
     */
    public void subtract(String symbol, int amountUsed) {
        Database.withDao(MaterialNameDao.class, dao -> {
            dao.subtractAmount(symbol, amountUsed);
            return null;
        });
    }

    public MaterialNameDao.Material find(String symbol) {
        return Database.withDao(MaterialNameDao.class, dao -> dao.findBySymbol(symbol));
    }

    /**
     * Zeroes every held amount. The material catalogue itself is reference data seeded by migration
     * and is never deleted — only the counts belong to the commander.
     */
    public void clear() {
        Database.withDao(MaterialNameDao.class, dao -> {
            dao.clearAmounts();
            return null;
        });
    }

    /**
     * No-op for the 147 materials the migration seeds, which is every one known as of Odyssey. It
     * exists so a material added by a future game update is registered rather than having its count
     * dropped on the floor. INSERT OR IGNORE rather than a read-then-write: a Materials snapshot
     * carries well over a hundred entries, and none of them need the round trip.
     */
    private void ensureKnown(MaterialNameDao dao, String symbol, MaterialsType type, String displayName) {
        String name = (displayName == null || displayName.isBlank())
                ? StringUtls.capitalizeWords(symbol)
                : displayName;
        dao.insertIfMissing(symbol, name, type.getType());
    }
}
