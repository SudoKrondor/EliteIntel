package elite.intel.ai.brain.actions.command;

import com.google.gson.JsonObject;

public interface CommandHandler {
    JsonObject handle(String action, JsonObject params, String responseText);
}
