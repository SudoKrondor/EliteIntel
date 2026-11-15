package elite.intel.ui.dialog;

import elite.intel.db.managers.QuietedNoticesManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A quieted notice is not shown (Storage S21, storage.md rule 13): APPLY's confirmation answers yes, so APPLY writes on
 * the press, and a quieted report is skipped. Only the quieted path is tested - it returns before any dialog is built,
 * so it runs headless; a notice not quieted opens a modal. Tests run with {@code java.awt.headless=true}
 * ({@code app/build.gradle}), so a dialog that was shown would throw {@code HeadlessException}.
 */
class HudConfirmDialogQuietTest {

    private final QuietedNoticesManager notices = QuietedNoticesManager.getInstance();

    @AfterEach
    void noneQuieted() {
        notices.resetAll();
    }

    @Test
    void aQuietedConfirmationAnswersYes() {
        notices.setQuieted(QuietableNotice.BINDFORGE_APPLY_CONFIRM.key(), true);

        assertTrue(HudConfirmDialog.confirmQuietable(null, QuietableNotice.BINDFORGE_APPLY_CONFIRM, "title",
                "question", "APPLY", "CANCEL"));
    }

    @Test
    void aQuietedReportIsNotShown() {
        notices.setQuieted(QuietableNotice.BINDFORGE_APPLY_REPORT.key(), true);

        assertDoesNotThrow(() -> HudConfirmDialog.infoQuietable(null, QuietableNotice.BINDFORGE_APPLY_REPORT, "title",
                "report", "CLOSE"));
    }
}
