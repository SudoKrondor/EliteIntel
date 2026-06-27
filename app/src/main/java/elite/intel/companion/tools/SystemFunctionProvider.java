package elite.intel.companion.tools;

import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.llm.LlmToolDefinition;

import java.util.Comparator;
import java.util.List;

/**
 * Provides the native tool specs for the system functions available to a thought, selected by source
 * via {@link SystemFunctionRegistry}. System functions are always present in the prompt.
 * <p>
 * Each {@link SystemFunction} self-describes its id, parameter schema and English model-facing
 * {@code llmDescription()}; this provider only packages the result as a provider-neutral
 * {@link LlmToolDefinition}. System functions carry no training phrases (the commander never triggers
 * them by voice).
 */
public final class SystemFunctionProvider {

    /**
     * Prompt-order lead group: the universal interaction tools come first; every other function
     * follows deterministically by id. Registry discovery order is non-deterministic, which would
     * churn the request and the prompt cache, so the order is fixed here. New functions not listed
     * here automatically sort into the alphabetical tail - no central list to keep in sync.
     */
    private static final List<String> LEAD_ORDER = List.of(SpeakFunction.ID, NothingToDoFunction.ID);

    private final SystemFunctionRegistry registry;

    /** Production constructor: the shared registry (each function self-describes its English description). */
    public SystemFunctionProvider() {
        this(SystemFunctionRegistry.getInstance());
    }

    /** Injectable constructor for tests. */
    SystemFunctionProvider(SystemFunctionRegistry registry) {
        this.registry = registry;
    }

    /** Returns the system function tool specs available to the given source. */
    public List<LlmToolDefinition> systemFunctions(ThoughtSource source) {
        if (registry.byId().isEmpty()) {
            registry.load();
        }
        return registry.forSource(source).stream()
                .sorted(Comparator
                        .comparingInt((SystemFunction f) -> leadRank(f.id()))
                        .thenComparing(SystemFunction::id))
                .map(function -> new LlmToolDefinition(
                        function.id(),
                        function.llmDescription(),
                        "",
                        function.parameters()))
                .toList();
    }

    /** Lead-group index, or a value past the group so unlisted functions fall to the alphabetical tail. */
    private static int leadRank(String id) {
        int index = LEAD_ORDER.indexOf(id);
        return index >= 0 ? index : LEAD_ORDER.size();
    }
}
