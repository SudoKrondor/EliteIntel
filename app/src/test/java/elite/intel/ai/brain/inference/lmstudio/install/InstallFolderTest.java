package elite.intel.ai.brain.inference.lmstudio.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class InstallFolderTest {

    @Test
    void aChosenFolderGetsAnLmStudioFolderOfItsOwn(@TempDir Path chosen) {
        InstallFolder folder = InstallFolder.inside(chosen);

        assertEquals(chosen.resolve("LMStudio"), folder.home(), "uninstall deletes the home, never the chosen folder");
        assertTrue(folder.writesPointer());
    }

    @Test
    void freeSpaceIsAskedOfTheNearestFolderThatExists(@TempDir Path chosen) throws IOException {
        InstallFolder folder = new InstallFolder(chosen.resolve("not").resolve("yet").resolve("LMStudio"), true);

        assertEquals(Files.getFileStore(chosen).getUsableSpace(), folder.usableBytes(), 64L * 1024 * 1024);
    }
}
