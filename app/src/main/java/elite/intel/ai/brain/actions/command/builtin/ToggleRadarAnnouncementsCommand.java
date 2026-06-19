package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.session.PlayerSession;

/**
 * Stage-4b self-describing command for "toggle radar announcements".
 * Owns its own execution (ownsExecution() == true): the dispatch map routes this
 * command's execute() in place of the legacy RadarAnnouncementOnOffHandler. Behaviour
 * is intentionally minimal (no confirmation announcement), matching the legacy handler 1:1.
 */
@RegisterCommand
public final class ToggleRadarAnnouncementsCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.TOGGLE_RADAR_ANNOUNCEMENTS;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        boolean isOn = params.get("state").getAsBoolean();
        PlayerSession.getInstance().setRadarContactAnnouncementOn(isOn);
    }
}
