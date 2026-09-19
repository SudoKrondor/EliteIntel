package elite.intel.gameapi.journal;

import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.LocationDto.LocationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Classification read off a body's designation alone. The FSS writes only FSSBodySignals for a body the commander
 * resolved on an earlier visit - no Scan follows - so 76 Leonis 6 b to 6 g arrived as rows with seven bio signals
 * each and no kind, and the system was reported as having one bio moon.
 */
class BodyNameClassifierTest {

    private static final String SYSTEM = "76 Leonis";

    @Test
    void aPlanetNumberFollowedByALetterIsAMoon() {
        assertEquals(LocationType.MOON, ScanBodyClassifier.classifyByName(SYSTEM, "76 Leonis 6 c"));
    }

    @Test
    void aBarePlanetNumberIsAPlanet() {
        assertEquals(LocationType.PLANET, ScanBodyClassifier.classifyByName(SYSTEM, "76 Leonis 6"));
    }

    @Test
    void starLettersAheadOfTheNumberStillNameAPlanetOrMoon() {
        String system = "Stuelou AF-P d6-996";
        assertEquals(LocationType.PLANET, ScanBodyClassifier.classifyByName(system, "Stuelou AF-P d6-996 A 2"));
        assertEquals(LocationType.MOON, ScanBodyClassifier.classifyByName(system, "Stuelou AF-P d6-996 A 2 a"));
        assertEquals(LocationType.PLANET, ScanBodyClassifier.classifyByName(system, "Stuelou AF-P d6-996 AB 1"));
    }

    @Test
    void aMoonOfAMoonIsStillAMoon() {
        assertEquals(LocationType.MOON, ScanBodyClassifier.classifyByName(SYSTEM, "76 Leonis 6 a a"));
    }

    @Test
    void starLettersAloneNameAStar() {
        assertEquals(LocationType.STAR, ScanBodyClassifier.classifyByName(SYSTEM, "76 Leonis B"));
    }

    @Test
    void ringsAndBeltsKeepTheirOwnKinds() {
        assertEquals(LocationType.PLANETARY_RING, ScanBodyClassifier.classifyByName(SYSTEM, "76 Leonis 6 A Ring"));
        assertEquals(LocationType.BELT_CLUSTER, ScanBodyClassifier.classifyByName(SYSTEM, "76 Leonis A Belt Cluster 1"));
    }

    /**
     * A system name ending in digits must not have its own name read as a planet number.
     */
    @Test
    void theSystemItselfIsNotABody() {
        assertNull(ScanBodyClassifier.classifyByName("HIP 12345", "HIP 12345"));
        assertNull(ScanBodyClassifier.classifyByName("Hyades Sector MH-V c2-8", "Hyades Sector MH-V c2-8"));
    }

    @Test
    void namedBodiesAndStationsAreNotGuessedAt() {
        assertNull(ScanBodyClassifier.classifyByName("Sol", "Earth"));
        assertNull(ScanBodyClassifier.classifyByName(SYSTEM, "Jameson Memorial"));
        assertNull(ScanBodyClassifier.classifyByName(SYSTEM, "Sawait 4"), "a body of another system");
    }

    @Test
    void nothingIsGuessedWithoutASystemName() {
        assertNull(ScanBodyClassifier.classifyByName(null, "76 Leonis 6 c"));
        assertNull(ScanBodyClassifier.classifyByName("", "76 Leonis 6 c"));
        assertNull(ScanBodyClassifier.classifyByName(SYSTEM, null));
    }

    /**
     * The stored-record rule both the FSS writer and the system tally apply: the tallies group bodies by kind,
     * so a row that carries bio signals but no kind is left out of "moons with bio signals".
     */
    @Test
    void anUnclassifiedRecordIsResolvedFromItsName() {
        LocationDto row = new LocationDto(27L, 358999069386L);
        row.setStarName(SYSTEM);
        row.setPlanetName("76 Leonis 6 c");
        row.setBioSignals(7);

        assertEquals(LocationType.MOON, ScanBodyClassifier.resolve(row));
    }

    @Test
    void aKindAScanEstablishedIsNeverSecondGuessed() {
        LocationDto row = new LocationDto(25L, 358999069386L);
        row.setStarName(SYSTEM);
        row.setPlanetName("76 Leonis 6 a");
        row.setLocationType(LocationType.PLANET);

        assertEquals(LocationType.PLANET, ScanBodyClassifier.resolve(row));
    }

    @Test
    void aRecordWhoseNameIsNotADesignationIsLeftAsItWas() {
        LocationDto row = new LocationDto(3L, 10477373803L);
        row.setStarName("Sol");
        row.setPlanetName("Earth");

        assertEquals(LocationType.UNCLASSIFIED, ScanBodyClassifier.resolve(row));
    }
}
