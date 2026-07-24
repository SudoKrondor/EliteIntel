package elite.intel.gameapi.journal.subscribers;

import elite.intel.gameapi.data.BioForms;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression cover for the NPE that killed the organic-scan handler:
 *
 * <pre>Cannot invoke "java.lang.Integer.intValue()" because "distance" is null</pre>
 *
 * <p>The handler runs on a virtual thread, so the throw surfaced only as an uncaught-exception log line while
 * the sample silently went unrecorded and unannounced. The trigger is a genus absent from our tables, which
 * happens routinely: both lookups are keyed by the localised genus name, so a non-English game client misses
 * every one of them.
 */
class ScanOrganicRequiredRangeTest {

    private static BioForms.BioDetails detailsWithRange(Integer colonyRange) {
        return new BioForms.BioDetails(1_000L, 0L, colonyRange, "None", 0.0, 0.0, null);
    }

    @Test
    void unknownGenusYieldsNoRangeInsteadOfThrowing() {
        assertNull(ScanOrganicSubscriber.requiredRange(null, null));
    }

    @Test
    void genusDefaultIsUsedWhenTheSpeciesIsUnknown() {
        assertEquals(500, ScanOrganicSubscriber.requiredRange(null, 500));
    }

    @Test
    void speciesRangeWinsOverTheGenusDefault() {
        assertEquals(150, ScanOrganicSubscriber.requiredRange(detailsWithRange(150), 500));
    }

    @Test
    void knownSpeciesWithNoRecordedRangeYieldsNoRange() {
        // colonyRange is a nullable column on the record; the old (int) cast threw here too.
        assertNull(ScanOrganicSubscriber.requiredRange(detailsWithRange(null), 500));
    }
}
