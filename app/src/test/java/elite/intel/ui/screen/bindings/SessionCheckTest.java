package elite.intel.ui.screen.bindings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The check at the BindForge click: once a session, and again after the bindings folder changes (Storage S17). */
class SessionCheckTest {

    @Test
    void theCheckRunsOnceUntilTheFolderChanges() {
        SessionCheck session = new SessionCheck();
        assertFalse(session.checked());
        assertTrue(session.start());
        assertTrue(session.checked());
        // WHY: a second click - and Not now, which leaves the flag set (S17 Q3) - runs nothing.
        assertFalse(session.start());
    }

    @Test
    void aChangeInTheFolderLetsTheNextClickRunItAgain() {
        SessionCheck session = new SessionCheck();
        session.start();
        session.again();
        assertFalse(session.checked());
        assertTrue(session.start());
        assertFalse(session.start());
    }

    @Test
    void aChangeBeforeTheFirstClickChangesNothing() {
        SessionCheck session = new SessionCheck();
        session.again();
        assertTrue(session.start());
    }
}
