package elite.intel.companion.tools;

import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.ThoughtSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * System function: change the companion's verbosity mode (how freely EVENT thoughts comment).
 * COMMANDER-only. Execution is wired in a later phase; this class only self-describes the tool.
 */
@RegisterSystemFunction
public final class ChangeVerbosityFunction implements SystemFunction {

    public static final String ID = "change_verbosity";

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
                new ActionParameterSpec("verbosity", "string", true,
                        "The new verbosity mode: quiet, normal, or chatty.",
                        List.of("quiet", "normal", "chatty"), null)
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER);
    }
}
