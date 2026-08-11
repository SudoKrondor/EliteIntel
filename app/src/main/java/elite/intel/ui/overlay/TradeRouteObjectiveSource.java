package elite.intel.ui.overlay;

import elite.intel.db.managers.TradeRouteManager;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStopDto;
import elite.intel.gameapi.search.spansh.traderoute.TradeCommodity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Projects the active trade route into a HUD objective: how far through the run
 * the commander is, and what to do at the next stop.
 * <p>
 * A leg is a buy/sell pair, so progress counts legs, not dockings.
 * <p>
 * Completed legs are DELETEd from the route table as they are flown, so the
 * original leg count is not stored anywhere. It is recovered from the surviving
 * 1-based {@code legNumber}: everything below the next leg has been flown, so
 * {@code completed = legNumber - 1} and {@code total = completed + remaining}.
 * <p>
 * Returns empty as soon as the route is gone, which is what makes the card
 * disappear when the route is cleared - no explicit clear path is needed.
 */
public class TradeRouteObjectiveSource implements HudObjectiveSource {

    private final TradeRouteManager tradeRouteManager;

    public TradeRouteObjectiveSource() {
        this(TradeRouteManager.getInstance());
    }

    /**
     * Seam for tests.
     */
    TradeRouteObjectiveSource(TradeRouteManager tradeRouteManager) {
        this.tradeRouteManager = tradeRouteManager;
    }

    @Override
    public Optional<HudObjective> currentObjective() {
        if (!tradeRouteManager.hasRoute()) return Optional.empty();

        TradeRouteManager.TradeRouteLegTuple<Integer, TradeStopDto> next = tradeRouteManager.getNextStop();
        if (next == null || next.getTradeStopDto() == null || next.getLegNumber() == null) {
            return Optional.empty();
        }

        List<TradeRouteManager.TradeRouteLegTuple<Integer, TradeStopDto>> remainingLegs =
                tradeRouteManager.getAllStops();
        int remaining = remainingLegs == null ? 0 : remainingLegs.size();
        int completed = legsCompleted(next.getLegNumber());
        int total = legsTotal(next.getLegNumber(), remaining);

        TradeStopDto stop = next.getTradeStopDto();
        List<HudRow> rows = new ArrayList<>();

        if (total > 0) {
            rows.add(HudRow.progress(HudText.get("overlay.card.row.legs"), completed, total));
        }
        commodityOf(stop).ifPresent(name -> rows.add(HudRow.of(HudText.get("overlay.card.row.commodity"), name)));
        location(stop.getSourceSystem(), stop.getSourceStation())
                .ifPresent(where -> rows.add(HudRow.of(HudText.get("overlay.card.row.buy"), where)));
        location(stop.getDestinationSystem(), stop.getDestinationStation())
                .ifPresent(where -> rows.add(HudRow.of(HudText.get("overlay.card.row.sell"), where, HudRow.State.GOOD)));

        return Optional.of(new HudObjective(
                "trade-route",
                HudText.get("overlay.card.title.tradeRoute"),
                HudText.get("overlay.card.subtitle.leg",
                        String.valueOf(next.getLegNumber()), String.valueOf(total)),
                rows,
                HudObjective.PRIORITY_STANDING));
    }

    /**
     * Legs already flown. Their rows are deleted from the route table, so the
     * only surviving evidence is that the next leg's 1-based number has moved
     * past them.
     */
    static int legsCompleted(int nextLegNumber) {
        return Math.max(0, nextLegNumber - 1);
    }

    /**
     * The route's original length: legs already flown plus the rows that are
     * still there (which include the leg being flown now).
     */
    static int legsTotal(int nextLegNumber, int remainingLegs) {
        return legsCompleted(nextLegNumber) + Math.max(0, remainingLegs);
    }

    /**
     * The commodity for this leg. A stop can list several; the first is the one
     * the route was planned around, and a HUD line has no room to enumerate.
     */
    private Optional<String> commodityOf(TradeStopDto stop) {
        List<TradeCommodity> commodities = stop.getCommodities();
        if (commodities == null || commodities.isEmpty()) return Optional.empty();
        TradeCommodity first = commodities.getFirst();
        if (first == null || first.getName() == null || first.getName().isBlank()) return Optional.empty();
        String name = first.getName().toUpperCase();
        return Optional.of(first.getAmount() > 0 ? name + " x" + first.getAmount() : name);
    }

    private Optional<String> location(String system, String station) {
        if (system == null || system.isBlank()) return Optional.empty();
        if (station == null || station.isBlank()) return Optional.of(system.toUpperCase());
        return Optional.of((system + " - " + station).toUpperCase());
    }
}
