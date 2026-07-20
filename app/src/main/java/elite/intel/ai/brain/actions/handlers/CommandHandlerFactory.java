package elite.intel.ai.brain.actions.handlers;

import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandHandler;
import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandRegistry;
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

    /**
     * Builds the shared command-handler map once and caches it, so every caller (execution gateway, danger
     * policy, response router, reflex resolvers) shares the same instance instead of re-running registration.
     * Relies on boot ordering: {@code App.main} loads {@code CommandRegistry} before any caller is constructed,
     * so the first call always sees the built-in commands. The empty-map guard only covers the degenerate case
     * where nothing has been registered yet. Custom-command edits are applied via
     * {@link #refreshCustomCommandHandlers()}.
     */
    public Map<String, IntelAction> registerCommandHandlers() {
        if (!commandHandlers.isEmpty()) {
            return commandHandlers; // built once; all callers share the same map
        }
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
        commandHandlers.entrySet().removeIf(entry -> entry.getValue() instanceof CustomCommandHandler);
        CustomCommandRegistry.getInstance().contributeToHandlerMap(commandHandlers);
    }
}
