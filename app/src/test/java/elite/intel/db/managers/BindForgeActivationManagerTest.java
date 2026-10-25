package elite.intel.db.managers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The "first-time startup complete" flag (storage.md, <em>First-time startup</em>; Storage S2b, 2026-10-25).
 */
class BindForgeActivationManagerTest {

    private final BindForgeActivationManager flag = BindForgeActivationManager.getInstance();

    @BeforeEach
    @AfterEach
    void notComplete() {
        flag.clearFirstTimeStartup();
    }

    @Test
    void theFlagStartsNotComplete() {
        assertFalse(flag.isFirstTimeStartupComplete());
    }

    @Test
    void markingItCompleteIsRead() {
        flag.markFirstTimeStartupComplete();

        assertTrue(flag.isFirstTimeStartupComplete());
    }

    @Test
    void markingItTwiceKeepsItComplete() {
        flag.markFirstTimeStartupComplete();
        flag.markFirstTimeStartupComplete();

        assertTrue(flag.isFirstTimeStartupComplete());
    }

    /** Run first-time startup again clears it, so the next click on the tab runs first-time startup. */
    @Test
    void clearingItMakesItNotComplete() {
        flag.markFirstTimeStartupComplete();

        flag.clearFirstTimeStartup();

        assertFalse(flag.isFirstTimeStartupComplete());
    }
}
