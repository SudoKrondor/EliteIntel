package elite.intel.companion.tools;

import com.google.gson.JsonObject;
import elite.intel.companion.model.ThoughtSource;

import java.util.EnumSet;
import java.util.Set;

/**
 * System function: end the current turn because there is nothing (more) to do. The explicit terminal
 * of the tool-calling-only loop, so a deliberately empty turn is distinguishable from an empty/invalid
 * LLM response. Note this is not "stay silent": not speaking is simply omitting the speak call, and a
 * turn may act without speaking. Available to both sources; no parameters. Execution is wired in a
 * later phase.
 */
@RegisterSystemFunction
public final class NothingToDoFunction implements SystemFunction {

    public static final String ID = "nothing_to_do";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String descriptionKey() {
        return ID;
    }

    @Override
    public Set<ThoughtSource> sources() {
        return EnumSet.of(ThoughtSource.COMMANDER, ThoughtSource.EVENT);
    }

    /**
     * No-op: ends the turn. {@code nothing_to_do} is a terminator signal owned by the {@code Thought}
     * (it is not routed to the execution gateway and produces no result to return or record).
     */
    @Override
    public JsonObject handle(String action, JsonObject params, String text) {
        return null;
    }
}
