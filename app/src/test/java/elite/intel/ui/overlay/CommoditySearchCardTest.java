package elite.intel.ui.overlay;

import elite.intel.db.dao.CommoditySearchResultDao.FoundLine;
import elite.intel.db.dao.CommoditySearchResultDao.FoundMarket;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static elite.intel.ui.overlay.HudCards.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The card that puts a commodity search's answer on screen: where to go, and what to load once there.
 * <p>
 * The figures were previously only inside the spoken sentence and the destination reminder, where no card
 * could read them - and a search made on behalf of a construction manifest answers with several goods at
 * once, which one sentence cannot hold either.
 */
class CommoditySearchCardTest {

    private static FoundMarket bujoldTerminal() {
        FoundMarket market = new FoundMarket();
        market.setCommodity("Steel");
        market.setStarSystem("Sterope");
        market.setStationName("Bujold Terminal");
        market.setStationType("Coriolis");
        market.setPrice(487);
        market.setSupply(4120);
        return market;
    }

    private static FoundLine line(String commodity, long price, long supply, int unitsToBuy) {
        FoundLine line = new FoundLine();
        line.setCommodity(commodity);
        line.setPrice(price);
        line.setSupply(supply);
        line.setUnitsToBuy(unitsToBuy);
        return line;
    }

    private static Optional<HudObjective> card(FoundMarket market, List<FoundLine> goods, String plotted) {
        return new CommoditySearchObjectiveSource(() -> market, () -> goods, () -> plotted).currentObjective();
    }

    private static Optional<HudObjective> card(FoundMarket market, String plotted) {
        return card(market, List.of(line("Steel", 487, 4120, 0)), plotted);
    }

    /**
     * The same card, read the other way round: the price is what the commander is PAID and the tonnage is
     * what the market WANTS. Without the side on the row a commander flying out to unload 300 tonnes of
     * tritium would be told the station has 300 tonnes of it in stock.
     */
    @Test
    void aSellResultSaysItIsSellingAndTotalsWhatGoesOut() {
        FoundMarket market = bujoldTerminal();
        market.setCommodity("Tritium");
        market.setSide("SELL");
        market.setPrice(55000);
        market.setSupply(9100);

        HudObjective objective = card(market, List.of(line("Tritium", 55000, 9100, 300)), "Sterope").orElseThrow();

        assertEquals("SELL CARGO", objective.title());
        assertTrue(labels(objective).contains("TO SELL"),
                "a single good still totals on a sell card: it is the load leaving the hold");
        assertEquals("300 T", valueOf(objective, "TO SELL"));
    }

    @Test
    void aRowWrittenBeforeSearchingHadADirectionIsABuy() {
        FoundMarket market = bujoldTerminal();
        market.setSide(null);

        assertEquals("COMMODITY FOUND", card(market, "Sterope").orElseThrow().title());
    }

    @Test
    void aSingleGoodReadsAsOneErrand() {
        HudObjective objective = card(bujoldTerminal(), "Sterope").orElseThrow();

        assertEquals("COMMODITY FOUND", objective.title());
        assertEquals("STEROPE", valueOf(objective, "SYSTEM"));
        assertEquals("BUJOLD TERMINAL", valueOf(objective, "STATION"));
        assertEquals("4.120 T 487 cr/t", valueOf(objective, "STEEL").replace(',', '.'),
                "with no stated requirement the card shows the stock on hand");
        assertFalse(labels(objective).contains("TO LOAD"), "there is no list to total");
    }

