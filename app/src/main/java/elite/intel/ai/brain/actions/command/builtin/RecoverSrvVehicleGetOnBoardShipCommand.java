package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.StatusFlags;
import elite.intel.session.ui.UINavigator;

/**
 * Stage-4b self-describing command for "recover srv".
 * Owns its own execution (ownsExecution() == true): the dispatch map routes this
 * command's execute() in place of the legacy RecoverSrvHandler.
 */
@RegisterCommand
public final class RecoverSrvVehicleGetOnBoardShipCommand implements IntelCommand {

    private final UINavigator navigator = new UINavigator();

    @Override
    public String id() {
        return CommandIds.RECOVER_SRV_VEHICLE_GET_ON_BOARD_SHIP;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        String ui_left = Bindings.GameCommand.BINDING_UI_LEFT.getGameBinding();
        String ui_up = Bindings.GameCommand.BINDING_UI_UP.getGameBinding();
        String ui_down = Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding();
        String ui_right = Bindings.GameCommand.BINDING_UI_RIGHT.getGameBinding();
        String activate = Bindings.GameCommand.BINDING_ACTIVATE.getGameBinding();

        GameControllerBus.publish(GameInputSequenceEvent.of(
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_FOCUS_ROLE_PANEL_BUGGY.getGameBinding()),
                // Ensure the cursor is at the top before navigating to recover SRV.
                GameInputStep.bindingTap(ui_left),
                GameInputStep.bindingTap(ui_left),
                GameInputStep.bindingTap(ui_up),
                GameInputStep.bindingTap(ui_up),
                GameInputStep.bindingTap(ui_up),
                // Recover SRV.
                GameInputStep.bindingTap(ui_down),
                GameInputStep.bindingTap(ui_right),
                GameInputStep.bindingTap(activate)
        ));
        navigator.assumeDefaultState(StatusFlags.GuiFocus.ROLE_PANEL);
    }
}
