package elite.intel.gameapi.search.spansh.commodity;

import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchResultDto.StationResult.MarketEntry;

/**
 * Which side of the counter the commander is standing on.
 * <p>
 * <b>The field names are the COMMANDER's, not the station's.</b> Both Spansh and the game's own
 * {@code Market.json} name every price from the player's side of the counter, and the two agree to the
 * credit - verified against Love Hub in Col 285 Sector IB-X d1-32, where {@code Food Cartridges} is
 * {@code buy_price} 113 / {@code sell_price} 73 in both, and the game's market screen shows 113 to buy and
 * 73 to sell. So {@code buy_price} is what the commander PAYS and {@code sell_price} is what the commander
 * IS PAID, and on any entry trading both ways the buy price is the higher.
 * <p>
 * Each price has a quantity beside it, again from the commander's side: {@code supply} is what is there to
 * buy, {@code demand} is what the station will take. Buying pairs {@code supply} with {@code buy_price},
 * selling pairs {@code demand} with {@code sell_price}, and the halves are never mixed. Reading the wrong
 * half does not fail, it answers a different question - a sell search that read {@code supply} would rank
 * markets by how much of the good they already have, which is close to the opposite of what was asked.
 * <p>
 * The whole of the difference between "where can I buy tritium" and "where can I sell tritium" is this
 * enum: the request filter, which figure means "enough", which price is better, and which of the two the
 * commander is told. Everything else - the escalation ladder, the first-hand market override, the reminder,
 * the route - is one question asked twice, so it is written once and handed this.
 */
public enum TradeSide {

    /**
     * The commander pays {@code buy_price}, out of the station's {@code supply}. Cheaper is better.
     */
    BUY,

    /**
     * The commander is paid {@code sell_price}, against the station's {@code demand}. Dearer is better.
     */
    SELL;

    /**
     * What a tonne changes hands for on this side, or null when the station is not trading the good that
     * way. A market that only buys a good lists it with no {@code buy_price}, and vice versa, so this is
     * also the test for whether the entry is any use at all.
     */
    public Integer priceOn(MarketEntry entry) {
        return this == BUY ? entry.getBuyPrice() : entry.getSellPrice();
    }

    /**
     * Tonnes the station can trade on this side: its stock when buying, the tonnage it is asking for when
     * selling. Zero when it says nothing, which is a market that is not really trading the good.
     */
    public long unitsOn(MarketEntry entry) {
        Long units = this == BUY ? entry.getSupply() : entry.getDemand();
        return units == null ? 0 : units;
    }

    /**
     * Whether a better price is a higher one. The commander wants to pay little and be paid much, so the
     * same "best price" request sorts the page in opposite directions.
     */
    public boolean dearerIsBetter() {
        return this == SELL;
    }
}
