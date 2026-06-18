package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

/**
 * Stage-4b self-describing command for "toggle route announcements".
 * Owns its own execution (ownsExecution() == true): the dispatch map routes this
 * command's execute() in place of the legacy RouteAnnouncementsOnOffHandler.
 */
@RegisterCommand
public final class ToggleRouteAnnouncementsCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.TOGGLE_ROUTE_ANNOUNCEMENTS;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        boolean isOn = params.get("state").getAsBoolean();
        PlayerSession playerSession = PlayerSession.getInstance();
        playerSession.setRouteAnnouncementOn(isOn);
        String state = StringUtls.localizedLlm(isOn ? "handler.state.on" : "handler.state.off");
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.announcements.route", state)));
    }
}
