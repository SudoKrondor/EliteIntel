package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_FOCUS_ROLE_PANEL;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_FOCUS_ROLE_PANEL_BUGGY;

/**
 * Stage-4b self-describing command for "display radar panel".
 * Owns its own execution (ownsExecution() == true): the dispatch map routes this
 * command's execute() in place of the legacy DisplayRadarPanelHandler.
 */
@RegisterCommand
public final class DisplayRadarPanelCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.DISPLAY_RADAR_PANEL;
    }

    @Override
    public boolean ownsExecution() {
        return true;
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
