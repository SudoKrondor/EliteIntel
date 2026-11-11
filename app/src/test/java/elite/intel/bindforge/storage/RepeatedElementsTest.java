package elite.intel.bindforge.storage;

import elite.intel.bindforge.storage.BindingsComparison.Differences;
import elite.intel.bindforge.storage.BindingsComparison.Factory;
import elite.intel.bindforge.storage.BindingsComparison.FileDifference;
import elite.intel.bindforge.storage.BindingsComparison.Kind;
import elite.intel.bindforge.storage.BindingsComparison.LineItem;
import elite.intel.bindforge.storage.BindingsComparison.NewerVersion;
import elite.intel.bindforge.storage.BindingsComparison.Side;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static elite.intel.bindforge.storage.BindingsComparisonTest.binds;
import static elite.intel.bindforge.storage.BindingsComparisonTest.binds43;
import static org.junit.jupiter.api.Assertions.*;

/**
 * An element the file holds more than once (storage.md rule 14, <em>Settled 2026-11-11</em> and <em>Settled with
 * Storage S18's sources</em>): every binds file the game saves holds {@code MouseGUI} twice, and such a file is still
 * compared row by row. Built from the reference files as the game saved them, and from files written here with the
 * element twice - the other tests' files hold it once.
 */
class RepeatedElementsTest {

    private static final String CUSTOM = "Custom.4.2.binds";
    private static final String NEWER = "Custom.4.3.binds";
    private static final Pattern MOUSE_GUI = Pattern.compile("<MouseGUI Value=\"([^\"]*)\" />");

    // ---- the bug: a file the game saved

    /**
     * Two bindings changed in the game in a file the game saved are two rows, not one whole-file row (Alan,
     * 2026-11-11).
     */
    @Test
    void aFileTheGameSavedGivesARowPerChangedSlot() throws IOException {
        String saved = reference("Custom.4.2.binds");
        assertEquals(2, mouseGuiValues(saved).size(), "the reference file holds MouseGUI twice");
        String changedInGame = saved
                .replace("<WingNavLock>\r\n\t\t<Primary Device=\"Keyboard\" Key=\"Key_Minus\" />",
                        "<WingNavLock>\r\n\t\t<Primary Device=\"Keyboard\" Key=\"Key_F9\" />")
                .replace("<OpenCodexGoToDiscovery>\r\n\t\t<Primary Device=\"Keyboard\" Key=\"Key_O\" />",
                        "<OpenCodexGoToDiscovery>\r\n\t\t<Primary Device=\"Keyboard\" Key=\"Key_F10\" />");
        assertNotEquals(saved, changedInGame);

        Differences differences = compare(saved, changedInGame);

        List<LineItem> rows = differences.items();
        assertEquals(List.of(CUSTOM + "|WingNavLock/Primary", CUSTOM + "|OpenCodexGoToDiscovery/Primary"),
                rows.stream().map(LineItem::id).toList());
        assertTrue(rows.stream().allMatch(row -> row.kind() == Kind.SLOT));
    }

    /** Each reference file the game saved reads element by element, so none is a whole-file row. */
    @Test
    void everyReferenceFileTheGameSavedReadsElementByElement() throws IOException {
        for (String name : List.of("Custom.3.0.binds", "Custom.4.2.binds", "Custom.4.2 (2).binds",
                "DualVirpilDawnTreader.4.1.binds")) {
            byte[] content = reference(name).getBytes(StandardCharsets.UTF_8);
            BindsElements elements = BindsElements.of(content).orElseThrow(() -> new AssertionError(name));
            BindsElements.Top mouseGui = elements.elements().get("MouseGUI");
            assertTrue(mouseGui.repeated(), name);
            assertFalse(mouseGui.copiesDiffer(), name);
        }
    }

    /**
     * Taking one of the two changed slots of the game's side and keeping the other splices only that slot: both copies
     * of {@code MouseGUI}, and every other byte, stay as they were.
     */
    @Test
    void aMergedSlotLeavesBothCopiesWhereTheyAre() throws IOException {
        String saved = reference("Custom.4.2.binds");
        String wingNav = "<WingNavLock>\r\n\t\t<Primary Device=\"Keyboard\" Key=\"Key_Minus\" />";
        String wingNavInGame = "<WingNavLock>\r\n\t\t<Primary Device=\"Keyboard\" Key=\"Key_F9\" />";
        String changedInGame = saved.replace(wingNav, wingNavInGame)
                .replace("<OpenCodexGoToDiscovery>\r\n\t\t<Primary Device=\"Keyboard\" Key=\"Key_O\" />",
                        "<OpenCodexGoToDiscovery>\r\n\t\t<Primary Device=\"Keyboard\" Key=\"Key_F10\" />");
        Differences differences = compare(saved, changedInGame);

        String merged = text(BindingsComparison.settle(files(saved), files(changedInGame), differences,
                Map.of(CUSTOM + "|WingNavLock/Primary", Side.LIVE,
                        CUSTOM + "|OpenCodexGoToDiscovery/Primary", Side.WORKING_COPY)).bindings().get(CUSTOM));

        assertEquals(saved.replace(wingNav, wingNavInGame), merged);
    }

