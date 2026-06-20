package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.NeutronStarRouteManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy ClearNeutronRouteHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ClearNeutronRouteCommand implements IntelCommand {
    public static final String ID = "clear_neutron_route";


    private final NeutronStarRouteManager manager = NeutronStarRouteManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        manager.clear();
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.neutronRoute.cleared")));
    }
}
