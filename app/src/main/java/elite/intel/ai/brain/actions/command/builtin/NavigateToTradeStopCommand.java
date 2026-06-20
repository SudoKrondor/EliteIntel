package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.TradeRouteManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.search.spansh.station.marketstation.TradeStopDto;
import elite.intel.search.spansh.traderoute.TradeCommodity;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Self-describing "navigate to trade stop" command.
 * Owns its own execution: body migrated 1:1 from the legacy NavigateToNextTradeStopHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateToTradeStopCommand implements IntelCommand {
    public static final String ID = "navigate_to_trade_stop";


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final TradeRouteManager tradeRouteManager = TradeRouteManager.getInstance();
    private final ReminderManager reminderManager = ReminderManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        final RoutePlotter routePlotter = new RoutePlotter();
        final LocationDto location = locationManager.findByLocationData(playerSession.getLocationData());
        if (!tradeRouteManager.hasRoute()) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.notFound")));
            return;
        }

        GameEvents.CargoEvent shipCargo = playerSession.getShipCargo();
        boolean cargoLoaded = shipCargo.getCount() > 0;

        TradeRouteManager.TradeRouteLegTuple<Integer, TradeStopDto> nextStop = tradeRouteManager.getNextStop();
        if (nextStop == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.noMoreStops")));
            return;
        }

        String sourceSystem = nextStop.getTradeStopDto().getSourceSystem();
        String sourceStation = nextStop.getTradeStopDto().getSourceStation();
        String destinationSystem = nextStop.getTradeStopDto().getDestinationSystem();
        String destinationStation = nextStop.getTradeStopDto().getDestinationStation();

        List<TradeCommodity> commodities = nextStop.getTradeStopDto().getCommodities();
        String commodityList = commodities.stream().map(TradeCommodity::getName).collect(Collectors.joining(", "));

        String message;
        if (!cargoLoaded) {
            boolean notInSourceSystem = !location.getStarName().equalsIgnoreCase(sourceSystem);
            boolean notAtTheSourceStation = location.getStationName() != null && !location.getStationName().equalsIgnoreCase(sourceStation);

            if (notInSourceSystem) {
                message = StringUtls.localizedLlm("handler.tradeStop.travelAndBuy", sourceSystem, sourceStation, commodityList, destinationSystem, destinationStation);
                routePlotter.plotRoute(sourceSystem);
            } else if (notAtTheSourceStation) {
                message = StringUtls.localizedLlm("handler.tradeStop.inSystemBuyAtStation", sourceStation, commodityList, destinationSystem, destinationStation);
            } else {
                message = StringUtls.localizedLlm("handler.tradeStop.atStationBuy", commodityList, destinationSystem, destinationStation);
            }
        } else {
            boolean notInDestinationSystem = !location.getStarName().equalsIgnoreCase(destinationSystem);
            boolean notAtTheDestinationStation = !location.getStationName().equalsIgnoreCase(destinationStation);

            if (notInDestinationSystem) {
                message = StringUtls.localizedLlm("handler.tradeStop.travelToSell", destinationSystem, destinationStation);
                routePlotter.plotRoute(destinationSystem);
            } else if (notAtTheDestinationStation) {
                message = StringUtls.localizedLlm("handler.tradeStop.headToStation", destinationStation);
            } else {
                message = StringUtls.localizedLlm("handler.tradeStop.sellHere");
            }
        }

        EventBusManager.publish(new MissionCriticalAnnouncementEvent(message));
        reminderManager.setReminder(message, destinationSystem);
    }
}
