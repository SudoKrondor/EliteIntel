package elite.intel.db.managers;

import elite.intel.db.dao.LocationBookmarkDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.bookmarks.LocationBookmark;

import java.util.List;
import java.util.Optional;

/**
 * The commander's bookmarks, numbered the way the card shows them: 1 is the newest.
 * <p>
 * The number is a position, not an id. Saving a bookmark pushes every other one down a place and deleting
 * one closes the gap, so the commander never meets a missing number - the card is always the reference for
 * what a number means right now.
 */
public final class LocationBookmarkManager {

    private static final LocationBookmarkManager INSTANCE = new LocationBookmarkManager();

    /**
     * What saving did: the bookmark's number on the list, and whether it was already there.
     */
    public record Saved(int number, boolean alreadyBookmarked) {
    }

    private LocationBookmarkManager() {
    }

    public static LocationBookmarkManager getInstance() {
        return INSTANCE;
    }

    /**
     * Saves a place, unless the same place is already on the list - then that bookmark's number is returned and
     * nothing is written.
     */
    public synchronized Saved save(LocationBookmark bookmark) {
        List<LocationBookmark> all = newestFirst();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).samePlaceAs(bookmark)) return new Saved(i + 1, true);
        }
        Database.withDao(LocationBookmarkDao.class, dao -> {
            dao.insert(bookmark.kind().name(), bookmark.starSystem(), bookmark.stationName(),
                    bookmark.planetName(), bookmark.latitude(), bookmark.longitude(), System.currentTimeMillis());
            return null;
        });
        return new Saved(1, false);
    }

    public List<LocationBookmark> newestFirst() {
        return Database.withDao(LocationBookmarkDao.class, LocationBookmarkDao::newestFirst);
    }

    public int count() {
        return Database.withDao(LocationBookmarkDao.class, LocationBookmarkDao::count);
    }

    /**
     * The bookmark at this number on the list, or empty when there is none.
     */
    public Optional<LocationBookmark> byNumber(int number) {
        List<LocationBookmark> all = newestFirst();
        if (number < 1 || number > all.size()) return Optional.empty();
        return Optional.of(all.get(number - 1));
    }

    /**
     * Deletes the bookmark at this number, returning what was deleted, or empty when there was none.
     */
    public synchronized Optional<LocationBookmark> delete(int number) {
        Optional<LocationBookmark> doomed = byNumber(number);
        doomed.ifPresent(bookmark -> Database.withDao(LocationBookmarkDao.class, dao -> dao.delete(bookmark.id())));
        return doomed;
    }
}
