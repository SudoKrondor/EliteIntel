package elite.intel.ai.brain.actions.command;

import com.google.gson.JsonObject;

public interface CommandHandler {
    void handle(String action, JsonObject params, String responseText);
}
