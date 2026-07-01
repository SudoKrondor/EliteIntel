package elite.intel.db.managers;

import elite.intel.db.dao.ShipMakeDao;
import elite.intel.db.util.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single owner of the {@code ship_make} table: maps an internal ship identifier
 * (e.g. "smallcombat01_nx") to a human-readable display name (e.g. "Kestrel Mk II").
 * <p>
 * Display names follow the game's locale: live-captured {@code *_Localised} values from the
 * journal overwrite the seeded English fallbacks as the player encounters each ship.
 */
public class ShipMakeManager {

    private static final Logger log = LogManager.getLogger(ShipMakeManager.class);

    private static ShipMakeManager instance;

    private final ConcurrentHashMap<String, String> displayNamesByIdentifier = new ConcurrentHashMap<>();

    private ShipMakeManager() {
        load();
    }

    public static synchronized ShipMakeManager getInstance() {
        if (instance == null) {
            instance = new ShipMakeManager();
        }
        return instance;
    }

    /**
     * Returns the display name for an internal ship identifier, or null when none is known.
     */
    public String getDisplayName(String shipIdentifier) {
        if (shipIdentifier == null) return null;
        return displayNamesByIdentifier.get(shipIdentifier.toLowerCase(Locale.ROOT));
    }

    /**
     * Persists and caches a display name. A DB write failure is non-fatal and leaves the cache unchanged.
     */
    public void upsert(String shipIdentifier, String displayName) {
        if (shipIdentifier == null || displayName == null) return;
        String key = shipIdentifier.toLowerCase(Locale.ROOT);
        try {
            Database.withDao(ShipMakeDao.class, dao -> {
                dao.upsert(key, displayName);
                return null;
            });
        } catch (RuntimeException e) {
            log.warn("Could not persist ship make {} -> {}: {}", key, displayName, e.getMessage());
            return; // WHY: DB write failure is non-fatal; skip the cache update to avoid divergence on restart
        }
        displayNamesByIdentifier.put(key, displayName);
    }

    private void load() {
        try {
            List<ShipMakeDao.ShipMake> makes = Database.withDao(ShipMakeDao.class, ShipMakeDao::findAll);
            for (ShipMakeDao.ShipMake make : makes) {
                if (make.getShipIdentifier() != null) {
                    displayNamesByIdentifier.put(make.getShipIdentifier().toLowerCase(Locale.ROOT), make.getDisplayName());
                }
            }
        } catch (RuntimeException e) {
            // WHY: a DB hiccup at startup must not poison the class; fall back to the title-case path until reload.
            log.warn("Could not load ship_make table; display names fall back to title-cased codenames: {}", e.getMessage());
        }
    }
}