    // ---- Q1: the number of copies does not count

    /** A file holding it once equals one holding it twice with the same value - no row (Q1). */
    @Test
    void theNumberOfCopiesIsNoDifference() {
        assertTrue(compare(once("0"), twice("0", "0")).none());
        assertTrue(compare(twice("0", "0"), once("0")).none());
    }

    /** A factory file holds it once; the game's save of it twice - still named a reset (Q1). */
    @Test
    void aSavedFactoryFileIsStillAReset() {
        String factoryFile = binds("KeyboardMouseOnly", "Key_Q", "Joy_XAxis", mouseGui("0") + menuGroups());
        Factory factory = new Factory(Set.of("KeyboardMouseOnly"), List.of(bytes(factoryFile)));

        Differences differences = BindingsComparison.compare(files(once("0")),
                files(binds("Custom", "Key_Q", "Joy_XAxis", mouseGui("0") + menuGroups() + mouseGui("0"))), factory);

        assertTrue(differences.anyReset());
    }

    /** A hand-copied factory file (one copy) lacks nothing its saved twin (two) has (Q1). */
    @Test
    void aPlainFileWithOneCopyLacksNothingItsTwinHas() {
        assertTrue(BindingsComparison.plainAgainstTwin("Custom.binds", bytes(twice("0", "0")), bytes(once("0")))
                .isEmpty());
    }

    // ---- one row, every copy written

    /** Changed in the game, it is one row for the element, and taking it writes every copy (rule 14). */
    @Test
    void aChangedRepeatedElementIsOneRowWrittenToEveryCopy() throws IOException {
        String mine = twice("0", "0");
        String game = twice("1", "1");
        Differences differences = compare(mine, game);

        LineItem row = only(differences);
        assertEquals(CUSTOM + "|MouseGUI", row.id());
        assertEquals(Kind.ELEMENT, row.kind());
        assertEquals("0", row.workingCopy());
        assertEquals("1", row.live());

        assertEquals(List.of("1", "1"), mouseGuiValues(merged(mine, game)));
    }

    /** Keeping mine of that row while taking another row of the game's leaves every copy mine. */
    @Test
    void keepingMineOfARepeatedElementLeavesEveryCopy() throws IOException {
        String mine = twice("0", "0");
        String game = binds("Custom", "Key_Q", "Joy_XAxis", mouseGui("1") + menuGroups() + mouseGui("1"));
        Differences differences = compare(mine, game);
        Map<String, Side> answers = new HashMap<>();
        differences.items().forEach(row -> answers.put(row.id(), Side.LIVE));
        answers.put(CUSTOM + "|MouseGUI", Side.WORKING_COPY);

        String merged = text(BindingsComparison.settle(files(mine), files(game), differences, answers)
                .bindings().get(CUSTOM));

        assertEquals(List.of("0", "0"), mouseGuiValues(merged));
        assertTrue(merged.contains("Key_Q"));
    }

    // ---- Q2: a write never adds or removes a copy

    /** The file written keeps its own count: one copy stays one, taking the game's value (Q2). */
    @Test
    void theFileWrittenKeepsItsOwnNumberOfCopies() throws IOException {
        assertEquals(List.of("1"), mouseGuiValues(merged(once("0"), twice("1", "1"))));
    }

    /** Held by the working copy only, every copy goes when the game's side is kept (Q2). */
    @Test
    void anElementTheGameDroppedLosesEveryCopy() throws IOException {
        String mine = twice("0", "0");
        String game = binds("Custom", "Key_A", "Joy_XAxis", menuGroups());

        LineItem row = only(compare(mine, game));
        assertEquals(CUSTOM + "|MouseGUI", row.id());

        assertEquals(List.of(), mouseGuiValues(merged(mine, game)));
    }

    /** Held by the game's side only, it is added once (Q2). */
    @Test
    void anElementOnlyTheGameHasIsAddedOnce() throws IOException {
        String mine = binds("Custom", "Key_A", "Joy_XAxis", menuGroups());

        assertEquals(List.of("1"), mouseGuiValues(merged(mine, twice("1", "1"))));
    }

