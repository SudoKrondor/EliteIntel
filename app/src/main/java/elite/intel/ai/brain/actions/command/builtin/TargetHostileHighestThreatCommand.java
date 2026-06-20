package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class TargetHostileHighestThreatCommand extends SimpleTapCommand {
    public static final String ID = "target_hostile_highest_threat";

    public TargetHostileHighestThreatCommand() {
        super(ID, Bindings.GameCommand.BINDING_SELECT_HIGHEST_THREAT.getGameBinding());
    }
}
