package elite.intel.ai.brain.inference.lmstudio.install;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LmStudioWindowHiderTest {

    private static final String HOME = "C:\\Users\\Cmdr\\.lmstudio";

    @Test
    void aWindowOfAProgramInTheHomeIsLmStudios() {
        // The windows a Windows tester saw (2026-11-10).
        assertTrue(LmStudioWindowHider.isLmStudios("", Optional.of(HOME + "\\internal\\utilities\\node.exe"), HOME));
    }

    @Test
    void theProgramPathIsMatchedWhateverItsCase() {
        assertTrue(LmStudioWindowHider.isLmStudios("", Optional.of("c:\\users\\cmdr\\.LMSTUDIO\\bin\\lms.exe"), HOME));
    }

    @Test
    void aWindowsTerminalWindowIsMatchedByTheProgramPathInItsTitle() {
        assertTrue(LmStudioWindowHider.isLmStudios(HOME + "\\internal\\utilities\\node.exe",
                Optional.of("C:\\Program Files\\WindowsApps\\Microsoft.WindowsTerminal\\WindowsTerminal.exe"), HOME));
    }

    @Test
    void aTitleWithForwardSlashesStillNamesTheHome() {
        assertTrue(LmStudioWindowHider.isLmStudios("C:/Users/Cmdr/.lmstudio/internal/utilities/node.exe",
                Optional.empty(), HOME));
    }

    @Test
    void aFolderThatOnlyStartsLikeTheHomeIsNotIt() {
        assertFalse(LmStudioWindowHider.isLmStudios("", Optional.of("C:\\Users\\Cmdr\\.lmstudio-old\\node.exe"), HOME));
    }

    @Test
    void anyOtherWindowIsLeftAlone() {
        assertFalse(LmStudioWindowHider.isLmStudios("Command Prompt", Optional.of("C:\\Windows\\System32\\cmd.exe"), HOME));
        assertFalse(LmStudioWindowHider.isLmStudios(null, Optional.empty(), HOME));
    }
}
