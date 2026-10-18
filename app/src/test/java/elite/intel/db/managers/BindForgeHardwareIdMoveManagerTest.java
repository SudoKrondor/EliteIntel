package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeHardwareIdMoveDao.MoveRow;
import elite.intel.db.util.Database;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bindings waiting to move from a controller's old hardware id to its name (migration 12008, Alan, 2026-10-17, A17).
 */
class BindForgeHardwareIdMoveManagerTest {

    private final BindForgeHardwareIdMoveManager moves = BindForgeHardwareIdMoveManager.getInstance();

    @BeforeEach
    @AfterEach
    void nothingWaiting() {
        noMoves();
    }

    @Test
    void anOldIdIsRecordedForTheControllerUpperCase() {
        moves.put("3344", "83f4", "3344", "83f3");

        assertEquals(List.of(new MoveRow("3344", "83F4", "3344", "83F3")), moves.findAll());
    }

    /** One record per controller: a later yes for it replaces the old id it holds. */
    @Test
    void aLaterOldIdReplacesTheOneBefore() {
        moves.put("3344", "83F4", "3344", "83F3");
        moves.put("3344", "83F4", "3344", "43F4");

        assertEquals(List.of(new MoveRow("3344", "83F4", "3344", "43F4")), moves.findAll());
    }

    /** One old id is one controller's: given to another, it leaves the first. */
    @Test
    void anOldIdGivenToAnotherControllerLeavesTheFirst() {
        moves.put("3344", "83F4", "3344", "83F3");
        moves.put("3344", "03F5", "3344", "83f3");

        assertEquals(List.of(new MoveRow("3344", "03F5", "3344", "83F3")), moves.findAll());
    }

    @Test
    void forgettingRemovesOnlyTheControllersNamed() {
        moves.put("3344", "83F4", "3344", "83F3");
        moves.put("3344", "03F5", "3344", "43F4");

        moves.forget(List.of(new MoveRow("3344", "83f4", "3344", "83F3")));

        assertEquals(List.of(new MoveRow("3344", "03F5", "3344", "43F4")), moves.findAll());
    }

    @Test
    void forgettingNothingIsAllowed() {
        moves.forget(List.of());

        assertTrue(moves.findAll().isEmpty());
    }

    static void noMoves() {
        try (Handle handle = Database.init()) {
            handle.execute("DELETE FROM bindforge_hardware_id_move");
        }
    }
}
