package elite.intel.db.managers;

import elite.intel.db.util.Database;
import elite.intel.gameapi.bookmarks.LocationBookmark;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bookmark numbers are positions on the commander's own list, so a number always means what the card shows. A new
 * bookmark goes to the bottom, where it moves nothing the commander arranged.
 */
class LocationBookmarkManagerTest {

    private final LocationBookmarkManager bookmarks = LocationBookmarkManager.getInstance();

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @BeforeEach
    void empty() {
        while (bookmarks.delete(1).isPresent()) {
            // the list has no other way to be emptied
        }
    }

    @Test
    void aNewBookmarkGoesToTheBottom() {
        bookmarks.save(LocationBookmark.system("Bookmark First"));
        LocationBookmarkManager.Saved saved = bookmarks.save(LocationBookmark.station("Bookmark Second", "Port"));

        assertEquals(new LocationBookmarkManager.Saved(2, false), saved);
        assertEquals(List.of("Bookmark First", "Port"), names());
    }

    @Test
    void savingTheSamePlaceAgainNamesTheBookmarkItAlreadyIs() {
        bookmarks.save(LocationBookmark.planet("Bookmark Sys", "Bookmark Sys 1"));
        bookmarks.save(LocationBookmark.system("Bookmark Other"));

        LocationBookmarkManager.Saved again = bookmarks.save(LocationBookmark.planet("bookmark sys", "BOOKMARK SYS 1"));

        assertEquals(new LocationBookmarkManager.Saved(1, true), again);
        assertEquals(2, bookmarks.count());
    }

    @Test
    void everyFieldComesBackAsSaved() {
        LocationBookmark camp = LocationBookmark.surface("Bookmark Sys", "Bookmark Sys 2 a", -12.3456, 98.7654);
        LocationBookmark port = LocationBookmark.planetaryPort("Bookmark Sys", "Bookmark Sys 2 a", "Hutton");
        bookmarks.save(camp);
        bookmarks.save(port);

        assertTrue(bookmarks.byNumber(2).orElseThrow().samePlaceAs(port));
        LocationBookmark read = bookmarks.byNumber(1).orElseThrow();
        assertEquals(camp.withId(read.id()), read);
        assertNull(bookmarks.byNumber(2).orElseThrow().latitude(), "a port has no coordinates to come back with");
    }

    @Test
    void deletingClosesTheGap() {
        bookmarks.save(LocationBookmark.system("Bookmark C"));
        bookmarks.save(LocationBookmark.system("Bookmark B"));
        bookmarks.save(LocationBookmark.system("Bookmark A"));

        assertEquals("Bookmark B", bookmarks.delete(2).orElseThrow().starSystem());
        assertEquals(List.of("Bookmark C", "Bookmark A"), names());
    }

    @Test
    void aNumberOffTheListIsNothing() {
        bookmarks.save(LocationBookmark.system("Bookmark Only"));

        assertTrue(bookmarks.byNumber(0).isEmpty());
        assertTrue(bookmarks.byNumber(2).isEmpty());
        assertTrue(bookmarks.delete(2).isEmpty());
        assertEquals(1, bookmarks.count());
    }

    @Test
    void aRenameIsKeptAndABlankNameTakesItBack() {
        bookmarks.save(LocationBookmark.station("Bookmark Eurybia", "Demolition Unlimited"));
        long id = bookmarks.byNumber(1).orElseThrow().id();

        bookmarks.rename(id, " Liz Ryder - Engineer ");
        assertEquals("Liz Ryder - Engineer", bookmarks.byNumber(1).orElseThrow().displayName());

        bookmarks.rename(id, "");
        assertFalse(bookmarks.byNumber(1).orElseThrow().isRenamed());
    }

    @Test
    void anImportGoesToTheBottomInTheFilesOrderAndSkipsWhatIsAlreadyThere() {
        bookmarks.save(LocationBookmark.system("Bookmark Known"));

        int added = bookmarks.importAll(List.of(
                LocationBookmark.system("Bookmark Newest").withDisplayName("Top"),
                LocationBookmark.system("bookmark known"),
                LocationBookmark.system("Bookmark Older"),
                LocationBookmark.system("Bookmark Older")));

        assertEquals(2, added);
        assertEquals(List.of("Bookmark Known", "Bookmark Newest", "Bookmark Older"),
                bookmarks.inOrder().stream().map(LocationBookmark::starSystem).toList());
        assertEquals("Top", bookmarks.byNumber(2).orElseThrow().displayName(), "the name comes in with the place");
    }

    @Test
    void theTickedBookmarksAreDeletedTogether() {
        bookmarks.save(LocationBookmark.system("Bookmark C"));
        bookmarks.save(LocationBookmark.system("Bookmark B"));
        bookmarks.save(LocationBookmark.system("Bookmark A"));
        List<LocationBookmark> all = bookmarks.inOrder();

        bookmarks.deleteAll(List.of(all.get(0).id(), all.get(2).id()));

        assertEquals(List.of("Bookmark B"), names());
    }

    @Test
    void aMoveTradesPlacesWithTheNeighbour() {
        bookmarks.save(LocationBookmark.system("Bookmark A"));
        bookmarks.save(LocationBookmark.system("Bookmark B"));
        bookmarks.save(LocationBookmark.system("Bookmark C"));
        long c = bookmarks.byNumber(3).orElseThrow().id();

        assertTrue(bookmarks.move(c, true));
        assertTrue(bookmarks.move(c, true));
        assertEquals(List.of("Bookmark C", "Bookmark A", "Bookmark B"), names());

        assertTrue(bookmarks.move(c, false));
        assertEquals(List.of("Bookmark A", "Bookmark C", "Bookmark B"), names());
    }

    @Test
    void nothingMovesPastEitherEnd() {
        bookmarks.save(LocationBookmark.system("Bookmark A"));
        bookmarks.save(LocationBookmark.system("Bookmark B"));

        assertFalse(bookmarks.move(bookmarks.byNumber(1).orElseThrow().id(), true));
        assertFalse(bookmarks.move(bookmarks.byNumber(2).orElseThrow().id(), false));
        assertFalse(bookmarks.move(-1, true), "a bookmark no longer on the list");
        assertEquals(List.of("Bookmark A", "Bookmark B"), names());
    }

    @Test
    void anArrangedListKeepsItsOrderWhenOneIsDeletedAndAnotherSaved() {
        bookmarks.save(LocationBookmark.system("Bookmark A"));
        bookmarks.save(LocationBookmark.system("Bookmark B"));
        bookmarks.save(LocationBookmark.system("Bookmark C"));
        bookmarks.move(bookmarks.byNumber(3).orElseThrow().id(), true);
        bookmarks.delete(1);

        LocationBookmarkManager.Saved saved = bookmarks.save(LocationBookmark.system("Bookmark D"));

        assertEquals(3, saved.number());
        assertEquals(List.of("Bookmark C", "Bookmark B", "Bookmark D"), names());
    }

    private List<String> names() {
        return bookmarks.inOrder().stream().map(LocationBookmark::placeName).toList();
    }
}
