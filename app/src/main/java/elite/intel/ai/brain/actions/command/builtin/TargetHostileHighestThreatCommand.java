package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class TargetHostileHighestThreatCommand extends SimpleTapCommand {
    public TargetHostileHighestThreatCommand() {
        super(CommandIds.TARGET_HOSTILE_HIGHEST_THREAT, Bindings.GameCommand.BINDING_SELECT_HIGHEST_THREAT.getGameBinding());
    }
}
