package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.session.ui.CommsPanel;
import elite.intel.session.ui.UINavigator;
import elite.intel.util.StringUtls;

/**
 * Stage-4b self-describing command for "show squadron panel".
 * No on-foot path; else
 * branch announces cantDoNow, matching the legacy handler 1:1.
 */
@RegisterCommand
public final class ShowSquadronPanelCommand implements IntelCommand {
    public static final String ID = "show_squadron_panel";

    @Override public String llmDescription() { return "Open the squadron panel."; }


    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        if (status.isInMainShip() || status.isInFighter()) {
            navigator.openAndNavigate(StatusFlags.GuiFocus.COMMS_PANEL, CommsPanel.SQUADRON);
        } else {
            return CommandOutcome.speak(StringUtls.localizedLlm("handler.common.cantDoNow"));
        }
        return null;
    }
}
