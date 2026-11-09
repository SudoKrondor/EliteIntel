package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.util.OsDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LmStudioHomeTest {

    @Test
    void withoutAPointerTheHomeIsDotLmstudio(@TempDir Path userHome) {
        assertEquals(userHome.resolve(".lmstudio"), LmStudioHome.locate(userHome).dir());
    }

    @Test
    void thePointerNamesTheHome(@TempDir Path userHome) throws IOException {
        Path elsewhere = userHome.resolve("games").resolve("LMStudio");
        Files.writeString(userHome.resolve(".lmstudio-home-pointer"), elsewhere + "\n");

        assertEquals(elsewhere, LmStudioHome.locate(userHome).dir());
    }

    @Test
    void anEmptyPointerFallsBackToTheDefault(@TempDir Path userHome) throws IOException {
        Files.writeString(userHome.resolve(".lmstudio-home-pointer"), "  \n");

        assertEquals(userHome.resolve(".lmstudio"), LmStudioHome.locate(userHome).dir());
    }

    @Test
    void aWrittenPointerIsReadBack(@TempDir Path userHome) throws IOException {
        Path elsewhere = userHome.resolve("games").resolve("LMStudio");

        LmStudioHome.writePointer(userHome, elsewhere);

        assertEquals(elsewhere.toString(), Files.readString(userHome.resolve(".lmstudio-home-pointer")),
                "plain path, no newline, as LM Studio's installers read it");
        assertEquals(elsewhere, LmStudioHome.locate(userHome).dir());
    }

    @Test
    void lmsIsFoundOnlyWhenItIsInBin(@TempDir Path userHome) throws IOException {
        LmStudioHome home = LmStudioHome.locate(userHome);
        assertFalse(home.lmsCommand().isPresent());

        Path bin = Files.createDirectories(home.dir().resolve("bin"));
        Path lms = Files.createFile(bin.resolve(OsDetector.getOs() == OsDetector.OS.WINDOWS ? "lms.exe" : "lms"));

        assertTrue(home.lmsCommand().isPresent());
        assertEquals(lms, home.lmsCommand().get());
    }
}
