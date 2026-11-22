package elite.intel.ui.screen.bindings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One refresh at a time, and a request made while one runs is never lost - the APPLY case the C5 review found: a
 * file-change refresh is running when APPLY finishes and asks for its own.
 */
class RefreshCoalescerTest {

    private final RefreshCoalescer refreshes = new RefreshCoalescer();

    @Test
    void aRequestWhileIdleStartsARefresh() {
        assertTrue(refreshes.tryStart());
    }

    @Test
    void aRequestWhileRunningDoesNotStartASecond() {
        refreshes.tryStart();

        assertFalse(refreshes.tryStart());
    }

    @Test
    void aRequestWhileRunningIsRunWhenItFinishes() {
        refreshes.tryStart();
        refreshes.tryStart();

        assertTrue(refreshes.finish(), "the request made mid-refresh was dropped");
    }

    @Test
    void manyRequestsWhileRunningAreOneMoreRefresh() {
        refreshes.tryStart();
        refreshes.tryStart();
        refreshes.tryStart();
        refreshes.tryStart();

        assertTrue(refreshes.finish());
        assertTrue(refreshes.tryStart(), "the follow-up refresh starts");
        assertFalse(refreshes.finish(), "and nothing is owed after it");
    }

    @Test
    void nothingAskedMeansNothingOwed() {
        refreshes.tryStart();

        assertFalse(refreshes.finish());
    }

    @Test
    void aRequestAfterFinishingStartsItsOwnRefresh() {
        refreshes.tryStart();
        refreshes.finish();

        assertTrue(refreshes.tryStart());
    }
}
