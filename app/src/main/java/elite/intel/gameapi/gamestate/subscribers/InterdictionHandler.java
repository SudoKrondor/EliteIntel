package elite.intel.gameapi.gamestate.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.gameapi.gamestate.status_events.BeingInterdictedEvent;
import elite.intel.ai.brain.actions.command.builtin.TargetHostileHighestThreatCommand;
import elite.intel.ai.brain.actions.command.builtin.SwitchToCombatModeCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class InterdictionHandler {

    private static final Logger log = LogManager.getLogger(InterdictionHandler.class);

    private final CommandHandlerFactory commandHandlerFactory = CommandHandlerFactory.getInstance();

    @Subscribe
    public void onInterdictedEvent(BeingInterdictedEvent event) {
        dispatchAsync(commandHandlerFactory.getCommandHandlers().get(SwitchToCombatModeCommand.ID), SwitchToCombatModeCommand.ID);
        dispatchAsync(commandHandlerFactory.getCommandHandlers().get(TargetHostileHighestThreatCommand.ID), TargetHostileHighestThreatCommand.ID);
    }

    /** Fires a command handler on its own thread, isolating any handler failure from the event bus. */
    private void dispatchAsync(IntelAction handler, String action) {
        if (handler == null) return;
        new Thread(() -> {
            try {
                handler.handle(action, null, "");
            } catch (Exception e) {
                log.error("Interdiction handler failed for action {}: {}", action, e.getMessage(), e);
            }
        }).start();
    }
}
