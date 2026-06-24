package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.session.PlayerSession;

/**
 * Stage-4b self-describing command for "toggle radar announcements".
 * Behaviour
 * is intentionally minimal (no confirmation announcement), matching the legacy handler 1:1.
 */
@RegisterCommand
public final class ToggleRadarAnnouncementsCommand implements IntelCommand {
    public static final String ID = "toggle_radar_announcements";

    @Override public String llmDescription() { return "Toggle radar contact announcements on or off."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        boolean isOn = params.get("state").getAsBoolean();
        PlayerSession.getInstance().setRadarContactAnnouncementOn(isOn);
    }
}
