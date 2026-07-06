package elite.intel.session;

import elite.intel.gameapi.journal.events.dto.LocationDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards {@link Status#getSituation(long, long, LocationDto)}: the flag-only overload is pure bit decoding,
 * so it is exercised here with the documented Elite Dangerous Status.json bit values (no database).
 */
class PlayerSituationTest {

    // Flags (ship/flight) - Status.json documented bit values.
    private static final long DOCKED = 1L;
    private static final long LANDED = 2L;
    private static final long SUPERCRUISE = 16L;
    private static final long HAS_LAT_LONG = 2097152L;
    private static final long IN_MAIN_SHIP = 16777216L;
    private static final long IN_FIGHTER = 33554432L;
    private static final long IN_SRV = 67108864L;

    // Flags2 (on-foot/suit) - Status.json documented bit values.
    private static final long ON_FOOT = 1L;
    private static final long IN_TAXI = 2L;
    private static final long ON_FOOT_IN_STATION = 8L;
    private static final long ON_FOOT_ON_PLANET = 16L;
    private static final long GLIDE_MODE = 4096L;
    private static final long ON_FOOT_IN_HANGAR = 8192L;
    private static final long ON_FOOT_SOCIAL_SPACE = 16384L;

    private final Status status = Status.getInstance();

    private PlayerSituation classify(long flags, long flags2, LocationDto location) {
        return status.getSituation(flags, flags2, location);
    }

    @Test
    void inShipDocked() {
        assertEquals(PlayerSituation.IN_SHIP_DOCKED, classify(IN_MAIN_SHIP | DOCKED, 0, null));
    }

    @Test
    void inShipLanded() {
        assertEquals(PlayerSituation.IN_SHIP_LANDED, classify(IN_MAIN_SHIP | LANDED, 0, null));
    }

    @Test
    void inShipSupercruise() {
        assertEquals(PlayerSituation.IN_SHIP_SUPERCRUISE, classify(IN_MAIN_SHIP | SUPERCRUISE, 0, null));
    }

    @Test
    void glideWinsOverSupercruise() {
        assertEquals(PlayerSituation.IN_SHIP_GLIDE, classify(IN_MAIN_SHIP | SUPERCRUISE, GLIDE_MODE, null));
    }

    @Test
    void inShipRingFromLocation() {
        LocationDto ring = new LocationDto(LocationDto.LocationType.PLANETARY_RING);
        assertEquals(PlayerSituation.IN_SHIP_RING, classify(IN_MAIN_SHIP, 0, ring));
    }

    @Test
    void supercruiseIgnoresRingLocation() {
        LocationDto ring = new LocationDto(LocationDto.LocationType.PLANETARY_RING);
        assertEquals(PlayerSituation.IN_SHIP_SUPERCRUISE, classify(IN_MAIN_SHIP | SUPERCRUISE, 0, ring));
    }

    @Test
    void inShipOrbitWhenLatLongPresent() {
        assertEquals(PlayerSituation.IN_SHIP_ORBIT, classify(IN_MAIN_SHIP | HAS_LAT_LONG, 0, null));
    }

    @Test
    void inShipDeepSpace() {
        assertEquals(PlayerSituation.IN_SHIP_DEEP_SPACE, classify(IN_MAIN_SHIP, 0, null));
    }

    @Test
    void inSrv() {
        assertEquals(PlayerSituation.IN_SRV, classify(IN_SRV, 0, null));
    }

    @Test
    void inFighter() {
        assertEquals(PlayerSituation.IN_FIGHTER, classify(IN_FIGHTER, 0, null));
    }

    @Test
    void inTaxi() {
        assertEquals(PlayerSituation.IN_TAXI, classify(0, IN_TAXI, null));
    }

    @Test
    void onFootInStation() {
        assertEquals(PlayerSituation.ON_FOOT_STATION, classify(0, ON_FOOT | ON_FOOT_IN_STATION, null));
    }

    @Test
    void onFootInHangar() {
        assertEquals(PlayerSituation.ON_FOOT_HANGAR, classify(0, ON_FOOT | ON_FOOT_IN_HANGAR, null));
    }

    @Test
    void onFootSocialSpace() {
        assertEquals(PlayerSituation.ON_FOOT_SOCIAL, classify(0, ON_FOOT | ON_FOOT_SOCIAL_SPACE, null));
    }

    @Test
    void onFootOnPlanet() {
        assertEquals(PlayerSituation.ON_FOOT_PLANET, classify(0, ON_FOOT | ON_FOOT_ON_PLANET, null));
    }

    @Test
    void onFootGeneric() {
        assertEquals(PlayerSituation.ON_FOOT, classify(0, ON_FOOT, null));
    }

    @Test
    void unknownWhenNothingSet() {
        assertEquals(PlayerSituation.UNKNOWN, classify(0, 0, null));
    }

    @Test
    void flagsForSituationRoundTripsThroughClassifier() {
        for (PlayerSituation situation : PlayerSituation.values()) {
            long[] flags = StatusFlags.flagsForSituation(situation);
            // IN_SHIP_RING is location-derived, not a flag, so its flags classify as deep space (documented
            // approximation in flagsForSituation); every other situation round-trips exactly.
            PlayerSituation expected = situation == PlayerSituation.IN_SHIP_RING
                    ? PlayerSituation.IN_SHIP_DEEP_SPACE
                    : situation;
            assertEquals(expected, classify(flags[0], flags[1], null),
                    "flagsForSituation round-trip for " + situation);
        }
    }

    @Test
    void detachedStatusReportsSituationFlags() {
        Status docked = Status.detached(PlayerSituation.IN_SHIP_DOCKED);
        assertTrue(docked.isDocked());
        assertTrue(docked.isInMainShip());
        assertEquals(PlayerSituation.IN_SHIP_DOCKED, docked.getSituation(null));
    }
}
