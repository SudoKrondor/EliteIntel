package elite.intel.bindforge.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

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
}
