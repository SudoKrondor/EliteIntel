package elite.intel.ui.overlay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The card shows one number for where the commander is in the route, in both places it appears.
 * <p>
 * It used to show legs *finished* on the bar and the leg *being flown* in the subtitle - "2 / 6" above
 * "LEG 3 OF 6" - two figures for one thing, which reads as an error. Worse, the total was inferred from the
 * rows still in the table, so a route that lost legs any other way reported a length that shrank as it went.
 * The length is now recorded when the route is plotted; the inference survives only for older routes.
 */
class TradeRouteProgressTest {

    @Test
    void theRecordedLengthIsWhatTheCardShows() {
        // Ten legs plotted, two flown: the route is still ten legs long.
        assertEquals(10, TradeRouteObjectiveSource.routeLength(10, 3, 8));
    }

    /**
     * The reported case: a ten-leg route whose first sale deleted six legs. Counting the survivors called it a
     * six-leg route; the recorded length still knows better, so the card cannot repeat that.
     */
    @Test
    void aRouteThatLostLegsStillReportsItsRealLength() {
        assertEquals(10, TradeRouteObjectiveSource.routeLength(10, 3, 4));
        assertEquals(6, TradeRouteObjectiveSource.legsTotal(3, 4), "what the old inference made of it");
    }

    @Test
    void aRoutePlottedBeforeTheLengthWasRecordedFallsBackToCounting() {
        assertEquals(5, TradeRouteObjectiveSource.routeLength(0, 3, 3));
        assertEquals(3, TradeRouteObjectiveSource.routeLength(0, 1, 3));
    }

    @Test
    void aRouteIsNeverShorterThanTheLegBeingFlown() {
        // Defensive: a stale or absent length must not render "LEG 7 OF 6".
        assertEquals(7, TradeRouteObjectiveSource.routeLength(6, 7, 0));
        assertEquals(1, TradeRouteObjectiveSource.routeLength(0, 0, 0));
    }

    // ── the inference kept for older routes ──────────────────────────────────

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
