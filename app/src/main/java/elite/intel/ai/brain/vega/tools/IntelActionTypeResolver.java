package elite.intel.ai.brain.vega.tools;

import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;

import java.util.function.Function;

/**
 * Resolves a tool-call id to its {@link IntelActionType}, so a {@code Thought} can tell what kind of thing
 * the LLM invoked. Commands and queries have handler-owned outcomes, macros narrate their own steps, and system
 * functions apply their individual interaction policy.
 * <p>
 * The resolution is a registry lookup; it is injected (a test seam) so unit tests need not load the
 * command/query/macro/system-function registries.
 */
public final class IntelActionTypeResolver {

    /** The kind of action behind a tool-call id. */
    public enum IntelActionType {
        /** Built-in command ({@code IntelCommand}): a side effect with a handler-owned spoken outcome. */
        COMMAND,
        /** Read-only query ({@code IntelQuery}): a handler-owned spoken answer. */
        QUERY,
        /** User-defined macro (custom command). */
        MACRO,
        /** Companion system function such as {@code speak} or {@code request_input}. */
        SYSTEM,
        /** Id not found in any registry. */
        UNKNOWN;

        /**
         * Whether this is a game action the LLM invoked - a command, query or macro - as opposed to a companion
         * system function or an unknown id. Queries receive a tool-call id for completed-record replay, and every
         * game action is reported as the turn's execution outcome. The single owner of that grouping.
         */
        public boolean isGameAction() {
            return this == COMMAND || this == QUERY || this == MACRO;
        }
    }

    private final Function<String, IntelActionType> resolver;

    /** Production: resolve against the live registries. */
    public IntelActionTypeResolver() {
        this(IntelActionTypeResolver::resolveFromRegistries);
    }

    /** Seam for tests/advanced wiring: supply the id-to-{@link IntelActionType} resolver directly. */
    public IntelActionTypeResolver(Function<String, IntelActionType> resolver) {
        this.resolver = resolver;
    }

    public IntelActionType resolve(String actionId) {
        return resolver.apply(actionId);
    }

    private static IntelActionType resolveFromRegistries(String actionId) {
        if (CommandRegistry.getInstance().byId().containsKey(actionId)) {
            return IntelActionType.COMMAND;
        }
        if (QueryRegistry.getInstance().byId().containsKey(actionId)) {
            return IntelActionType.QUERY;
        }
        if (SystemFunctionRegistry.getInstance().byId().containsKey(actionId)) {
            return IntelActionType.SYSTEM;
        }
        if (isMacro(actionId)) {
            return IntelActionType.MACRO;
        }
        return IntelActionType.UNKNOWN;
    }

    private static boolean isMacro(String actionId) {
        return CustomCommandRegistry.getInstance().getCustomCommands().stream()
                .anyMatch(macro -> actionId.equals(macro.getActionKey()));
    }
}
