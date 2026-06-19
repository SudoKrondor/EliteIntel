package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.db.managers.TradeRouteManager;
import elite.intel.gameapi.EventBusManager;
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

    private final TradeRouteManager tradeRouteManager = TradeRouteManager.getInstance();
    private final TradeProfileManager profileManager = TradeProfileManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return CommandIds.CALCULATE_TRADE_ROUTE;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        if (!profileManager.hasCargoCapacity()) {
            EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.tradeRoute.noCargoCapacity")));
            return;
        }

        TradeRouteSearchCriteria criteria = profileManager.getCriteria(true);
        EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.tradeRoute.calculating", criteria.getStation())));

        if (criteria == null) {
            return;
        }

        if (criteria.getStartingCapital() == 0) {
            String shipName = playerSession.getShipLoadout().getShipName();
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.noProfile", shipName)));
            return;
        }

        if (criteria.getMaxJumps() == 0) {
            String shipName = playerSession.getShipLoadout().getShipName();
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.noStops", shipName)));
            return;
        }


        if (criteria.getMaxLsFromArrival() == 0) {
            String shipName = playerSession.getShipLoadout().getShipName();
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.noDistance", shipName)));
            return;
        }

        TradeRouteResponse route = tradeRouteManager.calculateTradeRoute(criteria);
        if (route == null || route.getResult() == null || route.getResult().isEmpty()) {
            if (criteria.getStation() != null) {
                EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.notFound")));
            } else {
                String tryLanding = status.isDocked() ? "" : StringUtls.localizedLlm("handler.tradeRoute.tryLanding");
                EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.notFoundSpansh", tryLanding)));
            }
            return;
        }
        long totalProfit = route.getResult().stream()
                .mapToLong(TradeRouteTransaction::getTotalProfit)
                .sum();

        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.found", totalProfit)));
    }
}
