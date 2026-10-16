package elite.intel.ai.brain.vega;

import elite.intel.ai.brain.VegaIdentity;

import java.time.Duration;
import java.util.List;

/**
 * The single place for VEGA's settings. VEGA is the application's only command mode.
 */
public final class VegaConfig {

    // The name itself lives with the identity clause it appears in - see VegaIdentity.
    private static final String VEGA_NAME = VegaIdentity.name();

    // --- runtime tuning (provisional; TODO: back by GUI/DB settings) ---
    /**
     * Max read-only query handlers that may execute concurrently after their cognitive turns settle.
     */
    private static final int MAX_PARALLEL_QUERY_EXECUTIONS = 4;
    /**
     * How many function calls one commander turn may settle. Above one so a commander who asks two things in a
     * breath ("check the loadout, what is our cargo capacity") is answered rather than repaired down to half an
     * answer; two rather than more, because everything past the second call is far more often a model padding
     * its response than a commander asking for a third thing, and each extra call is another execution and
     * another spoken line.
     */
    private static final int MAX_COMMANDER_TOOL_CALLS = 2;
    /**
     * Hard lifecycle ceiling for a live thought.
     */
    private static final Duration THOUGHT_WATCHDOG_TIMEOUT = Duration.ofSeconds(60);
    /**
     * One deadline for queue wait plus all physical attempts of a logical LLM request.
     */
    private static final Duration LLM_LOGICAL_DEADLINE = Duration.ofSeconds(50);
    /**
     * Whether VEGA's internal-detail diagnostics (compose/LLM/exec/memory/lifecycle) are
     * promoted onto the always-visible SYSTEM LOG. On by default while the detailed-log GUI toggle for this
     * channel is still pending; set to {@code false} to keep only the per-turn headlines.
     */
    private static final boolean DIAGNOSTICS_VERBOSE = true;

    private VegaConfig() {
    }

    /**
     * VEGA's own name, woven into its persona prompt.
     */
    public static String vegaName() {
        return VEGA_NAME;
    }

    /**
     * The name forms recognized as a leading vocative on commander INPUT (the reflex vocative strip). Every
     * language we ship writes the name the same way, so this is the canonical name alone.
     * <p>
     * TODO: when the name becomes GUI/DB-configurable, its spoken variants must follow the configured value.
     */
    public static List<String> vegaNameForms() {
        return List.of(VEGA_NAME);
    }

    /**
     * Max read-only query handlers that may execute concurrently.
     */
    public static int maxParallelQueryExecutions() {
        return MAX_PARALLEL_QUERY_EXECUTIONS;
    }

    /**
     * Max function calls one commander turn settles; a longer response is repaired down to this.
     */
    public static int maxCommanderToolCalls() {
        return MAX_COMMANDER_TOOL_CALLS;
    }

    /**
     * Hard lifecycle ceiling for a live thought.
     */
    public static Duration thoughtWatchdogTimeout() {
        return THOUGHT_WATCHDOG_TIMEOUT;
    }

    /**
     * Total LLM request deadline, deliberately shorter than {@link #thoughtWatchdogTimeout()}.
     */
    public static Duration llmLogicalDeadline() {
        return LLM_LOGICAL_DEADLINE;
    }

    /**
     * Whether VEGA's internal-detail diagnostics are promoted onto the always-visible SYSTEM LOG (default on).
     */
    public static boolean diagnosticsVerbose() {
        return DIAGNOSTICS_VERBOSE;
    }

}
