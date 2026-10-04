package elite.intel.ui.dialog;

import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandDefinition;
import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Covers {@link CustomCommandEditorDialog#normalizePhrases}, the guarantee that however a commander
 * separates phrases (newlines, commas, or both) the stored form never contains an empty phrase
 * (no {@code ",,"}, no leading/trailing/stray comma) and preserves commas inside parameter templates;
 * and {@link CustomCommandEditorDialog#duplicateDraft}, what a copied command starts with.
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

    /**
     * A copy keeps what the commander wants to reuse - phrases and steps - but not the original's identity:
     * no id, so it saves as a new command, no action key, so Generate is the obvious next step, and no
     * description, which the editor cannot show and would go on describing the original.
     */
    @Test
    void duplicateDraftKeepsPhrasesAndStepsButNotIdentity() {
        List<CustomCommandStep> steps = List.of(
                new CustomCommandStep(CustomCommandStep.Type.RAW_KEY, null, 0, null, "KEY_ENTER", null),
                new CustomCommandStep(CustomCommandStep.Type.TYPE_TEXT, null, 0, "@VEGA "));
        CustomCommandDefinition source = new CustomCommandDefinition(
                "3c8aa017-c37c-4058-8aa2-0f8515e419f4", "talk_at_vega", "Talk at Vega", "Opens comms and addresses VEGA", "talk at vega", steps);

        CustomCommandDefinition draft = CustomCommandEditorDialog.duplicateDraft(source);

        assertEquals("", draft.getId());
        assertEquals("", draft.getActionKey());
        assertEquals(getText("actions.customCommands.editor.copyName", "Talk at Vega"), draft.getName());
        assertEquals("", draft.getDescription());
        assertEquals("talk at vega", draft.getPhrases());
        assertEquals(steps, draft.getSteps());
    }
}
