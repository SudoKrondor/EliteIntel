package elite.intel.bindforge.io;

import elite.intel.bindforge.io.BindingsLoader.SplitChoice;
import elite.intel.bindforge.model.BindingSection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A split preset file - its four lines naming different binds files (Storage S13f): what the user is offered, and that
 * the file chosen is read while a line still names it, General-first otherwise.
 */
class BindingsLoaderSplitPresetTest {

    private static final String PRESET_FILE = "StartPreset.4.start";

    @TempDir
    Path tempDir;

    private Path bindingsDir;
    private Path controlSchemes;

    @BeforeEach
    void folders() throws IOException {
        bindingsDir = Files.createDirectory(tempDir.resolve("Bindings"));
        controlSchemes = Files.createDirectory(tempDir.resolve("ControlSchemes"));
    }

    @AfterEach
    void noChoiceLeft() {
        SplitPresetChoice.forget();
    }

    @Test
    void eachFileIsOfferedWithTheSectionsNamingItInTheFilesOrder() throws IOException {
        startPreset("Custom\r\nVKB\r\nCustom\r\nVKB");
        binds("Custom.4.2.binds");
        binds("VKB.4.2.binds");

        assertEquals(List.of(
                        new SplitChoice("Custom", List.of(BindingSection.GENERAL, BindingSection.SRV)),
                        new SplitChoice("VKB", List.of(BindingSection.SHIP, BindingSection.ON_FOOT))),
                loader(Optional.empty()).splitChoices(bindingsDir));
    }

    @Test
    void fourLinesNamingOneFileOfferNothing() throws IOException {
        startPreset("Custom\r\nCustom\r\nCustom\r\nCustom");
        binds("Custom.4.2.binds");

        assertEquals(List.of(), loader(Optional.empty()).splitChoices(bindingsDir));
    }

    /** Names that differ only in case are one file (storage.md, the case rule). */
    @Test
    void namesDifferingOnlyInCaseAreOneFile() throws IOException {
        startPreset("Custom\r\ncustom\r\nCUSTOM\r\nCustom");
        binds("Custom.4.2.binds");

        assertEquals(List.of(), loader(Optional.empty()).splitChoices(bindingsDir));
    }

    @Test
    void aLineWhoseFileIsNowhereIsNotOffered() throws IOException {
        startPreset("Custom\r\nGone\r\nCustom\r\nCustom");
        binds("Custom.4.2.binds");

        // WHY: with only one file to read, there is nothing to choose.
        assertEquals(List.of(), loader(Optional.empty()).splitChoices(bindingsDir));
    }

    @Test
    void aFactoryFileCountsAsALinesFile() throws IOException {
        startPreset("Custom\r\nKeyboardMouseOnly\r\nCustom\r\nCustom");
        binds("Custom.4.2.binds");
        Files.writeString(controlSchemes.resolve("KeyboardMouseOnly.binds"), "<Root/>", StandardCharsets.UTF_8);

        assertEquals(List.of("Custom", "KeyboardMouseOnly"),
                loader(Optional.empty()).splitChoices(bindingsDir).stream().map(SplitChoice::bindName).toList());
    }

    @Test
    void noPresetFileOffersNothing() throws IOException {
        binds("Custom.4.2.binds");

        assertEquals(List.of(), loader(Optional.empty()).splitChoices(bindingsDir));
    }

    @Test
    void theFileChosenIsReadInsteadOfGenerals() throws IOException {
        startPreset("Custom\r\nVKB\r\nVKB\r\nVKB");
        binds("Custom.4.2.binds");
        binds("VKB.4.2.binds");

        assertEquals("VKB.4.2.binds", loader(Optional.of("VKB")).bindsFileToRead(bindingsDir).getName());
    }

    @Test
    void aChoiceNoLineNamesIsIgnored() throws IOException {
        startPreset("Custom\r\nCustom\r\nCustom\r\nCustom");
        binds("Custom.4.2.binds");
        binds("VKB.4.2.binds");

        // WHY: the lines changed since the user chose - the choice answers a question no longer asked.
        assertEquals("Custom.4.2.binds", loader(Optional.of("VKB")).bindsFileToRead(bindingsDir).getName());
    }

    @Test
    void aChoiceInAnotherCaseIsTheSameFile() throws IOException {
        startPreset("Custom\r\nVKB\r\nVKB\r\nVKB");
        binds("Custom.4.2.binds");
        binds("VKB.4.2.binds");

        assertEquals("VKB.4.2.binds", loader(Optional.of("vkb")).bindsFileToRead(bindingsDir).getName());
    }

    /**
     * The warning at Elite-Intel start follows the choice: with another file chosen than General's, General is the
     * section whose keys the assistant cannot see (review of S13f, 2026-11-11).
     */
    @Test
    void withAFileChosenGeneralsLineIsTheSectionNotRead() throws IOException {
        startPreset("Custom\r\nVKB\r\nVKB\r\nVKB");
        binds("Custom.4.2.binds");
        binds("VKB.4.2.binds");
        BindingsLoader loader = loader(Optional.of("VKB"));

        File read = loader.bindsFileToRead(bindingsDir);

        assertEquals(Map.of(BindingSection.GENERAL, "Custom"), loader.sectionsNotIn(bindingsDir, read));
    }

    @Test
    void theRunsChoiceIsWhatTheDefaultLoaderReads() throws IOException {
        startPreset("Custom\r\nVKB\r\nVKB\r\nVKB");
        binds("Custom.4.2.binds");
        binds("VKB.4.2.binds");
        SplitPresetChoice.choose("VKB");

        assertEquals("VKB.4.2.binds", new BindingsLoader(List::of).bindsFileToRead(bindingsDir).getName());
    }

    private BindingsLoader loader(Optional<String> chosen) {
        return new BindingsLoader(() -> List.of(controlSchemes), () -> chosen);
    }

    private void startPreset(String text) throws IOException {
        Files.writeString(bindingsDir.resolve(PRESET_FILE), text, StandardCharsets.UTF_8);
    }

    private void binds(String name) throws IOException {
        Files.writeString(bindingsDir.resolve(name), "<Root/>", StandardCharsets.UTF_8);
    }
}
