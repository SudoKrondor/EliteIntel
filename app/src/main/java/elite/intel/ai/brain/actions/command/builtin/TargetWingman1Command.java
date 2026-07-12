package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class TargetWingman1Command extends SimpleTapCommand {
    public static final String ID = "target_wingman_1";

    @Override
    public String llmDescription() {
        return "Target wing-mate 1 (Alpha).";
    }

    public TargetWingman1Command() {
        super(ID, Bindings.GameCommand.BINDING_TARGET_WINGMAN0.getGameBinding());
    }

    /** Wingman targeting only makes sense while in a wing. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInWing();
    }
}
