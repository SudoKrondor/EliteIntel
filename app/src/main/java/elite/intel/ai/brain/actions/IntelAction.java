package elite.intel.ai.brain.actions;

import elite.intel.ai.brain.actions.customcommand.CustomCommandParameterSpec;
import elite.intel.session.Status;
import java.util.List;

/**
 * Metadata-only supertype shared by IntelCommand and IntelQuery.
 * Carries ONLY what the LLM action-map generator needs: stable id,
 * context visibility, and parameter schema. Deliberately does NOT carry
 * handle(...) — execution stays in the two handler contracts
 * (CommandHandler delegate / IntelQuery.handle). This is a "view from
 * above" for action-map assembly and ordering, not a dispatch contract.
 */
public interface IntelAction {
    String id();

    default boolean isVisibleForLLM(Status status) {
        return true;
    }

    default List<CustomCommandParameterSpec> parameters() {
        return List.of();
    }
}
