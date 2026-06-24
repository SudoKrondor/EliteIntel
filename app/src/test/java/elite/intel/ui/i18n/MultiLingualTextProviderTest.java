package elite.intel.ui.i18n;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiLingualTextProviderTest {

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
    void topLevelVariantSeparatorStillSelectsSingleVariant() {
        Set<String> expected = Set.of("On it!", "Affirmative!", "Aye-aye!", "Certainly!", "Of course!", "Right away!");

        String selected = MultiLingualTextProvider.getText(Language.EN, "speech.affirmative");

        assertTrue(expected.contains(selected));
        assertFalse(selected.contains("|"));
    }
}
