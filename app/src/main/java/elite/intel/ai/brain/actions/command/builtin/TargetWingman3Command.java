package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class TargetWingman3Command extends SimpleTapCommand {
    public static final String ID = "target_wingman_3";

    @Override public String llmDescription() { return "Target wingman 3."; }

    public TargetWingman3Command() {
        super(ID, Bindings.GameCommand.BINDING_TARGET_WINGMAN2.getGameBinding());
    }

    /** Wingman targeting only makes sense while in a wing. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInWing();
    }
}
