package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class WingNavLockCommand extends SimpleTapCommand {
    public WingNavLockCommand() {
        super(CommandIds.WING_NAV_LOCK, Bindings.GameCommand.BINDING_WING_NAV_LOCK.getGameBinding());
    }
}
