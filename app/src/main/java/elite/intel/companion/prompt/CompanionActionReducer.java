package elite.intel.companion.prompt;

import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.llm.LlmToolDefinition;

import java.util.List;
import java.util.Set;

/**
 * Selects the game tools offered to the LLM for one thought turn, narrowed from the full catalog to a
 * prompt-sized set. This is the swap seam for the selection strategy: the contract is intentionally
 * stated as {@code (allowedCategories, currentInput) -> tools} and leaks no algorithm detail (no
 * phrase maps, no word-overlap), so the current word-overlap wrapper over the legacy
 * {@code elite.intel.ai.brain.Reducer} can later be replaced by a smarter (e.g. semantic) reducer
 * without touching any call site (see COMPANION_ARCHITECTURE.md §10.3).
 * <p>
 * The category set is supplied by {@code IntelActionAccessPolicy} per thought source; an EVENT thought
 * is given only {@code QUERY}, so it can never receive action/macro tools regardless of implementation.
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
}
