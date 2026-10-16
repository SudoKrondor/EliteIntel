package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.io.BindingsWorkingCopyRepository;
import elite.intel.bindforge.io.BindingsWriter;
import elite.intel.bindforge.io.DeviceReferenceRewrite;
import elite.intel.bindforge.io.DeviceRewriteResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Apply's half of onboarding a controller {@code .binds} names by hex (A6b, 2026-10-14) and of a rename (A11): every
 * {@code .binds} in the folder and its working copy move to the name, and a rewritten working copy keeps a baseline
 * matching its game file.
 */
class BindsDeviceRewriteTest {

    @TempDir
    Path temp;

    @Test
    void everyBindsFileAndItsWorkingCopyMoveToTheName() throws IOException {
        Path bindings = Files.createDirectories(temp.resolve("Bindings"));
        Path working = Files.createDirectories(temp.resolve("working"));
        String custom = "<Root><A><Primary Device=\"83f43344\" Key=\"Joy_1\" /></A></Root>";
        Files.writeString(bindings.resolve("Custom.4.2.binds"), custom);
        Files.writeString(bindings.resolve("Racing.4.2.binds"),
                "<Root><A><Primary Device=\"83f43344\" Key=\"Joy_2\" /></A></Root>");
        Files.writeString(working.resolve("Custom.4.2.binds"), custom);
        WorkingCopies copies = new WorkingCopies(working);

        DeviceApply.Rewritten result = new BindsDeviceRewrite(bindings, copies, new BindingsWriter(),
                temp.resolve("backups")).rewrite(new DeviceReferenceRewrite(Set.of("83f43344"), "LeftStick"));

        assertEquals(DeviceRewriteResult.Status.REWRITTEN, result.status());
        assertEquals(2, result.bindings(), "the game's files only - the working copy holds the same binding again");
        for (Path file : new Path[]{bindings.resolve("Custom.4.2.binds"), bindings.resolve("Racing.4.2.binds"),
                working.resolve("Custom.4.2.binds")}) {
            String text = Files.readString(file);
            assertTrue(text.contains("Device=\"LeftStick\""), file + ": " + text);
            assertFalse(text.contains("83f43344"), file + ": " + text);
        }
        assertTrue(copies.gameFileMatchesBaseline("Custom.4.2.binds", bindings.resolve("Custom.4.2.binds")),
                "the working copy's baseline follows the rewritten game file, or its next apply is refused");
    }

    /** A working copy the rewrite did not touch is a draft whose game file changed underneath it: keep its baseline. */
    @Test
    void anUntouchedWorkingCopyKeepsItsBaseline() throws IOException {
        Path bindings = Files.createDirectories(temp.resolve("Bindings"));
        Path working = Files.createDirectories(temp.resolve("working"));
        Path game = bindings.resolve("Custom.4.2.binds");
        Files.writeString(game, "<Root><A><Primary Device=\"83f43344\" Key=\"Joy_1\" /></A></Root>");
        Files.writeString(working.resolve("Custom.4.2.binds"),
                "<Root><A><Primary Device=\"Keyboard\" Key=\"Key_A\" /></A></Root>");
        WorkingCopies copies = new WorkingCopies(working);
        copies.recordGameFileBaseline("Custom.4.2.binds", game);

        DeviceApply.Rewritten result = new BindsDeviceRewrite(bindings, copies, new BindingsWriter(),
                temp.resolve("backups")).rewrite(new DeviceReferenceRewrite(Set.of("83f43344"), "LeftStick"));

        assertEquals(DeviceRewriteResult.Status.REWRITTEN, result.status());
        assertFalse(copies.gameFileMatchesBaseline("Custom.4.2.binds", game),
                "the draft's next apply must see that its game file changed");
    }

    @Test
    void hardwareIdCountsReadTheBindingsFolder() throws IOException {
        Path bindings = Files.createDirectories(temp.resolve("Bindings"));
        Files.writeString(bindings.resolve("Custom.4.2.binds"),
                "<Root><A><Primary Device=\"334483F3\" Key=\"Joy_1\" /></A></Root>");

        assertEquals(Map.of("334483F3", 1), new BindsDeviceRewrite(bindings, new WorkingCopies(temp),
                new BindingsWriter(), temp.resolve("backups")).hardwareIdCounts());
    }

    /** A rename (A11): the old name, however it is spelled, moves to the new one in every file. */
    @Test
    void aRenameMovesTheOldNameEverywhere() throws IOException {
        Path bindings = Files.createDirectories(temp.resolve("Bindings"));
        Files.writeString(bindings.resolve("Custom.4.2.binds"),
                "<Root><A><Primary Device=\"LVWAP\" Key=\"Joy_1\" /></A>"
                        + "<B><Primary Device=\"Keyboard\" Key=\"Key_A\" /></B></Root>");
        Files.writeString(bindings.resolve("Racing.4.2.binds"),
                "<Root><A><Primary Device=\"LVWAP\" Key=\"Joy_2\" /></A></Root>");

        DeviceApply.Rewritten result = new BindsDeviceRewrite(bindings, new WorkingCopies(temp.resolve("working")),
                new BindingsWriter(), temp.resolve("backups")).rewrite(DeviceReferenceRewrite.rename("LVWAP", "LeftStick"));

        assertEquals(DeviceRewriteResult.Status.REWRITTEN, result.status());
        assertEquals(2, result.bindings());
        for (Path file : new Path[]{bindings.resolve("Custom.4.2.binds"), bindings.resolve("Racing.4.2.binds")}) {
            String text = Files.readString(file);
            assertTrue(text.contains("Device=\"LeftStick\""), file + ": " + text);
            assertFalse(text.contains("LVWAP"), file + ": " + text);
        }
        assertTrue(Files.readString(bindings.resolve("Custom.4.2.binds")).contains("Device=\"Keyboard\""),
                "nothing but the renamed device moves");
    }

    @Test
    void nameCountsReadTheBindingsFolder() throws IOException {
        Path bindings = Files.createDirectories(temp.resolve("Bindings"));
        Files.writeString(bindings.resolve("Custom.4.2.binds"),
                "<Root><A><Primary Device=\"LVWAP\" Key=\"Joy_1\" /></A></Root>");

        assertEquals(1, new BindsDeviceRewrite(bindings, new WorkingCopies(temp), new BindingsWriter(),
                temp.resolve("backups")).nameCounts().get("LVWAP"));
    }

    /** Working copies in a folder of the test's own, never Elite-Intel's data folder. */
    private static final class WorkingCopies extends BindingsWorkingCopyRepository {
        private final Path folder;

        WorkingCopies(Path folder) {
            this.folder = folder;
        }

        @Override
        public Path getWorkingCopyPath(String presetFileName) {
            return folder.resolve(presetFileName);
        }
    }
}
