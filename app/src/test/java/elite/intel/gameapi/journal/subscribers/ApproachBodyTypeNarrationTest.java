package elite.intel.gameapi.journal.subscribers;

import elite.intel.gameapi.journal.events.dto.LocationDto;
import org.junit.jupiter.api.Test;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Approaching Hyades Sector MH-V c2-8 7 a was narrated as "we're in orbit for FLEET_CARRIER Hyades Sector
 * MH-V c2-8 7 a": the payload handed the narrator {@code LocationType.name()}, and the moon's record had been
 * typed FLEET_CARRIER by docking at a carrier parked at the same BodyID.
 */
class ApproachBodyTypeNarrationTest {

    @Test
    void aStationTypeContributesNoWordToTheReport() {
        // Whatever the record says, the narrator must never be handed an enum constant to read out.
        assertEquals("", ApproachBodySubscriber.bodyNoun(FLEET_CARRIER));
        assertEquals("", ApproachBodySubscriber.bodyNoun(STATION));
        assertEquals("", ApproachBodySubscriber.bodyNoun(null));
    }

    @Test
    void aBodyIsNamedInWords() {
        String planet = ApproachBodySubscriber.bodyNoun(PLANET);
        String moon = ApproachBodySubscriber.bodyNoun(MOON);
        assertFalse(planet.isBlank());
        assertFalse(moon.isBlank());
        assertNotEquals(PLANET.name(), planet);
        assertNotEquals(MOON.name(), moon);
    }

    @Test
    void aBodyLeftTypedAsAPlaceIsCorrected() {
        assertEquals(PLANET, typeAfterApproach(FLEET_CARRIER, null));
        assertEquals(PLANET, typeAfterApproach(STATION, null));
        // Only a moon's scan records a parent body, so it is the one kind still recoverable.
        assertEquals(MOON, typeAfterApproach(FLEET_CARRIER, 25L));
    }

    @Test
    void aBodyTypeIsLeftAlone() {
        assertEquals(MOON, typeAfterApproach(MOON, 25L));
        assertEquals(PLANET, typeAfterApproach(PLANET, null));
        assertNull(typeAfterApproach(null, null));
    }

    private static LocationDto.LocationType typeAfterApproach(LocationDto.LocationType stored, Long parentBodyId) {
        LocationDto location = new LocationDto(25L, 2283077046962L);
        location.setLocationType(stored);
        location.setParentBodyId(parentBodyId);
        ApproachBodySubscriber.restoreBodyType(location);
        return location.getLocationType();
    }
}
