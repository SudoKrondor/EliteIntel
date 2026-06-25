package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.db.managers.TradeRouteManager;
import elite.intel.search.spansh.traderoute.TradeRouteResponse;
import elite.intel.search.spansh.traderoute.TradeRouteSearchCriteria;
import elite.intel.search.spansh.traderoute.TradeRouteTransaction;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy CalculateTradeRouteHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class CalculateTradeRouteCommand implements IntelCommand {
    public static final String ID = "calculate_trade_route";

    @Override public String llmDescription() { return "Calculate a profitable trade route."; }


    private final TradeRouteManager tradeRouteManager = TradeRouteManager.getInstance();
    private final TradeProfileManager profileManager = TradeProfileManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        if (!profileManager.hasCargoCapacity()) {
            return CommandOutcome.speak(StringUtls.localizedLlm("handler.tradeRoute.noCargoCapacity"));
        }

        TradeRouteSearchCriteria criteria = profileManager.getCriteria(true);
        if (criteria == null) {
            return null;
        }

        if (criteria.getStartingCapital() == 0) {
            String shipName = playerSession.getShipLoadout().getShipName();
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.tradeRoute.noProfile", shipName));
        }

        if (criteria.getMaxJumps() == 0) {
            String shipName = playerSession.getShipLoadout().getShipName();
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.tradeRoute.noStops", shipName));
        }

        if (criteria.getMaxLsFromArrival() == 0) {
            String shipName = playerSession.getShipLoadout().getShipName();
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.tradeRoute.noDistance", shipName));
        }

        TradeRouteResponse route = tradeRouteManager.calculateTradeRoute(criteria);
        if (route == null || route.getResult() == null || route.getResult().isEmpty()) {
            if (criteria.getStation() != null) {
                return CommandOutcome.critical(StringUtls.localizedLlm("handler.tradeRoute.notFound"));
            }
            String tryLanding = status.isDocked() ? "" : StringUtls.localizedLlm("handler.tradeRoute.tryLanding");
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.tradeRoute.notFoundSpansh", tryLanding));
        }
        long totalProfit = route.getResult().stream()
                .mapToLong(TradeRouteTransaction::getTotalProfit)
                .sum();

        return CommandOutcome.critical(StringUtls.localizedLlm("handler.tradeRoute.found", totalProfit));
    }
}
