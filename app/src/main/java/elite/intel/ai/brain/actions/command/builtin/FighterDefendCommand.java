package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class FighterDefendCommand extends SimpleTapCommand {
    public FighterDefendCommand() {
        super(CommandIds.FIGHTER_DEFEND, Bindings.GameCommand.BINDING_REQUEST_DEFENSIVE_BEHAVIOUR.getGameBinding());
    }
}
