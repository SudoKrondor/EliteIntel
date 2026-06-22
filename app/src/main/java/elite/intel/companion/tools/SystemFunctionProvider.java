package elite.intel.companion.tools;

import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.llm.LlmToolDefinition;

import java.util.List;

/**
 * Provides the native tool specs for the system functions available to a thought, selected by source
 * via {@link SystemFunctionRegistry}. System functions are always present in the prompt.
 */
public final class SystemFunctionProvider {

    /** Returns the system function tool specs available to the given source. */
    public List<LlmToolDefinition> systemFunctions(ThoughtSource source) {
        // TODO: Phase 2 - SystemFunctionRegistry.forSource(source) -> LlmToolDefinition via the shared
        //       ActionParameterSpec -> native-schema converter (also used for game IntelActions).
        throw new UnsupportedOperationException("TODO: Phase 2");
    }
}
