package elite.intel.db.managers;

import elite.intel.db.dao.ExoMasteryDao;
import elite.intel.db.dao.ExoMasteryDao.Body;
import elite.intel.db.dao.ExoMasteryDao.Site;
import elite.intel.db.dao.ExoMasteryDao.Stats;
import elite.intel.db.util.Database;
import elite.intel.gameapi.exomastery.ExoMasteryCatalog;
import elite.intel.gameapi.exomastery.ExoMasteryCatalog.ExoBody;
import elite.intel.gameapi.exomastery.ExoMasteryCatalog.ExoSpecies;
import elite.intel.gameapi.exomastery.ExoMasteryCatalog.ExoSystem;
import org.jdbi.v3.core.Handle;

import java.time.Instant;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Where the exobiology money is, and how much of it the commander has already collected.
 * <p>
 * The catalogue is the commander's to load: nothing is here until they press Enable, and Disable
 * takes it out again - all but the bodies they have finished, which stay so that a later Enable never
 * sends them back to a planet the game will not let them sample twice. {@link #isEnabled()} is simply
 * whether the catalogue is loaded, so there is no separate switch to fall out of step with the rows.
 * <p>
 * A body is finished when the game says so (the survey-complete latch on its location row, relayed by
 * the {@code BioSurveyCompletedEvent}), when the last of its listed species has had its third scan, or
 * when the commander says it is. The three arrive here through {@link #setBodyCompleted},
 * {@link #recordSampled} and {@link #completeSystem}, and all three write the same flag.
 */
public class ExoMasteryManager {

    private static volatile ExoMasteryManager instance;

    /**
     * Cached {@link #isEnabled()}: the command classifier asks on every turn, and the answer changes
     * only through this class. Null until first asked.
     */
    private volatile Boolean enabled;

    private ExoMasteryManager() {
    }

    public static ExoMasteryManager getInstance() {
        if (instance == null) {
            synchronized (ExoMasteryManager.class) {
                if (instance == null) {
                    instance = new ExoMasteryManager();
                }
            }
        }
        return instance;
    }

    // -------------------------------------------------------------- catalogue

    /**
     * Whether the catalogue is loaded - the feature's on switch.
     */
    public boolean isEnabled() {
        Boolean known = enabled;
        if (known == null) {
            known = Database.withDao(ExoMasteryDao.class, dao -> dao.countSystems() > 0);
            enabled = known;
        }
        return known;
    }

    /**
     * Loads a catalogue, in one transaction so a failure part way leaves the previous state intact.
     * <p>
     * Every row is an upsert that leaves the progress flags alone, so re-importing over a catalogue
     * already loaded - or over the completed bodies kept through a purge - loses nothing. Bodies the
     * location table already knows to be sampled out are completed on the spot.
     *
     * @param progress told the import percentage as systems land, 0 to 100, on the calling thread
     */
    public void importCatalog(ExoMasteryCatalog catalog, IntConsumer progress) {
        List<ExoSystem> systems = catalog.systems();
        String now = Instant.now().toString();
        try (Handle handle = Database.init()) {
            handle.useTransaction(h -> {
                ExoMasteryDao dao = h.attach(ExoMasteryDao.class);
                int done = 0;
                int lastReported = -1;
                for (ExoSystem system : systems) {
                    importSystem(dao, system);
                    done++;
                    int percent = systems.isEmpty() ? 100 : Math.min(99, done * 100 / systems.size());
                    if (percent != lastReported) {
                        progress.accept(percent);
                        lastReported = percent;
                    }
                }
                dao.adoptCompletedLocations(now);
            });
        }
        enabled = null;
        progress.accept(100);
    }

    private static void importSystem(ExoMasteryDao dao, ExoSystem system) {
        dao.upsertSystem(system.systemAddress(), system.name(), system.x(), system.y(), system.z());
        for (ExoBody body : system.bodies()) {
            dao.upsertBody(system.systemAddress(), body.bodyId(), system.name(), body.name(), body.type(), body.value());
            for (ExoSpecies species : body.species()) {
                dao.upsertSpecies(system.systemAddress(), body.bodyId(), species.symbol(), species.name(),
                        species.count(), species.value());
            }
        }
    }

    /**
     * Unloads the catalogue, keeping every completed body and its species so the record of what has
     * been sampled - and the harvested total on the stats panel - survives the feature being off.
     */
    public void purge() {
        try (Handle handle = Database.init()) {
            handle.useTransaction(h -> {
                ExoMasteryDao dao = h.attach(ExoMasteryDao.class);
                dao.deleteSystems();
                dao.deleteUncompletedBodies();
                dao.deleteOrphanSpecies();
            });
        }
        enabled = null;
    }

    // ------------------------------------------------------------------ reads

    public Stats stats() {
        return Database.withDao(ExoMasteryDao.class, ExoMasteryDao::stats);
    }

    /**
     * The richest system with something left to sample, or null when the catalogue is sampled out or
     * not loaded.
     */
    public Site richestRemaining() {
        List<Site> sites = richestRemaining(1);
        return sites.isEmpty() ? null : sites.get(0);
    }

    /**
     * The systems with something left to sample, richest first, at most {@code limit} of them.
     */
    public List<Site> richestRemaining(int limit) {
        return Database.withDao(ExoMasteryDao.class, dao -> dao.richestRemaining(limit));
    }

    /**
     * The catalogued bodies in a system, the ones still to sample first.
     */
    public List<Body> bodiesIn(long systemAddress) {
        return Database.withDao(ExoMasteryDao.class, dao -> dao.bodiesIn(systemAddress));
    }

    /**
     * The bodies in a system still to be sampled.
     */
    public List<Body> remainingBodiesIn(long systemAddress) {
        return bodiesIn(systemAddress).stream().filter(body -> !body.completed()).toList();
    }

    /**
     * Whether this body is on the catalogue at all, completed or not.
     */
    public boolean isListed(long systemAddress, long bodyId) {
        return Database.withDao(ExoMasteryDao.class, dao -> dao.countBody(systemAddress, bodyId) > 0);
    }

    /**
     * Whether this system still has a catalogued body to sample.
     */
    public boolean hasRemaining(long systemAddress) {
        return Database.withDao(ExoMasteryDao.class, dao -> dao.countRemainingBodies(systemAddress) > 0);
    }

    // --------------------------------------------------------------- progress

    /**
     * Records that a body is sampled out - or, when the commander takes that back, that it is not.
     *
     * @return true when the flag changed
     */
    public boolean setBodyCompleted(long systemAddress, long bodyId, boolean completed) {
        String now = Instant.now().toString();
        return Database.withDao(ExoMasteryDao.class, dao -> dao.setBodyCompleted(systemAddress, bodyId, completed, now) > 0);
    }

    /**
     * Records the third scan of a species on a body. When it was the last listed species on that body,
     * the body is completed too: everything the catalogue promised there has been collected.
     *
     * @return true when this scan completed the body
     */
    public boolean recordSampled(long systemAddress, long bodyId, String speciesSymbol) {
        if (speciesSymbol == null || speciesSymbol.isBlank()) return false;
        String now = Instant.now().toString();
        return Database.withDao(ExoMasteryDao.class, dao -> {
            if (dao.markSampled(systemAddress, bodyId, speciesSymbol) == 0) return false;
            if (dao.countUnsampledSpecies(systemAddress, bodyId) > 0) return false;
            return dao.setBodyCompleted(systemAddress, bodyId, true, now) > 0;
        });
    }

    /**
     * Writes off every body still open in a system, for the commander who knows they sampled it all
     * before the app was there to see.
     *
     * @return the bodies written off, empty when there was nothing open
     */
    public List<Body> completeSystem(long systemAddress) {
        List<Body> open = remainingBodiesIn(systemAddress);
        if (open.isEmpty()) return open;
        String now = Instant.now().toString();
        Database.withDao(ExoMasteryDao.class, dao -> dao.completeSystem(systemAddress, now));
        return open;
    }

    /**
     * What a commander calls a body: its name with the system's taken off the front - "6 g" for
     * "76 Leonis 6 g". A body not named after its system keeps its whole name.
     */
    public static String shortBodyName(Body body) {
        String name = body.bodyName();
        String prefix = body.starSystem() + " ";
        if (name != null && name.regionMatches(true, 0, prefix, 0, prefix.length()) && name.length() > prefix.length()) {
            return name.substring(prefix.length());
        }
        return name;
    }
}
