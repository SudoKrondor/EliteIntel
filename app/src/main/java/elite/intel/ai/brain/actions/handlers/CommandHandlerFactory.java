package elite.intel.ai.brain.actions.handlers;

import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.customcommand.CustomCommandRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class CommandHandlerFactory {

    private static final Logger log = LogManager.getLogger(CommandHandlerFactory.class);
    private final Map<String, IntelAction> commandHandlers = new HashMap<>();
    private static CommandHandlerFactory instance;

    private CommandHandlerFactory() {
    }

    public static CommandHandlerFactory getInstance() {
        if (instance == null) {
            instance = new CommandHandlerFactory();
        }
        return instance;
    }

    public Map<String, IntelAction> registerCommandHandlers() {
        for (Map.Entry<String, IntelCommand> entry : CommandRegistry.getInstance().byId().entrySet()) {
            commandHandlers.put(entry.getKey(), entry.getValue());
        }
        log.info("Registered {} built-in command handler(s) from CommandRegistry", commandHandlers.size());

        CustomCommandRegistry.getInstance().contributeToHandlerMap(commandHandlers);

        return commandHandlers;
    }

    public Map<String, IntelAction> getCommandHandlers() {
        return commandHandlers;
    }

    /**
     * Refreshes only custom command handlers in-place so existing ResponseRouter references see edits immediately.
     */
    public void refreshCustomCommandHandlers() {
        commandHandlers.entrySet().removeIf(entry -> entry.getValue() instanceof elite.intel.ai.brain.actions.customcommand.CustomCommandHandler);
        CustomCommandRegistry.getInstance().contributeToHandlerMap(commandHandlers);
    }
}
