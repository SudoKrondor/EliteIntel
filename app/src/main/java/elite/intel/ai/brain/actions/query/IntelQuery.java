package elite.intel.ai.brain.actions.query;

import elite.intel.ai.brain.actions.IntelAction;

/**
 * Self-describing query. Owns its own metadata (id, parameter schema, description key)
 * on top of the shared {@link IntelAction#handle} execution contract.
 * <p>
 * Unlike {@code IntelCommand}, query handlers do not default {@code handle(...)}: the
 * existing query handlers each implement it with their own LLM-backed logic that
 * returns a JsonObject (the third argument being the original user input). This
 * interface only adds query-specific self-description; it does not touch execution.
 */
public interface IntelQuery extends IntelAction {

    /** i18n key for the lazy description; parallel to CommandI18nKeys.descriptionKey. */
    default String descriptionKey() {
        return QueryI18nKeys.descriptionKey(id());
    }
}
