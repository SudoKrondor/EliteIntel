package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.MissionManager;
import elite.intel.session.Status;

/**
 * Owns its own execution: body migrated 1:1 from the legacy ClearActiveMissionHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ClearActiveMissionsCommand implements IntelCommand {
    public static final String ID = "clear_active_missions";

    @Override
    public String llmDescription() {
        return "Clear the app's tracked list of active missions (does not abandon missions in-game).";
    }


    private final MissionManager missionManager = MissionManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    /** App-side bookkeeping (no game input); executable in any location. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        missionManager.clear();
        return null;
    }
}
