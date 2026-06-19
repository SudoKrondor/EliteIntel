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
 * Stage-4b self-describing command for "toggle all announcements".
 * Owns its own execution (ownsExecution() == true): the dispatch map routes this
 * command's execute() in place of the legacy ToggleAllAnnouncementsHandler. No parameters
 * beyond the LLM "state" flag.
 */
@RegisterCommand
public final class ToggleAllAnnouncementsCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.TOGGLE_ALL_ANNOUNCEMENTS;
    }

    @Override
    public boolean ownsExecution() {
        return true;
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
        playerSession.setRouteAnnouncementOn(isOn);
        playerSession.setRadarContactAnnouncementOn(isOn);
        playerSession.setMiningAnnouncementOn(isOn);
        playerSession.setNavigationAnnouncementOn(isOn);
        String state = StringUtls.localizedLlm(isOn ? "handler.state.on" : "handler.state.off");
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.announcements.all", state)));
    }
}
