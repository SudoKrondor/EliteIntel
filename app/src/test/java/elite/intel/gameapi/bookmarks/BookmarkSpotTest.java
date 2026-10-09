package elite.intel.gameapi.bookmarks;

import elite.intel.gameapi.bookmarks.BookmarkSpot.Here;
import elite.intel.session.PlayerSituation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * "Bookmark this" saves as much as the commander's situation can vouch for, and no more.
 */
class BookmarkSpotTest {

    private static final String SYSTEM = "Col 285 Sector AB-C d1-23";
    private static final String PLANET = SYSTEM + " A 2";

    @Test
    void supercruiseOutsideAnyGravityWellIsTheSystem() {
        LocationBookmark bookmark = resolve(here(PlayerSituation.IN_SHIP_SUPERCRUISE, null, false, null, false));

        assertEquals(LocationBookmark.system(SYSTEM), bookmark);
    }

    @Test
    void aCarrierIsOnlyItsSystemBecauseItWillMove() {
        LocationBookmark bookmark = resolve(here(PlayerSituation.IN_SHIP_DOCKED, "X7Z-1QK", true, null, false));

        assertEquals(LocationBookmark.Kind.SYSTEM, bookmark.kind());
    }

    @Test
    void anOrbitalStationIsTheStation() {
        LocationBookmark bookmark = resolve(here(PlayerSituation.IN_SHIP_DOCKED, "Jameson Memorial", false, null, false));

        assertEquals(LocationBookmark.station(SYSTEM, "Jameson Memorial"), bookmark);
        assertEquals("Jameson Memorial", bookmark.placeName());
    }

    @Test
    void walkingTheConcourseStillBookmarksTheStation() {
        LocationBookmark bookmark = resolve(here(PlayerSituation.ON_FOOT_SOCIAL, "Jameson Memorial", false, null, false));

        assertEquals(LocationBookmark.Kind.STATION, bookmark.kind());
    }

    @Test
    void aPortWithASurfaceUnderItIsAPlanetaryPortNamedByTheShortBody() {
        LocationBookmark bookmark = resolve(here(PlayerSituation.IN_SHIP_DOCKED, "Hutton Outpost", false, PLANET, true));

        assertEquals(LocationBookmark.planetaryPort(SYSTEM, PLANET, "Hutton Outpost"), bookmark);
        assertEquals("A 2 Hutton Outpost", bookmark.placeName());
    }

    @Test
    void onTheGroundTheExactSpotIsKept() {
        for (PlayerSituation situation : new PlayerSituation[]{
                PlayerSituation.IN_SHIP_LANDED, PlayerSituation.IN_SRV, PlayerSituation.ON_FOOT_PLANET}) {
            LocationBookmark bookmark = resolve(new Here(situation, SYSTEM, null, false, PLANET, true, 12.5, -45.25));

            assertEquals(LocationBookmark.surface(SYSTEM, PLANET, 12.5, -45.25), bookmark, situation.name());
            assertEquals(PLANET, bookmark.placeName(), "the full body name, so the coordinates have a world");
        }
    }

    @Test
    void flyingOverAPlanetKeepsThePlanetButNotThePositionThatIsStillMoving() {
        for (PlayerSituation situation : new PlayerSituation[]{
                PlayerSituation.IN_SHIP_ORBIT, PlayerSituation.IN_SHIP_GLIDE, PlayerSituation.IN_SHIP_SUPERCRUISE}) {
            LocationBookmark bookmark = resolve(new Here(situation, SYSTEM, null, false, PLANET, true, 12.5, -45.25));

            assertEquals(LocationBookmark.planet(SYSTEM, PLANET), bookmark, situation.name());
            assertFalse(bookmark.hasCoordinates(), situation.name());
        }
    }

    @Test
    void noSystemYetMeansNothingToBookmark() {
        assertTrue(BookmarkSpot.resolve(new Here(PlayerSituation.IN_SHIP_SUPERCRUISE, " ", null, false, null, false, 0, 0))
                .isEmpty());
    }

    private static Here here(PlayerSituation situation, String station, boolean carrier, String body, boolean latLong) {
        return new Here(situation, SYSTEM, station, carrier, body, latLong, 0, 0);
    }

    private static LocationBookmark resolve(Here here) {
        return BookmarkSpot.resolve(here).orElseThrow();
    }
}
