package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.gameapi.journal.events.dto.TargetLocation;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Stage-4b self-describing command for "cancel navigation".
 */
@RegisterCommand
public final class CancelNavigationCommand implements IntelCommand {
    public static final String ID = "cancel_navigation";

    @Override
    public String llmDescription() {
        return "Cancel and turn off the active navigation / surface-target tracking guidance.";
    }


    private final PlayerSession playerSession = PlayerSession.getInstance();

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
        playerSession.setTracking(new TargetLocation(false));
        return StringUtls.localizedLlm("handler.navigate.navigationOff");
    }
}
