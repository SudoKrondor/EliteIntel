package elite.intel.bindforge.io;

import elite.intel.bindforge.rules.KeyboardKeyAvailabilityService;
import elite.intel.io.TimestampedBackups;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code .binds} device rewrite (Foundation F2): moving a device's references when its name changes - naming a
 * device the file knows by hex, renaming one, and clearing one back to hex.
 */
class BindingsWriterDeviceRewriteTest {

    private static final String BINDS = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <Root PresetName="Custom" MajorVersion="4" MinorVersion="2">
            \t<YawAxisRaw>
            \t\t<Binding Device="334443F4" Key="Joy_XAxis" />
            \t\t<Inverted Value="0" />
            \t\t<Deadzone Value="0.00000000" />
            \t</YawAxisRaw>
            \t<UpThrustButton>
            \t\t<Primary Device="Throttle" Key="Joy_5">
            \t\t\t<Modifier Device="throttle" Key="Joy_1" />
            \t\t</Primary>
            \t\t<Secondary Device='Throttle' Key="Neg_Joy_YAxis" />
            \t</UpThrustButton>
            \t<FocusLeftPanel>
            \t\t<Primary Device="Keyboard" Key="Key_Q">
            \t\t\t<Modifier Device="Throttle" Key="Joy_2" />
            \t\t</Primary>
            \t\t<Secondary Device="ThrottleX" Key="Joy_3" SomeDevice="Throttle" />
            \t</FocusLeftPanel>
            </Root>
            """;

    @TempDir
    Path tempDir;

    private final BindingsWriter writer = new BindingsWriter();

    @Test
    void renameMovesEveryDeviceAttributeIgnoringCaseAndNothingElse() {
        RewrittenXml result =
                writer.rewriteDeviceReferences(BINDS, DeviceReferenceRewrite.rename("Throttle", "LeftStick"));

        assertEquals(4, result.references());
        assertEquals(withThrottleAs("LeftStick"), result.xml());
    }

    /**
     * Storage S2b (Alan, 2026-10-25): a case-only change rewrites every other spelling to the one chosen, and a
     * reference already spelled that way is neither rewritten nor counted.
     */
    @Test
    void aCaseOnlyRenameRewritesTheOtherSpellingsOnly() {
        RewrittenXml result =
                writer.rewriteDeviceReferences(BINDS, DeviceReferenceRewrite.rename("throttle", "Throttle"));

        assertEquals(1, result.references(), "only the lower-case modifier changes");
        assertEquals(BINDS.replace("Device=\"throttle\"", "Device=\"Throttle\""), result.xml());
    }

    @Test
    void hardwareIdIsMatchedVidThenPidIgnoringCase() {
        RewrittenXml result = writer.rewriteDeviceReferences(BINDS,
                DeviceReferenceRewrite.hardwareIdToName("3344", "43f4", "Stick"));

        assertEquals(1, result.references());
        assertTrue(result.xml().contains("<Binding Device=\"Stick\" Key=\"Joy_XAxis\" />"));
    }

    @Test
    void theReverseOrderIsMovedOnlyFromTheValueFound() {
        String reversed = BINDS.replace("334443F4", "43F43344");

        assertEquals(0, writer.rewriteDeviceReferences(reversed,
                DeviceReferenceRewrite.hardwareIdToName("3344", "43F4", "Stick")).references());
        assertEquals(1, writer.rewriteDeviceReferences(reversed,
                new DeviceReferenceRewrite(Set.of("43F43344"), "Stick")).references());
    }

    @Test
    void clearWritesVidThenPidUpperCase() {
        RewrittenXml result = writer.rewriteDeviceReferences(BINDS,
                DeviceReferenceRewrite.nameToHardwareId("Throttle", "3344", "83f3"));

        assertEquals(4, result.references());
        assertEquals(withThrottleAs("334483F3"), result.xml());
    }

    @Test
    void textWithNoReferenceComesBackUnchanged() {
        RewrittenXml result =
                writer.rewriteDeviceReferences(BINDS, DeviceReferenceRewrite.rename("T-Rudder", "Pedals"));

        assertEquals(0, result.references());
        assertEquals(BINDS, result.xml());
    }

    @Test
    void reservedNamesAndInvalidNamesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> DeviceReferenceRewrite.rename("Keyboard", "Keys"));
        assertThrows(IllegalArgumentException.class, () -> DeviceReferenceRewrite.rename("Throttle", "mouse"));
        assertThrows(IllegalArgumentException.class, () -> DeviceReferenceRewrite.rename("Throttle", "{NoDevice}"));
        assertThrows(IllegalArgumentException.class, () -> DeviceReferenceRewrite.rename("Throttle", "Left Stick"));
        assertThrows(IllegalArgumentException.class, () -> DeviceReferenceRewrite.rename("Throttle", "Bad\"Name"));
        assertThrows(IllegalArgumentException.class, () -> DeviceReferenceRewrite.rename("Throttle", "Throttle"));
        assertThrows(IllegalArgumentException.class, () -> DeviceReferenceRewrite.hardwareIdToName("334", "43F4", "X"));
        assertThrows(IllegalArgumentException.class, () -> new DeviceReferenceRewrite(Set.of(), "X"));
    }

    @Test
    void acrossFilesRewritesEveryFileThatNamesTheDeviceAndBacksEachUp() throws IOException {
        Path game = write("game/Custom.4.2.binds", BINDS);
        Path draft = write("draft/Custom.4.2.binds", BINDS);
        Path untouched = write("game/Other.4.2.binds", BINDS.replaceAll("(?i)throttle", "T-Rudder"));
        Path backups = tempDir.resolve("backups");

        DeviceRewriteResult result = writer.rewriteDeviceReferences(List.of(game, draft, untouched, game),
                DeviceReferenceRewrite.rename("Throttle", "LeftStick"), backups);

        assertEquals(DeviceRewriteResult.Status.REWRITTEN, result.status());
        assertTrue(result.succeeded());
        assertEquals(Map.of(game.toRealPath(), 4, draft.toRealPath(), 4), result.references());
        assertEquals(8, result.totalReferences());
        assertEquals(withThrottleAs("LeftStick"), read(game));
        assertEquals(withThrottleAs("LeftStick"), read(draft));
        assertEquals(BINDS.replaceAll("(?i)throttle", "T-Rudder"), read(untouched));
        assertEquals(2, countFiles(backups));
    }

    @Test
    void byteOrderMarkIsKept() throws IOException {
        Path file = tempDir.resolve("Custom.4.2.binds");
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = BINDS.getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, content, 0, bom.length);
        System.arraycopy(body, 0, content, bom.length, body.length);
        Files.write(file, content);

        writer.rewriteDeviceReferences(List.of(file), DeviceReferenceRewrite.rename("Throttle", "LeftStick"),
                tempDir.resolve("backups"));

        byte[] written = Files.readAllBytes(file);
        assertEquals((byte) 0xEF, written[0]);
        assertEquals((byte) 0xBB, written[1]);
        assertEquals((byte) 0xBF, written[2]);
    }

    @Test
    void noFileNamingTheDeviceWritesAndBacksUpNothing() throws IOException {
        Path game = write("game/Custom.4.2.binds", BINDS);
        Path backups = tempDir.resolve("backups");

        DeviceRewriteResult result = writer.rewriteDeviceReferences(List.of(game),
                DeviceReferenceRewrite.rename("T-Rudder", "Pedals"), backups);

        assertEquals(DeviceRewriteResult.Status.NO_REFERENCES, result.status());
        assertTrue(result.succeeded());
        assertEquals(BINDS, read(game));
        assertFalse(Files.exists(backups));
    }

    @Test
    void anUnreadableFileStopsItBeforeAnythingIsWritten() throws IOException {
        Path game = write("game/Custom.4.2.binds", BINDS);
        Path missing = tempDir.resolve("draft/Custom.4.2.binds");

        DeviceRewriteResult result = writer.rewriteDeviceReferences(List.of(game, missing),
                DeviceReferenceRewrite.rename("Throttle", "LeftStick"), tempDir.resolve("backups"));

        assertEquals(DeviceRewriteResult.Status.READ_FAILED, result.status());
        assertFalse(result.succeeded());
        assertEquals(missing.toAbsolutePath().normalize(), result.failedFile());
        assertEquals(BINDS, read(game));
    }

    @Test
    void aBackupFolderThatCannotBeMadeStopsItBeforeAnythingIsWritten() throws IOException {
        Path game = write("game/Custom.4.2.binds", BINDS);
        Path notAFolder = write("backups", "a file where the folder should be");

        DeviceRewriteResult result = writer.rewriteDeviceReferences(List.of(game),
                DeviceReferenceRewrite.rename("Throttle", "LeftStick"), notAFolder);

        assertEquals(DeviceRewriteResult.Status.BACKUP_FAILED, result.status());
        assertEquals(BINDS, read(game));
    }

    @Test
    void aFileTheGameSavedMeanwhileStopsItAndPutsBackWhatWasWritten() throws IOException {
        Path first = write("game/Custom.4.2.binds", BINDS);
        Path second = write("draft/Custom.4.2.binds", BINDS);
        String savedByGame = BINDS.replace("Key_Q", "Key_W");
        // Every file is backed up before any is written, so a backup that rewrites the second file stands in for
        // the game saving it between the read and the write.
        BindingsWriter writer = writerWhoseBackupOf(second, file -> Files.writeString(file, savedByGame));

        DeviceRewriteResult result = writer.rewriteDeviceReferences(List.of(first, second),
                DeviceReferenceRewrite.rename("Throttle", "LeftStick"), tempDir.resolve("backups"));

        assertEquals(DeviceRewriteResult.Status.STALE_FILE, result.status());
        assertEquals(second.toRealPath(), result.failedFile());
        assertTrue(result.notRestored().isEmpty());
        assertEquals(BINDS, read(first));
        assertEquals(savedByGame, read(second));
    }

    @Test
    void aFailedWriteStopsItAndPutsBackWhatWasWritten() throws IOException {
        Path first = write("game/Custom.4.2.binds", BINDS);
        Path second = write("draft/Custom.4.2.binds", BINDS);
        // A folder where the second file was makes reading and writing it fail.
        BindingsWriter writer = writerWhoseBackupOf(second, file -> {
            Files.delete(file);
            Files.createDirectory(file);
        });

        DeviceRewriteResult result = writer.rewriteDeviceReferences(List.of(first, second),
                DeviceReferenceRewrite.rename("Throttle", "LeftStick"), tempDir.resolve("backups"));

        assertEquals(DeviceRewriteResult.Status.WRITE_FAILED, result.status());
        assertEquals(second.toRealPath(), result.failedFile());
        assertEquals(BINDS, read(first));
    }

    @Test
    void anUnterminatedTagIsRefusedRatherThanRewrittenInPart() {
        String broken = BINDS.replace("<Secondary Device='Throttle' Key=\"Neg_Joy_YAxis\" />",
                "<Secondary Device='Throttle' Key=\"Neg_Joy_YAxis");

        assertThrows(IllegalArgumentException.class,
                () -> writer.rewriteDeviceReferences(broken, DeviceReferenceRewrite.rename("Throttle", "LeftStick")));
    }

    @Test
    void aFileThatCannotBeRewrittenWholeStopsItBeforeAnythingIsWritten() throws IOException {
        Path game = write("game/Custom.4.2.binds", BINDS);
        String brokenText = BINDS.replace("<Secondary Device='Throttle' Key=\"Neg_Joy_YAxis\" />",
                "<Secondary Device='Throttle' Key=\"Neg_Joy_YAxis");
        Path broken = write("draft/Custom.4.2.binds", brokenText);
        Path backups = tempDir.resolve("backups");

        DeviceRewriteResult result = writer.rewriteDeviceReferences(List.of(game, broken),
                DeviceReferenceRewrite.rename("Throttle", "LeftStick"), backups);

        assertEquals(DeviceRewriteResult.Status.UNSUPPORTED_XML, result.status());
        assertFalse(result.succeeded());
        assertEquals(broken.toRealPath(), result.failedFile());
        assertEquals(BINDS, read(game));
        assertEquals(brokenText, read(broken));
        assertFalse(Files.exists(backups));
    }

    @Test
    void oneFileNamedUnderTwoSpellingsIsRewrittenOnce() throws IOException {
        Path game = write("game/Custom.4.2.binds", BINDS);
        // The temp folder as JUnit names it and as the file system does - on Windows typically an 8.3 short name
        // (ALANRT~1) against the long one. Paths compare ignoring case on Windows, so only a spelling that differs
        // by more than case is one the old normalize() deduplication missed.
        Path otherSpelling = game.toRealPath();
        Assumptions.assumeFalse(otherSpelling.toString().equalsIgnoreCase(game.toAbsolutePath().normalize().toString()),
                "needs a temp folder reached through a short name or a link");

        DeviceRewriteResult result = writer.rewriteDeviceReferences(List.of(game, otherSpelling),
                DeviceReferenceRewrite.rename("Throttle", "LeftStick"), tempDir.resolve("backups"));

        assertEquals(DeviceRewriteResult.Status.REWRITTEN, result.status());
        assertEquals(Map.of(game.toRealPath(), 4), result.references());
        assertEquals(withThrottleAs("LeftStick"), read(game));
        assertEquals(1, countFiles(tempDir.resolve("backups")));
    }

    @Test
    void aFileTheGameSavedAfterTheRewriteWroteItIsNotPutBack() throws IOException {
        Path first = write("game/Custom.4.2.binds", BINDS);
        Path second = write("draft/Custom.4.2.binds", BINDS);
        Path realFirst = first.toRealPath();
        Path realSecond = second.toRealPath();
        String savedByGame = withThrottleAs("LeftStick").replace("Key_Q", "Key_W");
        // The first file is written, then the game saves over it; the second file's write fails.
        BindingsWriter writer = new BindingsWriter(new KeyboardKeyAvailabilityService(), new TimestampedBackups(),
                (file, content) -> {
                    if (file.equals(realSecond)) {
                        throw new IOException("disk full");
                    }
                    Files.write(file, content);
                    if (file.equals(realFirst) && !Arrays.equals(content, BINDS.getBytes(StandardCharsets.UTF_8))) {
                        Files.writeString(file, savedByGame);
                    }
                });

        DeviceRewriteResult result = writer.rewriteDeviceReferences(List.of(first, second),
                DeviceReferenceRewrite.rename("Throttle", "LeftStick"), tempDir.resolve("backups"));

        assertEquals(DeviceRewriteResult.Status.WRITE_FAILED, result.status());
        assertEquals(realSecond, result.failedFile());
        assertEquals(List.of(realFirst), result.notRestored());
        assertEquals(savedByGame, read(first));
        assertEquals(BINDS, read(second));
    }

    private interface FileAction {
        void run(Path file) throws IOException;
    }

    // A writer whose backup step, after copying target, does something to it - the hook a test needs between
    // the rewrite's read and its write.
    private BindingsWriter writerWhoseBackupOf(Path target, FileAction afterBackup) throws IOException {
        Path realTarget = target.toRealPath();
        return new BindingsWriter(new KeyboardKeyAvailabilityService(), new TimestampedBackups() {
            @Override
            public Path create(Path file, Path targetDirectory) throws IOException {
                Path copy = super.create(file, targetDirectory);
                if (file.equals(realTarget)) {
                    afterBackup.run(file);
                }
                return copy;
            }
        });
    }

    // BINDS with its four Throttle references - two slots and two modifiers, either quote, either case - renamed, and
    // the near misses (ThrottleX, a SomeDevice attribute) left alone.
    private static String withThrottleAs(String name) {
        return BINDS
                .replace("<Primary Device=\"Throttle\" Key=\"Joy_5\">", "<Primary Device=\"" + name + "\" Key=\"Joy_5\">")
                .replace("<Modifier Device=\"throttle\"", "<Modifier Device=\"" + name + "\"")
                .replace("<Secondary Device='Throttle'", "<Secondary Device='" + name + "'")
                .replace("<Modifier Device=\"Throttle\" Key=\"Joy_2\"", "<Modifier Device=\"" + name + "\" Key=\"Joy_2\"");
    }

    private Path write(String relative, String content) throws IOException {
        Path file = tempDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private long countFiles(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            return stream.count();
        }
    }
}
