package elite.intel.junit.db.managers;

import elite.intel.session.SystemSession;
import elite.intel.ui.overlay.OverlayProtocol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
        SystemSession.HudOverlayLayout tuned = new SystemSession.HudOverlayLayout(0.42, 1.25, 900, 1920, 40, "BOTH", "TOP_RIGHT");

        session.setHudOverlayLayout(tuned);

        assertEquals(tuned, session.getHudOverlayLayout());
    }

    /**
     * A window dragged to a second monitor left of the primary reports negative coordinates.
     */
    @Test
    void anOffPrimaryPositionSurvives() {
        session.setHudOverlayLayout(new SystemSession.HudOverlayLayout(0.25, 1.0, 760, -1200, 300, "DESKTOP", "BOTTOM"));

        SystemSession.HudOverlayLayout stored = session.getHudOverlayLayout();
        assertEquals(-1200, stored.x());
        assertEquals(300, stored.y());
    }

    /**
     * Defaults have to mean "not chosen": a font scale of 0 is what tells the app to keep deriving
     * the size from screen height instead of pinning a 1080p size onto a 4K display.
     * <p>
     * The position sentinel is deliberately not -1. That is an ordinary coordinate now: while it meant
     * "unset", an overlay parked just past the left edge of the screen was restored centred, because
     * nothing could tell the commander's coordinate from the absence of one.
     */
    @Test
    void theUnsetDefaultsAreDistinguishableFromChosenValues() {
        session.setHudOverlayLayout(new SystemSession.HudOverlayLayout(
                0.25, 0, 760, OverlayProtocol.POSITION_UNSET, OverlayProtocol.POSITION_UNSET, "DESKTOP", "BOTTOM"));

        SystemSession.HudOverlayLayout stored = session.getHudOverlayLayout();
        assertEquals(0d, stored.fontScale(), "0 means the commander never chose a text size");
        assertEquals(OverlayProtocol.POSITION_UNSET, stored.x(),
                "the sentinel means the overlay opens wherever it defaults to");
    }

    /**
     * The reported bug, as a row: a card nudged three pixels past the left edge came back centred on
     * every launch, its y remembered and its x silently discarded as if it had never been chosen.
     */
    @Test
    void aPositionJustPastTheLeftEdgeIsNotMistakenForUnset() {
        session.setHudOverlayLayout(new SystemSession.HudOverlayLayout(0.5, 1.0, 760, -3, 590, "DESKTOP", "TOP_RIGHT"));

        SystemSession.HudOverlayLayout stored = session.getHudOverlayLayout();
        assertEquals(-3, stored.x());
        assertNotEquals(OverlayProtocol.POSITION_UNSET, stored.x(),
                "a real coordinate must never collide with the sentinel");
    }

    /**
     * Where the HUD is drawn is the one overlay setting a commander cannot re-tune by eye: a VR
     * commander who has to re-pick "headset" on every launch would reasonably conclude the feature
     * does not work.
     */
    @Test
    void theChosenDisplayModeSurvivesARestart() {
        session.setHudOverlayLayout(new SystemSession.HudOverlayLayout(0.25, 1.0, 760, -1, -1, "VR", "BOTTOM"));

        assertEquals("VR", session.getHudOverlayLayout().displayMode());
    }

    /**
     * An installation upgrading into the migration has no stored mode, and the column default has to
     * be the desktop overlay - anything else would move a flat-screen commander's HUD into a headset
     * they do not own on the strength of a schema change.
     */
    @Test
    void theColumnDefaultIsTheDesktopOverlay() {
        assertEquals("DESKTOP", previous.displayMode(),
                "the value a row carries before anyone chooses a mode");
    }

    /**
     * In the headset there is no window to drag, so the chosen direction is the only record of where
     * the commander put the card - and re-picking it every launch is done wearing a headset that
     * cannot see this dialog.
     */
    @Test
    void theChosenVrPlacementSurvivesARestart() {
        session.setHudOverlayLayout(
                new SystemSession.HudOverlayLayout(0.25, 1.0, 760, -1, -1, "VR", "TOP_LEFT"));

        assertEquals("TOP_LEFT", session.getHudOverlayLayout().vrPosition());
    }

    /**
     * An installation upgrading into the migration has no stored placement, and the column default
     * has to be where the VR overlay already drew - below centre - so nobody's HUD moves on the
     * strength of a schema change.
     */
    @Test
    void theColumnDefaultIsWhereTheVrOverlayAlreadyDrew() {
        assertEquals("BOTTOM", previous.vrPosition(),
                "the value a row carries before anyone chooses a placement");
    }
}
