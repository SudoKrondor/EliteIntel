package elite.intel.companion.tools;

import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.ThoughtSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * System function: ask the commander a short clarifying question and wait for the reply before acting.
 * Distinct from speak: it expects an answer that continues the exchange, rather than just uttering a
 * phrase. COMMANDER-only (an EVENT thought never converses). The awaiting-reply state is wired with the
 * thought/dispatcher in a later phase; this class only self-describes the tool.
 */
@RegisterSystemFunction
public final class ClarifyFunction implements SystemFunction {

    public static final String ID = "clarify";

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
                new ActionParameterSpec("question", "string", true,
                        "The clarifying question to ask the commander.",
                        List.of(), null)
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER);
    }
}
