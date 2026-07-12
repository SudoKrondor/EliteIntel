package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression guard for the normal-space visibility contract of the docking-computer taxi command. */
class TaxiToLandingPadCommandVisibilityTest {

    private static final long DOCKED = 1L;
    private static final long LANDED = 2L;
    private static final long SUPERCRUISE = 16L;
    private static final long IN_MAIN_SHIP = 16_777_216L;
    private static final long IN_SRV = 67_108_864L;
    private static final long ON_FOOT = 1L;

    private final long savedFlags = Status.getInstance().getStatus().getFlags();
    private final long savedFlags2 = Status.getInstance().getStatus().getFlags2();
    private final TaxiToLandingPadCommand command = new TaxiToLandingPadCommand();

    @AfterEach
    void restoreStatus() {
        setStatus(savedFlags, savedFlags2);
    }

    @Test
    void isVisibleOnlyForMainShipInNormalSpace() {
        setStatus(IN_MAIN_SHIP, 0L);
        assertTrue(command.isVisibleForLLM(Status.getInstance()));

        setStatus(IN_MAIN_SHIP | SUPERCRUISE, 0L);
        assertFalse(command.isVisibleForLLM(Status.getInstance()));

        setStatus(IN_MAIN_SHIP | DOCKED, 0L);
        assertFalse(command.isVisibleForLLM(Status.getInstance()));

        setStatus(IN_MAIN_SHIP | LANDED, 0L);
        assertFalse(command.isVisibleForLLM(Status.getInstance()));

        setStatus(IN_SRV, 0L);
        assertFalse(command.isVisibleForLLM(Status.getInstance()));

        setStatus(0L, ON_FOOT);
        assertFalse(command.isVisibleForLLM(Status.getInstance()));
    }

    private static void setStatus(long flags, long flags2) {
        GameEvents.StatusEvent snapshot = Status.getInstance().getStatus();
        snapshot.setFlags(flags);
        snapshot.setFlags2(flags2);
        Status.getInstance().setStatus(snapshot);
    }
}
