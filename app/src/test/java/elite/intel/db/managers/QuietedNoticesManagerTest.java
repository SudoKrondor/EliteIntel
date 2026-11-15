package elite.intel.db.managers;

import elite.intel.db.util.Database;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The notices the user chose not to see again (migration 12025; Alan, 2026-11-14, Storage S21): a tick and an untick
 * are each stored as they happen, and Reset warnings clears every one.
 */
class QuietedNoticesManagerTest {

    private final QuietedNoticesManager notices = QuietedNoticesManager.getInstance();

    @BeforeEach
    @AfterEach
    void noneQuieted() {
        try (Handle handle = Database.init()) {
            handle.execute("DELETE FROM quieted_notices");
        }
    }

    @Test
    void nothingIsQuietedUntilTicked() {
        assertFalse(notices.isQuieted("bindforge.apply.confirm"));
        assertFalse(notices.anyQuieted());
    }

    @Test
    void aTickQuietsOnlyItsNotice() {
        notices.setQuieted("bindforge.apply.confirm", true);

        assertTrue(notices.isQuieted("bindforge.apply.confirm"));
        assertFalse(notices.isQuieted("bindforge.apply.report"));
        assertTrue(notices.anyQuieted());
    }

    /** The user changed their mind in the same dialog: the untick is stored too. */
    @Test
    void anUntickShowsItAgain() {
        notices.setQuieted("bindforge.apply.confirm", true);
        notices.setQuieted("bindforge.apply.confirm", false);

        assertFalse(notices.isQuieted("bindforge.apply.confirm"));
        assertFalse(notices.anyQuieted());
    }

    @Test
    void tickingTwiceIsOneTick() {
        notices.setQuieted("bindforge.apply.report", true);
        notices.setQuieted("bindforge.apply.report", true);
        notices.setQuieted("bindforge.apply.report", false);

        assertFalse(notices.isQuieted("bindforge.apply.report"));
    }

    @Test
    void untickingANoticeNeverTickedIsAllowed() {
        notices.setQuieted("bindforge.apply.report", false);

        assertFalse(notices.anyQuieted());
    }

    @Test
    void resetWarningsShowsEveryNoticeAgain() {
        notices.setQuieted("bindforge.apply.confirm", true);
        notices.setQuieted("bindforge.apply.report", true);
        notices.setQuieted("bindforge.firstTimeStartup.report", true);

        notices.resetAll();

        assertFalse(notices.anyQuieted());
        assertFalse(notices.isQuieted("bindforge.apply.confirm"));
    }
}
