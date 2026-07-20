package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.actions.handlers.commands.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class TargetHostileHighestThreatCommand extends SimpleTapCommand {
    public static final String ID = "target_hostile_highest_threat";

    @Override
    public String llmDescription() {
        return "Target the highest-threat hostile ship in range (priority combat target).";
    }

    /** Combat targeting: in a combat vehicle (ship/fighter) flying in normal space. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return (status.isInMainShip() || status.isInFighter()) && !status.isDocked() && !status.isLanded() && !status.isInSupercruise();
    }


    public TargetHostileHighestThreatCommand() {
        super(ID, Bindings.GameCommand.BINDING_SELECT_HIGHEST_THREAT.getGameBinding());
    }
}
