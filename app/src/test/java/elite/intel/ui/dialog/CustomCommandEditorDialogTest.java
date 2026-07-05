package elite.intel.ui.dialog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Covers {@link CustomCommandEditorDialog#normalizePhrases}, the guarantee that however a commander
 * separates phrases (newlines, commas, or both) the stored form never contains an empty phrase
 * (no {@code ",,"}, no leading/trailing/stray comma) and preserves commas inside parameter templates.
 */
class CustomCommandEditorDialogTest {

    @Test
    void newlineSeparatedPhrasesJoinWithSingleCommas() {
        assertEquals("go to mission, fly to mission",
                CustomCommandEditorDialog.normalizePhrases("go to mission\nfly to mission"));
    }

    @Test
    void trailingCommaPerLineDoesNotProduceDoubleComma() {
        String stored = CustomCommandEditorDialog.normalizePhrases("go to mission,\nfly to mission,");
        assertEquals("go to mission, fly to mission", stored);
        assertFalse(stored.contains(",,"));
    }

    @Test
    void runOfCommasCollapsesToOneSeparator() {
        assertEquals("a, b", CustomCommandEditorDialog.normalizePhrases("a,, b"));
        assertEquals("a, b", CustomCommandEditorDialog.normalizePhrases("a, , b"));
    }

    @Test
    void mixedNewlineAndCommaSeparatorsAreCanonicalised() {
        assertEquals("one, two, three",
                CustomCommandEditorDialog.normalizePhrases("one, two\nthree"));
    }

    @Test
    void commaInsideParameterTemplateIsPreserved() {
        assertEquals("go to {lat:number, lon:number}",
                CustomCommandEditorDialog.normalizePhrases("go to {lat:number, lon:number}"));
    }

    @Test
    void blankAndSeparatorOnlyInputYieldEmptyString() {
        assertEquals("", CustomCommandEditorDialog.normalizePhrases(""));
        assertEquals("", CustomCommandEditorDialog.normalizePhrases("   "));
        assertEquals("", CustomCommandEditorDialog.normalizePhrases(",\n,\n"));
        assertEquals("", CustomCommandEditorDialog.normalizePhrases(null));
    }
}
