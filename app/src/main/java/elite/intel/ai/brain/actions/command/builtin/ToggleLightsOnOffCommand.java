package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;

/**
 * Stage-4b self-describing command for "toggle lights".
 */
@RegisterCommand
public final class ToggleLightsOnOffCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.TOGGLE_LIGHTS_ON_OFF;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isInSrv()) {
            if (status.isSrvHighBeam()) {
                toggleLights(Bindings.GameCommand.BINDING_BUGGY_LIGHTS_TOGGLE.getGameBinding());
            } else {
                toggleLights(Bindings.GameCommand.BINDING_BUGGY_LIGHTS_TOGGLE.getGameBinding());
                toggleLights(Bindings.GameCommand.BINDING_BUGGY_LIGHTS_TOGGLE.getGameBinding());
            }
        }

        if (status.isInMainShip()) {
            toggleLights(Bindings.GameCommand.BINDING_SHIP_LIGHTS_TOGGLE.getGameBinding());
        }
    }

    private void toggleLights(String binding) {
        GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(binding)));
    }
}
