package elite.intel.bindforge.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The four-section read beside {@link BindingsLoader#getActivePresetName()}: which {@code StartPreset} file it
 * picks, and that the existing one-line answer is untouched.
 */
class BindingsLoaderStartPresetTest {

    @TempDir
    Path bindingsDir;

    private final BindingsLoader loader = new BindingsLoader();

    @Test
    void readsTheFourLineSpecimenFromTheFolder() throws IOException {
        copySpecimen("StartPreset.4.start");

        Optional<StartPresetSections> sections = loader.readStartPresetSections(bindingsDir);

        assertEquals(Optional.of(new StartPresetSections("KeyboardMouseOnly", "Custom", "Custom", "Custom", false)),
                sections);
    }

    @Test
    void numberedFileWinsOverThePreOdysseyFileWhenBothExist() throws IOException {
        copySpecimen("StartPreset.start");
        copySpecimen("StartPreset.4.start");

        assertEquals(Optional.of(bindingsDir.resolve("StartPreset.4.start")), loader.findLiveStartPresetFile(bindingsDir));
        assertEquals("KeyboardMouseOnly", loader.readStartPresetSections(bindingsDir).orElseThrow().general());
    }

    @Test
    void highestNumberWinsByValueNotByName() throws IOException {
        write("StartPreset.4.start", "Four");
        write("StartPreset.10.start", "Ten");

        assertEquals(Optional.of(bindingsDir.resolve("StartPreset.10.start")), loader.findLiveStartPresetFile(bindingsDir));
    }

    @Test
    void equalGenerationsAreBrokenByNameNotByListingOrder() throws IOException {
        write("StartPreset.04.start", "Padded");
        write("StartPreset.4.start", "Plain");

        assertEquals(Optional.of(bindingsDir.resolve("StartPreset.4.start")), loader.findLiveStartPresetFile(bindingsDir));
    }

    @Test
    void preOdysseyFileAloneIsReadAsOnePresetForAllFour() throws IOException {
        copySpecimen("StartPreset.start");

        Optional<StartPresetSections> sections = loader.readStartPresetSections(bindingsDir);

        assertEquals(Optional.of(new StartPresetSections("Custom", "Custom", "Custom", "Custom", true)), sections);
        assertTrue(sections.orElseThrow().singleLine());
    }

    @Test
    void unnumberedLookalikeIsPassedOver() throws IOException {
        write("StartPreset.backup.start", "Wrong");
        write("StartPreset.start", "Right");

        assertEquals(Optional.of(bindingsDir.resolve("StartPreset.start")), loader.findLiveStartPresetFile(bindingsDir));
    }

    @Test
    void noStartPresetIsEmptyNotAnError() throws IOException {
        write("Custom.4.2.binds", "<Root/>");

        assertTrue(loader.findLiveStartPresetFile(bindingsDir).isEmpty());
        assertTrue(loader.readStartPresetSections(bindingsDir).isEmpty());
    }

    @Test
    void missingFolderIsAnError() {
        Path missing = bindingsDir.resolve("not-there");

        assertThrows(IOException.class, () -> loader.readStartPresetSections(missing));
    }

    @Test
    void existingSingleAnswerStillTakesTheFirstNonEmptyLine() throws IOException {
        copySpecimen("StartPreset.4.start");
        write("KeyboardMouseOnly.binds", "<Root/>");
        write("Custom.4.2.binds", "<Root/>");

        // getLatestBindsFile resolves through the unchanged one-line read: General's preset, not Ship's.
        assertEquals("KeyboardMouseOnly.binds", loader.getLatestBindsFile(bindingsDir).getName());
    }

    private void copySpecimen(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/bindforge/startpreset/" + name)) {
            assertNotNull(in, "missing test resource " + name);
            Files.write(bindingsDir.resolve(name), in.readAllBytes());
        }
    }

    private void write(String name, String content) throws IOException {
        Files.writeString(bindingsDir.resolve(name), content, StandardCharsets.UTF_8);
    }
}
