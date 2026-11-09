package elite.intel.ui.dialog;

import elite.intel.ai.brain.inference.lmstudio.install.InstallStep;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioSetupOffer;
import elite.intel.i18n.Language;
import elite.intel.ui.i18n.MultiLingualTextProvider;
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
 * Every piece of text the install dialog shows exists in every language, and its figures substitute.
 * <p>
 * As in {@code JukeboxTabTextTest}, the keys are read out of the dialog's own source so this cannot drift from what
 * it asks for, and the dialog is not built - the build runs headless. The step names are built from
 * {@link InstallStep} at run time, so they are added here from the enum.
 */
class LmStudioInstallDialogTest {

    /**
     * Every bundle key written in the source, not only those inside {@code getText(...)}: the intro and most step
     * states reach it through a variable.
     */
    private static final Pattern KEY_LITERAL = Pattern.compile("\"((?:lmStudioInstall|button)\\.[A-Za-z_.]+)\"");

    @ParameterizedTest
    @EnumSource(Language.class)
    void everyLabelResolvesInEveryLanguage(Language language) throws IOException {
        for (String key : keysUsed()) {
            String text = MultiLingualTextProvider.getText(language, key);
            assertNotEquals(key, text, "no bundle defines " + key + ", so " + language + " would show the raw key");
            assertFalse(text.isBlank(), key + " is defined but empty in " + language);
        }
    }

    /**
     * A lone ASCII apostrophe in a pattern swallows the placeholders after it (see the MessageFormat trap in
     * {@code MultiLingualTextProvider}), so each figure is checked to have arrived.
     */
    @ParameterizedTest
    @EnumSource(Language.class)
    void theFiguresSubstituteInEveryLanguage(Language language) {
        assertSubstitutes(language, "lmStudioInstall.folder.space", "123.4", "10.0");
        assertSubstitutes(language, "lmStudioInstall.download.value", "2.1", "6.0", "48", "3");
        assertSubstitutes(language, "lmStudioInstall.result.failed", "Download the model", "HTTP 500");
    }

    @Test
    void theDialogAsksForTheLabelsWeThinkItDoes() throws IOException {
        Set<String> keys = keysUsed();

        assertTrue(keys.contains("lmStudioInstall.intro.install"));
        assertTrue(keys.contains("lmStudioInstall.state.alreadyDone"));
        assertTrue(keys.contains("lmStudioInstall.stop.message"));
        assertTrue(keys.contains("lmStudioInstall.step.GPU_CHECK"));
        assertTrue(keys.size() > 30, "expected the dialog's full label set, found " + keys.size());
    }

    @Test
    void settingUpAnExistingLmStudioShowsNoSpaceCheckOrInstall() {
        List<InstallStep> steps = LmStudioInstallDialog.stepsFor(LmStudioSetupOffer.SET_UP_EXISTING);

        assertEquals(InstallStep.START, steps.getFirst());
        assertFalse(steps.contains(InstallStep.CHECK));
        assertFalse(steps.contains(InstallStep.INSTALL));
        assertEquals(List.of(InstallStep.values()), LmStudioInstallDialog.stepsFor(LmStudioSetupOffer.INSTALL));
    }

    private static void assertSubstitutes(Language language, String key, String... figures) {
        String text = MultiLingualTextProvider.getText(language, key, (Object[]) figures);
        assertFalse(text.contains("{"), key + " left a placeholder in " + language + ": " + text);
        for (String figure : figures) {
            assertTrue(text.contains(figure), key + " lost " + figure + " in " + language + ": " + text);
        }
    }

    private static Set<String> keysUsed() throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        Matcher matcher = KEY_LITERAL.matcher(Files.readString(dialogSource()));
        while (matcher.find()) {
            String key = matcher.group(1);
            if (key.endsWith(".")) continue; // the step prefix, completed below
            keys.add(key);
        }
        for (InstallStep step : InstallStep.values()) {
            keys.add("lmStudioInstall.step." + step.name());
        }
        return keys;
    }

    /**
     * Tests run from the module directory, but tolerate being run from the repository root.
     */
    private static Path dialogSource() {
        String relative = "src/main/java/elite/intel/ui/dialog/LmStudioInstallDialog.java";
        Path fromModule = Path.of(relative);
        return Files.exists(fromModule) ? fromModule : Path.of("app").resolve(relative);
    }
}
