package elite.intel.bindforge.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static elite.intel.bindforge.storage.BindingsComparisonTest.binds;
import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static org.junit.jupiter.api.Assertions.*;

/**
 * What does not belong in the bindings folder (storage.md rule 14, <em>Settled 2026-11-08</em>; Storage S13d): found by
 * the check and moved to Leftover User Files.
 */
class LeftoversTest {

    @TempDir
    Path folder;

    @Test
    void theGamesOwnFilesStayAndEverythingElseIsToMove() throws Exception {
        write("Custom.4.2.binds", binds("Custom", "Key_A", "Joy_XAxis", ""));
        write("StartPreset.4.start", "Custom");
        write("BindingLoadingErrors.log", "");
        write("Custom.4.2 (2).binds", "copy");
        write("Custom.4.2.binds.backup", "old");
        write("StartPreset.start", "Custom");
        write("desktop.ini", "");

        Leftovers.Found found = Leftovers.read(folder);

        assertEquals(List.of("Custom.4.2 (2).binds", "Custom.4.2.binds.backup", "StartPreset.start", "desktop.ini"),
                found.others());
        assertTrue(found.plain().isEmpty());
    }

    /** Q5: a name the game's files have in any case is never moved - it could be the game's own. */
    @Test
    void theGamesNamesStayWhateverTheirCase() throws Exception {
        write("custom.4.2.BINDS", "x");
        write("startpreset.4.start", "Custom");
        write("bindingloadingerrors.LOG", "");

        assertTrue(Leftovers.read(folder).none());
    }

    @Test
    void aPlainFileThatReadsAsABindsFileIsJudgedAndOneThatDoesNotIsOnlyMoved() throws Exception {
        write("Spare.binds", binds("Spare", "Key_A", "Joy_XAxis", ""));
        write("Notes.binds", "<Notes><Line>hello</Line></Notes>");

        Leftovers.Found found = Leftovers.read(folder);

        assertEquals(Set.of("Spare.binds"), found.plain().keySet());
        assertEquals(List.of("Notes.binds"), found.others());
        assertEquals(List.of("Notes.binds", "Spare.binds"), found.files());
    }

    /** Q2: a folder inside the bindings folder is left alone. */
    @Test
    void aFolderIsLeftAlone() throws Exception {
        Files.createDirectories(folder.resolve("Old"));

        assertTrue(Leftovers.read(folder).none());
    }

    @Test
    void noBindingsFolderFindsNothing() throws Exception {
        assertSame(Leftovers.Found.NONE, Leftovers.read(folder.resolve("missing")));
    }

    /** Compared again before the write: a plain file by its bytes, which it is judged on; any other by name. */
    @Test
    void sameAsComparesPlainFilesByBytesAndOthersByName() {
        Leftovers.Found found = new Leftovers.Found(Map.of("Spare.binds", bytes("a")), List.of("x.txt"));

        assertTrue(found.sameAs(new Leftovers.Found(Map.of("Spare.binds", bytes("a")), List.of("x.txt"))));
        assertFalse(found.sameAs(new Leftovers.Found(Map.of("Spare.binds", bytes("b")), List.of("x.txt"))));
        assertFalse(found.sameAs(new Leftovers.Found(Map.of("Spare.binds", bytes("a")), List.of("y.txt"))));
    }

    @Test
    void thePlanMovesEveryFileAndConvertsOnlyAHandEditedPlainFile() throws IOException {
        Leftovers.Found found = new Leftovers.Found(Map.of("Spare.binds", bytes(binds("Spare", "Key_Z", "Joy_XAxis",
                ""))), List.of("Custom.4.2 (2).binds"));

        Leftovers.Plan plan = Leftovers.plan(found, Map.of("Custom.4.2.binds",
                bytes(binds("Custom", "Key_A", "Joy_XAxis", ""))), Map.of(), Map.of());

        assertEquals(Set.of("Custom.4.2 (2).binds", "Spare.binds"), plan.move());
        assertTrue(plan.changesTheSet());
        assertTrue(plan.plain().converted().containsKey("Spare.4.2.binds"));
    }

    @Test
    void aPlanWithOnlyOtherFilesConvertsNothing() throws IOException {
        Leftovers.Plan plan = Leftovers.plan(new Leftovers.Found(Map.of(), List.of("a.txt")), Map.of(), Map.of(),
                Map.of());

        assertEquals(Set.of("a.txt"), plan.move());
        assertFalse(plan.changesTheSet());
    }

    private void write(String name, String content) throws Exception {
        Files.write(folder.resolve(name), bytes(content));
    }
}
