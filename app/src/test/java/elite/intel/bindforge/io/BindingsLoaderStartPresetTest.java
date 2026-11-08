package elite.intel.bindforge.io;

import elite.intel.bindforge.model.BindingSection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Which binds file {@link BindingsLoader} reads, and which sections of the game's controls that leaves unread: the
 * preset file's lines in order, General first, each resolving to its preset's true binds file, else its factory binds
 * file from the first {@code ControlSchemes} folder that holds it; never a plain file, never the newest file (Alan,
 * 2026-11-08, Storage S13c). BindForge reads the preset file from the working copy since Storage S6.
 */
class BindingsLoaderStartPresetTest {

    private static final String SPECIMEN = "StartPreset.4.start";

    @TempDir
    Path tempDir;

    private Path bindingsDir;
    private Path firstInstallation;
    private Path secondInstallation;

    /** No installations: only the bindings folder is looked in. */
    private final BindingsLoader loader = new BindingsLoader(List::of);

    @BeforeEach
    void folders() throws IOException {
        bindingsDir = Files.createDirectory(tempDir.resolve("Bindings"));
        firstInstallation = Files.createDirectory(tempDir.resolve("FirstControlSchemes"));
        secondInstallation = Files.createDirectory(tempDir.resolve("SecondControlSchemes"));
    }

