package elite.intel.gameapi;

import elite.intel.gameapi.SurfaceVehicleDeployment.Decision;
import elite.intel.gameapi.SurfaceVehicleDeployment.Refusal;
import elite.intel.gameapi.SurfaceVehicleDeployment.ShipSituation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static elite.intel.gameapi.SurfaceVehicle.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Which bay opens, and when it does not.
 *
 * <p>The decision has four inputs that cannot be produced in a running game on demand - a hangar that is
 * or is not fitted, bays the commander has or has not configured, a ship landed or hovering, and an
 * altitude - so the whole table is exercised here rather than by flying to a planet and trying it.
 */
class SurfaceVehicleDeploymentTest {

    private static final ShipSituation LANDED_ON_SURFACE = new ShipSituation(true);
    private static final ShipSituation HOVERING = new ShipSituation(false);

    @Test
    void aShipWithNoHangarIsToldSoRatherThanHavingKeysPressedAtIt() {
        Decision decision = SurfaceVehicleDeployment.decide(
                false, List.of(SCARAB), null, null, LANDED_ON_SURFACE);

        assertFalse(decision.isAllowed());
        assertEquals(Refusal.NO_VEHICLE_BAY, decision.refusal());
    }

    @Test
    void baysNobodyHasConfiguredAreReportedRatherThanGuessed() {
        // The commander has a hangar and has never opened the settings. Deploying bay 1 on the assumption
        // it holds a Scarab is exactly the guess that gets a Rhino dropped from a landed ship.
        Decision decision = SurfaceVehicleDeployment.decide(
                true, bays(null, null, null, null), null, null, LANDED_ON_SURFACE);

        assertFalse(decision.isAllowed());
        assertEquals(Refusal.BAYS_NOT_CONFIGURED, decision.refusal());
    }

    @Test
    void anUnconfiguredHangarIsReportedBeforeANonsenseBayNumber() {
        // Order matters: a commander who has set nothing up needs to hear that, not that bay 9 is invalid,
        // which would send them looking for the wrong problem.
        Decision decision = SurfaceVehicleDeployment.decide(
                true, bays(null, null, null, null), 9, null, LANDED_ON_SURFACE);

        assertEquals(Refusal.BAYS_NOT_CONFIGURED, decision.refusal());
    }

    @Test
    void sayingNoBayDeploysTheFirstOne() {
        Decision decision = SurfaceVehicleDeployment.decide(
                true, bays(SCARAB, SCORPION, null, null), null, null, LANDED_ON_SURFACE);

        assertTrue(decision.isAllowed());
        assertEquals(1, decision.bay());
        assertEquals(SCARAB, decision.vehicle());
    }

    @Test
    void namingABayDeploysThatOne() {
        // The whole point of the change: a multi-bay hangar used to open the top bay whatever was asked for.
        Decision decision = SurfaceVehicleDeployment.decide(
                true, bays(SCARAB, SCORPION, null, null), 2, null, LANDED_ON_SURFACE);

        assertTrue(decision.isAllowed());
        assertEquals(2, decision.bay());
        assertEquals(SCORPION, decision.vehicle());
    }

    @Test
    void aBayNumberNoHangarHasIsQuotedBackRatherThanClamped() {
        // Mishearing is the likely cause, and quietly deploying bay 1 would open the wrong bay while
        // sounding like it understood.
        Decision decision = SurfaceVehicleDeployment.decide(
                true, bays(SCARAB, null, null, null), 7, null, LANDED_ON_SURFACE);

        assertFalse(decision.isAllowed());
        assertEquals(Refusal.NO_SUCH_BAY, decision.refusal());
        assertEquals(7, decision.requestedBay(), "the number heard has to be quotable back to the commander");
    }

    @Test
    void aBayNumberBelowOneIsAlsoRefused() {
        Decision decision = SurfaceVehicleDeployment.decide(
                true, bays(SCARAB, null, null, null), 0, null, LANDED_ON_SURFACE);

        assertEquals(Refusal.NO_SUCH_BAY, decision.refusal());
    }

    @Test
    void aBayThatExistsButWasLeftEmptyIsItsOwnAnswer() {
        Decision decision = SurfaceVehicleDeployment.decide(
                true, bays(SCARAB, null, null, null), 3, null, LANDED_ON_SURFACE);

        assertEquals(Refusal.BAY_EMPTY, decision.refusal());
        assertEquals(3, decision.requestedBay());
    }

    @Test
    void aShorterBayListReadsAsTheRestBeingEmpty() {
        Decision decision = SurfaceVehicleDeployment.decide(
                true, List.of(SCARAB), 2, null, LANDED_ON_SURFACE);

        assertEquals(Refusal.BAY_EMPTY, decision.refusal());
    }

    @Test
    void aBuggyNeedsTheShipOnTheGround() {
        Decision decision = SurfaceVehicleDeployment.decide(
                true, bays(SCARAB, null, null, null), 1, null, HOVERING);

        assertEquals(Refusal.NOT_LANDED, decision.refusal());
    }

    @Test
    void bothBuggiesDeployFromALandedShip() {
        for (SurfaceVehicle buggy : List.of(SCARAB, SCORPION)) {
            Decision decision = SurfaceVehicleDeployment.decide(
                    true, bays(buggy, null, null, null), 1, null, LANDED_ON_SURFACE);

            assertTrue(decision.isAllowed(), buggy + " should deploy from a landed ship");
        }
    }

