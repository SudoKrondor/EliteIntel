package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.session.Status;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_ACTIVATE_COMBAT_MODE;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_ACTIVATE_COMBAT_MODE_BUGGY;

/**
 * Stage-4b self-describing command for "switch to combat mode".
 */
@RegisterCommand
public final class SwitchToCombatModeCommand implements IntelCommand {
    public static final String ID = "switch_to_combat_mode";

    @Override
    public String llmDescription() {
        return "Switch the ship/SRV HUD to combat mode.";
    }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() || status.isInSrv();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isAnalysisMode()) {
            if (status.isInMainShip()) {
                GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_ACTIVATE_COMBAT_MODE.getGameBinding())));
            }

            // The SRV has its OWN HUD toggle binding. Tapping the ship's one from inside a buggy sends a
            // key the SRV does not listen to: the keystroke reports success, the HUD does not change, and
            // the commander is told nothing - which read as "combat mode is broken in the Rhino" when it
            // was equally broken in a Scarab and had been all along. The mirror command,
            // SwitchToAnalysisModeCommand, always used the buggy binding here, which is why switching the
            // other way worked.
            if (status.isInSrv()) {
                GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_ACTIVATE_COMBAT_MODE_BUGGY.getGameBinding())));
            }
        }
        return null;
    }
}
