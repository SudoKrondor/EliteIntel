package elite.intel.session;

import elite.intel.db.dao.PlayerDao;
import elite.intel.db.util.Database;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A database that already holds a corrupt folder setting must not stop the application starting.
 *
 * <p>This is the reported failure rather than a hypothetical one. A commander's stored journal folder was
 * the Elite Dangerous directory with a quoted journal file appended - what a file chooser returns when a
 * quoted path is pasted into its file-name box. Every read of it threw {@code InvalidPathException}
 * wrapped as a DAO failure, which killed the pre-scan thread and then the Swing thread building the
 * settings panel. No window appeared, so the setting could not be corrected from inside the application
 * and the only remedy support could offer was deleting the database.
 *
 * <p>The fix has to hold for a value that is ALREADY stored: sanitising new choices does nothing for
 * someone who has one saved.
 */
class CorruptJournalFolderStartupTest {

    /**
     * Exactly the shape reported from the field, with NUL standing in for the double quote. A quote is
     * illegal in a Windows path and legal in a POSIX one, so it would only reproduce the crash on Windows;
     * NUL is illegal everywhere, which is what makes this test mean the same thing on the build server.
     */
    private static final String CORRUPT =
            "C:\\Users\\BigDaddy\\Saved Games\\Frontier Developments\\Elite Dangerous\\\u0000Journal.log";

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @AfterEach
    void clearTheSetting() {
        store(null, null);
    }

    @Test
    void aCorruptJournalFolderAlreadyInTheDatabaseStillYieldsAUsablePath() {
        store(CORRUPT, null);

        Path journal = assertDoesNotThrow(() -> PlayerSession.getInstance().getJournalPath(),
                "this threw, and took the settings window down with it");

        assertNotNull(journal);
        assertTrue(journal.toString().contains("Elite Dangerous") || journal.toString().contains("ed-journal"),
                "an unusable setting should fall back to this platform's usual folder, was: " + journal);
    }

    @Test
    void aCorruptBindingsFolderAlreadyInTheDatabaseStillYieldsAUsablePath() {
        store(null, CORRUPT);

        Path bindings = assertDoesNotThrow(() -> PlayerSession.getInstance().getBindingsDir());

        assertNotNull(bindings);
    }

    @Test
    void theSettingsPanelCanStillReadTheFolderToShowIt() {
        store(CORRUPT, CORRUPT);

        // The settings panel builds its fields from exactly these two calls. They ran on the Swing thread,
        // so throwing here is what stopped the window opening at all.
        assertDoesNotThrow(() -> {
            PlayerSession.getInstance().getJournalPath().toString();
            PlayerSession.getInstance().getBindingsDir().toString();
        });
    }

    @Test
    void aCorruptSettingCanBeReplacedByChoosingARealFolder(@org.junit.jupiter.api.io.TempDir Path good) {
        store(CORRUPT, null);

        assertTrue(PlayerSession.getInstance().setJournalPath(good.toString()),
                "a real folder must be accepted so the commander can dig themselves out");
        assertTrue(PlayerSession.getInstance().getJournalPath().endsWith(good.getFileName()));
    }

    @Test
    void anotherUnusableChoiceIsRefusedAndLeavesTheGoodOneAlone(@org.junit.jupiter.api.io.TempDir Path good) {
        PlayerSession.getInstance().setJournalPath(good.toString());

        assertTrue(!PlayerSession.getInstance().setJournalPath(CORRUPT),
                "an unusable choice must report failure rather than quietly store itself");
        assertTrue(PlayerSession.getInstance().getJournalPath().endsWith(good.getFileName()),
                "and must leave the working setting in place");
    }

    private static void store(String journal, String bindings) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setJournalDirectory(journal);
            player.setBindingsDirectory(bindings);
            dao.save(player);
            return null;
        });
    }
}
