package elite.intel.ui.overlay;

import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.CommoditySearchResultDao.FoundLine;
import elite.intel.db.dao.CommoditySearchResultDao.FoundMarket;
import elite.intel.db.managers.CommoditySearchResultManager;
import elite.intel.db.managers.ShipRouteManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Projects the market a commodity search found into a HUD objective: what to buy, where, how much of it
 * is there and what it costs a tonne.
 * <p>
 * <b>Why this card exists at all.</b> The search already speaks the answer and leaves a destination
 * reminder, but the reminder is one sentence of prose written for the voice - the stock and the unit price
 * are inside it and nowhere a card can read them. The commander flying twelve jumps to buy steel wants the
 * figures on screen, not repeated on request.
 * <p>
 * <b>Why it is a list.</b> A search made on behalf of a standing manifest comes back with everything on it
 * that market stocks, so the card is a loading order - three or four goods with a tonnage each - rather
 * than one commodity. That is exactly the trip the commander cannot hold in their head at the commodity
 * market, which is where they will be reading it.
 * <p>
 * <b>Why it is tied to the plotted route.</b> A search result is cleared by nothing: the next search
 * replaces it and otherwise it sits in the database indefinitely. Shown on its own, last week's errand
 * would still be on screen today. Tying it to the route the search itself plotted means a result the
 * commander has moved on from cannot be drawn at all - the same rule {@link ShipRouteObjectiveSource}
 * applies to the reminder, and for the same reason.
 * <p>
 * Ranked {@link HudObjective#PRIORITY_STANDING} but registered after the route card: it says everything
 * the route card says about where the commander is going, plus what they are going there to buy.
 */
public class CommoditySearchObjectiveSource implements HudObjectiveSource {

    /**
     * Goods listed individually before the card is summarised instead. Four rows plus the market's own two
     * is already a tall card, and the tonnage total says the rest.
     */
    private static final int MAX_GOODS_LISTED = 4;

    private final Supplier<FoundMarket> found;
    private final Supplier<List<FoundLine>> goods;
    private final Supplier<String> destination;

    public CommoditySearchObjectiveSource() {
        this(() -> CommoditySearchResultManager.getInstance().get(),
                () -> CommoditySearchResultManager.getInstance().lines(),
                () -> ShipRouteManager.getInstance().getDestination());
    }

    /**
     * Seam for tests.
     */
    CommoditySearchObjectiveSource(Supplier<FoundMarket> found, Supplier<List<FoundLine>> goods,
                                   Supplier<String> destination) {
        this.found = found;
        this.goods = goods;
        this.destination = destination;
    }

    @Override
    public Optional<HudObjective> currentObjective() {
        FoundMarket market = found.get();
        if (market == null || market.getCommodity() == null || market.getCommodity().isBlank()) {
            return Optional.empty();
        }
        String system = trimToNull(market.getStarSystem());
        if (system == null) return Optional.empty();

        String plotted = trimToNull(destination.get());
        if (plotted == null || !plotted.equalsIgnoreCase(system)) return Optional.empty();

        List<FoundLine> shopping = goods.get();
        if (shopping == null || shopping.isEmpty()) return Optional.empty();

        List<HudRow> rows = new ArrayList<>();
        rows.add(HudRow.of(HudText.get("overlay.card.row.system"), system.toUpperCase(), HudRow.State.GOOD));
        String station = trimToNull(market.getStationName());
        if (station != null) {
            // A carrier jumps, so the port it names may not be where it was when Spansh last heard. The
            // commander is told in the spoken answer; the card has to agree with it.
            rows.add(HudRow.of(HudText.get("overlay.card.row.station"), station.toUpperCase(),
                    market.isFleetCarrier() ? HudRow.State.WARN : HudRow.State.NORMAL));
        }
        for (FoundLine line : shopping.subList(0, Math.min(shopping.size(), MAX_GOODS_LISTED))) {
            rows.add(HudRow.of(goodName(line), goodValue(line)));
        }
        if (shopping.size() > MAX_GOODS_LISTED) {
            rows.add(HudRow.of(HudText.get("overlay.card.row.moreGoods"),
                    String.valueOf(shopping.size() - MAX_GOODS_LISTED)));
        }
        int totalUnits = shopping.stream().mapToInt(FoundLine::getUnitsToBuy).sum();
        if (totalUnits > 0 && shopping.size() > 1) {
            rows.add(HudRow.of(HudText.get("overlay.card.row.toLoad"),
                    HudText.amount(totalUnits, "overlay.card.unit.tonnes"), HudRow.State.GOOD));
        }

        return Optional.of(new HudObjective(
                "commodity-search",
                HudText.get(shopping.size() > 1
                        ? "overlay.card.title.shoppingList"
                        : "overlay.card.title.commoditySearch"),
                null,
                rows,
                HudObjective.PRIORITY_STANDING));
    }

    /**
     * The good's own name is the row's label - a shopping list is read down the left-hand column, and
     * repeating the word COMMODITY four times says nothing.
     */
    private static String goodName(FoundLine line) {
        return FuzzySearch.localizedCommodityName(line.getCommodity()).toUpperCase();
    }

    /**
     * Tonnes to load and what they cost each. Falls back to the stock on hand when nobody said how many
     * were wanted - which is the case for a commander who simply asked where to buy a good.
     */
    private static String goodValue(FoundLine line) {
        String price = HudText.amount(line.getPrice(), "overlay.card.unit.creditsPerTon");
        long tonnes = line.getUnitsToBuy() > 0 ? line.getUnitsToBuy() : line.getSupply();
        if (tonnes <= 0) return price;
        return HudText.amount(tonnes, "overlay.card.unit.tonnes") + " " + price;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
