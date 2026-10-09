package elite.intel.gameapi.bookmarks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocationBookmarkTest {

    @Test
    void aBodyNotNamedAfterItsSystemKeepsItsWholeName() {
        assertEquals("Earth", LocationBookmark.planet("Sol", "Earth").shortPlanetName());
    }

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
}
