package elite.intel.companion.tools;

import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.ThoughtSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * System function: set the conversation topic. For a COMMANDER thought it also updates the global
 * topic model; for an EVENT thought it sets only the thought's topic (§4.1/§4.2). Available to both
 * sources. Execution is wired in a later phase; this class only self-describes the tool.
 */
@RegisterSystemFunction
public final class SetTopicFunction implements SystemFunction {

    public static final String ID = "set_topic";

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
                new ActionParameterSpec("topic", "string", true,
                        "One of the valid topic ids listed in the TOPICS section.",
                        List.of(), null)
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER, ThoughtSource.EVENT);
    }
}
