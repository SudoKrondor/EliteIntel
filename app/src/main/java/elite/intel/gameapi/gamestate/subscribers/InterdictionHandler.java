package elite.intel.gameapi.gamestate.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.ai.brain.actions.command.CommandHandler;
import elite.intel.gameapi.gamestate.status_events.BeingInterdictedEvent;
import elite.intel.ai.brain.actions.command.builtin.TargetHostileHighestThreatCommand;
import elite.intel.ai.brain.actions.command.builtin.SwitchToCombatModeCommand;


public class InterdictionHandler {

    private final CommandHandlerFactory commandHandlerFactory = CommandHandlerFactory.getInstance();

    @Subscribe
    public void onInterdictedEvent(BeingInterdictedEvent event) {
        CommandHandler activateCombatMode = commandHandlerFactory.getCommandHandlers().get(SwitchToCombatModeCommand.ID);
        if (activateCombatMode != null)
            new Thread(() -> activateCombatMode.handle(SwitchToCombatModeCommand.ID, null, "")).start();

        CommandHandler handler = commandHandlerFactory.getCommandHandlers().get(TargetHostileHighestThreatCommand.ID);
        if (handler != null)
            new Thread(() -> handler.handle(TargetHostileHighestThreatCommand.ID, null, "")).start();
    }
}
