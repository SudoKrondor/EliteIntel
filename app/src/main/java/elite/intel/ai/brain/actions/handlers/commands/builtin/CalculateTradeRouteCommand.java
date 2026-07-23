package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.db.managers.TradeRouteManager;
import elite.intel.gameapi.search.spansh.traderoute.TradeRouteResponse;
import elite.intel.gameapi.search.spansh.traderoute.TradeRouteSearchCriteria;
import elite.intel.gameapi.search.spansh.traderoute.TradeRouteTransaction;
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

    @Override
    public String llmDescription() {
        return "Calculate a profitable multi-hop commodity trade route using the saved trade profile (budget, cargo capacity, max stops and distance).";
    }


    private final TradeRouteManager tradeRouteManager = TradeRouteManager.getInstance();
    private final TradeProfileManager profileManager = TradeProfileManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return ID;
    }

    /** App-side route calculation (no game input); executable in any location. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        if (!profileManager.hasCargoCapacity()) {
            return StringUtls.localizedLlm("handler.tradeRoute.noCargoCapacity");
        }

        TradeRouteSearchCriteria criteria = profileManager.getCriteria(true);
        // getCriteria returns null when no starting station could be resolved (it already voiced why); guard
        // before dereferencing it, otherwise criteria.getStation() below throws an NPE.
        if (criteria == null) {
            return null;
        }
        // Start-of-processing filler: voiced while the search runs, never remembered.
        CompanionRuntime.narrator().filler(StringUtls.localizedLlm("handler.tradeRoute.calculating", criteria.getStation()), false);

        if (criteria.getStartingCapital() == 0) {
            String shipName = playerSession.getShipLoadout().getShipName();
            return StringUtls.localizedLlm("handler.tradeRoute.noProfile", shipName);
        }

        if (criteria.getMaxJumps() == 0) {
            String shipName = playerSession.getShipLoadout().getShipName();
            return StringUtls.localizedLlm("handler.tradeRoute.noStops", shipName);
        }


        if (criteria.getMaxLsFromArrival() == 0) {
            String shipName = playerSession.getShipLoadout().getShipName();
            return StringUtls.localizedLlm("handler.tradeRoute.noDistance", shipName);
        }

        // WHY: the Spansh optimisation below can run for minutes - far past the 60s thought watchdog, which by the
        // time it returns has already force-interrupted the commander turn that launched us. A returned outcome
        // would settle on that interrupted thought and be discarded as a late result (route saved, never voiced),
        // so the completed plot is announced on the EVENT lane instead - deliberately voiced AND remembered as an
        // event, since a plotted route is a real completion worth recall. The early validation guards above return
        // normally: they complete well within the watchdog window, so their COMMAND-outcome settlement still fires.
        TradeRouteResponse route = tradeRouteManager.calculateTradeRoute(criteria);
        String outcome;
        if (route == null || route.getResult() == null || route.getResult().isEmpty()) {
            if (criteria.getStation() != null) {
                outcome = StringUtls.localizedLlm("handler.tradeRoute.notFound");
            } else {
                String tryLanding = status.isDocked() ? "" : StringUtls.localizedLlm("handler.tradeRoute.tryLanding");
                outcome = StringUtls.localizedLlm("handler.tradeRoute.notFoundSpansh", tryLanding);
            }
        } else {
            long totalProfit = route.getResult().stream()
                    .mapToLong(TradeRouteTransaction::getTotalProfit)
                    .sum();
            outcome = StringUtls.localizedLlm("handler.tradeRoute.found", totalProfit);
        }

        CompanionRuntime.narrator().announce(outcome, false);
        return null;
    }
}
