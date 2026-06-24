package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class WingNavLockCommand extends SimpleTapCommand {
    public static final String ID = "wing_nav_lock";

    @Override public String llmDescription() { return "Engage wing navigation lock to follow a wing-mate."; }

    public WingNavLockCommand() {
        super(ID, Bindings.GameCommand.BINDING_WING_NAV_LOCK.getGameBinding());
    }
}
