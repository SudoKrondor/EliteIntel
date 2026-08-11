package elite.intel.ui.i18n;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Figures on the HUD are read, not parsed, so they belong to the commander's language rather than to
 * whatever locale the operating system underneath happens to be set to. These pin that the separator
 * follows the app's language setting - and only that, so the suite gives the same answer on an
 * Italian workstation as on an English one.
 * <p>
 * Language is DB-backed and shared by every test in the fork, so each test restores English.
 */
class LocalizedNumbersTest {

    @AfterEach
    void restoreLanguage() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @Test
    void englishGroupsWithCommas() {
        SystemSession.getInstance().setLanguage(Language.EN);

        assertEquals("1,500,000", LocalizedNumbers.grouped(1_500_000));
    }

    @Test
    void italianGroupsWithDots() {
        SystemSession.getInstance().setLanguage(Language.IT);

        assertEquals("1.500.000", LocalizedNumbers.grouped(1_500_000));
    }

    /**
     * WHY the restore is not optional: {@link Locale#setDefault} is process-wide, not test-scoped. This is
     * safe only because the {@code test} task forks one JVM and runs sequentially (no
     * {@code maxParallelForks} in {@code app/build.gradle}). Turning on JUnit parallel execution would let
     * this test change the locale under any test running beside it.
     */
    @Test
    void theJvmDefaultLocaleDoesNotDecideTheSeparator() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            SystemSession.getInstance().setLanguage(Language.EN);

            assertEquals("1,500,000", LocalizedNumbers.grouped(1_500_000),
                    "an English commander reads English figures on a German machine");
        } finally {
            Locale.setDefault(previous);
        }
    }

    /**
     * {@code ptbz} is a bundle-selection pseudo-tag with no locale data behind it. Mapping it to a
     * real locale is what stops Brazilian Portuguese silently formatting like the root locale.
     */
    @Test
    void bothPortugueseVariantsMapToRealLocales() {
        assertEquals("pt", LocalizedNumbers.locale(Language.PT).getLanguage());
        assertEquals("pt", LocalizedNumbers.locale(Language.PTBZ).getLanguage());
        assertNotEquals("", LocalizedNumbers.locale(Language.PTBZ).getCountry());
    }

    /**
     * Guards the switch: a language added to the enum without a locale here would not compile, but a
     * locale typed as a tag the JDK cannot resolve would silently format like root.
     */
    @Test
    void everyLanguageHasAResolvableLocale() {
        for (Language language : Language.values()) {
            Locale locale = LocalizedNumbers.locale(language);
            assertNotEquals("", locale.getLanguage(), language + " has no usable formatting locale");
        }
    }
}
