package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class CycleNextPageCommand extends SimpleTapCommand {
    public static final String ID = "cycle_next_page";

    @Override public String llmDescription() { return "Cycle to the next page within the current panel."; }

    public CycleNextPageCommand() {
        super(ID, Bindings.GameCommand.BINDING_CYCLE_NEXT_PAGE.getGameBinding());
    }

    /** Cockpit-panel UI tap: only while piloting the ship/SRV/fighter (no cockpit panels on foot). */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() || status.isInSrv() || status.isInFighter();
    }
}
