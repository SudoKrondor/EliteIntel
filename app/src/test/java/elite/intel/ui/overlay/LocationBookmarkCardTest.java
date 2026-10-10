package elite.intel.ui.overlay;

import elite.intel.gameapi.bookmarks.LocationBookmark;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bookmark list, a page at a time, numbered the way "navigate to bookmark N" reads it.
 */
class LocationBookmarkCardTest {

    private final List<LocationBookmark> bookmarks = new ArrayList<>();
    private final LocationBookmarkCard card =
            new LocationBookmarkCard(() -> bookmarks, new TemporaryHudCard(new AtomicLong()::get));

    @BeforeEach
    void english() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @AfterEach
    void restore() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @Test
    void eachRowIsTheNumberAndTheName() {
        bookmarks.addAll(systems(2));

        HudObjective objective = LocationBookmarkCard.card(bookmarks, 0).orElseThrow();

        assertEquals("BOOKMARKS", objective.title());
        assertEquals("PAGE 1 OF 1", objective.subtitle());
        assertEquals("1", objective.rows().getFirst().label());
        assertEquals("System 1", objective.rows().getFirst().value());
    }

    @Test
    void theSecondPageCarriesOnTheNumbering() {
        HudObjective objective = LocationBookmarkCard.card(systems(10), 1).orElseThrow();

        assertEquals(2, objective.rows().size());
        assertEquals("9", objective.rows().getFirst().label());
        assertEquals("PAGE 2 OF 2", objective.subtitle());
    }

    @Test
    void pagesStopAtBothEnds() {
        bookmarks.addAll(systems(10));
        card.open();

        assertEquals(LocationBookmarkCard.Turn.AT_END, card.previousPage());
        assertEquals(LocationBookmarkCard.Turn.MOVED, card.nextPage());
        assertEquals(LocationBookmarkCard.Turn.AT_END, card.nextPage());
        assertEquals("PAGE 2 OF 2", card.currentObjective().orElseThrow().subtitle());
    }

    @Test
    void aListThatShrankUnderThePageShowsItsLastPage() {
        HudObjective objective = LocationBookmarkCard.card(systems(3), 4).orElseThrow();

        assertEquals("PAGE 1 OF 1", objective.subtitle());
        assertEquals(3, objective.rows().size());
    }

    @Test
    void aSpotOnTheGroundShowsWhereOnTheBody() {
        LocationBookmark camp = LocationBookmark.surface("Sol", "Moon", 12.5, -45.25);

        assertEquals("Moon - 12.5000 / -45.2500", LocationBookmarkCard.label(camp));
    }

    @Test
    void aPortOnTheGroundShowsThePlanetToFlyTo() {
        LocationBookmark engineer = LocationBookmark.planetaryPort("Beta-3 Tucani", "Beta-3 Tucani 2 b a", "The Beach");

        assertEquals("The Beach (Beta-3 Tucani 2 b a)", LocationBookmarkCard.label(engineer));
    }

    @Test
    void aRenamedBookmarkShowsOnlyItsNewName() {
        LocationBookmark camp = LocationBookmark.surface("Sol", "Moon", 12.5, -45.25).withDisplayName("Brain trees");

        assertEquals("Brain trees", LocationBookmarkCard.label(camp));
    }

    @Test
    void noBookmarksIsNoCard() {
        assertTrue(LocationBookmarkCard.card(List.of(), 0).isEmpty());
    }

    private static List<LocationBookmark> systems(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(i -> LocationBookmark.system("System " + i)).toList();
    }
}
