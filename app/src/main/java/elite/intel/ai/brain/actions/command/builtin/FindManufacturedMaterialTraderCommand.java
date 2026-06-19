package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.RoutePlotter;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.search.spansh.station.TradersAndBrokersSearch;
import elite.intel.search.spansh.station.traderandbroker.TraderType;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

/**
 * Self-describing "find manufactured material trader" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindManufacturedMaterialTraderHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class FindManufacturedMaterialTraderCommand implements IntelCommand {

    private static final int DEFAULT_RANGE = 250;

    @Override
    public String id() {
        return CommandIds.FIND_MANUFACTURED_MATERIAL_TRADER;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();
        if(status.isInSrv() || status.isInMainShip()) {
            Number range = GetNumberFromParam.extractRangeParameter(params, DEFAULT_RANGE);
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.trader.searching", TraderType.MANUFACTURED.getType())));
            TradersAndBrokersSearch search = TradersAndBrokersSearch.getInstance();
            RoutePlotter routePlotter = new RoutePlotter();
            routePlotter.plotRoute(search.location(TraderType.MANUFACTURED, null, range));
        } else {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.notInShipOrSrv")));
        }
    }
}
