package elite.intel.db.util;

import elite.intel.session.SystemSession;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The overlay has no checkbox: the Show/Hide button is its only control, and this column is the whole
 * of its memory. A commander who flies with the overlay up must find it up next launch, and one who
 * has never turned it on must keep the hidden start every build so far has had. Both depend on the
 * column surviving the DAO's INSERT OR REPLACE, which rewrites every column of the row.
 */
class OverlayVisibleColumnTest {

    @AfterEach
    void restoreHiddenOverlay() {
        SystemSession.getInstance().setHudOverlayVisible(false);
    }

    @Test
    void migrationLeavesEveryInstallationStartingHidden() throws IOException {
        try (Handle handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            handle.execute("CREATE TABLE game_session (id INTEGER PRIMARY KEY)");
            handle.execute("INSERT INTO game_session (id) VALUES (1)");

            for (String statement : loadMigration().split(";")) {
                if (!statement.isBlank()) {
                    handle.execute(statement);
                }
            }

            assertFalse(overlayVisible(handle, 1), "an existing commander keeps the hidden start they have today");
            handle.execute("INSERT INTO game_session (id) VALUES (2)");
            assertFalse(overlayVisible(handle, 2), "a fresh install starts hidden");
        }
    }

    @Test
    void anOverlayLeftOnComesBackOn() {
        SystemSession session = SystemSession.getInstance();
        assertFalse(session.isHudOverlayVisible(), "the test database starts with the overlay hidden");

        session.setHudOverlayVisible(true);
        assertTrue(session.isHudOverlayVisible());

        // Any setter goes through the same load-mutate-save cycle; if the column were missing from the
        // DAO's statement this would quietly snap the overlay back to hidden.
        boolean noiseReduction = session.isNoiseReductionEnabled();
        session.setNoiseReductionEnabled(!noiseReduction);
        try {
            assertTrue(session.isHudOverlayVisible(), "saving an unrelated setting must not hide the overlay");
        } finally {
            session.setNoiseReductionEnabled(noiseReduction);
        }
    }

    @Test
    void aLayoutSaveDoesNotDecideWhetherTheOverlayExists() {
        // Dragging or resizing the overlay rewrites the whole layout record. Visibility is deliberately
        // not part of it, so a layout save cannot turn the overlay off underneath the commander.
        SystemSession session = SystemSession.getInstance();
        session.setHudOverlayVisible(true);

        SystemSession.HudOverlayLayout layout = session.getHudOverlayLayout();
        session.setHudOverlayLayout(new SystemSession.HudOverlayLayout(
                layout.alpha(), layout.fontScale(), layout.width(), 640, 480,
                layout.displayMode(), layout.vrPosition()));

        assertTrue(session.isHudOverlayVisible(), "moving the overlay must not hide it");
        assertEquals(640, session.getHudOverlayLayout().x(), "the layout save itself must still land");
    }

    private static boolean overlayVisible(Handle handle, int id) {
        return handle.createQuery("SELECT overlayVisible FROM game_session WHERE id = :id")
                .bind("id", id)
                .mapTo(Boolean.class)
                .one();
    }

    private static String loadMigration() throws IOException {
        try (InputStream input = OverlayVisibleColumnTest.class.getResourceAsStream(
                "/db-migration/01039__overlay_visible.sql")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
