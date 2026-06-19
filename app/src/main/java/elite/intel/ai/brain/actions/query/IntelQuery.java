package elite.intel.ai.brain.actions.query;

import elite.intel.ai.brain.actions.customcommand.CustomCommandParameterSpec;
import elite.intel.ai.brain.actions.handlers.query.QueryHandler;

import java.util.List;

/**
 * Self-describing query. Owns its own metadata (id, parameter schema, description key)
 * on top of the existing {@link QueryHandler} execution contract (analog of
 * {@code IntelCommand} over {@code CommandHandler}).
 * <p>
 * Unlike {@code IntelCommand}, {@code handle(...)} is intentionally NOT defaulted: the
 * 45 existing query handlers already implement it with their own LLM-backed logic that
 * returns a JsonObject. This interface only adds self-description; it does not touch
 * execution.
 */
public interface IntelQuery extends QueryHandler {

    String id();

    default List<CustomCommandParameterSpec> parameters() {
        return List.of();
    }

    /** i18n key for the lazy description; parallel to CommandI18nKeys.descriptionKey. */
    default String descriptionKey() {
        return QueryI18nKeys.descriptionKey(id());
    }
}
