package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

/**
 * Stage-4b self-describing command for "toggle route announcements".
 */
@RegisterCommand
public final class ToggleRouteAnnouncementsCommand implements IntelCommand {
    public static final String ID = "toggle_route_announcements";

    @Override public String llmDescription() { return "Toggle route and jump announcements on or off."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        boolean isOn = params.get("state").getAsBoolean();
        PlayerSession playerSession = PlayerSession.getInstance();
        playerSession.setRouteAnnouncementOn(isOn);
        String state = StringUtls.localizedLlm(isOn ? "handler.state.on" : "handler.state.off");
        return CommandOutcome.critical(StringUtls.localizedLlm("handler.announcements.route", state));
    }
}