    /**
     * The trip a construction manifest actually produces: several of the site's outstanding goods at one
     * market, which is the whole point of weighing the market against the list rather than one commodity.
     */
    @Test
    void severalGoodsReadAsALoadingOrderWithATotal() {
        HudObjective objective = card(bujoldTerminal(), List.of(
                line("Ceramic Composites", 724, 900, 85),
                line("Polymers", 682, 400, 170),
                line("Copper", 1050, 200, 85)), "Sterope").orElseThrow();

        assertEquals("SHOPPING LIST", objective.title());
        assertEquals("85 T 724 cr/t", valueOf(objective, "CERAMIC COMPOSITES"));
        assertEquals("170 T 682 cr/t", valueOf(objective, "POLYMERS"));
        assertEquals("85 T 1.050 cr/t", valueOf(objective, "COPPER").replace(',', '.'));
        assertEquals("340 T", valueOf(objective, "TO LOAD"));
        assertEquals(HudRow.State.GOOD, rowOf(objective, "TO LOAD").state());
    }

    /**
     * A card is one screen. Past a handful of goods the count says more than another four rows would.
     */
    @Test
    void aLongListIsTruncatedWithACountOfWhatIsLeft() {
        HudObjective objective = card(bujoldTerminal(), List.of(
                line("Steel", 5057, 900, 300),
                line("Polymers", 682, 400, 100),
                line("Copper", 1050, 200, 85),
                line("Water", 662, 100, 22),
                line("Semiconductors", 1526, 90, 34),
                line("Superconductors", 7657, 80, 60)), "Sterope").orElseThrow();

        assertEquals("2", valueOf(objective, "MORE GOODS"));
        assertFalse(labels(objective).contains("SEMICONDUCTORS"));
        assertEquals("601 T", valueOf(objective, "TO LOAD"),
                "the total counts everything, listed or not");
    }

    /**
     * A search result is cleared by nothing - the next search replaces it and otherwise it sits in the
     * database indefinitely. Tying it to the route the search plotted is what stops last week's errand
     * still being on screen today.
     */
    @Test
    void aResultForSomewhereTheCommanderIsNoLongerHeadedIsNotDrawn() {
        assertTrue(card(bujoldTerminal(), "Groombridge 34").isEmpty());
    }

    @Test
    void noPlottedRouteMeansNoCard() {
        assertTrue(card(bujoldTerminal(), null).isEmpty());
        assertTrue(card(bujoldTerminal(), "  ").isEmpty());
    }

    @Test
    void theRouteIsMatchedWithoutRegardToCase() {
        assertTrue(card(bujoldTerminal(), "sterope").isPresent());
    }

    /**
     * A carrier jumps: it may be a thousand light years from where Spansh last saw it. The spoken answer
     * warns about that, and the card has to agree with the voice.
     */
    @Test
    void aFleetCarrierPortIsFlaggedOnTheCard() {
        FoundMarket carrier = bujoldTerminal();
        carrier.setStationName("K7Q-BQL");
        carrier.setFleetCarrier(true);

        assertEquals(HudRow.State.WARN, rowOf(card(carrier, "Sterope").orElseThrow(), "STATION").state());
    }

    @Test
    void aStaticPortIsNotFlagged() {
        assertEquals(HudRow.State.NORMAL, rowOf(card(bujoldTerminal(), "Sterope").orElseThrow(), "STATION").state());
    }

    /**
     * Spansh does not always say how much is on sale, and a good with neither a requirement nor a known
     * stock is still worth naming with its price.
     */
    @Test
    void aGoodWithNoKnownTonnageShowsItsPriceAlone() {
        HudObjective objective = card(bujoldTerminal(), List.of(line("Steel", 487, 0, 0)), "Sterope")
                .orElseThrow();

        assertEquals("487 cr/t", valueOf(objective, "STEEL"));
    }

    @Test
    void nothingSearchedForYetDrawsNoCard() {
        assertTrue(card(null, "Sterope").isEmpty());
    }

    @Test
    void aMarketWithNoGoodsRecordedDrawsNoCard() {
        assertTrue(card(bujoldTerminal(), List.of(), "Sterope").isEmpty());
    }

    @Test
    void aResultWithNoSystemIsUseless() {
        FoundMarket nowhere = bujoldTerminal();
        nowhere.setStarSystem(null);

        assertTrue(card(nowhere, "Sterope").isEmpty());
    }
}
