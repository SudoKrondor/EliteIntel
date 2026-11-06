package elite.intel.bindforge.io;

import elite.intel.bindforge.model.BindingSection;
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

/**
 * {@link BindingsLoader#getActivePresetName()}'s one-line answer on the split specimen: the first non-empty line,
 * General's. The four-section read is {@link StartPresetSections}'s; BindForge reads the preset file from the working
 * copy since Storage S6.
 */
class BindingsLoaderStartPresetTest {

    private static final String SPECIMEN = "StartPreset.4.start";

    @TempDir
    Path bindingsDir;

    private final BindingsLoader loader = new BindingsLoader();

    @Test
    void theSingleAnswerTakesTheFirstNonEmptyLine() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/bindforge/startpreset/" + SPECIMEN)) {
            assertNotNull(in, "missing test resource " + SPECIMEN);
            Files.write(bindingsDir.resolve(SPECIMEN), in.readAllBytes());
        }
        // WHY: an empty binds file for each line's BindName - only the names matter to the one-line read.
        for (String binds : new String[]{"KeyboardMouseOnly.binds", "Custom.4.2.binds"}) {
            Files.writeString(bindingsDir.resolve(binds), "<Root/>", StandardCharsets.UTF_8);
        }

        // getLatestBindsFile resolves through the one-line read: General's preset, not Ship's.
        assertEquals("KeyboardMouseOnly.binds", loader.getLatestBindsFile(bindingsDir).getName());
    }

    @Test
    void aGeneralPresetWithNoFileIsReportedAgainstTheNextLinesFile() throws IOException {
        startPreset("KeyboardMouseOnly\r\nCustom\r\nCustom\r\nCustom");
        binds("Custom.4.2.binds");

        File read = loader.getLatestBindsFile(bindingsDir);

        assertEquals("Custom.4.2.binds", read.getName());
        assertEquals(Map.of(BindingSection.GENERAL, "KeyboardMouseOnly"), loader.sectionsNotIn(bindingsDir, read));
    }

    @Test
    void aGeneralPresetWithNoFileFallsToTheNextLineNotTheNewestFile() throws IOException {
        startPreset("KeyboardMouseOnly\r\nVKB\r\nVKB\r\nVKB");
        binds("VKB.4.2.binds");
        binds("Custom.4.2.binds");
        // WHY: the leftover file is the newest, which is what the old fallback would have picked.
        Files.setLastModifiedTime(bindingsDir.resolve("VKB.4.2.binds"), FileTime.fromMillis(1_000_000));
        Files.setLastModifiedTime(bindingsDir.resolve("Custom.4.2.binds"), FileTime.fromMillis(9_000_000));

        File read = loader.getLatestBindsFile(bindingsDir);

        assertEquals("VKB.4.2.binds", read.getName());
        assertEquals(Map.of(BindingSection.GENERAL, "KeyboardMouseOnly"), loader.sectionsNotIn(bindingsDir, read));
    }

    @Test
    void noLineWithAFileFallsBackToTheNewestFileAndReportsEverySection() throws IOException {
        startPreset("KeyboardMouseOnly\r\nKeyboardMouseOnly\r\nKeyboardMouseOnly\r\nKeyboardMouseOnly");
        binds("VKB.4.2.binds");
        binds("Custom.4.2.binds");
        Files.setLastModifiedTime(bindingsDir.resolve("VKB.4.2.binds"), FileTime.fromMillis(9_000_000));
        Files.setLastModifiedTime(bindingsDir.resolve("Custom.4.2.binds"), FileTime.fromMillis(1_000_000));

        File read = loader.getLatestBindsFile(bindingsDir);

        assertEquals("VKB.4.2.binds", read.getName());
        assertEquals(Map.of(
                BindingSection.GENERAL, "KeyboardMouseOnly",
                BindingSection.SHIP, "KeyboardMouseOnly",
                BindingSection.SRV, "KeyboardMouseOnly",
                BindingSection.ON_FOOT, "KeyboardMouseOnly"), loader.sectionsNotIn(bindingsDir, read));
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

    @Test
    void aVersionedOrDeDuplicatedFileCountsAsItsPresets() throws IOException {
        startPreset("Custom\r\nCustom\r\nCustom\r\nCustom");
        binds("Custom.4.2 (2).binds");

        assertEquals(Map.of(), loader.sectionsNotIn(bindingsDir, loader.getLatestBindsFile(bindingsDir)));
    }

    @Test
    void aSharedNamePrefixIsNotTheSamePreset() throws IOException {
        startPreset("Custom\r\nCustom\r\nCustom\r\nCustom");
        binds("CustomHotas.4.2.binds");

        assertEquals(Map.of(
                        BindingSection.GENERAL, "Custom",
                        BindingSection.SHIP, "Custom",
                        BindingSection.SRV, "Custom",
                        BindingSection.ON_FOOT, "Custom"),
                loader.sectionsNotIn(bindingsDir, loader.getLatestBindsFile(bindingsDir)));
    }

    @Test
    void aBlankLineIsNotReported() throws IOException {
        startPreset("\r\nCustom\r\n\r\nCustom");
        binds("Custom.4.2.binds");

        assertEquals(Map.of(), loader.sectionsNotIn(bindingsDir, loader.getLatestBindsFile(bindingsDir)));
    }

    @Test
    void aFolderWithNoStartPresetIsNotReported() throws IOException {
        binds("Custom.4.2.binds");

        assertEquals(Map.of(), loader.sectionsNotIn(bindingsDir, loader.getLatestBindsFile(bindingsDir)));
    }

    @Test
    void aStartPresetThatCannotBeReadIsNotReported() throws IOException {
        // WHY: bytes that are not UTF-8 make the read throw, and this check must not stop service startup.
        Files.write(bindingsDir.resolve(SPECIMEN), new byte[]{(byte) 0xC3, (byte) 0x28});
        binds("Custom.4.2.binds");

        assertEquals(Map.of(), loader.sectionsNotIn(bindingsDir, bindingsDir.resolve("Custom.4.2.binds").toFile()));
    }

    private void startPreset(String text) throws IOException {
        Files.writeString(bindingsDir.resolve(SPECIMEN), text, StandardCharsets.UTF_8);
    }

    private void binds(String name) throws IOException {
        Files.writeString(bindingsDir.resolve(name), "<Root/>", StandardCharsets.UTF_8);
    }
}
