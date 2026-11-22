package elite.intel.ai.brain.inference.lmstudio.install;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Taking LM Studio's {@code bin} folder out of the Windows user PATH, as smoke testing saw it added: appended after
 * the PATH's own trailing semicolon, so {@code ...WindowsApps;;D:\LMStudio\bin}.
 */
class UserPathTest {

    private static final Path BIN = Path.of("D:\\LMStudio\\bin");

    @Test
    void theEntryLmStudioAddedComesOutAndThePathIsAsItWas() {
        String before = "C:\\Users\\runneradmin\\.dotnet\\tools;C:\\Users\\runneradmin\\AppData\\Local\\Microsoft\\WindowsApps;";

        assertEquals(before, UserPath.withoutEntry(before + ";D:\\LMStudio\\bin", BIN));
    }

    @Test
    void caseAndATrailingBackslashDoNotHideTheEntry() {
        assertEquals("C:\\tools", UserPath.withoutEntry("C:\\tools;d:\\lmstudio\\BIN\\", BIN));
    }

    @Test
    void everyCopyOfTheEntryComesOut() {
        assertEquals("C:\\tools", UserPath.withoutEntry("D:\\LMStudio\\bin;C:\\tools;D:\\LMStudio\\bin", BIN));
    }

    @Test
    void everyOtherEntryIsKeptExactlyIncludingUnexpandedVariables() {
        String path = "%USERPROFILE%\\AppData\\Local\\Microsoft\\WindowsApps;;C:\\Program Files\\Git\\cmd";

        assertEquals(path, UserPath.withoutEntry(path, BIN));
    }
}
