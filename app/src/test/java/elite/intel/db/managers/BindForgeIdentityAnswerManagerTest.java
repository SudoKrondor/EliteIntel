package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeIdentityAnswerDao.AnswerRow;
import elite.intel.db.util.Database;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The identity question's remembered answers (migration 12007, Alan, 2026-10-15, A12).
 */
class BindForgeIdentityAnswerManagerTest {

    private final BindForgeIdentityAnswerManager answers = BindForgeIdentityAnswerManager.getInstance();

    @BeforeEach
    @AfterEach
    void noAnswers() {
        noIdentityAnswers();
    }

    @Test
    void anAnswerIsRecordedForItsDevice() {
        answers.put("LVWAP", "3344:83F4", "NOT_NOW");

        assertEquals(List.of(new AnswerRow("LVWAP", "3344:83F4", "NOT_NOW")), answers.findAll());
    }

    /** One row per missing device: the latest answer is the one that holds. */
    @Test
    void aLaterAnswerReplacesTheOneBefore() {
        answers.put("LVWAP", "3344:83F4", "NOT_NOW");
        answers.put("lvwap", "3344:03F5,3344:83F4", "NONE_OF_THESE");

        List<AnswerRow> rows = answers.findAll();
        assertEquals(1, rows.size(), "the name is compared ignoring case, as the game compares it");
        assertEquals("3344:03F5,3344:83F4", rows.getFirst().candidates());
        assertEquals("NONE_OF_THESE", rows.getFirst().answer());
    }

    @Test
    void forgettingRemovesOnlyTheDevicesNamed() {
        answers.put("LVWAP", "3344:83F4", "NOT_NOW");
        answers.put("RVWAP", "3344:83F4", "NONE_OF_THESE");

        answers.forget(Set.of("LVWAP"));

        assertEquals(List.of("RVWAP"), answers.findAll().stream().map(AnswerRow::deviceName).toList());
    }

    @Test
    void forgettingNothingIsAllowed() {
        answers.forget(Set.of());

        assertTrue(answers.findAll().isEmpty());
    }

    @Test
    void onlyTheTwoAnswersAreStored() {
        assertThrows(RuntimeException.class, () -> answers.put("LVWAP", "3344:83F4", "YES"));
    }

    static void noIdentityAnswers() {
        try (Handle handle = Database.init()) {
            handle.execute("DELETE FROM bindforge_identity_answer");
        }
    }
}
