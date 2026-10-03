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
 * Self-describing "decrease speed by N" command.
 * Owns its own execution: body migrated 1:1 from the legacy speed-control handler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class DecreaseSpeedCommand implements IntelCommand {
    public static final String ID = "decrease_speed";

    @Override
    public String llmDescription() {
        return "Decrease the ship throttle by the number of notches given in 'key'.";
    }


    private static final String PARAM_KEY = "key";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY,
                "number",
                true,
                "The numeric amount to decrease speed by, as spoken by the commander (e.g. the 25 in 'decrease speed by 25').",
                List.of("25", "10"),
                "Extract the number the commander wants to subtract from the current speed."
        );
        key.validate();
        return List.of(key);
    }

    @Override
    public String id() {
        return ID;
    }

    /**
     * The ship throttle keys: the main ship, a fighter or the Nomad (which flies on the ship's controls). A
     * wheeled SRV ignores them - its throttle is a separate set of buggy controls.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() || status.isInNomad() || status.isInFighter();
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public String bindingName() {
        return Bindings.GameCommand.BINDING_DECREASE_SPEED.getGameBinding();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        JsonElement key = params.get(PARAM_KEY);
        Integer num = key == null ? null : StringUtls.getIntSafely(key.getAsString());
        if (num == null) {
            return StringUtls.localizedResponse("handler.speed.invalidAmount");
        }

        String decrease = bindingName();
        for (int i = 0; i < num; i++) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingHold(decrease, IncreaseSpeedCommand.THROTTLE_STEP_HOLD_MS)));
            GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_3));
        }
        return null;
    }
}
