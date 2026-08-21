package elite.intel.gameapi.search.spansh.commodity;

import elite.intel.db.managers.StationMarketsManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spansh is crowd-sourced and goes stale. The case this exists for is real: Boldyr Dredging
 * Installation in Mat Zemlya is listed as selling Silver at 25,961, and the game's own Market.json
 * for that settlement reports Stock 0. The commander flew there, found nothing, asked again, and was
 * sent back to the settlement he was standing in.
 */
class FirstHandMarketDataTest {

    private static final Instant SPANSH_SYNC = Instant.parse("2026-08-20T09:00:00Z");
    private static final Instant WE_WERE_THERE = Instant.parse("2026-08-21T10:21:53Z");

    private static CommoditySearchResult market(String system, String station, long supply, Instant updatedAt) {
        CommoditySearchResult result = new CommoditySearchResult();
        result.setStarSystem(system);
        result.setStationName(station);
        result.setCommodity("Silver");
        result.setSupply(supply);
        result.setMarketUpdatedAt(updatedAt == null ? null : updatedAt.toString());
        return result;
    }

    private static SpanshCommoditySearch.MarketSightings sawStock(int stock, Instant seenAt) {
        return (system, station, symbol) ->
                Optional.of(new StationMarketsManager.Sighting(system, station, seenAt, stock));
    }

    private static final SpanshCommoditySearch.MarketSightings NEVER_BEEN_THERE =
            (system, station, symbol) -> Optional.empty();

    @Test
    @DisplayName("a market we emptied ourselves is dropped, however confident Spansh is")
    void ourOwnEmptyMarketIsDropped() {
        List<CommoditySearchResult> markets = List.of(market("Mat Zemlya", "Boldyr Dredging Installation", 340, SPANSH_SYNC));

        List<CommoditySearchResult> kept = SpanshCommoditySearch.correctWithFirstHandData(
                markets, "silver", sawStock(0, WE_WERE_THERE));

        assertTrue(kept.isEmpty(), "the game says that settlement stocks no silver; it is not an answer");
    }

    @Test
    @DisplayName("a market we have never docked at is left alone")
    void unseenMarketsAreUntouched() {
        List<CommoditySearchResult> markets = List.of(market("Deciat", "Garay Terminal", 340, SPANSH_SYNC));

        List<CommoditySearchResult> kept = SpanshCommoditySearch.correctWithFirstHandData(
                markets, "silver", NEVER_BEEN_THERE);

        assertEquals(1, kept.size());
        assertEquals(340, kept.getFirst().getSupply(), "nothing first-hand to correct it with");
    }

    @Test
    @DisplayName("what we actually saw replaces what Spansh remembers")
    void supplyIsCorrectedToWhatWeSaw() {
        List<CommoditySearchResult> markets = List.of(market("Deciat", "Garay Terminal", 340, SPANSH_SYNC));

        List<CommoditySearchResult> kept = SpanshCommoditySearch.correctWithFirstHandData(
                markets, "silver", sawStock(12, WE_WERE_THERE));

        assertEquals(1, kept.size(), "a part load is still worth flying to");
        assertEquals(12, kept.getFirst().getSupply());
    }

    @Test
    @DisplayName("once Spansh is the fresher of the two, our old visit stops overriding it")
    void aStaleSightingDoesNotBlacklistAMarketForever() {
        // Otherwise one empty visit would hide a market for good, long after it restocked.
        List<CommoditySearchResult> markets = List.of(market("Deciat", "Garay Terminal", 340, WE_WERE_THERE));

        List<CommoditySearchResult> kept = SpanshCommoditySearch.correctWithFirstHandData(
                markets, "silver", sawStock(0, SPANSH_SYNC));

        assertEquals(1, kept.size());
        assertEquals(340, kept.getFirst().getSupply());
    }

    @Test
    @DisplayName("a Spansh row with no timestamp loses to something the game told us")
    void undatedSpanshDataLosesToASighting() {
        List<CommoditySearchResult> markets = List.of(market("Mat Zemlya", "Boldyr Dredging Installation", 340, null));

        assertTrue(SpanshCommoditySearch.correctWithFirstHandData(
                markets, "silver", sawStock(0, WE_WERE_THERE)).isEmpty());
    }

    @Test
    @DisplayName("a legacy good with no symbol cannot be matched, so nothing is second-guessed")
    void noSymbolMeansNoOverride() {
        List<CommoditySearchResult> markets = List.of(market("Mat Zemlya", "Boldyr Dredging Installation", 340, SPANSH_SYNC));

        assertSame(markets, SpanshCommoditySearch.correctWithFirstHandData(markets, null, sawStock(0, WE_WERE_THERE)));
    }
}
