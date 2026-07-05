package elite.intel.junit.gameapi.journal.events.dto;

import elite.intel.gameapi.journal.events.dto.LocationDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The body name is the authoritative ring signal, so a "<parent> <letter> Ring" body must classify
 * as PLANETARY_RING regardless of which code path last wrote it - this centralizes the guard so we
 * stop chasing individual classifiers (Scan, FSS, Location, pre-scan).
 */
class LocationDtoRingTest {

    @Test
    void ringNameLocksInPlanetaryRingType() {
        LocationDto dto = new LocationDto(15L, 633608901490L);
        dto.setPlanetName("Praea Euq XN-E c13-2 2 A Ring");
        assertEquals(LocationDto.LocationType.PLANETARY_RING, dto.getLocationType());
        assertEquals("Praea Euq XN-E c13-2 2", dto.getParentBodyName());
    }

    @Test
    void ringCannotBeDowngradedByAnotherClassifier() {
        LocationDto dto = new LocationDto(15L, 633608901490L);
        dto.setPlanetName("Barnard's Star 5 B Ring");
        // A later classifier (e.g. Scan/pre-scan seeing a planet parent) tries to call it a MOON.
        dto.setLocationType(LocationDto.LocationType.MOON);
        assertEquals(LocationDto.LocationType.PLANETARY_RING, dto.getLocationType());
    }

    @Test
    void typeSetBeforeNameIsCorrectedWhenRingNameArrives() {
        LocationDto dto = new LocationDto(15L, 633608901490L);
        // Some handlers set the type first (default classifiers), then the name.
        dto.setLocationType(LocationDto.LocationType.MOON);
        dto.setPlanetName("Sol 4 A Ring");
        assertEquals(LocationDto.LocationType.PLANETARY_RING, dto.getLocationType());
    }

    @Test
    void nonRingBodiesAreUnaffected() {
        LocationDto planet = new LocationDto(9L, 1L);
        planet.setPlanetName("Praea Euq VM-H b27-0 A 1");
        planet.setLocationType(LocationDto.LocationType.PLANET);
        assertEquals(LocationDto.LocationType.PLANET, planet.getLocationType());

        // "Belt Cluster" and words merely ending in "ring" must not be treated as rings.
        LocationDto belt = new LocationDto(4L, 1L);
        belt.setPlanetName("Praea Euq VM-H b27-0 A A Belt Cluster 1");
        belt.setLocationType(LocationDto.LocationType.BELT_CLUSTER);
        assertEquals(LocationDto.LocationType.BELT_CLUSTER, belt.getLocationType());
    }
}
