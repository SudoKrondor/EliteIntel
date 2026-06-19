package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_ACTIVATE_ANALYSIS_MODE;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_ACTIVATE_ANALYSIS_MODE_BUGGY;

/**
 * Stage-4b self-describing command for "switch to analysis mode".
 */
@RegisterCommand
public final class SwitchToAnalysisModeCommand implements IntelCommand {
    public static final String ID = "switch_to_analysis_mode";


    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();
        if (!status.isAnalysisMode()) {
            if (status.isInMainShip()) {
                GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_ACTIVATE_ANALYSIS_MODE.getGameBinding())));
            }

            if (status.isInSrv()) {
                GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_ACTIVATE_ANALYSIS_MODE_BUGGY.getGameBinding())));
            }
        }
    }
}
