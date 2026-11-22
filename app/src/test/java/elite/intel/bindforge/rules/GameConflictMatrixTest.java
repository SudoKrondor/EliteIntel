package elite.intel.bindforge.rules;

import elite.intel.bindforge.model.BindingDisplayNames;
import elite.intel.bindforge.model.BindingSection;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Holds {@link GameConflictMatrix} to the table of in-game results it copies, and proves that the matrix adds conflicts
 * without taking any of our own rules away.
 */
class GameConflictMatrixTest {

    private static final Path MATRIX =
            Path.of("../docs/02-features/bindforge/domain-knowledge/EliteDangerous-ConflictMatrix.md");
    private static final Map<String, BindingSection> SECTIONS = Map.of(
            "General Controls", BindingSection.GENERAL,
            "Ship Controls", BindingSection.SHIP,
            "SRV Controls", BindingSection.SRV,
            "On Foot Controls", BindingSection.ON_FOOT);

    private record Cell(BindingSection section, String row, String column, boolean warns) {
    }

    private static List<String> cells(String line) {
        return Arrays.stream(line.strip().replaceAll("^\\||\\|$", "").split("\\|")).map(String::strip).toList();
    }

    private static List<Cell> table() throws IOException {
        List<String> lines = Files.readAllLines(MATRIX);
        List<Cell> table = new ArrayList<>();
        BindingSection section = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("## ")) section = SECTIONS.get(line.substring(3).strip());
            if (!line.startsWith("| |")) continue;
            List<String> columns = cells(line);
            for (int r = i + 2; lines.get(r).startsWith("|"); r++) {
                List<String> row = cells(lines.get(r));
                String rowGroup = row.getFirst().replace("*", "");
                for (int c = 1; c < row.size(); c++) {
                    if (!row.get(c).equals("—")) {
                        table.add(new Cell(section, rowGroup, columns.get(c), row.get(c).contains("YES")));
                    }
                }
            }
        }
        return table;
    }

    @Test
    void agreesWithEveryCellOfTheInGameResults() throws IOException {
        List<Cell> table = table();
        assertFalse(table.isEmpty());
        for (Cell cell : table) {
            assertEquals(cell.warns(), GameConflictMatrix.warns(cell.section(), cell.row(), cell.column()), cell.toString());
        }
        assertEquals(95, table.stream().filter(Cell::warns).count() / 2);
    }

    /**
     * A group spelled differently from the control name table would never match a control, and nothing would say so.
     */
    @Test
    void everyGroupThatWarnsExistsInTheControlNameTable() throws IOException {
        Set<String> named = BindingDisplayNames.all().values().stream()
                .map(control -> control.section() + "|" + control.group())
                .collect(Collectors.toSet());
        for (Cell cell : table()) {
            if (cell.warns()) assertTrue(named.contains(cell.section() + "|" + cell.row()), cell.row());
        }
    }

    @Test
    void addsWhatOurRulesCleared() {
        // Different screens to our rules, one warning to the game.
        assertFalse(BindingConflictRules.isSafeOverlap("ExplorationSAAExitThirdPerson", "PrimaryFire"));
        assertFalse(BindingConflictRules.isSafeOverlap("UI_Up", "GalnetAudio_Play_Pause"));
        assertFalse(BindingConflictRules.isSafeOverlap("BuggyTurretPitchUpButton", "GalaxyMapOpen_Buggy"));
    }

    @Test
    void aPairTheGameLetsThroughKeepsOurRule() {
        // Both no in the matrix. Our rules report the first and clear the second, and neither changes.
        assertFalse(GameConflictMatrix.warns("CamTranslateForward", "UI_Up"));
        assertFalse(BindingConflictRules.isSafeOverlap("CamTranslateForward", "UI_Up"));
        assertFalse(GameConflictMatrix.warns("ExplorationFSSZoomIn", "PrimaryFire"));
        assertTrue(BindingConflictRules.isSafeOverlap("ExplorationFSSZoomIn", "PrimaryFire"));
    }

    @Test
    void neverCrossesASection() {
        assertFalse(GameConflictMatrix.warns("PrimaryFire", "BuggyPrimaryFireButton"));
        assertTrue(BindingConflictRules.isSafeOverlap("PrimaryFire", "BuggyPrimaryFireButton"));
    }
}
