package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.TradeRouteManager;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStopDto;
import elite.intel.gameapi.search.spansh.traderoute.TradeCommodity;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
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
    public static final String ID = "navigate_to_next_trade_stop";

    @Override
    public String llmDescription() {
        return "Plot a route to the next stop on the active trade route (buy or sell leg depending on current cargo).";
    }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final TradeRouteManager tradeRouteManager = TradeRouteManager.getInstance();
    private final ReminderManager reminderManager = ReminderManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    /** Route plotting taps the ship-only GalaxyMapOpen bind; works only in the main-ship cockpit. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        final RoutePlotter routePlotter = new RoutePlotter();
        final LocationDto location = locationManager.findByLocationData(playerSession.getLocationData());
        if (!tradeRouteManager.hasRoute()) {
            return StringUtls.localizedResponse("handler.tradeRoute.notFound");
        }

        GameEvents.CargoEvent shipCargo = playerSession.getShipCargo();
        // Limpets sit in the hold but are not trade goods: a hold with only limpets still needs a buy leg.
        boolean cargoLoaded = shipCargo.getTradeableCount() > 0;

        TradeRouteManager.TradeRouteLegTuple<Integer, TradeStopDto> nextStop = tradeRouteManager.getNextStop();
        if (nextStop == null) {
            return StringUtls.localizedResponse("handler.tradeRoute.noMoreStops");
        }

        String sourceSystem = nextStop.getTradeStopDto().getSourceSystem();
        String sourceStation = nextStop.getTradeStopDto().getSourceStation();
        String destinationSystem = nextStop.getTradeStopDto().getDestinationSystem();
        String destinationStation = nextStop.getTradeStopDto().getDestinationStation();

        List<TradeCommodity> commodities = nextStop.getTradeStopDto().getCommodities();
        String commodityList = commodities.stream().map(TradeCommodity::getName).collect(Collectors.joining(", "));

        String message;
        // The reminder is heard on arrival in the system it is filed under, so it is the errand there and
        // nothing about the flight: the buy leg is filed under the SOURCE system (it used to go under the
        // destination, where "travel to the source and buy" was read back after the goods were already
        // aboard), and once the hold is loaded the only errand left is the sale at the destination.
        String reminder;
        String reminderSystem;
        if (!cargoLoaded) {
            reminder = StringUtls.localizedResponse("handler.tradeStop.buyReminder",
                    sourceStation, commodityList, destinationSystem, destinationStation);
            reminderSystem = sourceSystem;
        } else {
            reminder = StringUtls.localizedResponse("handler.tradeStop.headToStation", destinationStation);
            reminderSystem = destinationSystem;
        }
        if (!cargoLoaded) {
            boolean notInSourceSystem = !location.isInSystem(sourceSystem);
            String dockedAt = locationManager.findCurrentStation().getStationName();
            boolean notAtTheSourceStation = dockedAt != null && !dockedAt.equalsIgnoreCase(sourceStation);

            if (notInSourceSystem) {
                message = routePlotter.plotRouteAnd(
                        StringUtls.localizedResponse("handler.tradeStop.travelAndBuy", sourceSystem, sourceStation, commodityList, destinationSystem, destinationStation),
                        sourceSystem);
            } else if (notAtTheSourceStation) {
                message = StringUtls.localizedResponse("handler.tradeStop.inSystemBuyAtStation", sourceStation, commodityList, destinationSystem, destinationStation);
            } else {
                message = StringUtls.localizedResponse("handler.tradeStop.atStationBuy", commodityList, destinationSystem, destinationStation);
            }
        } else {
            boolean notInDestinationSystem = !location.isInSystem(destinationSystem);
            boolean notAtTheDestinationStation = !location.isAtStation(destinationStation);

            if (notInDestinationSystem) {
                message = routePlotter.plotRouteAnd(
                        StringUtls.localizedResponse("handler.tradeStop.travelToSell", destinationSystem, destinationStation),
                        destinationSystem);
            } else if (notAtTheDestinationStation) {
                message = StringUtls.localizedResponse("handler.tradeStop.headToStation", destinationStation);
            } else {
                message = StringUtls.localizedResponse("handler.tradeStop.sellHere");
            }
        }

        reminderManager.setReminder(reminder, reminderSystem);
        return message;
    }
}
