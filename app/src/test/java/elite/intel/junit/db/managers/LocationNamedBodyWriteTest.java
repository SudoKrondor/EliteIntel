package elite.intel.junit.db.managers;

import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.DockedMarket;
import elite.intel.session.PlayerSession;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A place is addressed by its name, because a BodyID does not identify one. Docking writes the station's
 * record under the BodyID of the body we dropped at, so a body with a station - or with a fleet carrier
 * parked at it - answers to the same BodyID twice. Writing through the ID picked whichever record came
 * first: that is how a moon came to be stored as a FLEET_CARRIER and narrated as one.
 */
class LocationNamedBodyWriteTest {

    private static final AtomicInteger RUN = new AtomicInteger();

    private final LocationManager locations = LocationManager.getInstance();

    private long systemAddress;
    private long bodyId;
    private String star;
    private String planet;
    private String carrier;

    @BeforeEach
    void setUp() {
        int run = RUN.incrementAndGet();
        star = String.format("Named Body Test %03d", run);
        systemAddress = 2283077046962L + run;
        bodyId = 25;
        planet = star + " 7 a";
        carrier = "GHY-L8X " + run;

        LocationDto body = new LocationDto(bodyId, systemAddress);
        body.setStarName(star);
        body.setPlanetName(planet);
        body.setLocationType(MOON);
        body.setGravity(0.08);
        locations.save(body);

        // What docking at a carrier parked here leaves behind: the body's record, re-labelled and saved
        // under the carrier's name, still claiming the body's BodyID.
        LocationDto dockedAtCarrier = new LocationDto(bodyId, systemAddress);
        dockedAtCarrier.setStarName(star);
        dockedAtCarrier.setStationName(carrier);
        dockedAtCarrier.setLocationType(FLEET_CARRIER);
        locations.save(dockedAtCarrier);
    }

    @Test
    void aDropAtTheBodyWritesTheBodyAndNotTheCarrierParkedAtIt() {
        locations.updateNamedBody(systemAddress, bodyId, planet, here -> here.setBodyType("Planet"));

        LocationDto body = locations.findBySystemAddress(systemAddress, planet);
        assertEquals("Planet", body.getBodyType());
        assertEquals(MOON, body.getLocationType(), "the body's own classification must survive the drop");
        assertEquals(0.08, body.getGravity(), 1e-9, "the body's scan data must survive the drop");

        LocationDto stillACarrier = storedUnder(carrier);
        assertEquals(FLEET_CARRIER, stillACarrier.getLocationType());
        assertNull(stillACarrier.getBodyType(), "the drop was at the body, so it must not touch the carrier");
    }

    @Test
    void aNamesakeInAnotherSystemIsNotAdopted() {
        // Station names repeat across the galaxy; the record under that name may belong to someone else.
        String station = "Abasheli City " + systemAddress;
        LocationDto elsewhere = new LocationDto(46L, systemAddress + 500_000L);
        elsewhere.setStarName(star + " Far");
        elsewhere.setStationName(station);
        elsewhere.setGravity(3.0);
        locations.save(elsewhere);

        locations.updateNamedBody(systemAddress, 46L, station, here -> {
            here.setStarName(star);
            here.setStationName(station);
            here.setLocationType(STATION);
        });

        LocationDto stored = storedUnder(station);
        assertEquals(STATION, stored.getLocationType());
        assertEquals(0.0, stored.getGravity(), 1e-9, "the namesake's data must not follow its name");
        assertEquals(systemAddress, stored.getSystemAddress());
    }

    @Test
    void theCurrentStationIsThePadWeAreOn_notTheBodyItIsParkedAt() {
        String carrierName = "GHY-L8X current " + systemAddress;
        long marketId = 3_712_500_736L + systemAddress % 1000;

        LocationDto parkedHere = new LocationDto(marketId, systemAddress);
        parkedHere.setStarName(star);
        parkedHere.setStationName(carrierName);
        parkedHere.setMarketID(marketId);
        parkedHere.setLocationType(FLEET_CARRIER);
        locations.save(parkedHere);

        // The ship is on the carrier's pad, but the current location is still the moon it is parked at.
        PlayerSession.getInstance().setCurrentLocationId(bodyId, systemAddress);
        DockedMarket.getInstance().arrived(marketId, carrierName);
        try {
            LocationDto here = locations.findCurrentStation();
            assertEquals(carrierName, here.getStationName());
            assertEquals(FLEET_CARRIER, here.getLocationType());
        } finally {
            DockedMarket.getInstance().departed();
        }

        // Off the pad, the question falls back to where we are.
        LocationDto offPad = locations.findCurrentStation();
        assertEquals(planet, offPad.getPlanetName());
    }

    /**
     * Read straight through the table's unique key. {@code findAllBySystemAddress} cannot be used here: it
     * collapses its results into a map keyed by BodyID, so of two records sharing one it returns a single row.
     */
    private static LocationDto storedUnder(String locationName) {
        return Database.withDao(LocationDao.class, dao -> {
            LocationDao.Location row = dao.findByLocationName(locationName);
            assertNotNull(row, "no record stored under " + locationName);
            return GsonFactory.getGson().fromJson(row.getJson(), LocationDto.class);
        });
    }
}
