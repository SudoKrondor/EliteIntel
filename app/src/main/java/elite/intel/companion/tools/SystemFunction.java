package elite.intel.companion.tools;

import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.companion.model.ThoughtSource;

import java.util.Set;

/**
 * Self-describing companion system function (speak, classify_turn, clarify,
 * search_in_memory), modeled on {@code IntelCommand} and - like every other invokable tool -
 * an {@link IntelAction}: it owns its metadata (id, parameter schema via {@link IntelAction#parameters()},
 * the sources that may use it, an English model-facing {@link IntelAction#llmDescription()}) and its
 * execution via {@link IntelAction#handle}. It is auto-discovered by {@link SystemFunctionRegistry}.
 * <p>
 * Because system functions are {@code IntelAction}s, the {@code ExecutionGateway} runs them through the
 * exact same {@code handle} path as commands/queries/macros and stays agnostic to the tool kind. A
 * function reaches the gateways/state it needs statically via {@code CompanionRuntime}.
 */
public interface SystemFunction extends IntelAction {

    /** Thought sources allowed to use this function (COMMANDER and/or EVENT). */
    Set<ThoughtSource> sources();

    /** Whether this function is offered to a thought of the given source. */
    default boolean availableFor(ThoughtSource source) {
        return sources().contains(source);
    }
}