    @Test
    void theSingleAnswerTakesTheFirstNonEmptyLine() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/bindforge/startpreset/" + SPECIMEN)) {
            assertNotNull(in, "missing test resource " + SPECIMEN);
            Files.write(bindingsDir.resolve(SPECIMEN), in.readAllBytes());
        }
        // WHY: an empty binds file for each line's BindName - only the names matter to the one-line read.
        for (String binds : new String[]{"KeyboardMouseOnly.4.2.binds", "Custom.4.2.binds"}) {
            Files.writeString(bindingsDir.resolve(binds), "<Root/>", StandardCharsets.UTF_8);
        }

        // getLatestBindsFile resolves through the one-line read: General's preset, not Ship's.
        assertEquals("KeyboardMouseOnly.4.2.binds", loader.getLatestBindsFile(bindingsDir).getName());
    }

    @Test
    void aGeneralPresetWithNoFileIsReportedAgainstTheNextLinesFile() throws IOException {
        startPreset("KeyboardMouseOnly\r\nCustom\r\nCustom\r\nCustom");
        binds("Custom.4.2.binds");

        File read = loader.getLatestBindsFile(bindingsDir);

        assertEquals("Custom.4.2.binds", read.getName());
        assertEquals(Map.of(BindingSection.GENERAL, "KeyboardMouseOnly"), loader.sectionsNotIn(bindingsDir, read));
    }

    /** A preset line in another case names the file, as the game reads it (Alan, 2026-11-07, Storage S13a D6). */
    @Test
    void aPresetLineInAnotherCaseFindsItsFile() throws IOException {
        startPreset("custom\r\ncustom\r\ncustom\r\ncustom");
        binds("Custom.4.2.binds");
        binds("Other.4.2.binds");

        assertEquals("Custom.4.2.binds", loader.getLatestBindsFile(bindingsDir).getName());
    }

    @Test
    void aGeneralPresetWithNoFileFallsToTheNextLineNotTheNewestFile() throws IOException {
        startPreset("KeyboardMouseOnly\r\nVKB\r\nVKB\r\nVKB");
        binds("VKB.4.2.binds");
        binds("Custom.4.2.binds");
        // WHY: the other file is the newest, which the newest-file fallback before Storage S13c would have picked.
        Files.setLastModifiedTime(bindingsDir.resolve("VKB.4.2.binds"), FileTime.fromMillis(1_000_000));
        Files.setLastModifiedTime(bindingsDir.resolve("Custom.4.2.binds"), FileTime.fromMillis(9_000_000));

        File read = loader.getLatestBindsFile(bindingsDir);

        assertEquals("VKB.4.2.binds", read.getName());
        assertEquals(Map.of(BindingSection.GENERAL, "KeyboardMouseOnly"), loader.sectionsNotIn(bindingsDir, read));
    }

    /** No newest-file fallback (Storage S13c): when no line resolves, nothing is read. */
    @Test
    void noLineWithAFileReadsNothingNotTheNewestFile() throws IOException {
        startPreset("KeyboardMouseOnly\r\nKeyboardMouseOnly\r\nKeyboardMouseOnly\r\nKeyboardMouseOnly");
        binds("VKB.4.2.binds");
        binds("Custom.4.2.binds");

        assertThrows(IOException.class, () -> loader.getLatestBindsFile(bindingsDir));
    }

    @Test
    void aFolderWithNoStartPresetReadsNothing() throws IOException {
        binds("Custom.4.2.binds");

        assertThrows(IOException.class, () -> loader.getLatestBindsFile(bindingsDir));
    }

    /** A plain file never counts as a preset's file; the preset's factory file is read instead (Storage S13c). */
    @Test
    void aPresetWithOnlyAPlainFileReadsItsFactoryFile() throws IOException {
        startPreset("KeyboardMouseOnly\r\nKeyboardMouseOnly\r\nKeyboardMouseOnly\r\nKeyboardMouseOnly");
        binds("KeyboardMouseOnly.binds");
        Path factory = factory(firstInstallation, "KeyboardMouseOnly.binds");

        File read = withInstallations(firstInstallation).getLatestBindsFile(bindingsDir);

        assertEquals(factory.toFile(), read);
        assertEquals(Map.of(), loader.sectionsNotIn(bindingsDir, read));
    }

    @Test
    void aPresetWithOnlyAPlainFileAndNoFactoryFileReadsNothing() throws IOException {
        startPreset("Mine\r\nMine\r\nMine\r\nMine");
        binds("Mine.binds");
        factory(firstInstallation, "KeyboardMouseOnly.binds");

        assertThrows(IOException.class, () -> withInstallations(firstInstallation).getLatestBindsFile(bindingsDir));
    }

    @Test
    void aTrueFileBeatsAPlainOne() throws IOException {
        startPreset("Custom\r\nCustom\r\nCustom\r\nCustom");
        binds("Custom.4.2.binds");
        binds("Custom.binds");
        Files.setLastModifiedTime(bindingsDir.resolve("Custom.4.2.binds"), FileTime.fromMillis(1_000_000));
        Files.setLastModifiedTime(bindingsDir.resolve("Custom.binds"), FileTime.fromMillis(9_000_000));

        assertEquals("Custom.4.2.binds", loader.getLatestBindsFile(bindingsDir).getName());
    }

    @Test
    void aTrueFileBeatsTheFactoryFileOfTheSameName() throws IOException {
        startPreset("KeyboardMouseOnly\r\nKeyboardMouseOnly\r\nKeyboardMouseOnly\r\nKeyboardMouseOnly");
        binds("KeyboardMouseOnly.4.2.binds");
        factory(firstInstallation, "KeyboardMouseOnly.binds");

        assertEquals(bindingsDir.resolve("KeyboardMouseOnly.4.2.binds").toFile(),
                withInstallations(firstInstallation).getLatestBindsFile(bindingsDir));
    }

    /** General's factory file is its file: the next line's own file is not read before it. */
    @Test
    void generalOnAFactoryFileIsReadBeforeTheNextLinesOwnFile() throws IOException {
        startPreset("KeyboardMouseOnly\r\nCustom\r\nCustom\r\nCustom");
        binds("Custom.4.2.binds");
        Path factory = factory(firstInstallation, "KeyboardMouseOnly.binds");

        File read = withInstallations(firstInstallation).getLatestBindsFile(bindingsDir);

        assertEquals(factory.toFile(), read);
        assertEquals(Map.of(BindingSection.SHIP, "Custom", BindingSection.SRV, "Custom", BindingSection.ON_FOOT,
                "Custom"), loader.sectionsNotIn(bindingsDir, read));
    }

    @Test
    void theFirstInstallationThatHoldsTheFactoryFileIsRead() throws IOException {
        startPreset("KeyboardMouseOnly\r\nKeyboardMouseOnly\r\nKeyboardMouseOnly\r\nKeyboardMouseOnly");
        factory(firstInstallation, "XboxPadDefaults.binds");
        Path second = factory(secondInstallation, "KeyboardMouseOnly.binds");

        assertEquals(second.toFile(),
                withInstallations(firstInstallation, secondInstallation).getLatestBindsFile(bindingsDir));

        Path first = factory(firstInstallation, "KeyboardMouseOnly.binds");
        assertEquals(first.toFile(),
                withInstallations(firstInstallation, secondInstallation).getLatestBindsFile(bindingsDir));
    }

    @Test
    void aFactoryFileInAnotherCaseIsFound() throws IOException {
        startPreset("keyboardmouseonly\r\nkeyboardmouseonly\r\nkeyboardmouseonly\r\nkeyboardmouseonly");
        Path factory = factory(firstInstallation, "KeyboardMouseOnly.binds");

        File read = withInstallations(firstInstallation).getLatestBindsFile(bindingsDir);

        assertEquals(factory.toFile(), read);
        assertEquals(Map.of(), loader.sectionsNotIn(bindingsDir, read));
    }

    /** One installation that cannot be listed does not hide another's factory file. */
    @Test
    void aControlSchemesFolderThatCannotBeListedIsSkipped() throws IOException {
        startPreset("KeyboardMouseOnly\r\nKeyboardMouseOnly\r\nKeyboardMouseOnly\r\nKeyboardMouseOnly");
        Path gone = tempDir.resolve("UnpluggedControlSchemes");
        Path factory = factory(secondInstallation, "KeyboardMouseOnly.binds");

        assertEquals(factory.toFile(), withInstallations(gone, secondInstallation).getLatestBindsFile(bindingsDir));
    }

    /**
     * Two versions of one preset - only a hand rename makes them - read the highest version, not the newest file
     * (Alan, 2026-11-08, Storage S13c).
     */
    @Test
    void twoVersionsOfAPresetReadTheHighest() throws IOException {
        startPreset("Custom\r\nCustom\r\nCustom\r\nCustom");
        binds("Custom.4.2.binds");
        binds("Custom.4.3.binds");
        Files.setLastModifiedTime(bindingsDir.resolve("Custom.4.3.binds"), FileTime.fromMillis(1_000_000));
        Files.setLastModifiedTime(bindingsDir.resolve("Custom.4.2.binds"), FileTime.fromMillis(9_000_000));

        assertEquals("Custom.4.3.binds", loader.getLatestBindsFile(bindingsDir).getName());
    }

    @Test
    void theReverseSplitReportsTheSectionsOnAnotherPreset() throws IOException {
        startPreset("VKB\r\nCustom\r\nVKB\r\nCustom");
        binds("VKB.4.2.binds");
        binds("Custom.4.2.binds");

        File read = loader.getLatestBindsFile(bindingsDir);

        assertEquals("VKB.4.2.binds", read.getName());
        assertEquals(Map.of(BindingSection.SHIP, "Custom", BindingSection.ON_FOOT, "Custom"),
                loader.sectionsNotIn(bindingsDir, read));
    }

    @Test
    void sectionsComeBackInTheFilesOrder() throws IOException {
        startPreset("KeyboardMouseOnly\r\nVKB\r\nCustom\r\nCustom");
        binds("VKB.4.2.binds");

        File read = loader.getLatestBindsFile(bindingsDir);

        assertEquals(List.of(BindingSection.GENERAL, BindingSection.SRV, BindingSection.ON_FOOT),
                List.copyOf(loader.sectionsNotIn(bindingsDir, read).keySet()));
    }

    @Test
    void everySectionOnTheFileReadIsNotReported() throws IOException {
        startPreset("VKB\r\nVKB\r\nVKB\r\nVKB");
        binds("VKB.4.2.binds");
        binds("Custom.4.2.binds");

        assertEquals(Map.of(), loader.sectionsNotIn(bindingsDir, loader.getLatestBindsFile(bindingsDir)));
    }

    @Test
    void theSingleLineFormIsNotReported() throws IOException {
        startPreset("VKB");
        binds("VKB.4.2.binds");

        assertEquals(Map.of(), loader.sectionsNotIn(bindingsDir, loader.getLatestBindsFile(bindingsDir)));
    }

    /**
     * A copy Windows named {@code Custom.4.2 (2).binds} is not a binds file's name, so it is no preset's file - read
     * as BindForge reads it (preset-editor.md, <em>The BindName list</em>; Storage S13aR, D7).
     */
    @Test
    void aDeDuplicatedCopyIsNotTheFileOfItsPreset() throws IOException {
        startPreset("Custom\r\nCustom\r\nCustom\r\nCustom");
        Path copy = binds("Custom.4.2 (2).binds");

        assertThrows(IOException.class, () -> loader.getLatestBindsFile(bindingsDir));
        assertEquals(everySectionOn("Custom"), loader.sectionsNotIn(bindingsDir, copy.toFile()));
    }

    /**
     * {@code Custom.Old.4.2.binds} is BindName {@code Custom.Old}, not a file of preset {@code Custom} (Storage S13aR,
     * D7).
     */
    @Test
    void aDottedNameIsItsOwnBindName() throws IOException {
        startPreset("Custom\r\nCustom\r\nCustom\r\nCustom");
        Path dotted = binds("Custom.Old.4.2.binds");

        assertThrows(IOException.class, () -> loader.getLatestBindsFile(bindingsDir));
        assertEquals(everySectionOn("Custom"), loader.sectionsNotIn(bindingsDir, dotted.toFile()));
    }

    @Test
    void aSharedNamePrefixIsNotTheSamePreset() throws IOException {
        startPreset("Custom\r\nCustom\r\nCustom\r\nCustom");
        Path other = binds("CustomHotas.4.2.binds");

        assertThrows(IOException.class, () -> loader.getLatestBindsFile(bindingsDir));
        assertEquals(everySectionOn("Custom"), loader.sectionsNotIn(bindingsDir, other.toFile()));
    }

    @Test
    void aBlankLineIsNotReported() throws IOException {
        startPreset("\r\nCustom\r\n\r\nCustom");
        binds("Custom.4.2.binds");

        assertEquals(Map.of(), loader.sectionsNotIn(bindingsDir, loader.getLatestBindsFile(bindingsDir)));
    }

    @Test
    void aFolderWithNoStartPresetIsNotReported() throws IOException {
        Path custom = binds("Custom.4.2.binds");

        assertEquals(Map.of(), loader.sectionsNotIn(bindingsDir, custom.toFile()));
    }

    @Test
    void aStartPresetThatCannotBeReadIsNotReported() throws IOException {
        // WHY: bytes that are not UTF-8 make the read throw, and this check must not stop service startup.
        Files.write(bindingsDir.resolve(SPECIMEN), new byte[]{(byte) 0xC3, (byte) 0x28});
        binds("Custom.4.2.binds");

        assertEquals(Map.of(), loader.sectionsNotIn(bindingsDir, bindingsDir.resolve("Custom.4.2.binds").toFile()));
    }

    private static Map<BindingSection, String> everySectionOn(String preset) {
        return Map.of(BindingSection.GENERAL, preset, BindingSection.SHIP, preset, BindingSection.SRV, preset,
                BindingSection.ON_FOOT, preset);
    }

    private static BindingsLoader withInstallations(Path... controlSchemes) {
        return new BindingsLoader(() -> List.of(controlSchemes));
    }

    private void startPreset(String text) throws IOException {
        Files.writeString(bindingsDir.resolve(SPECIMEN), text, StandardCharsets.UTF_8);
    }

    private Path binds(String name) throws IOException {
        return Files.writeString(bindingsDir.resolve(name), "<Root/>", StandardCharsets.UTF_8);
    }

    private static Path factory(Path controlSchemes, String name) throws IOException {
        return Files.writeString(controlSchemes.resolve(name), "<Root/>", StandardCharsets.UTF_8);
    }
}
