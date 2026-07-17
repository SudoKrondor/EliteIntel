package elite.intel.companion.prompt;

import elite.intel.ai.embed.SemanticQuery;
import elite.intel.companion.model.GameStateSnapshot;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.llm.LlmToolDefinition;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Selects the game tools offered to the LLM for one thought turn, narrowed from the full catalog to a
 * prompt-sized set. This is the swap seam for the selection strategy: its required inputs are the allowed
 * categories and current input; optional per-turn {@link SemanticQuery} and {@link GameStateSnapshot} inputs let
 * callers reuse already-computed work and one visibility context without becoming cross-turn caches. Reducers that
 * do not understand those inputs simply ignore them. The seam otherwise leaks no algorithm
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
     * Returns the tool definitions to offer this turn, optionally reusing an embedding prepared by the caller for
     * the same input. The default preserves the two-argument contract for reducers that have no use for it.
     *
     * @param semanticQuery optional embedding prepared for this exact input during the current turn
     */
    default List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories, String currentInput,
                                                SemanticQuery semanticQuery) {
        return selectTools(allowedCategories, currentInput);
    }

    /**
     * Returns this turn's tools using the immutable game state captured at commander intake. Implementations that
     * do not gate game actions by status may keep the default and ignore the snapshot.
     *
     * @param gameStateSnapshot commander-turn visibility state; {@code null} for non-commander/legacy callers
     */
    default List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories, String currentInput,
                                                SemanticQuery semanticQuery,
                                                GameStateSnapshot gameStateSnapshot) {
        return selectTools(allowedCategories, currentInput, semanticQuery);
    }

    /**
     * Resolves one action id against the current turn's visible catalog, bypassing semantic narrowing. This is
     * used only to re-offer a previously validated clarification target; implementations should still apply the
     * supplied live-state visibility snapshot. The default preserves compatibility with simple test reducers by
     * using their blank-input "offer all" behavior.
     */
    default Optional<LlmToolDefinition> findToolById(Set<IntelActionCategory> allowedCategories, String actionId,
                                                     GameStateSnapshot gameStateSnapshot) {
        if (actionId == null || actionId.isBlank()) {
            return Optional.empty();
        }
        return selectTools(allowedCategories, "", null, gameStateSnapshot).stream()
                .filter(tool -> actionId.equals(tool.name()))
                .findFirst();
    }
}
