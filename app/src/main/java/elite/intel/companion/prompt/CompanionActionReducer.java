package elite.intel.companion.prompt;

import elite.intel.ai.embed.SemanticQuery;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.llm.LlmToolDefinition;

import java.util.List;
import java.util.Set;

/**
 * Selects the game tools offered to the LLM for one thought turn, narrowed from the full catalog to a
 * prompt-sized set. This is the swap seam for the selection strategy: the contract is intentionally
 * stated as {@code (allowedCategories, currentInput) -> tools}. An optional per-turn
 * {@link SemanticQuery} lets cooperating routing stages reuse already-computed work without becoming a
 * cross-turn cache; reducers that do not understand it simply ignore it. The seam otherwise leaks no algorithm
 * detail (no phrase maps, no word-overlap), so the current word-overlap wrapper over the legacy
 * {@code elite.intel.ai.brain.Reducer} can later be replaced by a smarter (e.g. semantic) reducer
 * without touching any call site (see COMPANION_ARCHITECTURE.md §10.3).
 * <p>
 * The category set is supplied by {@code IntelActionAccessPolicy} per thought source; an EVENT thought
 * is given no game tools, so it cannot receive an action, query, or macro regardless of implementation.
 */
public interface CompanionActionReducer {

    /**
     * Returns the tool definitions to offer this turn.
     *
     * @param allowedCategories the IntelAction categories this thought may use
     * @param currentInput      the commander reply (or event summary) used to narrow the set
     * @return reduced, prompt-ready tool definitions (empty when nothing is relevant)
     */
    List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories, String currentInput);

    /**
     * Returns the tool definitions to offer this turn, optionally reusing hints prepared while the same input was
     * routed. The default preserves the two-argument contract for reducers that have no use for such hints.
     *
     * @param semanticQuery optional embedding prepared for this exact input during the current turn's intake
     */
    default List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories, String currentInput,
                                                SemanticQuery semanticQuery) {
        return selectTools(allowedCategories, currentInput);
    }
}
