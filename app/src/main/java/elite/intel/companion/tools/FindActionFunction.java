package elite.intel.companion.tools;

import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.ThoughtSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * System function: search the action catalog for a ship/game action matching a description. COMMANDER-only.
 * Execution is wired in a later phase; this class only self-describes the tool.
 */
@RegisterSystemFunction
public final class FindActionFunction implements SystemFunction {

    public static final String ID = "find_action";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String descriptionKey() {
        return ID;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return List.of(
                new ActionParameterSpec("query", "string", true,
                        "Description of the action to find.",
                        List.of(), null)
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER);
    }
}
