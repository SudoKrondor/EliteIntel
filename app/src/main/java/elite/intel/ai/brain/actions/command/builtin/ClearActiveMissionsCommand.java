package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.db.managers.MissionManager;

/**
 * Owns its own execution: body migrated 1:1 from the legacy ClearActiveMissionHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ClearActiveMissionsCommand implements IntelCommand {

    private final MissionManager missionManager = MissionManager.getInstance();

    @Override
    public String id() {
        return CommandIds.CLEAR_ACTIVE_MISSIONS;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        missionManager.clear();
    }
}
