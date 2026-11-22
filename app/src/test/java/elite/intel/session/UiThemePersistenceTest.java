package elite.intel.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiThemePersistenceTest {

    private final SystemSession session = SystemSession.getInstance();

    @AfterEach
    void restoreDefaultTheme() {
        session.setUiTheme("");
    }

    @Test
    void chosenThemeRoundTripsThroughTheSessionDatabase() {
        session.setUiTheme("night-ops.json");

        assertEquals("night-ops.json", session.getUiTheme());
    }

    /**
     * Every setting is saved by replacing the whole game_session row, so a column left out of that save would be
     * wiped back to Default by an unrelated change.
     */
    @Test
    void savingAnotherSettingKeepsTheChosenTheme() {
        session.setUiTheme("night-ops.json");

        session.setRmsThresholdLow(session.getRmsThresholdLow());

        assertEquals("night-ops.json", session.getUiTheme());
    }
}
