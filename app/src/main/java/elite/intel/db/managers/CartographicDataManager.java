package elite.intel.db.managers;

import elite.intel.db.dao.CartographicDataDao;
import elite.intel.db.dao.CartographicDataDao.StoredBody;
import elite.intel.db.dao.CartographicDataDao.StoredSystem;
import elite.intel.db.util.Database;
import elite.intel.gameapi.cartography.CartographicBody;
import elite.intel.gameapi.cartography.CartographicValue;

import java.util.*;

/**
 * The exploration data the commander's ship carries and has not sold, and what it is worth.
 * <p>
 * Everything is filed under the ship that scanned it, because that is where the game keeps it: a sale sells
 * the docked ship's data, a death loses the lost ship's, and data left in another ship waits there for it.
 * The ship is whichever one the last {@code Loadout} named, held in memory - the pre-scan replays that event
 * at startup before anything asks.
 */
public final class CartographicDataManager {

    /**
     * Before any {@code Loadout} has been seen. Data scanned then is still kept, under this.
     */
    static final long UNKNOWN_SHIP = -1;

    private static final CartographicDataManager INSTANCE = new CartographicDataManager();

    private volatile long shipId = UNKNOWN_SHIP;

    private CartographicDataManager() {
    }

    public static CartographicDataManager getInstance() {
        return INSTANCE;
    }

    /**
     * The commander is now flying this ship.
     */
    public void boardShip(long shipId) {
        this.shipId = shipId;
    }

    public void recordBody(long systemAddress, String starSystem, CartographicBody body) {
        long ship = shipId;
        Database.withDao(CartographicDataDao.class, dao -> {
            dao.upsertBody(ship, body.bodyName(), systemAddress, starSystem, body.primaryStar(), body.starType(),
                    body.stellarMass(), body.planetClass(), body.terraformable(), body.massEM(),
                    body.wasDiscovered(), body.wasMapped());
            return null;
        });
    }

    /**
     * A surface mapping finished. A body the ship holds no scan of has nothing to price, so it is ignored.
     */
    public void recordMapping(String bodyName, boolean efficient) {
        long ship = shipId;
        Database.withDao(CartographicDataDao.class, dao -> dao.markMapped(ship, bodyName, efficient));
    }

    public void recordHonk(long systemAddress, String starSystem, int bodyCount) {
        long ship = shipId;
        Database.withDao(CartographicDataDao.class, dao -> {
            dao.upsertHonk(ship, systemAddress, starSystem, bodyCount);
            return null;
        });
    }

    public void recordAllBodiesFound(long systemAddress) {
        long ship = shipId;
        Database.withDao(CartographicDataDao.class, dao -> dao.markAllBodiesFound(ship, systemAddress));
    }

    /**
     * Universal Cartographics bought these systems' data from the ship.
     */
    public void sold(Collection<String> starSystems) {
        long ship = shipId;
        Database.withDao(CartographicDataDao.class, dao -> {
            for (String starSystem : starSystems) {
                dao.deleteBodiesOf(ship, starSystem);
                dao.deleteSystem(ship, starSystem);
            }
            return null;
        });
    }

    /**
     * The ship was destroyed, and its data with it.
     */
    public void lost() {
        long ship = shipId;
        Database.withDao(CartographicDataDao.class, dao -> {
            dao.deleteAllBodies(ship);
            dao.deleteAllSystems(ship);
            return null;
        });
    }

    /**
     * What the ship's unsold data is worth: one system's, and all of it.
     */
    public record Worth(long system, long unsold) {
    }

    /**
     * The worth of the data in the ship, read in one pass because the HUD asks on every poll.
     *
     * @param systemAddress the system whose share is wanted
     */
    public Worth worth(long systemAddress) {
        Map<Long, Long> values = values();
        return new Worth(values.getOrDefault(systemAddress, 0L),
                values.values().stream().mapToLong(Long::longValue).sum());
    }

    /**
     * Each system's value, by system address.
     */
    private Map<Long, Long> values() {
        long ship = shipId;
        List<StoredBody> bodies = Database.withDao(CartographicDataDao.class, dao -> dao.bodies(ship));
        List<StoredSystem> systems = Database.withDao(CartographicDataDao.class, dao -> dao.systems(ship));

        Map<Long, List<CartographicBody>> bodiesBySystem = new HashMap<>();
        for (StoredBody stored : bodies) {
            bodiesBySystem.computeIfAbsent(stored.systemAddress(), k -> new ArrayList<>()).add(stored.body());
        }
        Map<Long, StoredSystem> honks = new HashMap<>();
        for (StoredSystem system : systems) {
            honks.put(system.systemAddress(), system);
        }

        Set<Long> addresses = new HashSet<>(bodiesBySystem.keySet());
        addresses.addAll(honks.keySet());
        Map<Long, Long> values = new HashMap<>();
        for (long address : addresses) {
            StoredSystem honk = honks.get(address);
            values.put(address, CartographicValue.system(bodiesBySystem.getOrDefault(address, List.of()),
                    honk == null ? null : honk.honkBodyCount(), honk != null && honk.allBodiesFound()));
        }
        return values;
    }
}
