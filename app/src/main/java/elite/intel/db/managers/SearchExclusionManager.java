package elite.intel.db.managers;

import elite.intel.db.dao.SearchExcludedSystemDao;
import elite.intel.db.util.Database;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The star systems the commander has struck from searches, because the station a search named there was not.
 */
public final class SearchExclusionManager {

    private static SearchExclusionManager instance;

    private SearchExclusionManager() {
    }

    public static synchronized SearchExclusionManager getInstance() {
        if (instance == null) {
            instance = new SearchExclusionManager();
        }
        return instance;
    }

    /**
     * @return true when the system was added, false when it was already excluded
     */
    public boolean exclude(String starSystem) {
        return Database.withDao(SearchExcludedSystemDao.class, dao -> dao.exclude(starSystem.strip())) > 0;
    }

    /**
     * @return true when the system was excluded and no longer is, false when it was never excluded
     */
    public boolean allow(String starSystem) {
        return Database.withDao(SearchExcludedSystemDao.class, dao -> dao.allow(starSystem.strip())) > 0;
    }

    /**
     * Every excluded system, lower-cased, so a whole page of search hits is checked against one read.
     */
    public Set<String> excludedKeys() {
        return Database.withDao(SearchExcludedSystemDao.class, SearchExcludedSystemDao::all).stream()
                .map(SearchExclusionManager::key)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static String key(String starSystem) {
        return starSystem.strip().toLowerCase(Locale.ROOT);
    }
}
