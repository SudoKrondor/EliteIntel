package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.gameapi.inputs.UiNavCommon;

/**
 * Self-describing "launch ship" command.
 * Owns its own execution: body migrated 1:1 from the legacy LaunchShipHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class LaunchShipDetachFromStationCommand implements IntelCommand {
    public static final String ID = "launch_ship_detach_from_station";


    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        UiNavCommon.close();
        UiNavCommon.prepToKnownUiPositionWhileInTheShipAtStation();
        GameControllerBus.publish(GameInputSequenceEvent.of(
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_ACTIVATE.getGameBinding())
        ));
    }
}
