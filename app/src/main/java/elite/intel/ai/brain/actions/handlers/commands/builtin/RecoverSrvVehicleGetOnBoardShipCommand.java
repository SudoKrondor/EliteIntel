package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.session.ui.UINavigator;

/**
 * Stage-4b self-describing command for "recover srv".
 */
@RegisterCommand
public final class RecoverSrvVehicleGetOnBoardShipCommand implements IntelCommand {
    public static final String ID = "recover_srv_vehicle_get_on_board_ship";

    @Override
    public String llmDescription() {
        return "Return the deployed SRV or buggy to the main ship and board it, placing the vehicle in the ship's hangar. The SRV goes to the ship; this does not call the ship to the commander.";
    }


    private final UINavigator navigator = new UINavigator();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInSrv();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
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
                GameInputStep.bindingTap(activate),
                GameInputStep.bindingTap(activate) // << Rhino needs two taps
        ));
        navigator.assumeDefaultState(StatusFlags.GuiFocus.CENTRAL_PANEL);
        return null;
    }
}
