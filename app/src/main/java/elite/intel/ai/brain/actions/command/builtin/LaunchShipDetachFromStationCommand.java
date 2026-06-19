package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.UiNavCommon;
import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;

/**
 * Self-describing "launch ship" command.
 * Owns its own execution: body migrated 1:1 from the legacy LaunchShipHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class LaunchShipDetachFromStationCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.LAUNCH_SHIP_DETACH_FROM_STATION;
    }

    @Override
    public boolean ownsExecution() {
        return true;
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
