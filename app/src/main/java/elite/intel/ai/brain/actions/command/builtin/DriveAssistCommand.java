package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class DriveAssistCommand extends SimpleTapCommand {
    public static final String ID = "drive_assist";

    @Override public String llmDescription() { return "Toggle SRV drive assist on or off."; }

    public DriveAssistCommand() {
        super(ID, Bindings.GameCommand.BINDING_DRIVE_ASSIST.getGameBinding());
    }

    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInSrv();
    }
}
