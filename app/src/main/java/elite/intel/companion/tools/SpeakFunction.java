package elite.intel.companion.tools;

import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.ThoughtSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * System function: speak a phrase to the commander via the SpeechGateway. Available to both sources.
 * The {@code confirmation_request} marker flags a dangerous-action confirmation prompt (§2.13); only
 * such a speak runs immediately while a dangerous tool-call set is frozen. Execution is wired in a
 * later phase; this class only self-describes the tool.
 */
@RegisterSystemFunction
public final class SpeakFunction implements SystemFunction {

    public static final String ID = "speak";

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
                new ActionParameterSpec("text", "string", true,
                        "The exact words to speak to the commander.",
                        List.of(), null),
                new ActionParameterSpec("confirmation_request", "boolean", false,
                        "True only if this phrase requests confirmation of a dangerous action; otherwise omit.",
                        List.of(), null)
        );
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER, ThoughtSource.EVENT);
    }
}
