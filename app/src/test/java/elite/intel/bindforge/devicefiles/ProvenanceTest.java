package elite.intel.bindforge.devicefiles;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The enum and the database column have to agree on four spellings. Nothing in the compiler checks that, so
 * these pin the conversion the manager does at the SQL seam.
 */
class ProvenanceTest {

    /** Lowercase, exactly as the CHECK constraint on {@code bindforge_device_installs} lists them. */
    @Test
    void eachValueStoresAsTheSpellingTheColumnAccepts() {
        assertEquals("frontier", Provenance.FRONTIER.stored());
        assertEquals("user_preexisting", Provenance.USER_PREEXISTING.stored());
        assertEquals("bindforge", Provenance.BINDFORGE.stored());
        assertEquals("unknown", Provenance.UNKNOWN.stored());
    }

    @Test
    void everyValueSurvivesTheRoundTrip() {
        for (Provenance provenance : Provenance.values()) {
            assertEquals(provenance, Provenance.fromStored(provenance.stored()));
        }
    }

    /**
     * A column holding something outside the four would mean the CHECK constraint had been bypassed, so this
     * fails loudly rather than resolving to a default. Guessing here would be guessing at a permission.
     */
    @Test
    void aStoredValueOutsideTheFourIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Provenance.fromStored("not a provenance"));
        assertThrows(IllegalArgumentException.class, () -> Provenance.fromStored(""));
    }
}