    /**
     * The Rhino is not gated on the ship's state at all. The altitude band it drops from was measured, and
     * the reading this decision saw disagreed with the commander at the controls often enough that the
     * refusal fired on nearly every deployment made from inside the band - so the check came back out and
     * the game is left to refuse a drop it cannot make.
     */
    @Test
    void theRhinoIsNeverRefusedForTheShipsState() {
        for (ShipSituation situation : List.of(LANDED_ON_SURFACE, HOVERING)) {
            Decision decision = SurfaceVehicleDeployment.decide(
                    true, bays(RHINO, null, null, null), 1, null, situation);

            assertTrue(decision.isAllowed(), "the Rhino should deploy from " + situation);
            assertEquals(RHINO, decision.vehicle());
        }
    }

    @Test
    void aRhinoInAHigherBayIsStillTheBayThatOpens() {
        // The vehicle governs the condition, not the bay number - and the Rhino's condition is now none.
        Decision decision = SurfaceVehicleDeployment.decide(
                true, bays(SCARAB, SCARAB, RHINO, null), 3, null, LANDED_ON_SURFACE);

        assertTrue(decision.isAllowed());
        assertEquals(3, decision.bay());
        assertEquals(RHINO, decision.vehicle());
    }

    /**
     * The buggies keep their gate: it reads the landed flag, which is the one thing about the ship's state
     * that has never been in doubt.
     */
    @Test
    void theBuggiesStillNeedTheGround() {
        for (SurfaceVehicle buggy : List.of(SCARAB, SCORPION)) {
            Decision decision = SurfaceVehicleDeployment.decide(
                    true, bays(buggy, null, null, null), 1, null, HOVERING);

            assertEquals(Refusal.NOT_LANDED, decision.refusal(), buggy + " needs the ship on the ground");
        }
    }

    // ---- naming the vehicle instead of the bay ------------------------------------------------

    @Test
    void namingAVehicleFindsTheBayHoldingIt() {
        // What a commander actually says. They know they want the Scorpion; they do not know, and should
        // not have to, that the Scorpion lives in bay 2.
        Decision decision = SurfaceVehicleDeployment.decide(
                true, bays(SCARAB, SCORPION, null, null), null, SCORPION, LANDED_ON_SURFACE);

        assertTrue(decision.isAllowed());
        assertEquals(2, decision.bay());
        assertEquals(SCORPION, decision.vehicle());
    }

    @Test
    void aVehicleInNoBayIsSaidSoRatherThanDeployingSomethingElse() {
        Decision decision = SurfaceVehicleDeployment.decide(
                true, bays(SCARAB, SCARAB, null, null), null, RHINO, LANDED_ON_SURFACE);

        assertFalse(decision.isAllowed());
        assertEquals(Refusal.VEHICLE_NOT_LOADED, decision.refusal());
        assertEquals(RHINO, decision.requestedVehicle(), "the answer has to name what they asked for");
    }

    @Test
    void twoOfTheSameVehicleDeploysTheLowestBay() {
        // Either would satisfy the request, so the choice is made deterministically - and lands on the
        // one they would have got by saying nothing at all.
        Decision decision = SurfaceVehicleDeployment.decide(
                true, bays(null, SCARAB, null, SCARAB), null, SCARAB, LANDED_ON_SURFACE);

        assertEquals(2, decision.bay());
    }

    @Test
    void aVehicleNamedByItselfIsStillStateGated() {
        // Looking the bay up does not exempt it from the rule that governs that vehicle - and the Rhino,
        // which has no rule left, is found and deployed from wherever the ship happens to be.
        Decision scarabHovering = SurfaceVehicleDeployment.decide(
                true, bays(SCARAB, RHINO, null, null), null, SCARAB, HOVERING);
        assertEquals(Refusal.NOT_LANDED, scarabHovering.refusal());

        Decision rhinoOnTheGround = SurfaceVehicleDeployment.decide(
                true, bays(SCARAB, RHINO, null, null), null, RHINO, LANDED_ON_SURFACE);
        assertTrue(rhinoOnTheGround.isAllowed());
        assertEquals(2, rhinoOnTheGround.bay());
    }

    @Test
    void namingABayAndAVehicleThatDisagreeReportsWhatIsActuallyInThere() {
        // "Deploy the Scorpion from bay 1" with a Scarab in bay 1. Deploying the Scarab would be the wrong
        // vehicle out of the right hole; the bay number alone cannot tell them what went wrong.
        Decision decision = SurfaceVehicleDeployment.decide(
                true, bays(SCARAB, SCORPION, null, null), 1, SCORPION, LANDED_ON_SURFACE);

        assertFalse(decision.isAllowed());
        assertEquals(Refusal.BAY_HOLDS_OTHER, decision.refusal());
        assertEquals(1, decision.requestedBay());
        assertEquals(SCARAB, decision.vehicle(), "what is really in the bay");
        assertEquals(SCORPION, decision.requestedVehicle(), "what they asked for");
    }

    @Test
    void namingABayAndTheVehicleThatIsInItJustWorks() {
        Decision decision = SurfaceVehicleDeployment.decide(
                true, bays(SCARAB, SCORPION, null, null), 2, SCORPION, LANDED_ON_SURFACE);

        assertTrue(decision.isAllowed());
        assertEquals(2, decision.bay());
    }

    @Test
    void anUnconfiguredHangarIsReportedBeforeAVehicleLookup() {
        // Same ordering rule as for bay numbers: the setup problem is the one to report first.
        Decision decision = SurfaceVehicleDeployment.decide(
                true, bays(null, null, null, null), null, SCARAB, LANDED_ON_SURFACE);

        assertEquals(Refusal.BAYS_NOT_CONFIGURED, decision.refusal());
    }

    private static List<SurfaceVehicle> bays(SurfaceVehicle... bays) {
        return Arrays.asList(bays);
    }
}
