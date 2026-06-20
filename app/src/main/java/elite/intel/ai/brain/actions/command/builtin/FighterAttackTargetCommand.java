package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class FighterAttackTargetCommand extends SimpleTapCommand {
    public static final String ID = "fighter_attack_target";

    public FighterAttackTargetCommand() {
        super(ID, Bindings.GameCommand.BINDING_REQUEST_FOCUS_TARGET.getGameBinding());
    }
}
