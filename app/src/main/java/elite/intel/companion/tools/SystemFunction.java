package elite.intel.companion.tools;

import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.ThoughtSource;

import java.util.List;
import java.util.Set;

/**
 * Self-describing companion system function (speak, nothing_to_do, set_topic, remember, recall,
 * find_action, change_verbosity), modeled on {@code IntelCommand}: it owns its own metadata
 * (id, parameter schema, description key, the sources that may use it) and is auto-discovered by
 * {@link SystemFunctionRegistry}.
 * <p>
 * The prompt layer never generates schemas itself; each function describes its parameters via
 * {@link #parameters()}, reusing the existing {@link ActionParameterSpec} (deliberate reuse of the
 * project's parameter-spec owner).
 * <p>
 * System functions are not game actions: they act on the thought/topic/memory or speak via the
 * gateways. The execution contract is defined when the concrete functions are implemented (Phase 2+).
 */
public interface SystemFunction {

    /** Unique tool name as it appears in the native tool schema (e.g. "speak", "set_topic"). */
    String id();

    /** Parameter schema for native tool-calling; empty for no-arg functions. */
    default List<ActionParameterSpec> parameters() {
        return List.of();
    }

    /** i18n key for this function's description shown to the LLM. */
    String descriptionKey();

    /** Thought sources allowed to use this function (COMMANDER and/or EVENT). */
    Set<ThoughtSource> sources();

    /** Whether this function is offered to a thought of the given source. */
    default boolean availableFor(ThoughtSource source) {
        return sources().contains(source);
    }
}
