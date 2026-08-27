package elite.intel.ui.i18n;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MultiLingualTextProviderTest {

    private static final String BLOCKING_CONFLICT_KEY = "speech.bindingConflictsBlocking";

    @Test
    void choiceFormatSpeechKeysDoNotSplitAsVariants() {
        String[] keys = {"speech.bindingsMissing", "speech.bindingConflicts"};

        for (Language language : Language.values()) {
            for (String key : keys) {
                String singular = assertDoesNotThrow(
                        () -> MultiLingualTextProvider.getText(language, key, 1),
                        language + " " + key + " singular"
                );
                String plural = assertDoesNotThrow(
                        () -> MultiLingualTextProvider.getText(language, key, 2),
                        language + " " + key + " plural"
                );

                assertFalse(singular.contains("{"), language + " " + key + " singular was not formatted");
                assertFalse(plural.contains("{"), language + " " + key + " plural was not formatted");
                assertFalse(singular.contains("|"), language + " " + key + " singular was split incorrectly");
                assertFalse(plural.contains("|"), language + " " + key + " plural was split incorrectly");
            }
        }
    }

    @Test
    void blockingConflictWarningNamesTheKeysInBothNumbersInEveryLanguage() {
        for (Language language : Language.values()) {
            String one = assertDoesNotThrow(
                    () -> MultiLingualTextProvider.getText(language, BLOCKING_CONFLICT_KEY, 1, "A"),
                    language + " singular");
            String many = assertDoesNotThrow(
                    () -> MultiLingualTextProvider.getText(language, BLOCKING_CONFLICT_KEY, 4, "A, D, S, W"),
                    language + " plural");

            assertTrue(one.contains("A"), language + " singular did not name the conflicting key");
            assertTrue(many.contains("A, D, S, W"), language + " plural did not name the conflicting keys");
            for (String rendered : new String[]{one, many}) {
                assertFalse(rendered.contains("{"), language + " was not formatted");
                assertFalse(rendered.contains("|"), language + " was split as a variant");
                assertFalse(rendered.contains("''"), language + " left a doubled apostrophe unformatted");
            }
            assertNotEquals(one, many, language + " renders the same wording for one key and for four");
        }
    }

    @Test
    void blockingConflictWarningHasNoLoneApostropheToSwallowItsPlaceholder() {
        // Passing no args returns the raw pattern, unformatted. The key only became a MessageFormat
        // pattern when it started naming the keys, and in a pattern a lone ASCII apostrophe opens a
        // quoted region: the elisions in the French, Italian, Portuguese and Spanish prose would then
        // eat the placeholder, and the commander would hear a literal "{1}". Asserted on the pattern
        // rather than on any translated phrase, so rewording a sentence never breaks this.
        for (Language language : Language.values()) {
            String pattern = MultiLingualTextProvider.getText(language, BLOCKING_CONFLICT_KEY);

            assertFalse(pattern.replace("''", "").contains("'"),
                    language + " has an unescaped apostrophe in " + BLOCKING_CONFLICT_KEY);
        }
    }

    @Test
    void topLevelVariantSeparatorStillSelectsSingleVariant() {
        Set<String> expected = Set.of("On it!", "Affirmative!", "Aye-aye!", "Certainly!", "Of course!", "Right away!");

        String selected = MultiLingualTextProvider.getText(Language.EN, "speech.affirmative");

        assertTrue(expected.contains(selected));
        assertFalse(selected.contains("|"));
    }
}
