package elite.intel.companion.confirm;

import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.companion.model.llm.LlmToolInvocation;

import java.util.Map;

/**
 * {@link DangerousActionPolicy} that reuses the command model's own {@link IntelCommand#isDangerous()}
 * flag - the single existing owner of "this command is dangerous" - rather than a second classification.
 * A tool-call is dangerous when its name resolves to an {@code IntelCommand} marked dangerous; queries
 * (read-only), system functions and macros are not dangerous here.
 * <p>
 * Argument-dependent danger (e.g. a value that makes an otherwise-safe command dangerous, §1.6.19) is a
 * later refinement; this first cut is the per-command flag.
 */
public final class CommandFlagDangerousActionPolicy implements DangerousActionPolicy {

    private final Map<String, IntelAction> commandHandlers;

    /** Production: resolves against the shared command handler map (same source as the execution gateway). */
    public CommandFlagDangerousActionPolicy() {
        this(CommandHandlerFactory.getInstance().registerCommandHandlers());
    }

    /** Test seam: inject a command handler map. */
    CommandFlagDangerousActionPolicy(Map<String, IntelAction> commandHandlers) {
        this.commandHandlers = commandHandlers;
    }

    @Override
    public boolean isDangerous(LlmToolInvocation invocation) {
        IntelAction action = commandHandlers.get(invocation.name());
        return action instanceof IntelCommand command && command.isDangerous();
    }
}
