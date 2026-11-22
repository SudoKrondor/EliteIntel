package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.preseteditor.BindNameEntry;
import elite.intel.bindforge.preseteditor.BindNameList;
import elite.intel.bindforge.preseteditor.BindsFileOrigin;
import elite.intel.bindforge.preseteditor.NotOfferedFile;
import elite.intel.bindforge.preseteditor.NotOfferedReason;
import elite.intel.i18n.Language;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import elite.intel.ui.screen.bindings.PresetEditorPanel.Choice;
import elite.intel.ui.screen.bindings.PresetEditorPanel.Kind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Preset Editor's rules (P4): what each dropdown offers, which item shows a line, what a change and SET ALL TO SAME
 * produce, the consistency warning, and its text in every language.
 */
class PresetEditorPanelTest {

    private static final BindNameList LIST = new BindNameList(List.of(
            new BindNameEntry("Custom", BindsFileOrigin.USER, List.of(Path.of("Custom.4.2.binds"))),
            new BindNameEntry("KeyboardMouseOnly", BindsFileOrigin.FACTORY,
                    List.of(Path.of("/game/ControlSchemes/KeyboardMouseOnly.binds")))),
            List.of(new NotOfferedFile(Path.of("Spare.4.2.binds"), BindsFileOrigin.USER,
                    NotOfferedReason.NAME_MISMATCH, "Custom")));

    private static final StartPresetSections SPECIMEN =
            new StartPresetSections("KeyboardMouseOnly", "Custom", "Custom", "Custom", false);

    @Test
    void aDropdownOffersTheListMarkingUserAndFactory() {
        assertEquals(List.of(new Choice("Custom", Kind.USER), new Choice("KeyboardMouseOnly", Kind.FACTORY)),
                PresetEditorPanel.choices(LIST, "Custom"));
    }

    @Test
    void aLineNamingNothingOfferedStaysInItsDropdownFirst() {
        List<Choice> choices = PresetEditorPanel.choices(LIST, "Gone");

        assertEquals(new Choice("Gone", Kind.MISSING), choices.getFirst());
        assertEquals(3, choices.size());
        assertEquals(new Choice("Gone", Kind.MISSING), PresetEditorPanel.selected(choices, "Gone"));
    }

    @Test
    void anEmptyLineIsMissingToo() {
        assertEquals(new Choice("", Kind.MISSING), PresetEditorPanel.choices(LIST, "").getFirst());
        assertTrue(PresetEditorPanel.unnamed(LIST, ""));
    }

    @Test
    void setAllsListHasNoMissingItem() {
        assertEquals(2, PresetEditorPanel.choices(LIST, null).size());
    }

    @Test
    void aLineDifferingOnlyInCaseShowsTheOfferedEntry() {
        List<Choice> choices = PresetEditorPanel.choices(LIST, "custom");

        assertEquals(2, choices.size(), "the game matches ignoring case, so custom is not missing");
        assertEquals(new Choice("Custom", Kind.USER), PresetEditorPanel.selected(choices, "custom"));
    }

    @Test
    void theRefusedSectionsAreTheOnesAPPLYWouldRefuse() {
        StartPresetSections sections = new StartPresetSections("Custom", "", "Gone", "custom", false);

        assertEquals(List.of(BindingSection.SHIP, BindingSection.SRV),
                PresetEditorPanel.unnamedSections(sections, LIST));
    }

    @Test
    void splitWhenTheLinesDisagreeIgnoringCase() {
        assertTrue(PresetEditorPanel.split(SPECIMEN));
        assertFalse(PresetEditorPanel.split(new StartPresetSections("Custom", "custom", "Custom", "CUSTOM", false)));
        assertFalse(PresetEditorPanel.split(new StartPresetSections("Custom", "Custom", "Custom", "Custom", true)));
    }

    @Test
    void aChangeReplacesOneLineInTheFourLineForm() {
        StartPresetSections changed = PresetEditorPanel.with(
                new StartPresetSections("Custom", "Custom", "Custom", "Custom", true),
                BindingSection.SRV, "KeyboardMouseOnly");

        assertEquals("Custom", changed.general());
        assertEquals("Custom", changed.ship());
        assertEquals("KeyboardMouseOnly", changed.srv());
        assertEquals("Custom", changed.onFoot());
        assertFalse(changed.singleLine());
    }

    @Test
    void setAllPutsOneNameOnEveryLine() {
        StartPresetSections all = PresetEditorPanel.allTo("Custom");

        for (BindingSection section : List.of(BindingSection.GENERAL, BindingSection.SHIP, BindingSection.SRV,
                BindingSection.ON_FOOT)) {
            assertEquals("Custom", all.forSection(section));
        }
        assertFalse(PresetEditorPanel.split(all));
    }

    @Test
    void labelsNameTheBindName() {
        assertEquals("Custom", PresetEditorPanel.label(new Choice("Custom", Kind.USER)));
        assertTrue(PresetEditorPanel.label(new Choice("KeyboardMouseOnly", Kind.FACTORY)).contains("KeyboardMouseOnly"));
        assertTrue(PresetEditorPanel.label(new Choice("Gone", Kind.MISSING)).contains("Gone"));
        assertFalse(PresetEditorPanel.label(new Choice("", Kind.MISSING)).isBlank());
    }

    @ParameterizedTest
    @EnumSource(NotOfferedReason.class)
    void everyReasonNotOfferedIsASentenceNamingTheFile(NotOfferedReason reason) {
        String text = PresetEditorPanel.notOfferedText(
                new NotOfferedFile(Path.of("Spare.4.2.binds"), BindsFileOrigin.USER, reason, "Detail"));

        assertTrue(text.contains("Spare.4.2.binds"), text);
        assertFalse(text.contains("{"), text);
        assertFalse(text.contains("bindings.presetEditor"), "a raw key: " + text);
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void everyStringResolvesInEveryLanguageWithItsApostrophesRight(Language language) throws IOException {
        Set<String> keys = keysUsed();
        assertTrue(keys.size() >= 20, "the panel's keys: " + keys);
        for (String key : keys) {
            String raw = MultiLingualTextProvider.getText(language, key);
            assertNotEquals(key, raw, "no bundle defines " + key + " for " + language);
            assertFalse(raw.isBlank(), key + " is empty in " + language);
            if (raw.contains("{0}")) {
                String formatted = MultiLingualTextProvider.getText(language, key, "AAA", "BBB");
                assertTrue(formatted.contains("AAA"), key + " lost its placeholder in " + language + ": " + formatted);
                assertFalse(formatted.contains("{"), key + " in " + language + ": " + formatted);
                assertFalse(formatted.contains("''"), key + " in " + language + ": " + formatted);
            } else {
                assertFalse(raw.contains("''"), key + " takes no arguments, so its apostrophe is single: " + raw);
            }
        }
    }

    private static Set<String> keysUsed() throws IOException {
        Pattern literal = Pattern.compile("\"(bindings\\.(?:presetEditor\\.[A-Za-z.]+|tab\\.presetEditor))\"");
        Set<String> keys = new LinkedHashSet<>();
        Matcher matcher = literal.matcher(Files.readString(panelSource()));
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return keys;
    }

    /** Tests run from the module directory, but tolerate being run from the repository root. */
    private static Path panelSource() {
        String relative = "src/main/java/elite/intel/ui/screen/bindings/PresetEditorPanel.java";
        Path fromModule = Path.of(relative);
        return Files.exists(fromModule) ? fromModule : Path.of("app").resolve(relative);
    }
}
