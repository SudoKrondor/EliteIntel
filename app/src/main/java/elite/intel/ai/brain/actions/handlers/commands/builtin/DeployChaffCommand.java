package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.actions.handlers.commands.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class DeployChaffCommand extends SimpleTapCommand {
    public static final String ID = "deploy_chaff";

    @Override
    public String llmDescription() {
        return "Launch chaff to break incoming missile and gimballed-weapon locks.";
    }

    public DeployChaffCommand() {
        super(ID, Bindings.GameCommand.BINDING_FIRE_CHAFF_LAUNCHER.getGameBinding());
    }

    /** Countermeasure fired only while flying the main ship in normal space (not docked/landed/supercruise). */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() && !status.isDocked() && !status.isLanded() && !status.isInSupercruise();
    }
}
