package elite.intel.ui.support;

import elite.intel.bindforge.io.BindingSaveResult;
import elite.intel.ui.dialog.QuietableNotice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Only the Bind Editor's "Saved." can be quieted (Alan, 2026-11-14): "No changes." and every refusal always show
 * (storage.md rule 13).
 */
class BindingSaveResultPresenterTest {

    @Test
    void onlySavedCanBeQuieted() {
        for (BindingSaveResult result : BindingSaveResult.values()) {
            if (result == BindingSaveResult.SAVED) {
                assertEquals(QuietableNotice.BINDFORGE_ASSIGN_SAVED, BindingSaveResultPresenter.quietableFor(result));
            } else {
                assertNull(BindingSaveResultPresenter.quietableFor(result), result + " always shows");
            }
        }
    }
}
