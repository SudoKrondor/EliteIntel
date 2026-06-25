package elite.intel.ai.hands;

import elite.intel.ai.hands.BindingConflictScanner.CandidateConflict;
import elite.intel.ai.hands.BindingConflictScanner.Conflict;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Encodes Elite's EXACT-chord matching: a binding fires only when its precise key-set is held, so
 * two bindings conflict only when their chords are identical (same context). Bare and modified
 * variants of a key coexist and never conflict.
 */
class BindingConflictScannerTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Set<String>> bindings(Object... pairs) {
        Map<String, Set<String>> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((String) pairs[i], (Set<String>) pairs[i + 1]);
        }
        return m;
    }

    @Test
    void bareKeyAndModifiedChordOnSameKeyDoNotConflict() {
        // The corrected model: bare Key_Y and Ctrl+Shift+Alt+Y are distinct chords, both fire.
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "HeadLookReset", Set.of("Key_Y"),
                "GalaxyMapOpen", Set.of("Key_LeftControl", "Key_LeftShift", "Key_LeftAlt", "Key_Y")));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void subsetModifiersDoNotConflict() {
        // Ctrl+Y vs Ctrl+Shift+Y: different exact chords, no conflict.
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "ActionOne", Set.of("Key_LeftControl", "Key_Y"),
                "ActionTwo", Set.of("Key_LeftControl", "Key_LeftShift", "Key_Y")));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void identicalChordInSameContextConflicts() {
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "ActionOne", Set.of("Key_LeftControl", "Key_Y"),
                "ActionTwo", Set.of("Key_LeftControl", "Key_Y")));

        assertEquals(1, conflicts.size());
        Conflict c = conflicts.get(0);
        assertEquals("ActionOne", c.actionA()); // ordered A < B
        assertEquals("ActionTwo", c.actionB());
        assertNotNull(c.description());
    }

    @Test
    void slotOrderDoesNotMatterChordIsAKeySet() {
        // Same two keys, primary/modifier roles swapped -> identical chord -> conflict.
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "ActionOne", Set.of("Key_Y", "Key_LeftControl"),
                "ActionTwo", Set.of("Key_LeftControl", "Key_Y")));

        assertEquals(1, conflicts.size());
    }

    @Test
    void identicalChordInDifferentVehicleStatesNeverConflicts() {
        // Same chord, but one is the SRV (_Buggy) variant: mutually exclusive context.
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "ToggleCargoScoop", Set.of("Key_Y"),
                "ToggleCargoScoop_Buggy", Set.of("Key_Y")));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void identicalChordInACameraSubModeNotFlaggedAgainstShip() {
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "FreeCamZoomIn", Set.of("Key_Y"),
                "GalaxyMapOpen", Set.of("Key_Y")));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void uiNavigationNeverConflictsWithShipAction() {
        // The UI panel is its own input context - ship controls are disabled while it is open, so a
        // shared key cannot fire both. CycleNextSubsystem (ship) vs UI_Right (UI) is safe.
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "CycleNextSubsystem", Set.of("Key_RightArrow"),
                "UI_Right", Set.of("Key_RightArrow")));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void twoUiActionsOnTheSameChordStillConflict() {
        // UI is a context, not a blanket sub-state: two panel actions on one chord do collide.
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "UI_Up", Set.of("Key_W"),
                "UI_Down", Set.of("Key_W")));

        assertEquals(1, conflicts.size());
    }

    @Test
    void constructionPanelNeverConflictsWithShipAction() {
        // The construction/colonisation panel is a separate UI panel, mutually exclusive with flight.
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "ChangeConstructionOption", Set.of("Key_J"),
                "Hyperspace", Set.of("Key_J")));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void radialWheelNeverConflictsWithOtherHumanoidAction() {
        // While a radial wheel is shown the game blocks every other control, so the wheel cannot
        // co-fire with another on-foot action even though both are in the humanoid context.
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "HumanoidItemWheelButton", Set.of("Key_G"),
                "HumanoidPrimaryInteractButton", Set.of("Key_G")));

        assertTrue(conflicts.isEmpty());
    }

    // --- candidateConflict: vetting a single chord before it is saved ---

    @Test
    void candidateConflictsOnlyForAnIdenticalChord() {
        Map<String, Set<String>> existing = bindings("LandingGearToggle", Set.of("Key_LeftControl", "Key_Y"));
        CandidateConflict conflict = BindingConflictScanner.candidateConflict(
                "GalaxyMapOpen", Set.of("Key_LeftControl", "Key_Y"), existing);
        assertNotNull(conflict);
        assertEquals("LandingGearToggle", conflict.otherBinding());
    }

    @Test
    void candidateBareChordIsCleanWhenOnlyModifiedVariantsExist() {
        // bare Y is free even though Ctrl+Shift+Alt+Y is taken (different chord).
        Map<String, Set<String>> existing = bindings(
                "PitchDownButton", Set.of("Key_LeftControl", "Key_LeftAlt", "Key_Y"));
        CandidateConflict conflict = BindingConflictScanner.candidateConflict(
                "GalaxyMapOpen", Set.of("Key_Y"), existing);
        assertNull(conflict);
    }

    @Test
    void candidateNeverConflictsWithItsOwnOtherSlot() {
        Map<String, Set<String>> existing = bindings("GalaxyMapOpen", Set.of("Key_Y"));
        CandidateConflict conflict = BindingConflictScanner.candidateConflict(
                "GalaxyMapOpen", Set.of("Key_Y"), existing);
        assertNull(conflict);
    }

    @Test
    void candidateCleanWhenNothingMatches() {
        Map<String, Set<String>> existing = bindings("SomethingElse", Set.of("Key_LeftShift", "Key_T"));
        CandidateConflict conflict = BindingConflictScanner.candidateConflict(
                "GalaxyMapOpen", Set.of("Key_LeftControl", "Key_Y"), existing);
        assertNull(conflict);
    }
}
