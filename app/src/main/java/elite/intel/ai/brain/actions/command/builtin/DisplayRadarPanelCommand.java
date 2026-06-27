package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.session.Status;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_FOCUS_ROLE_PANEL;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_FOCUS_ROLE_PANEL_BUGGY;

/**
 * Stage-4b self-describing command for "display radar panel".
 */
@RegisterCommand
public final class DisplayRadarPanelCommand implements IntelCommand {
    public static final String ID = "display_radar_panel";

    @Override public String llmDescription() { return "Open the radar / sensors panel."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isInMainShip()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_FOCUS_ROLE_PANEL.getGameBinding())));
        }

        if (status.isInSrv()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_FOCUS_ROLE_PANEL_BUGGY.getGameBinding())));
        }
    }
}
