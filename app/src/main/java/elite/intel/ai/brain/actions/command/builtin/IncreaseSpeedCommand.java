package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.util.AudioPlayer;
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


    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                "key",
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

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public String bindingName() {
        return Bindings.GameCommand.BINDING_INCREASE_SPEED.getGameBinding();
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        JsonElement key = params.get("key");
        Integer num = key == null ? null : StringUtls.getIntSafely(key.getAsString());
        if (num == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.speed.invalidAmount")));
            return;
        }
        String increase = bindingName();
        for (int i = 0; i < num; i++) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(increase)));
            AudioPlayer.getInstance().playBeep(AudioPlayer.BEEP_2);
        }
    }
}
