package elite.intel.ai.brain.actions.command.builtin;

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
    public static final String ID = "clear_active_missions";

    @Override public String llmDescription() { return "Clear the tracked active missions."; }


    private final MissionManager missionManager = MissionManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        missionManager.clear();
    }
}
