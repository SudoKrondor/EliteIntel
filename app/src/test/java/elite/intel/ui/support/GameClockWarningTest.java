package elite.intel.ui.support;

import elite.intel.i18n.Language;
import elite.intel.ui.event.GameClockDisagreementEvent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game-clock dialog in every language: the amount lands in the sentence, a whole-hour gap gets the time-zone
 * cause and steps and a drift only the drift cause and the sync, and the body - the one line run through
 * MessageFormat - keeps its apostrophes.
 */
class GameClockWarningTest {

    @ParameterizedTest
    @EnumSource(Language.class)
    void aWholeHourGapGetsTheTimeZoneSteps(Language language) {
        String text = GameClockWarning.message(new GameClockDisagreementEvent(Duration.ofSeconds(3601), 1), language);

        assertRendered(text);
        assertTrue(text.contains("1 "), text);
        assertTrue(text.contains("5."), "time-zone steps run to five: " + text);
        assertTrue(text.contains(getText(language, "gameClock.dialog.cause.timeZone")), text);
        assertFalse(text.contains(getText(language, "gameClock.dialog.cause.drift")), text);
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void aDriftGetsOnlyTheSync(Language language) {
        String text = GameClockWarning.message(new GameClockDisagreementEvent(Duration.ofSeconds(-185), 0), language);

        assertRendered(text);
        assertTrue(text.contains("3 "), text);
        assertFalse(text.contains("5."), "sync steps stop at four: " + text);
        assertTrue(text.contains(getText(language, "gameClock.dialog.cause.drift")), text);
        assertFalse(text.contains(getText(language, "gameClock.dialog.cause.timeZone")), text);
    }

    @ParameterizedTest
    @EnumSource(value = Language.class, names = {"FR", "IT"})
    void apostrophesSurviveTheFormatting(Language language) {
        String text = GameClockWarning.message(new GameClockDisagreementEvent(Duration.ofSeconds(3601), 1), language);

        assertTrue(text.contains("d'accord"), text);
        assertFalse(text.contains("''"), text);
    }

    private static void assertRendered(String text) {
        assertFalse(text.contains("{"), "placeholder left unformatted: " + text);
        assertFalse(text.contains("gameClock."), "missing key: " + text);
    }
}
