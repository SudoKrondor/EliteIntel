package elite.intel.ui.overlay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Completed legs are DELETEd from the route table as they are flown, so the
 * route's original length is recorded nowhere. It is recovered from the
 * surviving 1-based legNumber. If that drifts, the overlay confidently shows a
 * wrong "2 of 3" and nothing else catches it.
 */
class TradeRouteProgressTest {

    @Test
    void aFreshRouteHasFlownNothing() {
        assertEquals(0, TradeRouteObjectiveSource.legsCompleted(1));
        assertEquals(3, TradeRouteObjectiveSource.legsTotal(1, 3));
    }

    @Test
    void midRouteRecoversTheLegsWhoseRowsAreGone() {
        // legs 1 and 2 flown and deleted; 3, 4, 5 remain
        assertEquals(2, TradeRouteObjectiveSource.legsCompleted(3));
        assertEquals(5, TradeRouteObjectiveSource.legsTotal(3, 3));
    }

    @Test
    void theLastLegStillReportsTheWholeRoute() {
        assertEquals(4, TradeRouteObjectiveSource.legsCompleted(5));
        assertEquals(5, TradeRouteObjectiveSource.legsTotal(5, 1));
    }

    @Test
    void aLegNumberBelowOneCannotProduceNegativeProgress() {
        // Defensive: a 0/absent legNumber must not render as "-1 of N".
        assertEquals(0, TradeRouteObjectiveSource.legsCompleted(0));
        assertEquals(2, TradeRouteObjectiveSource.legsTotal(0, 2));
    }
}
