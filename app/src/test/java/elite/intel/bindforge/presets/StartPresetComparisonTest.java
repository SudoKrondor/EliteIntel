package elite.intel.bindforge.presets;

import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.presets.StartPresetComparison.Change;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The per-line three-way compare: four independent lines, and a conflict only where both sides moved one line to
 * different values.
 */
class StartPresetComparisonTest {

    private static final StartPresetSections MASTER = four("KeyboardMouseOnly", "Custom", "Custom", "Custom");

    @Test
    void allThreeTheSameIsUnchanged() {
        StartPresetComparison comparison = StartPresetComparison.of(MASTER, MASTER, MASTER);

        assertTrue(comparison.allUnchanged());
        assertFalse(comparison.gameChanged());
        assertTrue(comparison.conflicts().isEmpty());
    }

    @Test
    void aDraftStillOnTheMasterLeavesOnlyTheGamesChange() {
        StartPresetComparison comparison =
                StartPresetComparison.of(MASTER, MASTER, four("KeyboardMouseOnly", "Other", "Custom", "Custom"));

        assertEquals(Change.GAME_ONLY, comparison.line(BindingSection.SHIP).change());
        assertEquals("Custom", comparison.line(BindingSection.SHIP).draft());
        assertEquals(Change.UNCHANGED, comparison.line(BindingSection.GENERAL).change());
    }

    @Test
    void aGameChangeAndADraftChangeOnDifferentLinesDoNotConflict() {
        StartPresetComparison comparison = StartPresetComparison.of(MASTER,
                four("KeyboardMouseOnly", "Custom", "Rover", "Custom"),
                four("KeyboardMouseOnly", "Ship2", "Custom", "Custom"));

        assertEquals(changes(Change.UNCHANGED, Change.GAME_ONLY, Change.DRAFT_ONLY, Change.UNCHANGED), changesOf(comparison));
        assertTrue(comparison.conflicts().isEmpty());
        assertTrue(comparison.gameChanged());
    }

    @Test
    void bothMovingOneLineToTheSameValueIsNotAConflict() {
        StartPresetSections both = four("Custom", "Custom", "Custom", "Custom");

        StartPresetComparison comparison = StartPresetComparison.of(MASTER, both, both);

        assertEquals(Change.BOTH_SAME, comparison.line(BindingSection.GENERAL).change());
        assertTrue(comparison.conflicts().isEmpty());
        assertFalse(comparison.allUnchanged());
    }

    @Test
    void bothMovingOneLineToDifferentValuesIsAConflict() {
        StartPresetComparison comparison = StartPresetComparison.of(MASTER,
                four("KeyboardMouseOnly", "Custom", "Custom", "Feet"),
                four("KeyboardMouseOnly", "Custom", "Custom", "Walk"));

        List<StartPresetComparison.Line> conflicts = comparison.conflicts();
        assertEquals(1, conflicts.size());
        StartPresetComparison.Line line = conflicts.getFirst();
        assertEquals(BindingSection.ON_FOOT, line.section());
        assertEquals("Custom", line.master());
        assertEquals("Feet", line.draft());
        assertEquals("Walk", line.game());
    }

    @Test
    void singleLineFormEqualsFourIdenticalLines() {
        StartPresetSections single = new StartPresetSections("Custom", "Custom", "Custom", "Custom", true);

        assertTrue(StartPresetComparison.of(single, single, four("Custom", "Custom", "Custom", "Custom"))
                .allUnchanged());
    }

    @Test
    void aLineEmptiedInTheGameIsAChange() {
        StartPresetComparison comparison =
                StartPresetComparison.of(MASTER, MASTER, four("KeyboardMouseOnly", "", "", ""));

        assertEquals(changes(Change.UNCHANGED, Change.GAME_ONLY, Change.GAME_ONLY, Change.GAME_ONLY), changesOf(comparison));
    }

    @Test
    void linesAreInFileOrder() {
        StartPresetComparison comparison = StartPresetComparison.of(MASTER, MASTER, MASTER);

        assertEquals(List.of(BindingSection.GENERAL, BindingSection.SHIP, BindingSection.SRV, BindingSection.ON_FOOT),
                comparison.lines().stream().map(StartPresetComparison.Line::section).toList());
        assertThrows(IllegalArgumentException.class, () -> comparison.line(BindingSection.OTHER));
    }

    private static StartPresetSections four(String general, String ship, String srv, String onFoot) {
        return new StartPresetSections(general, ship, srv, onFoot, false);
    }

    private static List<Change> changes(Change... changes) {
        return List.of(changes);
    }

    private static List<Change> changesOf(StartPresetComparison comparison) {
        return comparison.lines().stream().map(StartPresetComparison.Line::change).toList();
    }
}
