package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class FighterAttackTargetCommand extends SimpleTapCommand {
    public FighterAttackTargetCommand() {
        super(CommandIds.FIGHTER_ATTACK_TARGET, Bindings.GameCommand.BINDING_REQUEST_FOCUS_TARGET.getGameBinding());
    }
}
