package elite.intel.db.managers;

import elite.intel.db.dao.LocationBookmarkDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.bookmarks.LocationBookmark;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The commander's bookmarks, numbered the way the card shows them: 1 is the top of the list.
 * <p>
 * The commander arranges the list on the Bookmarks page, so a page of the card holds what they put there. A
 * new bookmark goes to the bottom, where it moves nothing they arranged.
 * <p>
 * The number is a position, not an id. Deleting a bookmark closes the gap, so the commander never meets a
 * missing number - the card is always the reference for what a number means right now.
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
        List<LocationBookmark> all = inOrder();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).samePlaceAs(bookmark)) return new Saved(i + 1, true);
        }
        insert(bookmark);
        return new Saved(all.size() + 1, false);
    }

    /**
     * Adds bookmarks from an export file, skipping any place already on the list - or already earlier in the
     * same file. They go to the bottom of the list in the order the file had them.
     *
     * @return how many were added
     */
    public synchronized int importAll(List<LocationBookmark> incoming) {
        List<LocationBookmark> known = new ArrayList<>(inOrder());
        List<LocationBookmark> fresh = new ArrayList<>();
        for (LocationBookmark bookmark : incoming) {
            if (known.stream().anyMatch(bookmark::samePlaceAs)) continue;
            known.add(bookmark);
            fresh.add(bookmark);
        }
        fresh.forEach(this::insert);
        return fresh.size();
    }

    /**
     * Gives a bookmark the commander's own name; a blank name takes the rename back.
     */
    public void rename(long id, String displayName) {
        Database.withDao(LocationBookmarkDao.class, dao -> dao.rename(id, LocationBookmark.storedName(displayName)));
    }

    /**
     * Deletes these bookmarks, by row id - the commander picked them on the list, not by number.
     */
    public synchronized void deleteAll(Collection<Long> ids) {
        Database.withDao(LocationBookmarkDao.class, dao -> {
            ids.forEach(dao::delete);
            return null;
        });
    }

    private void insert(LocationBookmark bookmark) {
        Database.withDao(LocationBookmarkDao.class, dao -> {
            dao.insert(bookmark.kind().name(), bookmark.starSystem(), bookmark.stationName(),
                    bookmark.planetName(), bookmark.latitude(), bookmark.longitude(), bookmark.displayName(),
                    System.currentTimeMillis());
            return null;
        });
    }

    /**
     * Moves a bookmark one place up the list (toward number 1), or down. Nothing happens at either end.
     *
     * @return whether it moved
     */
    public synchronized boolean move(long id, boolean up) {
        List<LocationBookmark> all = inOrder();
        int from = indexOf(all, id);
        int to = up ? from - 1 : from + 1;
        if (from < 0 || to < 0 || to >= all.size()) return false;
        long neighbour = all.get(to).id();
        Database.withDao(LocationBookmarkDao.class, dao -> {
            dao.swap(id, neighbour);
            return null;
        });
        return true;
    }

    private static int indexOf(List<LocationBookmark> all, long id) {
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id() == id) return i;
        }
        return -1;
    }

    public List<LocationBookmark> inOrder() {
        return Database.withDao(LocationBookmarkDao.class, LocationBookmarkDao::inOrder);
    }

    public int count() {
        return Database.withDao(LocationBookmarkDao.class, LocationBookmarkDao::count);
    }

    /**
     * The bookmark at this number on the list, or empty when there is none.
     */
    public Optional<LocationBookmark> byNumber(int number) {
        List<LocationBookmark> all = inOrder();
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
