package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class FighterAttackTargetCommand extends SimpleTapCommand {
    public static final String ID = "fighter_attack_target";

    @Override
    public String llmDescription() {
        return "Order the deployed ship-launched fighter (or NPC crew) to attack the commander's current target.";
    }

    public FighterAttackTargetCommand() {
        super(ID, Bindings.GameCommand.BINDING_REQUEST_FOCUS_TARGET.getGameBinding());
    }

    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isFighterOut();
    }
}
