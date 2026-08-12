package elite.intel.ui.overlay;

import elite.intel.db.dao.TradeRouteDao;
import elite.intel.db.managers.TradeRouteManager;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static elite.intel.ui.overlay.HudCards.rowOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Walks a ten-leg route from end to end through the real manager, DAO and card, and reads what the commander
 * would see: LEG 1 OF 10 through LEG 10 OF 10, with the bar agreeing every step of the way.
 * <p>
 * The reported failure was a ten-leg route that announced "LEG 3 OF 6" after one sale, with a "2 / 6" bar
 * above it. Two faults met: one sale deleted every leg ending at that market, and the total was counted from
 * the legs that survived it.
 */
class TradeRouteCardWalkthroughTest {

    /**
     * A loop that revisits two stations, which is what made the old deletion so destructive.
     */
    private static final long MARKET_A = 3223343616L;
    private static final long MARKET_B = 3229009920L;

    private final TradeRouteObjectiveSource source =
            new TradeRouteObjectiveSource(TradeRouteManager.getInstance());

    @BeforeEach
    @AfterEach
    void clearRoute() {
        SystemSession.getInstance().setLanguage(Language.EN);
        Database.withDao(TradeRouteDao.class, dao -> {
            dao.clear();
            return null;
        });
    }

    @Test
    void aTenLegRouteCountsFromOneToTenAsItIsFlown() {
        savePlottedRoute(10);

        List<String> seen = new ArrayList<>();
        for (int flown = 0; flown < 10; flown++) {
            HudObjective card = source.currentObjective().orElseThrow(
                    () -> new AssertionError("the route vanished mid-flight"));
            seen.add(card.subtitle());
            sellAtThisLegsDestination();
        }

        assertEquals(List.of(
                "LEG 1 OF 10", "LEG 2 OF 10", "LEG 3 OF 10", "LEG 4 OF 10", "LEG 5 OF 10",
                "LEG 6 OF 10", "LEG 7 OF 10", "LEG 8 OF 10", "LEG 9 OF 10", "LEG 10 OF 10"), seen);
        assertTrue(source.currentObjective().isEmpty(), "the card goes when the last leg is sold");
    }

    /**
     * The exact complaint: after one sale a ten-leg route said six.
     */
    @Test
    void oneSaleDoesNotShortenATenLegRoute() {
        savePlottedRoute(10);

        sellAtThisLegsDestination();

        HudObjective card = source.currentObjective().orElseThrow();
        assertEquals("LEG 2 OF 10", card.subtitle());
    }

    @Test
    void theBarAndTheSubtitleNeverDisagree() {
        savePlottedRoute(10);
        sellAtThisLegsDestination();
        sellAtThisLegsDestination();

        HudObjective card = source.currentObjective().orElseThrow();
        HudRow legs = rowOf(card, "LEGS");

        assertEquals("LEG 3 OF 10", card.subtitle());
        assertEquals(3, legs.current(), "the bar counts the leg being flown, like the subtitle");
        assertEquals(10, legs.max());
    }

    /**
     * Stores a plotted route the way {@code TradeRouteManager.save} does: dense 1-based legs, length on each.
     */
    private static void savePlottedRoute(int legs) {
        Database.withDao(TradeRouteDao.class, dao -> {
            for (int i = 1; i <= legs; i++) {
                long destination = i % 2 == 0 ? MARKET_A : MARKET_B;
                TradeRouteDao.TradeRoute leg = new TradeRouteDao.TradeRoute();
                leg.setLegNumber(i);
                leg.setTotalLegs(legs);
                leg.setJson("{\"stopNumber\":" + i
                        + ",\"sourceSystem\":\"Sol\",\"sourceStation\":\"Abraham Lincoln\""
                        + ",\"destinationSystem\":\"Alpha Centauri\",\"destinationStation\":\"Hutton Orbital\""
                        + ",\"destinationMarketId\":" + destination + "}");
                dao.save(leg);
            }
            return null;
        });
    }

    /**
     * The commander docks and sells: the leg they were flying is retired.
     */
    private static void sellAtThisLegsDestination() {
        var next = TradeRouteManager.getInstance().getNextStop();
        if (next == null) return;
        TradeRouteManager.getInstance().deleteForMarketId(next.getTradeStopDto().getDestinationMarketId());
    }
}
