package elite.intel.bindforge.storage;

import elite.intel.bindforge.storage.BindingsComparison.Kind;
import elite.intel.bindforge.storage.BindingsComparison.LineItem;
import elite.intel.bindforge.storage.BindingsComparison.Side;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static elite.intel.bindforge.storage.BindingsComparisonTest.binds;
import static elite.intel.bindforge.storage.BindingsComparisonTest.binds43;
import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.text;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain {@code .binds} judged against the settled set (storage.md rule 14, <em>Settled with Storage S13b's sources</em>
 * and <em>S13e's</em>): a twin compared by value - lacking only what it has, the plain file goes as it is; holding lines
 * it lacks, those are rows, and each answered in-game goes into the twin - the factory file's values send it to Leftover
 * User Files as it is, anything else converts it first. One that does not read as a binds file is never judged
 * ({@link LeftoversTest}).
 */
class PlainBindsTest {

    private static final String CUSTOM = "Custom.4.2.binds";
    private static final Map<String, byte[]> CUSTOM_SET = Map.of(CUSTOM,
            bytes(binds("Custom", "Key_A", "Joy_XAxis", "")), "StartPreset.4.start", bytes("Custom"));

    private static final String UNBOUND_PRIMARY = "<Primary Device=\"{NoDevice}\" Key=\"\" />";
    private static final String PITCH_UP = "\t<PitchUpButton>\r\n"
            + "\t\t<Primary Device=\"Keyboard\" Key=\"Key_W\" />\r\n"
            + "\t\t<Secondary Device=\"{NoDevice}\" Key=\"\" />\r\n"
            + "\t</PitchUpButton>\r\n";

    @Test
    void aConvertedFileTakesTheVersionItsRootCarries() throws IOException {
        PlainBinds.Plan plan = plan(Map.of("Spare.binds", binds43("Frontier", "Key_Z", "Joy_XAxis", "")), Map.of());

        assertEquals(List.of(new PlainBinds.Tidied("Spare.binds", PlainBinds.Fate.CONVERTED, "Spare.4.3.binds")),
                plan.tidied());
        assertEquals(binds43("Spare", "Key_Z", "Joy_XAxis", ""), text(plan.converted().get("Spare.4.3.binds")));
    }

    /** CREATE's version rule: a root with no version names the file 4.2. */
    @Test
    void aRootWithNoVersionIsNamedFourTwo() throws IOException {
        String unversioned = binds("Frontier", "Key_Z", "Joy_XAxis", "")
                .replace(" MajorVersion=\"4\" MinorVersion=\"2\"", "");

        PlainBinds.Plan plan = plan(Map.of("Spare.binds", unversioned), Map.of());

        assertTrue(plan.converted().containsKey("Spare.4.2.binds"), plan.converted().keySet().toString());
    }

    /** A factory file of that name whose values differ: converted, every byte kept. */
    @Test
    void aFileThatDiffersFromTheFactoryFileIsConverted() throws IOException {
        PlainBinds.Plan plan = plan(Map.of("KeyboardMouseOnly.binds", binds("Copied", "Key_Z", "Joy_XAxis", "")),
                Map.of("KeyboardMouseOnly", bytes(binds("KeyboardMouseOnly", "Key_A", "Joy_XAxis", ""))));

        assertEquals(PlainBinds.Fate.CONVERTED, plan.tidied().getFirst().fate());
        assertEquals("KeyboardMouseOnly.4.2.binds", plan.tidied().getFirst().about());
    }

    @Test
    void aFileEqualToTheFactoryFileButItsNameIsNotConverted() throws IOException {
        PlainBinds.Plan plan = plan(Map.of("KeyboardMouseOnly.binds", binds("Copied", "Key_A", "Joy_XAxis", "")),
                Map.of("KeyboardMouseOnly", bytes(binds("KeyboardMouseOnly", "Key_A", "Joy_XAxis", ""))));

        assertEquals(PlainBinds.Fate.FACTORY, plan.tidied().getFirst().fate());
        assertTrue(plan.converted().isEmpty());
        assertFalse(plan.changesTheSet());
    }

    /** Two spellings, only where the file system heeds case: the first is converted, the second is its twin. */
    @Test
    void aSecondSpellingIsATwinOfTheFirstConverted() throws IOException {
        PlainBinds.Plan plan = plan(Map.of("Spare.binds", binds("Spare", "Key_A", "Joy_XAxis", ""),
                "spare.binds", binds("Spare", "Key_B", "Joy_XAxis", "")), Map.of());

        assertEquals(List.of(new PlainBinds.Tidied("Spare.binds", PlainBinds.Fate.CONVERTED, "Spare.4.2.binds"),
                new PlainBinds.Tidied("spare.binds", PlainBinds.Fate.TWIN, "Spare.4.2.binds")), plan.tidied());
        assertEquals(1, plan.converted().size());
    }

    // ---- against its twin (Storage S13e)

    /** An unbound slot, and an element only the twin has, are the plain file lacking them - no row (Q2). */
    @Test
    void aPlainFileLackingOnlyWhatItsTwinHasGivesNoRow() {
        Map<String, byte[]> twins = Map.of(CUSTOM, bytes(binds("Custom", "Key_A", "Joy_XAxis", PITCH_UP)));
        String plain = binds("Frontier", "Key_A", "Joy_XAxis", "")
                .replace("<Primary Device=\"Keyboard\" Key=\"Key_A\" />", UNBOUND_PRIMARY);

        assertEquals(List.of(), PlainBinds.rows(Map.of("Custom.binds", bytes(plain)), twins));
    }

    /** A binding both hold with different values is one the twin lacks (Q1); the root's PresetName is no row. */
    @Test
    void aBindingHeldDifferentlyIsARowTheTwinMineThePlainFileInGame() {
        List<PlainBinds.Rows> rows = PlainBinds.rows(Map.of("Custom.binds",
                bytes(binds("Frontier", "Key_Z", "Joy_XAxis", ""))), CUSTOM_SET);

        assertEquals(1, rows.size());
        assertEquals(CUSTOM, rows.getFirst().twin());
        assertEquals("Custom.binds", rows.getFirst().plain());
        LineItem row = rows.getFirst().rows().items().getFirst();
        assertEquals(List.of(row), rows.getFirst().rows().items());
        assertEquals(Kind.SLOT, row.kind());
        assertEquals("Custom.binds|YawLeftButton/Primary", row.id());
        assertEquals("Key_A", row.workingCopy());
        assertEquals("Key_Z", row.live());
    }

    @Test
    void anElementOnlyThePlainFileHasIsARow() {
        List<PlainBinds.Rows> rows = PlainBinds.rows(Map.of("Custom.binds",
                bytes(binds("Custom", "Key_A", "Joy_XAxis", PITCH_UP))), CUSTOM_SET);

        LineItem row = rows.getFirst().rows().items().getFirst();
        assertEquals(Kind.ELEMENT, row.kind());
        assertEquals("PitchUpButton", row.element());
        assertNull(row.workingCopy());
    }

    /** An element only the plain file has, with nothing bound in it, is the plain file lacking a binding (Q2). */
    @Test
    void anElementOnlyThePlainFileHasWithNothingBoundIsNoRow() {
        String empty = PITCH_UP.replace("<Primary Device=\"Keyboard\" Key=\"Key_W\" />", UNBOUND_PRIMARY);

        assertEquals(List.of(), PlainBinds.rows(Map.of("Custom.binds",
                bytes(binds("Custom", "Key_A", "Joy_XAxis", empty))), CUSTOM_SET));
    }

    /** The twin is found ignoring case. */
    @Test
    void aTwinInAnotherCaseIsFound() {
        List<PlainBinds.Rows> rows = PlainBinds.rows(Map.of("custom.binds",
                bytes(binds("Custom", "Key_Z", "Joy_XAxis", ""))), CUSTOM_SET);

        assertEquals(CUSTOM, rows.getFirst().twin());
    }

    /** Each row kept in-game goes into the twin; every other byte is the twin's. */
    @Test
    void aRowAnsweredInGameGoesIntoTheTwin() throws IOException {
        Map<String, Side> answers = new HashMap<>();
        answers.put("Custom.binds|YawLeftButton/Primary", Side.LIVE);
        answers.put("Custom.binds|PitchUpButton", Side.LIVE);

        PlainBinds.Plan plan = plan(Map.of("Custom.binds", binds("Frontier", "Key_Z", "Joy_XAxis", PITCH_UP)),
                Map.of(), answers);

        assertEquals(List.of(new PlainBinds.Tidied("Custom.binds", PlainBinds.Fate.MERGED, CUSTOM)), plan.tidied());
        assertEquals(binds("Custom", "Key_Z", "Joy_XAxis", PITCH_UP), text(plan.merged().get(CUSTOM)));
        assertTrue(plan.converted().isEmpty());
        assertTrue(plan.changesTheSet());
        assertEquals(binds("Custom", "Key_Z", "Joy_XAxis", PITCH_UP),
                text(plan.appliedTo(CUSTOM_SET).get(CUSTOM)), "the merged twin in place of the set's");
    }

    /** Only the rows answered in-game: the other keeps the twin's line. */
    @Test
    void aRowKeptMineLeavesTheTwinsLine() throws IOException {
        PlainBinds.Plan plan = plan(Map.of("Custom.binds", binds("Frontier", "Key_Z", "Joy_XAxis", PITCH_UP)),
                Map.of(), Map.of("Custom.binds|YawLeftButton/Primary", Side.WORKING_COPY,
                        "Custom.binds|PitchUpButton", Side.LIVE));

        assertEquals(binds("Custom", "Key_A", "Joy_XAxis", PITCH_UP), text(plan.merged().get(CUSTOM)));
    }

    /** Every row kept mine - or none answered, a row the warning never showed (Q4) - and the twin is untouched. */
    @Test
    void noRowTakenLeavesTheTwinAsItIs() throws IOException {
        PlainBinds.Plan plan = plan(Map.of("Custom.binds", binds("Frontier", "Key_Z", "Joy_XAxis", "")), Map.of());

        assertEquals(List.of(new PlainBinds.Tidied("Custom.binds", PlainBinds.Fate.TWIN, CUSTOM)), plan.tidied());
        assertTrue(plan.merged().isEmpty());
        assertFalse(plan.changesTheSet());
    }

    /**
     * No twin in the set, but the write takes one of its BindName out of the bindings folder: only moved, its rows'
     * answers dropped - converted, it would put back the name the user took away (the 2026-11-09 review, finding 1).
     */
    @Test
    void aPlainFileWhoseTwinLeavesIsNeitherConvertedNorMerged() throws IOException {
        Map<String, byte[]> files = Map.of("spare.binds", bytes(binds("Spare", "Key_Z", "Joy_XAxis", "")));

        PlainBinds.Plan plan = PlainBinds.plan(files, CUSTOM_SET, Set.of("Spare.4.2.binds"), Map.of(),
                Map.of("spare.binds|YawLeftButton/Primary", Side.LIVE));

        assertEquals(List.of(new PlainBinds.Tidied("spare.binds", PlainBinds.Fate.TWIN_LEAVES, "Spare.4.2.binds")),
                plan.tidied());
        assertFalse(plan.changesTheSet());
    }

    /** A twin in the set wins over one leaving - a newer version's older name leaves while the newer one is written. */
    @Test
    void aTwinInTheSetWinsOverOneLeaving() throws IOException {
        Map<String, byte[]> files = Map.of("custom.binds", bytes(binds("Custom", "Key_A", "Joy_XAxis", "")));

        PlainBinds.Plan plan = PlainBinds.plan(files, CUSTOM_SET, Set.of("Custom.4.1.binds"), Map.of(), Map.of());

        assertEquals(List.of(new PlainBinds.Tidied("custom.binds", PlainBinds.Fate.TWIN, CUSTOM)), plan.tidied());
    }

    /** XML that is not under a {@code <Root>} has no version to name a file by - not read as a binds file. */
    @Test
    void xmlThatIsNotUnderARootDoesNotReadAsABindsFile() {
        assertFalse(PlainBinds.readsAsBindsFile(bytes("<Notes><Line>hello</Line></Notes>")));
    }

    @Test
    void noPlainFilesIsNoPlan() throws IOException {
        assertSame(PlainBinds.Plan.NONE, PlainBinds.plan(Map.of(), CUSTOM_SET, Set.of(), Map.of(), Map.of()));
        assertEquals(List.of(), PlainBinds.rows(Map.of(), CUSTOM_SET));
    }

    private static PlainBinds.Plan plan(Map<String, String> plain, Map<String, byte[]> factory) throws IOException {
        return plan(plain, factory, Map.of());
    }

    private static PlainBinds.Plan plan(Map<String, String> plain, Map<String, byte[]> factory,
                                        Map<String, Side> answers) throws IOException {
        Map<String, byte[]> files = new TreeMap<>();
        plain.forEach((name, content) -> files.put(name, bytes(content)));
        return PlainBinds.plan(files, CUSTOM_SET, Set.of(), factory, answers);
    }
}
