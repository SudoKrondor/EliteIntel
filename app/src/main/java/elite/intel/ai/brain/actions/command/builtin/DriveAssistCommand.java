package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class DriveAssistCommand extends SimpleTapCommand {
    public static final String ID = "drive_assist";

    public DriveAssistCommand() {
        super(ID, Bindings.GameCommand.BINDING_DRIVE_ASSIST.getGameBinding());
    }
}
