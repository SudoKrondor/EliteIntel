package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.gameapi.inputs.UiNavCommon;
import elite.intel.session.Status;

/**
 * Self-describing "launch ship" command.
 * Owns its own execution: body migrated 1:1 from the legacy LaunchShipHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class LaunchShipDetachFromStationCommand implements IntelCommand {
    public static final String ID = "launch_ship_detach_from_station";

    @Override
    public String llmDescription() {
        return "Undock: launch the main ship and detach it from the station landing pad. Only for leaving a dock, never for deploying an SRV, fighter, or Nomad.";
    }

    /**
     * Undocking only makes sense while sitting in the main ship on a station/port pad. Restricting visibility to
     * that context keeps "launch"/"detach" from competing with the vehicle-deploy commands (SRV/fighter/Nomad),
     * which are never available at a dock - the game-state gate the legacy path relied on.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() && status.isDocked();
    }

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
