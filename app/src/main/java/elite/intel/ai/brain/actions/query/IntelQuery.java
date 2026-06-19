package elite.intel.ai.brain.actions.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.IntelAction;

/**
 * Self-describing query. Owns its own metadata (id, parameter schema, description key)
 * on top of the query execution contract (analog of
 * {@code IntelCommand} over {@code CommandHandler}).
 * <p>
 * Unlike {@code IntelCommand}, {@code handle(...)} is intentionally NOT defaulted: the
 * 45 existing query handlers already implement it with their own LLM-backed logic that
 * returns a JsonObject. This interface only adds self-description; it does not touch
 * execution.
 */
public interface IntelQuery extends IntelAction {

    /** Query execution contract: analyze the action and return the response payload as JSON. */
    JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception;

    /** i18n key for the lazy description; parallel to CommandI18nKeys.descriptionKey. */
    default String descriptionKey() {
        return QueryI18nKeys.descriptionKey(id());
    }
}
