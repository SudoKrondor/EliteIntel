package elite.intel.gameapi.search.spansh.station;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The ladder every station search climbs rather than call a thing that exists nonexistent.
 */
class SearchRadiiTest {

    @Test
    void theRadiusWidensTwiceBeforeGivingUp() {
        assertEquals(List.of(40, 80, 1000), SearchRadii.widening(40));
    }

    /**
     * A commander who already asked for a wide sweep keeps it: the bubble rung must never narrow the search.
     */
    @Test
    void anAlreadyWideAskIsNeverNarrowed() {
        assertEquals(List.of(2000, 4000), SearchRadii.widening(2000));
    }
}
