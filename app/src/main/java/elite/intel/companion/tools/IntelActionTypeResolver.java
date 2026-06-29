package elite.intel.companion.tools;

import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.customcommand.CustomCommandRegistry;
import elite.intel.ai.brain.actions.query.QueryRegistry;

import java.util.function.Function;

/**
 * Resolves a tool-call id to its {@link IntelActionType}, so a {@code Thought} can tell what kind of thing
 * the LLM invoked. A {@code COMMAND}/{@code QUERY} has a handler-owned spoken outcome (its
 * {@code text_to_speech_response} is voiced when present, and the LLM's own {@code speak} is withheld for
 * the turn); a {@code MACRO}, {@code SYSTEM} function or {@code UNKNOWN} id is not a handler outcome (it
 * neither vocalizes here nor withholds the LLM's speak).
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
        /** Companion system function (speak, nothing_to_do, set_importance, search_in_memory, change_*). */
        SYSTEM,
        /** Id not found in any registry. */
        UNKNOWN
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
