package elite.intel.gameapi.bookmarks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocationBookmarkTest {

    @Test
    void askingTwiceWithoutMovingIsTheSameSpot() {
        LocationBookmark first = LocationBookmark.surface("Sol", "Moon", 10.00001, 20.00001);
        LocationBookmark again = LocationBookmark.surface("sol", "MOON", 10.0002, 20.0002).withId(7);

        assertTrue(first.samePlaceAs(again));
    }

    @Test
    void anotherSpotOnTheSameMoonIsAnotherBookmark() {
        LocationBookmark camp = LocationBookmark.surface("Sol", "Moon", 10.0, 20.0);
        LocationBookmark crater = LocationBookmark.surface("Sol", "Moon", 10.5, 20.0);

        assertFalse(camp.samePlaceAs(crater));
    }

    @Test
    void theSameNamesAtDifferentPrecisionsAreDifferentPlaces() {
        assertFalse(LocationBookmark.system("Sol").samePlaceAs(LocationBookmark.planet("Sol", "Earth")));
    }

    @Test
    void vegaSaysTheCommandersNameOnlyWhenTheyGaveOne() {
        LocationBookmark camp = LocationBookmark.surface("Sol", "Moon", 10.0, 20.0);

        assertEquals("Moon", camp.spokenName(), "the place, never the coordinates");
        assertEquals("Brain trees", camp.withDisplayName("  Brain trees ").spokenName());
    }

    @Test
    void aBlankNameTakesTheRenameBack() {
        LocationBookmark renamed = LocationBookmark.system("Sol").withDisplayName("Home");

        assertFalse(renamed.withDisplayName("   ").isRenamed());
        assertFalse(renamed.withDisplayName(null).isRenamed());
    }

    @Test
    void theNameIsNotPartOfThePlace() {
        assertTrue(LocationBookmark.system("Sol").samePlaceAs(LocationBookmark.system("Sol").withDisplayName("Home")));
    }
}
