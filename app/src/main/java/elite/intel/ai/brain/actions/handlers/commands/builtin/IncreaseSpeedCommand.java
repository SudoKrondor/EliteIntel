package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.Status;
import elite.intel.util.AudioPlayer;
import elite.intel.util.PlayBeepEvent;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Self-describing "increase speed by N" command.
 * Owns its own execution: body migrated 1:1 from the legacy speed-control handler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class IncreaseSpeedCommand implements IntelCommand {
    public static final String ID = "increase_speed";

    /**
     * How long each throttle step holds its key. WHY not a tap: the throttle keys are read as held state each
     * frame, and a tap (about 20 ms on the Robot path) can fall between two frames - the beeps sounded but the
     * throttle never moved, while the same key pressed by hand worked. 120 ms is what UI select already needs
     * to register (see RequestDockingCommand). Shared with {@link DecreaseSpeedCommand}.
     */
    static final int THROTTLE_STEP_HOLD_MS = 120;

    @Override
    public String llmDescription() {
        return "Increase the ship throttle by the number of notches given in 'key'.";
    }


    private static final String PARAM_KEY = "key";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY,
                "number",
                true,
                "The numeric amount to increase speed by, as spoken by the commander (e.g. the 25 in 'increase speed by 25').",
                List.of("25", "10"),
                "Extract the number the commander wants to add to the current speed."
        );
        key.validate();
        return List.of(key);
    }

    @Override
    public String id() {
        return ID;
    }

    /**
     * Ship throttle keys: main ship, fighter or Nomad (a wheeled SRV ignores them), and not while stationary.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return (status.isInMainShip() || status.isInFighter() || status.isInNomad()) && (!status.isDocked() && !status.isOnFoot());
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public String bindingName() {
        return Bindings.GameCommand.BINDING_INCREASE_SPEED.getGameBinding();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        JsonElement key = params.get(PARAM_KEY);
        Integer num = key == null ? null : StringUtls.getIntSafely(key.getAsString());
        if (num == null) {
            return StringUtls.localizedResponse("handler.speed.invalidAmount");
        }
        String increase = bindingName();
        for (int i = 0; i < num; i++) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingHold(increase, THROTTLE_STEP_HOLD_MS)));
            GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_3));
        }
        return null;
    }
}
