package elite.intel.companion.tools;

import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.llm.LlmToolDefinition;

import java.util.List;

/**
 * Provides the native tool specs for the system functions available to a thought, selected by source
 * via {@link SystemFunctionRegistry}. System functions are always present in the prompt.
 * <p>
 * Each {@link SystemFunction} self-describes its id and parameter schema; this provider only resolves
 * the model-facing description (English, via {@link CompanionFunctionTextProvider}) and packages the
 * result as a provider-neutral {@link LlmToolDefinition}. System functions carry no training phrases
 * (the commander never triggers them by voice).
 */
public final class SystemFunctionProvider {

    private final SystemFunctionRegistry registry;
    private final CompanionFunctionTextProvider descriptions;

    /** Production constructor: the shared registry and the English description owner. */
    public SystemFunctionProvider() {
        this(SystemFunctionRegistry.getInstance(), new CompanionFunctionTextProvider());
    }

    /** Injectable constructor for tests. */
    SystemFunctionProvider(SystemFunctionRegistry registry, CompanionFunctionTextProvider descriptions) {
        this.registry = registry;
        this.descriptions = descriptions;
    }

    /** Returns the system function tool specs available to the given source. */
    public List<LlmToolDefinition> systemFunctions(ThoughtSource source) {
        if (registry.byId().isEmpty()) {
            registry.load();
        }
        return registry.forSource(source).stream()
                .map(function -> new LlmToolDefinition(
                        function.id(),
                        descriptions.describe(function.descriptionKey()),
                        "",
                        function.parameters()))
                .toList();
    }
}
