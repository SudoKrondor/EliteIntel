package elite.intel.db.managers;

import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.Test;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.FLEET_CARRIER;
import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.MOON;
import static org.junit.jupiter.api.Assertions.*;

/**
 * What names a system for the writers that file bodies under it. A system entered by carrier, or one the
 * session started docked in, never gets a PRIMARY_STAR record - only a jump writes that - so the name has to
 * come from whatever record the address holds, and from the session for the first body in a system nothing
 * has written to yet. Addresses here are shared with no other test: the table lives across the run.
 */
class LocationManagerStarNameTest {

    private final LocationManager locations = LocationManager.getInstance();
    private final PlayerSession player = PlayerSession.getInstance();

    @Test
    void anyRecordUnderTheAddressNamesTheSystem() {
        long systemAddress = 358999069401L;
        LocationDto carrier = new LocationDto(3712500736L, systemAddress);
        carrier.setStarName("76 Leonis");
        carrier.setStationName("GHY-L8X");
        carrier.setLocationType(FLEET_CARRIER);
        locations.save(carrier);

        assertEquals("76 Leonis", locations.findStarName(systemAddress));
    }

    @Test
    void theSessionNamesTheSystemWhenNothingIsOnRecordForIt() {
        long systemAddress = 358999069402L;
        player.setCurrentLocationId(0L, systemAddress);
        player.setCurrentPrimaryStarName("Synuefe XR-H d11-102");

        assertEquals("Synuefe XR-H d11-102", locations.findStarName(systemAddress));
    }

    @Test
    void aSystemNeitherOnRecordNorCurrentHasNoName() {
        long systemAddress = 358999069403L;
        player.setCurrentLocationId(0L, 358999069404L);
        player.setCurrentPrimaryStarName("Somewhere Else");

        assertNull(locations.findStarName(systemAddress));
    }

    /**
     * A row is keyed on its name for life, but a carrier's row moves system with the carrier: the system
     * column must follow the record, or a lookup by system name finds the carrier where it no longer is.
     */
    @Test
    void aResavedRecordIsFoundUnderItsNewSystem() {
        String carrierName = "TEST-CARRIER-STARNAME";
        LocationDto carrier = new LocationDto(3712500737L, 358999069405L);
        carrier.setStarName("Departure Test System");
        carrier.setStationName(carrierName);
        carrier.setLocationType(FLEET_CARRIER);
        locations.save(carrier);

        carrier.setStarName("Arrival Test System");
        carrier.setSystemAddress(358999069406L);
        locations.save(carrier);

        assertTrue(locations.findByPrimaryStar("Departure Test System").values().stream()
                .noneMatch(row -> carrierName.equals(row.getStationName())), "still filed under the system it left");
        assertTrue(locations.findByPrimaryStar("Arrival Test System").values().stream()
                .anyMatch(row -> carrierName.equals(row.getStationName())), "not filed under the system it is in");
    }

    @Test
    void aBlankNameOnRecordDoesNotCountAsAName() {
        long systemAddress = 358999069407L;
        LocationDto moon = new LocationDto(12L, systemAddress);
        moon.setStarName("");
        moon.setPlanetName("Unnamed Test Moon");
        moon.setLocationType(MOON);
        locations.save(moon);
        player.setCurrentLocationId(0L, 1L);

        assertNull(locations.findStarName(systemAddress));
    }
}
