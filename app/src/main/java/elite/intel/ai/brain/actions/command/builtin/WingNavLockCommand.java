package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class WingNavLockCommand extends SimpleTapCommand {
    public static final String ID = "wing_nav_lock";

    @Override
    public String llmDescription() {
        return "Engage wing navigation lock to lock onto and follow a wing-mate's frame shift drive (follow into supercruise/jump).";
    }

    public WingNavLockCommand() {
        super(ID, Bindings.GameCommand.BINDING_WING_NAV_LOCK.getGameBinding());
    }

    /// selects
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInWing();
    }
}
