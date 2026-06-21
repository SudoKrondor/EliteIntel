package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_EXPLORATION_FSSDISCOVERY_SCAN;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_SET_SPEED_ZERO;

/**
 * Stage-4b self-describing command for "open fss and scan system".
 */
@RegisterCommand
public final class OpenFssScanSystemCommand implements IntelCommand {
    public static final String ID = "open_fss_scan_system";


    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        if (status.isScoopingFuel()) {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.supercruise.scooping")));
            return;
        }

        if (!status.isInSupercruise()) {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.supercruise.mustBeSupercruise")));
            return;
        }

        String stop = BINDING_SET_SPEED_ZERO.getGameBinding();
        String fssControl = BINDING_EXPLORATION_FSSDISCOVERY_SCAN.getGameBinding();

        GameControllerBus.publish(GameInputSequenceEvent.of(
                GameInputStep.bindingTap(stop),
                GameInputStep.delay(200),
                GameInputStep.bindingTap(fssControl)
        ));
    }
}
