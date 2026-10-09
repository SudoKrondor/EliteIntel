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
 * Bookmark numbers are positions on the list, newest first, so a number always means what the card shows.
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
    void theNewestIsNumberOne() {
        bookmarks.save(LocationBookmark.system("Bookmark First"));
        LocationBookmarkManager.Saved saved = bookmarks.save(LocationBookmark.station("Bookmark Second", "Port"));

        assertEquals(new LocationBookmarkManager.Saved(1, false), saved);
        assertEquals(List.of("Port", "Bookmark First"), names());
    }

    @Test
    void savingTheSamePlaceAgainNamesTheBookmarkItAlreadyIs() {
        bookmarks.save(LocationBookmark.planet("Bookmark Sys", "Bookmark Sys 1"));
        bookmarks.save(LocationBookmark.system("Bookmark Other"));

        LocationBookmarkManager.Saved again = bookmarks.save(LocationBookmark.planet("bookmark sys", "BOOKMARK SYS 1"));

        assertEquals(new LocationBookmarkManager.Saved(2, true), again);
        assertEquals(2, bookmarks.count());
    }

    @Test
    void everyFieldComesBackAsSaved() {
        LocationBookmark camp = LocationBookmark.surface("Bookmark Sys", "Bookmark Sys 2 a", -12.3456, 98.7654);
        LocationBookmark port = LocationBookmark.planetaryPort("Bookmark Sys", "Bookmark Sys 2 a", "Hutton");
        bookmarks.save(camp);
        bookmarks.save(port);

        assertTrue(bookmarks.byNumber(1).orElseThrow().samePlaceAs(port));
        LocationBookmark read = bookmarks.byNumber(2).orElseThrow();
        assertEquals(camp.withId(read.id()), read);
        assertNull(bookmarks.byNumber(1).orElseThrow().latitude(), "a port has no coordinates to come back with");
    }

    @Test
    void deletingClosesTheGap() {
        bookmarks.save(LocationBookmark.system("Bookmark C"));
        bookmarks.save(LocationBookmark.system("Bookmark B"));
        bookmarks.save(LocationBookmark.system("Bookmark A"));

        assertEquals("Bookmark B", bookmarks.delete(2).orElseThrow().starSystem());
        assertEquals(List.of("Bookmark A", "Bookmark C"), names());
    }

    @Test
    void aNumberOffTheListIsNothing() {
        bookmarks.save(LocationBookmark.system("Bookmark Only"));

        assertTrue(bookmarks.byNumber(0).isEmpty());
        assertTrue(bookmarks.byNumber(2).isEmpty());
        assertTrue(bookmarks.delete(2).isEmpty());
        assertEquals(1, bookmarks.count());
    }

    private List<String> names() {
        return bookmarks.newestFirst().stream().map(LocationBookmark::placeName).toList();
    }
}
