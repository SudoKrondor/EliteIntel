package elite.intel.diagnostics;

import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.query.QueryRegistry;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.Status;

/**
 * Applies a game context to {@link Status} so state-gated commands become visible to the router in
 * diagnostics mode. The companion path enforces {@code isVisibleForLLM}, which reads the live {@link Status}
 * flags, so a command such as "drop from supercruise" only routes when the supercruise flag is set. Two ways
 * to set it, both mirroring {@code CompanionRoutingHarness}:
 * <ul>
 *   <li>{@link #applyVisibleFor(String)} — the preferred, deterministic one: given the EXPECTED action id, it
 *       probes the same {@link #PROBE_STATES} the routing test does and picks the first context in which that
 *       action is visible. This is how the test suite sets "where we are"; the tester emits {@code @visible
 *       <id>} before each utterance and never has to guess a context.</li>
 *   <li>{@link #apply(String)} — a named context ({@code main_ship}, {@code supercruise}, ...) for manual use.</li>
 * </ul>
 * Flag bit values mirror {@code StatusFlags}.
 */
final class DiagnosticsContext {

    private static final long FLAG_DOCKED = 1L;
    private static final long FLAG_LANDED = 2L;
    private static final long FLAG_SUPERCRUISE = 16L;
    private static final long FLAG_IN_MAIN_SHIP = 16_777_216L;
    private static final long FLAG_IN_SRV = 67_108_864L;
    private static final long FLAG2_ON_FOOT = 1L;

    /**
     * Candidate {@code {flags, flags2}} contexts, tried in order (mirrors {@code CompanionRoutingHarness}):
     * main-ship-in-normal-space first so state-independent and main-ship commands take it, then the rarer
     * gated contexts.
     */
    private static final long[][] PROBE_STATES = {
            {FLAG_IN_MAIN_SHIP, 0L},
            {FLAG_IN_MAIN_SHIP | FLAG_SUPERCRUISE, 0L},
            {FLAG_IN_MAIN_SHIP | FLAG_DOCKED, 0L},
            {FLAG_IN_MAIN_SHIP | FLAG_LANDED, 0L},
            {FLAG_IN_SRV, 0L},
            {0L, FLAG2_ON_FOOT},
    };

    private DiagnosticsContext() {
    }

    /**
     * Sets the game context to the first {@link #PROBE_STATES probe state} in which {@code actionId} is visible
     * to the router (exactly as the routing test's {@code applyStateFor} does), so the expected command can be
     * offered. Falls back to main ship if the id is unknown or never visible. Returns the applied situation
     * label for logging, or {@code "unknown-action"} when the id resolves to no action.
     */
    static String applyVisibleFor(String actionId) {
        IntelAction action = lookup(actionId);
        Status status = Status.getInstance();
        GameEvents.StatusEvent snapshot = status.getStatus();
        status.setFighterOut(false);
        if (action == null) {
            applyFlags(status, snapshot, FLAG_IN_MAIN_SHIP, 0L);
            return "unknown-action";
        }
        for (long[] candidate : PROBE_STATES) {
            applyFlags(status, snapshot, candidate[0], candidate[1]);
            if (action.isVisibleForLLM(status)) {
                return label(candidate[0], candidate[1], false);
            }
        }
        // Fighter orders gate on isFighterOut (a Status boolean, not a flag): try with a fighter deployed.
        applyFlags(status, snapshot, FLAG_IN_MAIN_SHIP, 0L);
        status.setFighterOut(true);
        if (action.isVisibleForLLM(status)) {
            return label(FLAG_IN_MAIN_SHIP, 0L, true);
        }
        status.setFighterOut(false);
        applyFlags(status, snapshot, FLAG_IN_MAIN_SHIP, 0L); // fallback
        return "main_ship(fallback)";
    }

    /**
     * Applies a named context (one of {@code main_ship}, {@code supercruise}, {@code docked}, {@code landed},
     * {@code srv}, {@code on_foot}); returns {@code false} for an unknown name. A context switch also clears
     * the fighter-out gate.
     */
    static boolean apply(String name) {
        long flags;
        long flags2 = 0L;
        switch (name) {
            case "main_ship" -> flags = FLAG_IN_MAIN_SHIP;
            case "supercruise" -> flags = FLAG_IN_MAIN_SHIP | FLAG_SUPERCRUISE;
            case "docked" -> flags = FLAG_IN_MAIN_SHIP | FLAG_DOCKED;
            case "landed" -> flags = FLAG_IN_MAIN_SHIP | FLAG_LANDED;
            case "srv" -> flags = FLAG_IN_SRV;
            case "on_foot" -> {
                flags = 0L;
                flags2 = FLAG2_ON_FOOT;
            }
            default -> {
                return false;
            }
        }
        Status status = Status.getInstance();
        GameEvents.StatusEvent snapshot = status.getStatus();
        applyFlags(status, snapshot, flags, flags2);
        status.setFighterOut(false);
        return true;
    }

    /** Sets whether a fighter is deployed - a {@link Status} boolean (not a flag) that fighter-order commands gate on. */
    static void setFighterOut(boolean out) {
        Status.getInstance().setFighterOut(out);
    }

    private static void applyFlags(Status status, GameEvents.StatusEvent snapshot, long flags, long flags2) {
        snapshot.setFlags(flags);   // keep the other status fields, override only the flags
        snapshot.setFlags2(flags2);
        status.setStatus(snapshot);
    }

    private static IntelAction lookup(String actionId) {
        IntelAction action = CommandRegistry.getInstance().byId().get(actionId);
        return action != null ? action : QueryRegistry.getInstance().byId().get(actionId);
    }

    private static String label(long flags, long flags2, boolean fighter) {
        String base;
        if ((flags2 & FLAG2_ON_FOOT) != 0) base = "on_foot";
        else if ((flags & FLAG_IN_SRV) != 0) base = "srv";
        else if ((flags & FLAG_SUPERCRUISE) != 0) base = "supercruise";
        else if ((flags & FLAG_DOCKED) != 0) base = "docked";
        else if ((flags & FLAG_LANDED) != 0) base = "landed";
        else base = "main_ship";
        return fighter ? base + "+fighter" : base;
    }
}