    /**
     * Every row answered in-game is the game's file whole, copies that differ and all - an overwrite copies the file as
     * it is (Q5).
     */
    @Test
    void everyRowInGameIsTheGamesFileAsItIs() throws IOException {
        String mine = twice("0", "0");
        String game = twice("1", "0");

        assertEquals(game, gamesFileWhole(mine, game));
    }

    // ---- Q3: copies that differ

    /** Copies that differ are one row showing each; taking that side writes its first copy to every copy (Q3). */
    @Test
    void copiesThatDifferAreOneRowAndTheFirstIsWrittenToEvery() throws IOException {
        String mine = twice("0", "0");
        String game = twice("1", "0");
        Differences differences = compare(mine, game);

        LineItem row = only(differences);
        assertEquals("1 / 0", row.live());

        assertEquals(List.of("1", "1"), mouseGuiValues(merged(mine, game)));
    }

    /**
     * Keeping mine of a row whose copies differ, while taking another row of the game's, still makes mine's copies
     * equal - its first - so the merge never writes copies that differ (Q3; the S18 review's D1).
     */
    @Test
    void keepingMineOfCopiesThatDifferMakesThemEqual() throws IOException {
        String mine = twice("1", "0");
        String game = binds("Custom", "Key_Q", "Joy_XAxis", mouseGui("0") + menuGroups() + mouseGui("0"));
        Differences differences = compare(mine, game);

        String merged = text(BindingsComparison.settle(files(mine), files(game), differences, Map.of(
                CUSTOM + "|MouseGUI", Side.WORKING_COPY,
                CUSTOM + "|YawLeftButton/Primary", Side.LIVE)).bindings().get(CUSTOM));

        assertEquals(List.of("1", "1"), mouseGuiValues(merged));
        assertTrue(merged.contains("Key_Q"));
    }

    /** The same for a newer version: keeping the game's side of copies that differ makes them equal (Q3). */
    @Test
    void keepingTheGamesCopiesThatDifferInANewerVersionMakesThemEqual() throws IOException {
        String older = twice("0", "0");
        String newer = binds43("Custom", "Key_Q", "Joy_XAxis", mouseGui("1") + menuGroups() + mouseGui("0"));
        Differences differences = BindingsComparison.compare(files(older), Map.of(NEWER, bytes(newer)), Factory.NONE);

        String merged = text(BindingsComparison.settle(files(older), Map.of(NEWER, bytes(newer)), differences, Map.of(
                NEWER + "|MouseGUI", Side.LIVE,
                NEWER + "|YawLeftButton/Primary", Side.WORKING_COPY)).bindings().get(NEWER));

        assertEquals(List.of("1", "1"), mouseGuiValues(merged));
        assertTrue(merged.contains("Key=\"Key_A\""));
    }

    /** Copies that differ are never equal to copies that agree, whichever value they agree on (Q3). */
    @Test
    void copiesThatDifferAreADifferenceFromEitherValue() {
        assertFalse(compare(twice("0", "0"), twice("0", "1")).none());
        assertFalse(compare(twice("1", "1"), twice("0", "1")).none());
        assertTrue(compare(twice("0", "1"), twice("0", "1")).none());
    }

    // ---- Q4: never per slot

    /** A repeated button is one row for the whole element, never a row per slot (Q4). */
    @Test
    void aRepeatedButtonIsOneRowForTheWholeElement() throws IOException {
        String button = "\t<Repeated>\r\n\t\t<Primary Device=\"Keyboard\" Key=\"%s\" />\r\n"
                + "\t\t<Secondary Device=\"{NoDevice}\" Key=\"\" />\r\n\t</Repeated>\r\n";
        String mine = binds("Custom", "Key_A", "Joy_XAxis", button.formatted("Key_1") + button.formatted("Key_1"));
        String game = binds("Custom", "Key_A", "Joy_XAxis", button.formatted("Key_2") + button.formatted("Key_2"));
        Differences differences = compare(mine, game);

        LineItem row = only(differences);
        assertEquals(CUSTOM + "|Repeated", row.id());
        assertEquals(Kind.ELEMENT, row.kind());

        String merged = merged(mine, game);
        assertFalse(merged.contains("Key_1"));
        assertEquals(2, merged.split("Key_2", -1).length - 1);
    }

    // ---- a newer version

