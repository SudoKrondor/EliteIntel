package elite.intel.bindforge.storage;

import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.storage.BindingsComparison.Differences;
import elite.intel.bindforge.storage.BindingsComparison.Factory;
import elite.intel.bindforge.storage.BindingsComparison.FileDifference;
import elite.intel.bindforge.storage.BindingsComparison.Kind;
import elite.intel.bindforge.storage.BindingsComparison.LineItem;
import elite.intel.bindforge.storage.BindingsComparison.Presence;
import elite.intel.bindforge.storage.BindingsComparison.Side;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.text;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The check's comparison for the binds and preset files (storage.md rule 14; Storage S8a): by value, a row per slot,
 * a file on one side only, a newer version left out, a reset named - and the files a set of answers settles on.
 */
class BindingsComparisonTest {

    private static final String CUSTOM = "Custom.4.2.binds";
    private static final String PRESET = "StartPreset.4.start";
    private static final String YAW_PRIMARY = CUSTOM + "|YawLeftButton/Primary";
    private static final String AXIS = CUSTOM + "|YawAxisRaw";

    // ---- comparing

    /** A line ending, an indent, a byte-order mark or an XML declaration is not a change - values, never bytes. */
    @Test
    void theSameValuesWrittenDifferentlyAreNoDifference() {
        String game = "﻿" + binds("Custom", "Key_A", "Joy_XAxis", "").replace("\r\n", "\n").replace("\t", "  ")
                .replace("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n", "");

        Differences differences = compare(Map.of(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", "")),
                Map.of(CUSTOM, game));

        assertTrue(differences.none(), differences.toString());
    }

    /** A button is compared slot by slot - Primary and Secondary are separate rows (overview.md, Merge grain). */
    @Test
    void aChangedButtonSlotIsOneRow() {
        Differences differences = compare(Map.of(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", "")),
                Map.of(CUSTOM, binds("Custom", "Key_Q", "Joy_XAxis", "")));

        LineItem row = only(differences);
        assertEquals(YAW_PRIMARY, row.id());
        assertEquals(Kind.SLOT, row.kind());
        assertEquals("YawLeftButton", row.element());
        assertEquals("Primary", row.part());
        assertEquals("Key_A", row.workingCopy());
        assertEquals("Key_Q", row.live());
    }

    /** An axis is one row: its binding, Inverted and Deadzone together. */
    @Test
    void aChangedAxisIsOneRowForTheWholeElement() {
        Differences differences = compare(Map.of(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", "")),
                Map.of(CUSTOM, binds("Custom", "Key_A", "Joy_YAxis", "")));

        LineItem row = only(differences);
        assertEquals(AXIS, row.id());
        assertEquals(Kind.ELEMENT, row.kind());
        assertEquals("RVWAP Joy_XAxis, Inverted 0, Deadzone 0.00000000", row.workingCopy());
        assertEquals("RVWAP Joy_YAxis, Inverted 0, Deadzone 0.00000000", row.live());
    }

    @Test
    void anElementOnOneSideOnlyIsARowWithNothingOnTheOther() {
        Differences differences = compare(Map.of(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", "")),
                Map.of(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", "\t<NewThing Value=\"1\" />\r\n")));

        LineItem row = only(differences);
        assertEquals(Kind.ELEMENT, row.kind());
        assertNull(row.workingCopy());
        assertEquals("1", row.live());
    }

    @Test
    void aChangedPresetNameIsTheFilesDetailsRow() {
        Differences differences = compare(Map.of(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", "")),
                Map.of(CUSTOM, binds("Renamed", "Key_A", "Joy_XAxis", "")));

        LineItem row = only(differences);
        assertEquals(Kind.DETAILS, row.kind());
        assertEquals("Custom 4.2", row.workingCopy());
        assertEquals("Renamed 4.2", row.live());
    }

    /** A file on one side only is a difference, one row (Alan, 2026-10-30) - the game's new Custom file among them. */
    @Test
    void aFileOnOneSideOnlyIsOneRow() {
        Differences differences = compare(Map.of(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", "")),
                Map.of("Spare.4.2.binds", binds("Spare", "Key_A", "Joy_XAxis", "")));

        assertEquals(List.of(Presence.WORKING_COPY_ONLY, Presence.LIVE_ONLY),
                differences.files().stream().map(FileDifference::presence).toList());
        LineItem missing = differences.files().getFirst().items().getFirst();
        assertEquals(Kind.FILE, missing.kind());
        assertEquals("Custom 4.2", missing.workingCopy());
        assertNull(missing.live());
        LineItem added = differences.files().get(1).items().getFirst();
        assertNull(added.workingCopy());
        assertEquals("Spare 4.2", added.live());
    }

    /** A newer version beside an older one is Storage S10's - both left out, and named (Alan, 2026-10-30). */
    @Test
    void aNewerVersionAndTheOneItReplacesAreLeftOut() {
        Differences differences = compare(Map.of(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", "")),
                Map.of("Custom.4.3.binds", binds("Custom", "Key_A", "Joy_XAxis", "")));

        assertTrue(differences.none());
        assertEquals(List.of(CUSTOM, "Custom.4.3.binds"), differences.leftOut());
    }

    @Test
    void anOlderVersionInTheGamesFolderIsNotLeftOut() {
        Differences differences = compare(Map.of(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", "")),
                Map.of(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""),
                        "Custom.4.1.binds", binds("Custom", "Key_A", "Joy_XAxis", "")));

        assertEquals(List.of("Custom.4.1.binds"), differences.files().stream().map(FileDifference::file).toList());
        assertEquals(List.of(), differences.leftOut());
    }

    @Test
    void thePresetFileIsComparedLineByLine() {
        Differences differences = compare(Map.of(PRESET, "Custom\r\nCustom\r\nCustom\r\nCustom"),
                Map.of(PRESET, "Custom\nKeyboardMouseOnly\nCustom\nCustom\n"));

        LineItem row = only(differences);
        assertEquals(Kind.PRESET_LINE, row.kind());
        assertEquals(BindingSection.SHIP, row.section());
        assertEquals("Custom", row.workingCopy());
        assertEquals("KeyboardMouseOnly", row.live());
    }

    @Test
    void aPresetFileWithTheSameLinesWrittenDifferentlyIsNoDifference() {
        assertTrue(compare(Map.of(PRESET, "Custom\r\nCustom\r\nCustom\r\nCustom"),
                Map.of(PRESET, "﻿Custom\nCustom\nCustom\nCustom\n")).none());
    }

    /** A file that cannot be read element by element is compared whole, as one row. */
    @Test
    void aBindsFileThatCannotBeReadIsOneWholeFileRow() {
        Differences differences = compare(Map.of(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", "")),
                Map.of(CUSTOM, "<Root><broken></Root>"));

        LineItem row = only(differences);
        assertEquals(Kind.FILE, row.kind());
        assertEquals(Presence.BOTH, differences.files().getFirst().presence());
    }

    /** Equal to a factory binds file but for its PresetName: a game update probably reset it. */
    @Test
    void aBindsFileMatchingAFactoryOneIsAReset() {
        String factoryFile = binds("KeyboardMouseOnly", "Key_Q", "Joy_XAxis", "");
        Factory factory = new Factory(Set.of("KeyboardMouseOnly"), List.of(bytes(factoryFile)));

        Map<String, byte[]> workingCopy = files(Map.of(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", "")));

        Differences reset = BindingsComparison.compare(workingCopy,
                files(Map.of(CUSTOM, binds("Custom", "Key_Q", "Joy_XAxis", ""))), factory);
        Differences edited = BindingsComparison.compare(workingCopy,
                files(Map.of(CUSTOM, binds("Custom", "Key_W", "Joy_XAxis", ""))), factory);

        assertTrue(reset.anyReset());
        assertFalse(edited.anyReset(), "a change in the game is not a reset");
    }

    /** Every line naming a factory binds file, where the working copy's named the user's own. */
    @Test
    void aPresetFileNamingOnlyFactoryFilesIsAReset() {
        Factory factory = new Factory(Set.of("KeyboardMouseOnly"), List.of());
        Map<String, byte[]> workingCopy = files(Map.of(PRESET, "Custom\r\nCustom\r\nCustom\r\nCustom",
                CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", "")));

        Differences reset = BindingsComparison.compare(workingCopy, files(Map.of(PRESET,
                "KeyboardMouseOnly\r\nKeyboardMouseOnly\r\nKeyboardMouseOnly\r\nKeyboardMouseOnly",
                CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""))), factory);
        Differences split = BindingsComparison.compare(workingCopy, files(Map.of(PRESET,
                "Custom\r\nKeyboardMouseOnly\r\nCustom\r\nCustom",
                CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""))), factory);

        assertTrue(reset.anyReset());
        assertFalse(split.anyReset(), "a line moved to a factory file in the game is the user's choice");
    }

    // ---- settling

    /** Keeping the working copy's side everywhere: its bytes, and the live-only file removed from the game's folder. */
    @Test
    void keepingMineEverywhereKeepsTheWorkingCopyAndRemovesTheLiveOnlyFile() throws IOException {
        Map<String, byte[]> workingCopy = files(Map.of(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", "")));
        Map<String, byte[]> live = files(Map.of(CUSTOM, binds("Custom", "Key_Q", "Joy_XAxis", ""),
                "Spare.4.2.binds", binds("Spare", "Key_A", "Joy_XAxis", "")));
        Differences differences = BindingsComparison.compare(workingCopy, live, Factory.NONE);

        BindingsComparison.Settled settled = BindingsComparison.settle(workingCopy, live, differences,
                every(differences, Side.WORKING_COPY));

        assertArrayEquals(workingCopy.get(CUSTOM), settled.bindings().get(CUSTOM));
        assertFalse(settled.bindings().containsKey("Spare.4.2.binds"));
        assertEquals(Set.of("Spare.4.2.binds"), settled.removeFromLive());
    }

    /** Taking the game's side everywhere: its bytes, its new file, and a file it lacks gone from the working copy. */
    @Test
    void takingTheGamesEverywhereTakesItsBytesAndItsFiles() throws IOException {
        Map<String, byte[]> workingCopy = files(Map.of(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""),
                "Old.4.2.binds", binds("Old", "Key_A", "Joy_XAxis", "")));
        Map<String, byte[]> live = files(Map.of(CUSTOM, binds("Custom", "Key_Q", "Joy_XAxis", "").replace("\r\n", "\n"),
                "Spare.4.2.binds", binds("Spare", "Key_A", "Joy_XAxis", "")));
        Differences differences = BindingsComparison.compare(workingCopy, live, Factory.NONE);

        BindingsComparison.Settled settled = BindingsComparison.settle(workingCopy, live, differences,
                every(differences, Side.LIVE));

        assertArrayEquals(live.get(CUSTOM), settled.bindings().get(CUSTOM), "the game's bytes, as they are");
        assertArrayEquals(live.get("Spare.4.2.binds"), settled.bindings().get("Spare.4.2.binds"));
        assertFalse(settled.bindings().containsKey("Old.4.2.binds"));
        assertEquals(Set.of(), settled.removeFromLive());
    }

    /**
     * A merge replaces exactly the slot answered in-game and leaves every other byte of the working copy's file as it
     * was - its line endings and byte-order mark included.
     */
    @Test
    void aMergeSplicesTheAnsweredSlotIntoTheWorkingCopysFile() throws IOException {
        String mine = "﻿" + binds("Custom", "Key_A", "Joy_XAxis", "");
        Map<String, byte[]> workingCopy = files(Map.of(CUSTOM, mine));
        Map<String, byte[]> live = files(Map.of(CUSTOM,
                binds("Custom", "Key_Q", "Joy_YAxis", "").replace("\r\n", "\n")));
        Differences differences = BindingsComparison.compare(workingCopy, live, Factory.NONE);
        Map<String, Side> answers = Map.of(YAW_PRIMARY, Side.LIVE, AXIS, Side.WORKING_COPY);

        byte[] merged = BindingsComparison.settle(workingCopy, live, differences, answers).bindings().get(CUSTOM);

        assertEquals(mine.replace("Key=\"Key_A\"", "Key=\"Key_Q\""), text(merged));
    }

    /**
     * An element the game added is inserted before {@code </Root>}, with the game's indent and the working copy's line
     * ending; one the game dropped is removed with its line. The button kept as mine keeps the merge a real one.
     */
    @Test
    void aMergeInsertsAndRemovesWholeElements() throws IOException {
        String mine = binds("Custom", "Key_A", "Joy_XAxis", "\t<Dropped Value=\"0\" />\r\n");
        String theirs = binds("Custom", "Key_Q", "Joy_XAxis", "\t<NewThing Value=\"1\" />\r\n");
        Map<String, byte[]> workingCopy = files(Map.of(CUSTOM, mine));
        Map<String, byte[]> live = files(Map.of(CUSTOM, theirs));
        Differences differences = BindingsComparison.compare(workingCopy, live, Factory.NONE);
        Map<String, Side> answers = Map.of(YAW_PRIMARY, Side.WORKING_COPY, CUSTOM + "|Dropped", Side.LIVE,
                CUSTOM + "|NewThing", Side.LIVE);

        byte[] merged = BindingsComparison.settle(workingCopy, live, differences, answers).bindings().get(CUSTOM);

        assertEquals(binds("Custom", "Key_A", "Joy_XAxis", "\t<NewThing Value=\"1\" />\r\n"), text(merged));
    }

    @Test
    void aMergedPresetFileIsWrittenInTheGamesForm() throws IOException {
        Map<String, byte[]> workingCopy = files(Map.of(PRESET, "Custom\r\nCustom\r\nCustom\r\nCustom"));
        Map<String, byte[]> live = files(Map.of(PRESET, "Custom\nShipOnly\nSrvOnly\nCustom"));
        Differences differences = BindingsComparison.compare(workingCopy, live, Factory.NONE);
        Map<String, Side> answers = Map.of(PRESET + "|SHIP", Side.LIVE, PRESET + "|SRV", Side.WORKING_COPY);

        byte[] merged = BindingsComparison.settle(workingCopy, live, differences, answers).bindings().get(PRESET);

        assertEquals("Custom\r\nShipOnly\r\nCustom\r\nCustom", text(merged));
    }

    @Test
    void aRowWithNoAnswerIsRefused() {
        Map<String, byte[]> workingCopy = files(Map.of(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", "")));
        Map<String, byte[]> live = files(Map.of(CUSTOM, binds("Custom", "Key_Q", "Joy_YAxis", "")));
        Differences differences = BindingsComparison.compare(workingCopy, live, Factory.NONE);

        assertThrows(IllegalArgumentException.class, () -> BindingsComparison.settle(workingCopy, live, differences,
                Map.of(YAW_PRIMARY, Side.LIVE)));
    }

    // ---- helpers

    /**
     * A small binds file in the game's form: CRLF, tabs, the XML declaration - a button, an axis, a setting and the
     * keyboard layout, with {@code extra} lines before {@code </Root>}.
     */
    static String binds(String presetName, String yawKey, String axisKey, String extra) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n"
                + "<Root PresetName=\"" + presetName + "\" MajorVersion=\"4\" MinorVersion=\"2\">\r\n"
                + "\t<KeyboardLayout>en-US</KeyboardLayout>\r\n"
                + "\t<YawLeftButton>\r\n"
                + "\t\t<Primary Device=\"Keyboard\" Key=\"" + yawKey + "\" />\r\n"
                + "\t\t<Secondary Device=\"{NoDevice}\" Key=\"\" />\r\n"
                + "\t</YawLeftButton>\r\n"
                + "\t<YawAxisRaw>\r\n"
                + "\t\t<Binding Device=\"RVWAP\" Key=\"" + axisKey + "\" />\r\n"
                + "\t\t<Inverted Value=\"0\" />\r\n"
                + "\t\t<Deadzone Value=\"0.00000000\" />\r\n"
                + "\t</YawAxisRaw>\r\n"
                + "\t<MouseSensitivity Value=\"1.00000000\" />\r\n"
                + extra
                + "</Root>\r\n";
    }

    private static Differences compare(Map<String, String> workingCopy, Map<String, String> live) {
        return BindingsComparison.compare(files(workingCopy), files(live), Factory.NONE);
    }

    private static Map<String, byte[]> files(Map<String, String> texts) {
        Map<String, byte[]> files = new HashMap<>();
        texts.forEach((name, text) -> files.put(name, bytes(text)));
        return files;
    }

    private static LineItem only(Differences differences) {
        assertEquals(1, differences.items().size(), differences.items().toString());
        return differences.items().getFirst();
    }

    private static Map<String, Side> every(Differences differences, Side side) {
        Map<String, Side> answers = new HashMap<>();
        differences.items().forEach(item -> answers.put(item.id(), side));
        return answers;
    }
}
