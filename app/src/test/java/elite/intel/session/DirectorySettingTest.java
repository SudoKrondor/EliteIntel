package elite.intel.session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The folder settings, against the value that took a commander's installation down.
 *
 * <p>Their stored journal folder was the Elite Dangerous directory with a quoted journal file appended to
 * it - what {@code JFileChooser} returns when a quoted path is pasted into its file-name box. Reading it
 * threw, which killed the pre-scan thread and then the thread building the settings panel, so no window
 * ever appeared and the one screen that could have fixed the setting was the one that could not open.
 *
 * <p>Note the illegal character differs by platform: a double quote is illegal in a Windows path and
 * perfectly legal in a POSIX one, so the tests that must fail parsing everywhere use NUL instead.
 */
class DirectorySettingTest {

    private static final Path FALLBACK = Path.of("/fallback/journal");

    // ---------------------------------------------------------------- reading

    @Test
    void anUnreadableSettingFallsBackInsteadOfThrowing() {
        // NUL cannot appear in a path on any platform, so this is the portable stand-in for the quote that
        // broke the Windows installation.
        assertEquals(FALLBACK, DirectorySetting.resolve("C:\\Elite\u0000Dangerous", FALLBACK),
                "a stored value that cannot be parsed must not be allowed to throw");
    }

    @Test
    void nothingStoredMeansThePlatformDefault() {
        assertEquals(FALLBACK, DirectorySetting.resolve(null, FALLBACK));
        assertEquals(FALLBACK, DirectorySetting.resolve("   ", FALLBACK));
    }

    @Test
    void aFolderThatIsNotThereRightNowIsStillTheCommandersSetting(@TempDir Path root) {
        Path absent = root.resolve("drive-not-mounted-yet");

        assertEquals(absent, DirectorySetting.resolve(absent.toString(), FALLBACK),
                "an unmounted drive is still the configured folder - the validator warns, it is not replaced");
    }

    // ---------------------------------------------------------------- writing

    @Test
    void anOrdinaryFolderChoiceIsStoredAsChosen(@TempDir Path root) {
        assertEquals(root.toString(), sanitized(root.toString()));
    }

    @Test
    void choosingAJournalFileStoresTheFolderHoldingIt(@TempDir Path root) throws IOException {
        Path journal = Files.writeString(root.resolve("Journal.2026-08-30T233705.01.log"), "{}");

        assertEquals(root.toString(), sanitized(journal.toString()),
                "picking a journal when asked for the journal folder has an obvious intention");
    }

    /**
     * The reported failure, reproduced in the shape a chooser produces it: the folder on show, then a
     * quoted absolute path pasted into the file-name box.
     */
    @Test
    void aQuotedPathPastedIntoTheChooserIsRecoveredRatherThanStored(@TempDir Path root) throws IOException {
        Path journal = Files.writeString(root.resolve("Journal.2026-08-30T233705.01.log"), "{}");
        String asChooserReturnsIt = root + separator() + "\"" + journal + "\"";

        assertEquals(root.toString(), sanitized(asChooserReturnsIt),
                "the commander plainly meant that journal's folder, and it is recoverable from the mess");
    }

    @Test
    void aQuotedFolderIsAccepted(@TempDir Path root) {
        assertEquals(root.toString(), sanitized("\"" + root + "\""));
    }

    @Test
    void aChoiceThatIsNoFolderAtAllIsRefusedRatherThanStored(@TempDir Path root) {
        assertTrue(DirectorySetting.sanitize(root.resolve("no-such-folder").toString()).isEmpty(),
                "storing this is what made the application unable to start");
        assertTrue(DirectorySetting.sanitize("C:\\Elite\u0000Dangerous").isEmpty());
        assertTrue(DirectorySetting.sanitize(null).isEmpty());
        assertTrue(DirectorySetting.sanitize("  ").isEmpty());
    }

    @Test
    void whatIsStoredCanAlwaysBeReadBack(@TempDir Path root) throws IOException {
        // The invariant that keeps the application startable: a write only ever stores something a read
        // survives, so no setting the commander can choose leaves them unable to open the window.
        Path journal = Files.writeString(root.resolve("Journal.log"), "{}");
        for (String choice : new String[]{root.toString(), journal.toString(),
                "\"" + root + "\"", root + separator() + "\"" + journal + "\""}) {
            Optional<String> stored = DirectorySetting.sanitize(choice);
            assertTrue(stored.isPresent(), "should have been usable: " + choice);
            Path readBack = DirectorySetting.resolve(stored.get(), FALLBACK);
            assertNotNull(readBack);
            assertEquals(root, readBack, "stored then read back should be the folder, for: " + choice);
        }
    }

    private static String sanitized(String choice) {
        return DirectorySetting.sanitize(choice)
                .orElseThrow(() -> new AssertionError("should have been usable: " + choice));
    }

    private static String separator() {
        return java.io.File.separator;
    }
}
