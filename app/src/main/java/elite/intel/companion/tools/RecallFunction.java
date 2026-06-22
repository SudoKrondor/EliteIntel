package elite.intel.companion.tools;

import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.ThoughtSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * System function: load stored memory. {@code scope=llm_memory} returns all remembered facts;
 * {@code scope=topic_memory} returns entries for one topic (topic required, query optional). Short-term
 * timeline and long-term summary are not recallable here (already in the prompt). COMMANDER-only.
 * Execution is wired in a later phase; this class only self-describes the tool.
 */
@RegisterSystemFunction
public final class RecallFunction implements SystemFunction {

    public static final String ID = "recall";

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
                new ActionParameterSpec("scope", "string", true,
                        "Either llm_memory or topic_memory.",
                        List.of("llm_memory", "topic_memory"), null),
                new ActionParameterSpec("topic", "string", false,
                        "Required when scope is topic_memory: the topic id to recall.",
                        List.of("navigation"), null),
                new ActionParameterSpec("query", "string", false,
                        "Optional plain-text filter within the topic.",
                        List.of(), null)
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER);
    }
}
