package elite.intel.gameapi.search.spansh.commodity;

import elite.intel.db.managers.StationMarketsManager;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spansh is a crowd-sourced copy of a market, and its price is whatever the last commander through
 * uploaded. It does not go stale gracefully: a market's price moves as its demand is consumed.
 *
 * <p>Measured live on 2026-08-28. Spansh quoted Bari Gateway in Col 285 Sector IB-X d1-60 at 57,844 a tonne
 * for Tritium with 14,713 demand, from a row stamped 2026-08-18. The commander crossed two systems on the
 * strength of that number and the board was paying 53,992 against 6,862 demand - 3.4 million credits short
 * over a full hold.
 */
class StalePriceTest {

    /**
     * Spansh stamps its rows {@code 2026-08-18 04:56:12+00}: a space instead of the T, an offset of
     * {@code +00} rather than a Z. {@link Instant#parse} throws on every one, which made every row look
     * undated - so the first-hand override won unconditionally and a quote's age was unknowable.
     */
    @Test
    void spanshsOwnTimestampFormatIsUnderstood() {
        assertEquals(Instant.parse("2026-08-18T04:56:12Z"),
                SpanshCommoditySearch.parseInstant("2026-08-18 04:56:12+00"));
        assertEquals(Instant.parse("2026-08-18T04:56:12Z"),
                SpanshCommoditySearch.parseInstant("2026-08-18 04:56:12+00:00"));
        assertEquals(Instant.parse("2026-08-18T04:56:12Z"),
                SpanshCommoditySearch.parseInstant("2026-08-18T04:56:12Z"), "the ISO form still works");
        assertNull(SpanshCommoditySearch.parseInstant("rubbish"));
        assertNull(SpanshCommoditySearch.parseInstant(null));
    }

    @Test
    void theAgeOfAQuoteIsMeasurable() {
        String tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS).toString().replace("T", " ").substring(0, 19) + "+00";

        assertEquals(10, SpanshCommoditySearch.daysSinceUpdate(tenDaysAgo).orElseThrow());
        assertEquals(OptionalLong.empty(), SpanshCommoditySearch.daysSinceUpdate(null));
        assertEquals(OptionalLong.empty(), SpanshCommoditySearch.daysSinceUpdate("not a date"));
    }

    @Test
    void ourOwnLookAtTheBoardCorrectsThePriceNotJustTheQuantity() {
        CommoditySearchResult bari = spanshSaid("Col 285 Sector IB-X d1-60", "Bari Gateway", 57844, 14713);

        List<CommoditySearchResult> kept = SpanshCommoditySearch.correctWithFirstHandData(
                List.of(bari), "tritium", weSaw(6862, 53992), TradeSide.SELL);

        assertEquals(53992, kept.getFirst().getPrice(), "what the game was actually paying");
        assertEquals(6862, kept.getFirst().getSupply());
        assertTrue(kept.getFirst().isSeenFirstHand(), "no need to hedge a figure the game gave us");
    }

    @Test
    void aStaleSightingNoLongerOverridesAFresherSpanshRow() {
        // This is what the unparseable timestamp was hiding: every row counted as undated, so an old
        // sighting won every time and one empty visit blacklisted a market for good.
        CommoditySearchResult market = spanshSaid("Col 285 Sector IB-X d1-60", "Bari Gateway", 57844, 14713);
        market.setMarketUpdatedAt(Instant.now().toString());

        List<CommoditySearchResult> kept = SpanshCommoditySearch.correctWithFirstHandData(
                List.of(market), "tritium",
                (system, station, symbol) -> Optional.of(new StationMarketsManager.Sighting(
                        system, station, Instant.now().minus(30, ChronoUnit.DAYS), 0, 0, 0, 0)),
                TradeSide.SELL);

        assertEquals(1, kept.size(), "Spansh has heard about it more recently than we stood in it");
        assertEquals(57844, kept.getFirst().getPrice());
    }

    /**
     * The correction has to land before the sort, or the order is decided on figures no longer in the
     * results - which is how a market corrected down to 53,992 could still lead a best-price search.
     */
    @Test
    void aMarketCorrectedDownwardsLosesItsPlaceAtTheHead() {
        List<CommoditySearchResult> corrected = SpanshCommoditySearch.correctWithFirstHandData(
                List.of(spanshSaid("Col 285 Sector IB-X d1-60", "Bari Gateway", 57844, 14713),
                        spanshSaid("Col 285 Sector IB-X d1-32", "Love Hub", 57000, 212636)),
                "tritium",
                (system, station, symbol) -> "Bari Gateway".equals(station)
                        ? weSaw(6862, 53992).lastSeen(system, station, symbol)
                        : Optional.empty(),
                TradeSide.SELL);

        List<CommoditySearchResult> ranked = SpanshCommoditySearch.sortBest(corrected, false, TradeSide.SELL);

        assertEquals("Love Hub", ranked.getFirst().getStationName(),
                "once Bari is known to pay 53,992 it is no longer the best-paying market");
    }

    private static SpanshCommoditySearch.MarketSightings weSaw(int demand, int sellPrice) {
        return (system, station, symbol) -> Optional.of(new StationMarketsManager.Sighting(
                system, station, Instant.now(), 0, demand, 0, sellPrice));
    }

    private static CommoditySearchResult spanshSaid(String system, String station, double price, long units) {
        CommoditySearchResult result = new CommoditySearchResult();
        result.setStarSystem(system);
        result.setStationName(station);
        result.setStationType("Coriolis Starport");
        result.setPrice(price);
        result.setSupply(units);
        result.setMarketUpdatedAt("2026-08-18 04:56:12+00");
        return result;
    }
}
