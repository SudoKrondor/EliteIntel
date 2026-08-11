package elite.intel.ui.overlay;

import elite.intel.db.dao.RouteMonetisationDao.MonetisationTransaction;
import elite.intel.db.managers.MonetizeRouteManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Projects a monetised route - a single opportunistic buy/sell pair found for
 * cargo space on an existing journey - into a HUD objective.
 * <p>
 * No clear path is needed here: {@code MarketSellEventSubscriber} clears the
 * transaction once the cargo is sold, and {@code ClearRemindersCommand} clears
 * it on request. This source returns empty as soon as the transaction is gone,
 * so the card disappears on its own in both cases.
 * <p>
 * Ranked below an active task but alongside other standing plans: it is a
 * suggestion attached to a journey rather than the journey itself.
 */
public class MonetizedRouteObjectiveSource implements HudObjectiveSource {

    private final MonetizeRouteManager monetizeRouteManager;

    public MonetizedRouteObjectiveSource() {
        this(MonetizeRouteManager.getInstance());
    }

    /**
     * Seam for tests.
     */
    MonetizedRouteObjectiveSource(MonetizeRouteManager monetizeRouteManager) {
        this.monetizeRouteManager = monetizeRouteManager;
    }

    @Override
    public Optional<HudObjective> currentObjective() {
        MonetisationTransaction tx = monetizeRouteManager.getTransaction();
        if (tx == null) return Optional.empty();

        String commodity = firstNonBlank(tx.getSourceCommodity(), tx.getDestinationCommodity());
        if (commodity == null) return Optional.empty();

        List<HudRow> rows = new ArrayList<>();
        rows.add(HudRow.of(HudText.get("overlay.card.row.commodity"), commodity.toUpperCase()));

        location(tx.getSourceStarSystem(), tx.getSourceStationName())
                .ifPresent(where -> rows.add(HudRow.of(HudText.get("overlay.card.row.buy"), where)));
        location(tx.getDestinationStarSystem(), tx.getDestinationStationName())
                .ifPresent(where -> rows.add(HudRow.of(HudText.get("overlay.card.row.sell"), where, HudRow.State.GOOD)));

        long margin = (long) tx.getDestinationSellPrice() - tx.getSourceBuyPrice();
        if (margin > 0) {
            rows.add(HudRow.of(HudText.get("overlay.card.row.margin"),
                    HudText.amount(margin, "overlay.card.unit.creditsPerTon")));
        }

        return Optional.of(new HudObjective(
                "monetized-route",
                HudText.get("overlay.card.title.cargoOpportunity"),
                null,
                rows,
                HudObjective.PRIORITY_STANDING));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    private Optional<String> location(String system, String station) {
        if (system == null || system.isBlank()) return Optional.empty();
        if (station == null || station.isBlank()) return Optional.of(system.toUpperCase());
        return Optional.of((system + " - " + station).toUpperCase());
    }
}
