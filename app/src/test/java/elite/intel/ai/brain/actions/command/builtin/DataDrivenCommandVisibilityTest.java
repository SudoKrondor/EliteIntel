package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These three commands are data-driven (remembered landing coordinates, saved codex entries, the held carrier
 * route) and infer no parameters from the commander, so they must be offered in any normal control mode rather
 * than gated on a narrow game situation (surface lat/long, galaxy-map focus). Regression guard for the
 * over-gating that hid them from the companion router.
 */
class DataDrivenCommandVisibilityTest {

    // StatusFlags bit values (mirror elite.intel.session.StatusFlags).
    private static final long IN_MAIN_SHIP = 16_777_216L;
    private static final long IN_SRV = 67_108_864L;
    private static final long ON_FOOT = 1L; // flags2

    private final long savedFlags = Status.getInstance().getStatus().getFlags();
    private final long savedFlags2 = Status.getInstance().getStatus().getFlags2();

    @AfterEach
    void restoreStatus() {
        setStatus(savedFlags, savedFlags2);
    }

    private static void setStatus(long flags, long flags2) {
        GameEvents.StatusEvent snapshot = Status.getInstance().getStatus();
        snapshot.setFlags(flags);
        snapshot.setFlags2(flags2);
        Status.getInstance().setStatus(snapshot);
    }

    private void assertVisibleInEveryControlMode(elite.intel.ai.brain.actions.IntelAction command) {
        Status status = Status.getInstance();
        setStatus(IN_MAIN_SHIP, 0L);
        assertTrue(command.isVisibleForLLM(status), command.id() + " must be offered in the main ship");
        setStatus(IN_SRV, 0L);
        assertTrue(command.isVisibleForLLM(status), command.id() + " must be offered in the SRV");
        setStatus(0L, ON_FOOT);
        assertTrue(command.isVisibleForLLM(status), command.id() + " must be offered on foot");
    }

    @Test
    void landingZoneNavigationIsOfferedEverywhere() {
        assertVisibleInEveryControlMode(new NavigateToLandingZoneCommand());
    }

    @Test
    void codexEntryNavigationIsOfferedEverywhere() {
        assertVisibleInEveryControlMode(new NavigateToBioSampleCodexEntryCommand());
    }

    @Test
    void enterCarrierDestinationIsOfferedEverywhere() {
        assertVisibleInEveryControlMode(new EnterFleetCarrierDestinationCommand());
    }
}
