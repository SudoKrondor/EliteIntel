package elite.intel.gameapi;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StarKindTest {

    @ParameterizedTest
    @ValueSource(strings = {"N", "n"})
    void neutronStars(String code) {
        assertEquals(StarKind.NEUTRON, StarKind.of(code));
    }

    @ParameterizedTest
    @ValueSource(strings = {"D", "DA", "DAB", "DAO", "DAZ", "DAV", "DB", "DBZ", "DBV", "DO", "DOV", "DQ", "DC", "DCV", "DX"})
    void whiteDwarfs(String code) {
        assertEquals(StarKind.WHITE_DWARF, StarKind.of(code));
    }

    @ParameterizedTest
    @ValueSource(strings = {"W", "WN", "WNC", "WC", "WO"})
    void wolfRayetStars(String code) {
        assertEquals(StarKind.WOLF_RAYET, StarKind.of(code));
    }

    @ParameterizedTest
    @ValueSource(strings = {"X", "RoguePlanet", "Nebula", "StellarRemnantNebula"})
    void rareObjects(String code) {
        assertEquals(StarKind.RARE, StarKind.of(code));
    }

    /**
     * O is a scoopable main-sequence star, and WO and DO are not: the codes have to match whole.
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "O", "B", "A", "F", "G", "K", "M", "H", "L", "T", "Y", "TTS", "C", "S"})
    void everythingElseIsOrdinary(String code) {
        assertEquals(StarKind.ORDINARY, StarKind.of(code));
    }
}
