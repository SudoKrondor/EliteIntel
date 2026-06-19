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
 * Stage-4b self-describing command for "toggle discovery announcements".
 */
@RegisterCommand
public final class ToggleDiscoveryAnnouncementsCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.TOGGLE_DISCOVERY_ANNOUNCEMENTS;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        if (params.get("state") == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.common.llmParamFailed")));
            return;
        }
        boolean isOn = params.get("state").getAsBoolean();
        PlayerSession playerSession = PlayerSession.getInstance();
        playerSession.setDiscoveryAnnouncementOn(isOn);
        String state = StringUtls.localizedLlm(isOn ? "handler.state.on" : "handler.state.off");
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.announcements.discovery", state)));
    }
}
