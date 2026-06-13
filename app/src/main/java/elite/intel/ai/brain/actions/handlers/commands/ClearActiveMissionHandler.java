package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.db.managers.MissionManager;

public class ClearActiveMissionHandler implements CommandHandler {

    private final MissionManager missionManager = MissionManager.getInstance();

    @Override
    public void handle(String action, JsonObject params, String responseText) {
        missionManager.clear();
    }
}
