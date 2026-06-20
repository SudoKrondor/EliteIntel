package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;

/**
 * Stage-4b self-describing command for "toggle night vision".
 * Also carries its game
 * binding via bindingName() (sourced from Bindings, NOT a literal) so command-model
 * parity stays matched once the enum is eventually removed.
 */
@RegisterCommand
public final class ToggleNightVisionOnOffCommand implements IntelCommand {
    public static final String ID = "toggle_night_vision_on_off";


    @Override
    public String id() {
        return ID;
    }

    @Override
    public String bindingName() {
        return Bindings.GameCommand.BINDING_NIGHT_VISION_TOGGLE.getGameBinding();
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_NIGHT_VISION_TOGGLE.getGameBinding())));
    }
}
