package elite.intel.junit.db.managers;

import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A trade route needs a station to start from, and the cheapest one to find is a station the commander has
 * already docked at. This is the search that supplies it: stations out of our own records, nearest first,
 * inside a light year radius of where we are - so it has to measure a real sphere and not the cube the
 * database narrows the search with, and it has to reach systems other than the one we are standing in.
 */
class KnownStationRadiusSearchTest {

    private static final AtomicInteger RUN = new AtomicInteger();

    private final LocationManager locations = LocationManager.getInstance();

    private long systemAddress;
    private String namespace;

    @BeforeEach
    void setUp() {
        int run = RUN.incrementAndGet();
        namespace = String.format("Radius Test %03d", run);
        systemAddress = 881167680000L + (run * 100L);
    }

    @Test
    void aStationInAnotherSystemIsFoundWhenItIsInsideTheRadius() {
        saveSystem("Near", 10, 0, 0, "Near Dock");
        saveSystem("Far", 60, 0, 0, "Far Dock");

        List<String> found = stationNamesWithin(new LocationDao.Coordinates(star("Here"), 0, 0, 0), 20);

        assertTrue(found.contains(station("Near Dock")), "a station 10 ly away should be reachable from our records");
        assertFalse(found.contains(station("Far Dock")), "a station 60 ly away is outside a 20 ly search");
    }

    /**
     * The database narrows on a cube; the corners of that cube stick out past the radius.
     */
    @Test
    void aStationInTheCornerOfTheSearchBoxIsRejected() {
        // 15,15,15 is inside a 20 ly cube but ~26 ly away.
        saveSystem("Corner", 15, 15, 15, "Corner Dock");

        List<String> found = stationNamesWithin(new LocationDao.Coordinates(star("Here"), 0, 0, 0), 20);

        assertFalse(found.contains(station("Corner Dock")), "26 ly away is outside a 20 ly search");
    }

    @Test
    void stationsComeBackNearestFirst() {
        saveSystem("Middle", 0, 8, 0, "Middle Dock");
        saveSystem("Closest", 0, 2, 0, "Closest Dock");
        saveSystem("Furthest", 0, 15, 0, "Furthest Dock");

        List<String> found = stationNamesWithin(new LocationDao.Coordinates(star("Here"), 0, 0, 0), 20);

        assertEquals(
                List.of(station("Closest Dock"), station("Middle Dock"), station("Furthest Dock")),
                found.stream().filter(name -> name.startsWith(namespace)).toList()
        );
    }

    /**
     * 0,0,0 is what an unrecorded position looks like, so such a system cannot be measured against.
     */
    @Test
    void aSystemWithNoRecordedCoordinatesIsSkipped() {
        saveSystem("Unplaced", 0, 0, 0, "Unplaced Dock");

        List<String> found = stationNamesWithin(new LocationDao.Coordinates(star("Here"), 0, 0, 0), 20);

        assertFalse(found.contains(station("Unplaced Dock")), "a system at the unset origin has no coordinates to search by");
    }

    @Test
    void noCoordinatesToSearchFromYieldsNothingRatherThanEverything() {
        saveSystem("Near", 10, 0, 0, "Near Dock");

        assertTrue(locations.findKnownStationsWithin(null, 20).isEmpty());
    }

    private List<String> stationNamesWithin(LocationDao.Coordinates origin, double lightYears) {
        return locations.findKnownStationsWithin(origin, lightYears).stream()
                .map(LocationDto::getStationName)
                .toList();
    }

    /**
     * One system: its primary star carries the coordinates, and a station orbiting it carries none.
     */
    private void saveSystem(String systemSuffix, double x, double y, double z, String stationSuffix) {
        long address = systemAddress++;
        String starName = star(systemSuffix);

        LocationDto primaryStar = new LocationDto(0L, address);
        primaryStar.setLocationType(LocationDto.LocationType.PRIMARY_STAR);
        primaryStar.setStarName(starName);
        primaryStar.setPlanetName(starName);
        primaryStar.setX(x);
        primaryStar.setY(y);
        primaryStar.setZ(z);
        locations.save(primaryStar);

        LocationDto dock = new LocationDto(1L, address);
        dock.setLocationType(LocationDto.LocationType.STATION);
        dock.setStarName(starName);
        // A docked row is written over the body we dropped at, so it is that body's name that ends up in
        // the locationName column - the station name itself lives in the JSON.
        dock.setPlanetName(station(stationSuffix));
        dock.setStationName(station(stationSuffix));
        locations.save(dock);
    }

    private String star(String suffix) {
        return namespace + " " + suffix;
    }

    private String station(String suffix) {
        return namespace + " " + suffix;
    }
}
