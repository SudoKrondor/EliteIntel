package elite.intel.gameapi.gamestate.subscribers;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.ai.brain.actions.command.CommandHandler;
import elite.intel.gameapi.gamestate.status_events.BeingInterdictedEvent;


public class InterdictionHandler {

    private final CommandHandlerFactory commandHandlerFactory = CommandHandlerFactory.getInstance();

    @Subscribe
    public void onInterdictedEvent(BeingInterdictedEvent event) {
        CommandHandler activateCombatMode = commandHandlerFactory.getCommandHandlers().get(CommandIds.SWITCH_TO_COMBAT_MODE);
        if (activateCombatMode != null)
            new Thread(() -> activateCombatMode.handle(CommandIds.SWITCH_TO_COMBAT_MODE, null, "")).start();

        CommandHandler handler = commandHandlerFactory.getCommandHandlers().get(CommandIds.TARGET_HOSTILE_HIGHEST_THREAT);
        if (handler != null)
            new Thread(() -> handler.handle(CommandIds.TARGET_HOSTILE_HIGHEST_THREAT, null, "")).start();
    }
}
