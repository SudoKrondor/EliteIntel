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
 * Self-describing "show station services panel" command.
 * Owns its own execution: body migrated 1:1 from the legacy OpenStationServicesHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ShowStationServicesPanelCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.SHOW_STATION_SERVICES_PANEL;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        UiNavCommon.close();
        UiNavCommon.prepToKnownUiPositionWhileInTheShipAtStation();
        GameControllerBus.publish(GameInputSequenceEvent.of(
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_ACTIVATE.getGameBinding())
        ));
    }
}
