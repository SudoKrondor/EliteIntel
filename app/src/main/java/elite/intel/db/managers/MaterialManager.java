package elite.intel.db.managers;

import elite.intel.db.dao.MaterialNameDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.search.edsm.dto.MaterialsType;
import elite.intel.util.StringUtls;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    /**
     * Symbols a journal event has moved during this run. Only the startup rebuild reads it, and only
     * once; it is bounded by the size of the material catalogue.
     */
    private final Set<String> reportedLive = ConcurrentHashMap.newKeySet();

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
        String key = key(symbol);
        if (key == null) return;
        reportedLive.add(key);
        Database.withDao(MaterialNameDao.class, dao -> {
            ensureKnown(dao, key, type, displayName);
            dao.setAmount(key, amount);
            return null;
        });
    }

    /**
     * Adds a pickup reported by MaterialCollected, which carries a delta rather than a total. Also the
     * credit side of a trader exchange and of a mission's materials reward, both of which can hand over
     * a material the commander has never held.
     */
    public void collect(String symbol, MaterialsType type, int count, String displayName) {
        String key = key(symbol);
        if (key == null) return;
        reportedLive.add(key);
        Database.withDao(MaterialNameDao.class, dao -> {
            ensureKnown(dao, key, type, displayName);
            dao.addAmount(key, count);
            return null;
        });
    }

    /**
     * Deducts material spent on an engineering roll, a synthesis, a trade, a tech-broker unlock, a
     * research donation, or thrown away outright.
     */
    public void subtract(String symbol, int amountUsed) {
        String key = key(symbol);
        if (key == null) return;
        reportedLive.add(key);
        Database.withDao(MaterialNameDao.class, dao -> {
            dao.subtractAmount(key, amountUsed);
            return null;
        });
    }

    public MaterialNameDao.Material find(String symbol) {
        String key = key(symbol);
        if (key == null) return null;
        return Database.withDao(MaterialNameDao.class, dao -> dao.findBySymbol(key));
    }

    /**
     * Replaces the whole held inventory with {@code holdings}; anything absent from it drops to zero.
     * For the startup reconstruction only — every other caller records a single movement and must not
     * touch materials it did not hear about.
     * <p>
     * Materials the live journal has already reported on are left alone: the rebuild replays what
     * happened before app start and runs concurrently with the live parser, so a live report is by
     * definition the newer fact and must not be overwritten by a replay that could not have seen it.
     */
    public void replaceAll(Collection<Holding> holdings) {
        List<MaterialNameDao.Holding> rows = holdings.stream()
                .filter(holding -> symbolKey(holding.symbol()) != null)
                .map(holding -> new MaterialNameDao.Holding(
                        symbolKey(holding.symbol()),
                        displayNameOr(holding.symbol(), holding.displayName()),
                        holding.type().getType(),
                        holding.amount()))
                .toList();
        Database.withDao(MaterialNameDao.class, dao -> {
            dao.replaceAllAmounts(rows, Set.copyOf(reportedLive));
            return null;
        });
    }


    /**
     * Storage cap per symbol, for callers that have to apply the ceiling before they reach the DB.
     */
    public Map<String, Integer> capsBySymbol() {
        return Database.withDao(MaterialNameDao.class, dao -> dao.listAll().stream()
                .collect(Collectors.toMap(MaterialNameDao.Material::getSymbol,
                        MaterialNameDao.Material::getMaxCapacity)));
    }

    /**
     * One material and how many are held, as the startup reconstruction works it out.
     */
    public record Holding(String symbol, MaterialsType type, int amount, String displayName) {
    }

    /**
     * Zeroes every held amount. The material catalogue itself is reference data seeded by migration
     * and is never deleted — only the counts belong to the commander.
     */
    public void clear() {
        reportedLive.clear();   // nothing is held, so nothing has been reported
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
    /**
     * Normalizes a journal material name to the inventory key. Every seeded row is lower-case, and most
     * events already spell the symbol that way, but MissionCompleted's MaterialsReward reports it in
     * mixed case ({@code "HybridCapacitors"} for {@code hybridcapacitors}) — matched raw, that credit
     * would silently create a second row for the same material. ROOT locale because the symbols are
     * ASCII identifiers, not text: a Turkish default locale would otherwise fold {@code I} to {@code ı}.
     *
     * @return the key, or null when there is nothing to match on
     */
    public static String symbolKey(String symbol) {
        if (symbol == null || symbol.isBlank()) return null;
        return symbol.trim().toLowerCase(Locale.ROOT);
    }

    private String key(String symbol) {
        return symbolKey(symbol);
    }

    private void ensureKnown(MaterialNameDao dao, String symbol, MaterialsType type, String displayName) {
        dao.insertIfMissing(symbol, displayNameOr(symbol, displayName), type.getType());
    }

    /**
     * The journal's display name when it gave one, otherwise a readable form of the symbol.
     */
    private static String displayNameOr(String symbol, String displayName) {
        return (displayName == null || displayName.isBlank())
                ? StringUtls.capitalizeWords(symbol)
                : displayName;
    }
}
