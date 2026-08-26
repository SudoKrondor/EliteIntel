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

    // --- map camera vs UI navigation: the one overlay where two families are live at once ---

    @Test
    void mapPanKeysConflictWithUiNavigationOnTheSameChord() {
        // Reported in the field: W/A/S/D bound to both map movement and panel navigation, so the
        // galaxy map would not pan. While the map is open both families fire off the same chord.
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "CamTranslateForward", Set.of("Key_W"),
                "UI_Up", Set.of("Key_W")));

        assertEquals(1, conflicts.size());
        assertTrue(conflicts.get(0).description().contains("map"));
    }

    @Test
    void everyMapCameraFamilyConflictsWithUiNavigation() {
        // One chord per family, so the assertion names which pairs collided rather than just counting -
        // a count alone would still pass if one family dropped out and another pair appeared.
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "CamTranslateLeft", Set.of("Key_A"),
                "UI_Left", Set.of("Key_A"),
                "CamPitchUp", Set.of("Key_W"),
                "UI_Up", Set.of("Key_W"),
                "CamYawRight", Set.of("Key_D"),
                "UI_Right", Set.of("Key_D"),
                "CamZoomIn", Set.of("Key_S"),
                "UI_Down", Set.of("Key_S"),
                "GalaxyMapHome", Set.of("Key_H"),
                "UI_Select", Set.of("Key_H")));

        assertEquals(
                List.of("CamPitchUp|UI_Up",
                        "CamTranslateLeft|UI_Left",
                        "CamYawRight|UI_Right",
                        "CamZoomIn|UI_Down",
                        "GalaxyMapHome|UI_Select"),
                conflicts.stream().map(c -> c.actionA() + "|" + c.actionB()).sorted().toList());
    }

    @Test
    void mapVersusUiConflictIsMarkedBlocking() {
        // Blocking = EliteIntel cannot drive the game at all, not "may interfere". RoutePlotter walks the
        // galaxy map to its search field with UI_Left/UI_Right/UI_Select; if those chords also pan the map,
        // focus never lands in the field and the system name is typed into nothing.
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "CamTranslateForward", Set.of("Key_W"),
                "UI_Up", Set.of("Key_W")));

        assertEquals(1, conflicts.size());
        assertTrue(conflicts.get(0).blocking());
    }

    @Test
    void ordinaryConflictsAreNotBlocking() {
        // Everything else stays "may interfere": announced once, not on every start.
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "UI_Up", Set.of("Key_W"),
                "UI_Down", Set.of("Key_W"),
                "DeployHardpointToggle", Set.of("Key_U"),
                "LandingGearToggle", Set.of("Key_U")));

        assertEquals(2, conflicts.size());
        assertTrue(conflicts.stream().noneMatch(Conflict::blocking));
    }

    @Test
    void theFieldReportedWasdLayoutIsBlockingOnAllFourAxes() {
        // Verbatim from the commander bundle of 2026-08-26: UI_* primaries on a gamepad with W/A/S/D added
        // as keyboard secondaries, on top of Frontier's own W/A/S/D map pan. Two plot attempts, every
        // keystroke reporting success, and no NavRoute event in the journal either time.
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "CamTranslateForward", Set.of("Key_W"),
                "CamTranslateBackward", Set.of("Key_S"),
                "CamTranslateLeft", Set.of("Key_A"),
                "CamTranslateRight", Set.of("Key_D"),
                "UI_Up", Set.of("Key_W"),
                "UI_Down", Set.of("Key_S"),
                "UI_Left", Set.of("Key_A"),
                "UI_Right", Set.of("Key_D")));

        assertEquals(4, conflicts.size());
        assertTrue(conflicts.stream().allMatch(Conflict::blocking));
    }

    @Test
    void separatingMapKeysFromUiKeysClearsTheBlockingConflict() {
        // The remedy is separation, not a particular layout: W/A/S/D for the map with the arrow keys for the
        // interface is clean, and so is the reverse. Only sharing the chords is not.
        assertTrue(BindingConflictScanner.scanKeysets(bindings(
                "CamTranslateForward", Set.of("Key_W"),
                "CamTranslateLeft", Set.of("Key_A"),
                "UI_Up", Set.of("Key_UpArrow"),
                "UI_Left", Set.of("Key_LeftArrow"))).isEmpty());

        assertTrue(BindingConflictScanner.scanKeysets(bindings(
                "CamTranslateForward", Set.of("Key_UpArrow"),
                "CamTranslateLeft", Set.of("Key_LeftArrow"),
                "UI_Up", Set.of("Key_W"),
                "UI_Left", Set.of("Key_A"))).isEmpty());
    }

    @Test
    void mapCameraStillDoesNotConflictWithAShipAction() {
        // Ship controls ARE disabled while the map is open - only the UI_* overlap is new.
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "CamTranslateForward", Set.of("Key_W"),
                "SetSpeed100", Set.of("Key_W")));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void unrelatedCameraFamiliesStillDoNotConflictWithUiNavigation() {
        // FreeCam / placement / store / vanity cameras cannot be open alongside a UI panel, so they
        // keep their sub-state exemption; only the map camera loses it.
        List<Conflict> conflicts = BindingConflictScanner.scanKeysets(bindings(
                "MoveFreeCamForward", Set.of("Key_W"),
                "MovePlacementCamForward", Set.of("Key_E"),
                "StoreCamZoomIn", Set.of("Key_R"),
                "PitchCameraUp", Set.of("Key_T"),
                "UI_Up", Set.of("Key_W"),
                "UI_Down", Set.of("Key_E"),
                "UI_Left", Set.of("Key_R"),
                "UI_Right", Set.of("Key_T")));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void mapCameraCandidateChordIsRejectedAgainstUiNavigation() {
        // The editor save-guard and live keyboard widget see it too.
        Map<String, Set<String>> existing = bindings("UI_Up", Set.of("Key_W"));
        CandidateConflict conflict = BindingConflictScanner.candidateConflict(
                "CamTranslateForward", Set.of("Key_W"), existing);

        assertNotNull(conflict);
        assertEquals("UI_Up", conflict.otherBinding());
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

    // --- recommendVehicleTwins: nudging ship/SRV twins onto the same key ---

    @Test
    void twinsOnDifferentKeysAreRecommendedToUnify() {
        List<BindingConflictScanner.Recommendation> recs = BindingConflictScanner.recommendVehicleTwinsKeysets(bindings(
                "HeadLookToggle", Set.of("Key_O"),
                "HeadLookToggle_Buggy", Set.of("Key_P")));

        assertEquals(1, recs.size());
        assertEquals("HeadLookToggle", recs.get(0).shipAction());
        assertEquals("HeadLookToggle_Buggy", recs.get(0).buggyAction());
    }

    @Test
    void twinsOnTheSameKeyAreNotRecommended() {
        // Already unified - nothing to nudge. (And the scanner never flags it as a conflict either.)
        List<BindingConflictScanner.Recommendation> recs = BindingConflictScanner.recommendVehicleTwinsKeysets(bindings(
                "HeadLookToggle", Set.of("Key_O"),
                "HeadLookToggle_Buggy", Set.of("Key_O")));

        assertTrue(recs.isEmpty());
    }

    @Test
    void unboundTwinIsNotRecommended() {
        // Only the SRV variant is bound; the ship twin's absence is a missing-binding concern.
        List<BindingConflictScanner.Recommendation> recs = BindingConflictScanner.recommendVehicleTwinsKeysets(bindings(
                "HeadLookToggle_Buggy", Set.of("Key_P")));

        assertTrue(recs.isEmpty());
    }

    @Test
    void nonTwinActionsAreNeverRecommended() {
        List<BindingConflictScanner.Recommendation> recs = BindingConflictScanner.recommendVehicleTwinsKeysets(bindings(
                "GalaxyMapOpen", Set.of("Key_O"),
                "SystemMapOpen", Set.of("Key_P")));

        assertTrue(recs.isEmpty());
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
