package elite.intel.junit.gameapi;

import elite.intel.gameapi.i18n.EventsTextProvider;
import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A surface scan says how many of each signal it found - 29 mining locations and 2 geological sites on one
 * body - and which of those is worth the detour is the question the report exists to answer. The payload
 * used to carry only the type, so the count never reached the commander in any language.
 */
class SignalReportCountTest {

    @Test
    void everyLanguageReportsBothTheTypeAndHowManyWereFound() {
        for (Language language : Language.values()) {
            String line = EventsTextProvider.getText(language, "event.signals.type", "Planetary Mining Location", 29);
            assertTrue(line.contains("Planetary Mining Location"), language + ": " + line);
            assertTrue(line.contains("29"), language + ": lost the count -> " + line);
        }
    }
}
