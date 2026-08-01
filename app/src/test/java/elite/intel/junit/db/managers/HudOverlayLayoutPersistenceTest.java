package elite.intel.junit.db.managers;

import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The overlay layout is the one thing about the overlay the commander tunes by eye, against the game
 * behind it. Losing it on restart means doing that again every launch, so it lives in game_session
 * with the other app-wide settings rather than in a field.
 */
class HudOverlayLayoutPersistenceTest {

    private final SystemSession session = SystemSession.getInstance();

    private SystemSession.HudOverlayLayout previous;

    @BeforeEach
    void remember() {
        previous = session.getHudOverlayLayout();
    }

    @AfterEach
    void restore() {
        session.setHudOverlayLayout(previous);
    }

    @Test
    void aTunedLayoutIsReadBackWholeAndUnchanged() {
        SystemSession.HudOverlayLayout tuned = new SystemSession.HudOverlayLayout(0.42, 1.25, 900, 1920, 40);

        session.setHudOverlayLayout(tuned);

        assertEquals(tuned, session.getHudOverlayLayout());
    }

    /**
     * A window dragged to a second monitor left of the primary reports negative coordinates.
     */
    @Test
    void anOffPrimaryPositionSurvives() {
        session.setHudOverlayLayout(new SystemSession.HudOverlayLayout(0.25, 1.0, 760, -1200, 300));

        SystemSession.HudOverlayLayout stored = session.getHudOverlayLayout();
        assertEquals(-1200, stored.x());
        assertEquals(300, stored.y());
    }

    /**
     * Defaults have to mean "not chosen": a font scale of 0 is what tells the app to keep deriving
     * the size from screen height instead of pinning a 1080p size onto a 4K display.
     */
    @Test
    void theUnsetDefaultsAreDistinguishableFromChosenValues() {
        session.setHudOverlayLayout(new SystemSession.HudOverlayLayout(0.25, 0, 760, -1, -1));

        SystemSession.HudOverlayLayout stored = session.getHudOverlayLayout();
        assertEquals(0d, stored.fontScale(), "0 means the commander never chose a text size");
        assertEquals(-1, stored.x(), "-1 means the overlay opens wherever it defaults to");
    }
}
