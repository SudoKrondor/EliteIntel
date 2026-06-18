package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.NeutronStarRouteManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy ClearNeutronRouteHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ClearNeutronRouteCommand implements IntelCommand {

    private final NeutronStarRouteManager manager = NeutronStarRouteManager.getInstance();

    @Override
    public String id() {
        return CommandIds.CLEAR_NEUTRON_ROUTE;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        manager.clear();
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.neutronRoute.cleared")));
    }
}