    /** A newer version the game saved is reconciled row by row, and keeping mine writes every copy of it (rule 14). */
    @Test
    void aNewerVersionTheGameSavedIsReconciledRowByRow() throws IOException {
        String older = twice("0", "0");
        String newer = binds43("Custom", "Key_A", "Joy_XAxis",
                mouseGui("1") + menuGroups() + mouseGui("1") + "\t<NewThing Value=\"1\" />\r\n");
        Differences differences = BindingsComparison.compare(files(older), Map.of(NEWER, bytes(newer)), Factory.NONE);

        NewerVersion version = differences.newer().getFirst();
        assertEquals(List.of("NewThing"), version.newSlots());
        assertEquals(List.of(NEWER + "|MouseGUI"), version.items().stream().map(LineItem::id).toList());

        String merged = text(BindingsComparison.settle(files(older), Map.of(NEWER, bytes(newer)), differences,
                Map.of(NEWER + "|MouseGUI", Side.WORKING_COPY)).bindings().get(NEWER));
        assertEquals(List.of("0", "0"), mouseGuiValues(merged));
        assertTrue(merged.contains("NewThing"));
    }

    // ---- helpers

    /** A binds file with {@code MouseGUI} once, as Frontier's factory files hold it. */
    private static String once(String value) {
        return binds("Custom", "Key_A", "Joy_XAxis", mouseGui(value) + menuGroups());
    }

    /** A binds file with {@code MouseGUI} twice, as the game saves one: before and after {@code EnableMenuGroups}. */
    private static String twice(String first, String second) {
        return binds("Custom", "Key_A", "Joy_XAxis", mouseGui(first) + menuGroups() + mouseGui(second));
    }

    private static String mouseGui(String value) {
        return "\t<MouseGUI Value=\"" + value + "\" />\r\n";
    }

    private static String menuGroups() {
        return "\t<EnableMenuGroups Value=\"0\" />\r\n";
    }

    private static List<String> mouseGuiValues(String text) {
        Matcher matcher = MOUSE_GUI.matcher(text);
        List<String> values = new ArrayList<>();
        while (matcher.find()) values.add(matcher.group(1));
        return values;
    }

    private static Differences compare(String mine, String game) {
        return BindingsComparison.compare(files(mine), files(game), Factory.NONE);
    }

    /**
     * The merge with every row of {@code game} answered in-game but one: the game also changes {@code YawLeftButton}'s
     * Primary, and that row keeps mine - so the working copy's file is spliced rather than replaced whole, which every
     * row answered in-game would do (Q5).
     */
    private static String merged(String mine, String game) throws IOException {
        String changed = game.replace("Key=\"Key_A\"", "Key=\"Key_Q\"");
        Differences differences = compare(mine, changed);
        Map<String, Side> answers = new HashMap<>();
        differences.items().forEach(row -> answers.put(row.id(), Side.LIVE));
        assertNotNull(answers.put(CUSTOM + "|YawLeftButton/Primary", Side.WORKING_COPY), "no row for the yaw slot");
        String merged = text(BindingsComparison.settle(files(mine), files(changed), differences, answers)
                .bindings().get(CUSTOM));
        assertTrue(merged.contains("Key=\"Key_A\""), "the row answered mine kept mine");
        return merged;
    }

    /** The working copy's file once every row of {@code mine} against {@code game} is answered in-game. */
    private static String gamesFileWhole(String mine, String game) throws IOException {
        Differences differences = compare(mine, game);
        Map<String, Side> answers = new HashMap<>();
        differences.items().forEach(row -> answers.put(row.id(), Side.LIVE));
        return text(BindingsComparison.settle(files(mine), files(game), differences, answers).bindings().get(CUSTOM));
    }

    private static LineItem only(Differences differences) {
        assertEquals(1, differences.files().size(), differences.toString());
        FileDifference file = differences.files().getFirst();
        assertEquals(1, file.items().size(), file.toString());
        return file.items().getFirst();
    }

    private static Map<String, byte[]> files(String binds) {
        Map<String, byte[]> files = new TreeMap<>();
        files.put(CUSTOM, bytes(binds));
        return files;
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String text(byte[] content) {
        return new String(content, StandardCharsets.UTF_8);
    }

    /**
     * A reference file as the game saved it, read where it is (as {@code preseteditor.Specimens} does). The game
     * writes CRLF, but git stores these files with LF and a checkout may hand them back either way ({@code autocrlf}),
     * so the line ends are put back to CRLF here - the edits above match the game's bytes on every checkout.
     */
    private static String reference(String name) throws IOException {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve("docs"))) root = root.getParent();
        assertNotNull(root, "could not find the repository root");
        return Files.readString(root.resolve("docs/02-features/bindforge/reference-data").resolve(name),
                StandardCharsets.UTF_8).replaceAll("\r?\n", "\r\n");
    }
}
