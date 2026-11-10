package elite.intel.ai.brain.inference.lmstudio.install;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.util.OsDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

    @Test
    void lmsWithAnInstallRecordNamingAProgramThatIsThereIsInstalled(@TempDir Path userHome) throws IOException {
        LmStudioHome home = LmStudioHome.locate(userHome);
        Path lms = createLms(home);
        Path llmster = Files.createFile(Files.createDirectories(home.dir().resolve("llmster")).resolve("llmster"));
        writeRecord(home.dir(), "llmster-install-location.json", llmster);

        assertEquals(lms, home.installedLms().orElseThrow());
        assertEquals(List.of(llmster), home.installedPrograms());
    }

    @Test
    void theDesktopAppsRecordCountsToo(@TempDir Path userHome) throws IOException {
        LmStudioHome home = LmStudioHome.locate(userHome);
        createLms(home);
        Path app = Files.createFile(userHome.resolve("LM Studio.exe"));
        writeRecord(home.dir(), "app-install-location.json", app);

        assertTrue(home.installedLms().isPresent());
    }

    @Test
    void lmsLeftBehindByAnUninstallIsNotAnInstall(@TempDir Path userHome) throws IOException {
        LmStudioHome home = LmStudioHome.locate(userHome);
        createLms(home);
        assertFalse(home.installedLms().isPresent(), "no install record at all");

        writeRecord(home.dir(), "app-install-location.json", userHome.resolve("Programs").resolve("LM Studio.exe"));
        assertFalse(home.installedLms().isPresent(), "the record names a program the uninstaller removed");
        assertTrue(home.installedPrograms().isEmpty());
    }

    @Test
    void anUnreadableInstallRecordIsNoInstall(@TempDir Path userHome) throws IOException {
        LmStudioHome home = LmStudioHome.locate(userHome);
        createLms(home);
        Path internal = Files.createDirectories(home.dir().resolve(".internal"));
        Files.writeString(internal.resolve("llmster-install-location.json"), "not json {");
        Files.writeString(internal.resolve("app-install-location.json"), "[\"a list\"]");

        assertFalse(home.installedLms().isPresent());
    }

    @Test
    void anInstallRecordWithoutLmsIsNoInstall(@TempDir Path userHome) throws IOException {
        LmStudioHome home = LmStudioHome.locate(userHome);
        Path app = Files.createFile(userHome.resolve("LM Studio.exe"));
        writeRecord(home.dir(), "app-install-location.json", app);

        assertFalse(home.installedLms().isPresent());
    }

    private static Path createLms(LmStudioHome home) throws IOException {
        Path bin = Files.createDirectories(home.dir().resolve("bin"));
        return Files.createFile(bin.resolve(OsDetector.getOs() == OsDetector.OS.WINDOWS ? "lms.exe" : "lms"));
    }

    /**
     * An install record as LM Studio writes it: {@code {"path":...,"argv":[],"cwd":...}}.
     */
    static void writeRecord(Path home, String name, Path program) throws IOException {
        JsonObject record = new JsonObject();
        record.addProperty("path", program.toString());
        record.add("argv", new JsonArray());
        record.addProperty("cwd", String.valueOf(program.getParent()));
        Path internal = Files.createDirectories(home.resolve(".internal"));
        Files.writeString(internal.resolve(name), record.toString());
    }
}
