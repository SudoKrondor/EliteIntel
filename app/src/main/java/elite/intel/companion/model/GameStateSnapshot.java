package elite.intel.companion.model;

import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.Status;

import java.util.Objects;

/**
 * Immutable visibility inputs captured once for a commander turn. Exact reflex and reduction derive their candidate
 * sets from this same snapshot, so a live status update cannot split one turn across incompatible game contexts.
 * This is a routing snapshot only; execution is not revalidated against live state.
 */
public record GameStateSnapshot(long shipStatusFlags, long playerContextFlags, boolean isFighterOut) {

    /** Captures the current process-wide game state in one status-row read. */
    public static GameStateSnapshot capture() {
        return capture(Status.getInstance());
    }

    /** Captures the visibility fields from the supplied status source. */
    public static GameStateSnapshot capture(Status source) {
        Status required = Objects.requireNonNull(source, "source");
        GameEvents.StatusEvent status = Objects.requireNonNull(required.getStatus(), "source status");
        return new GameStateSnapshot(status.getFlags(), status.getFlags2(), required.isFighterOut());
    }

    /** Builds a detached status view for existing {@code IntelAction.isVisibleForLLM} predicates. */
    public Status visibilityStatus() {
        Status status = Status.detached(shipStatusFlags, playerContextFlags);
        status.setFighterOut(isFighterOut);
        return status;
    }
}
