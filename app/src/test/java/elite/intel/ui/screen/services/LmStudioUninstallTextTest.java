package elite.intel.ui.screen.services;

import elite.intel.i18n.Language;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The uninstall's text on the AI Services panel exists in every language, and its figures substitute. The keys are
 * read out of the panel's own source, as in {@code JukeboxTabTextTest}.
 */
class LmStudioUninstallTextTest {

    private static final Pattern KEY_LITERAL = Pattern.compile("\"(lmStudioUninstall\\.[A-Za-z_.]+)\"");

    @ParameterizedTest
    @EnumSource(Language.class)
    void everyLabelResolvesInEveryLanguage(Language language) throws IOException {
        for (String key : keysUsed()) {
            String text = MultiLingualTextProvider.getText(language, key);
            assertNotEquals(key, text, "no bundle defines " + key + ", so " + language + " would show the raw key");
            assertFalse(text.isBlank(), key + " is defined but empty in " + language);
        }
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void theFolderAndModelsSubstituteInEveryLanguage(Language language) {
        String text = MultiLingualTextProvider.getText(language, "lmStudioUninstall.confirm.message",
                "D:\\Games\\LMStudio", "• gemma-4-E4B-it-Q4_K_M.gguf  5.0 GB");
        assertFalse(text.contains("{"), "a placeholder was left in " + language + ": " + text);
        assertTrue(text.contains("D:\\Games\\LMStudio"), language + ": " + text);
        assertTrue(text.contains("gemma-4-E4B-it-Q4_K_M.gguf"), language + ": " + text);

        String failed = MultiLingualTextProvider.getText(language, "lmStudioUninstall.failed.message", "HTTP 500");
        assertTrue(failed.contains("HTTP 500"), language + ": " + failed);
    }

    @Test
    void thePanelAsksForTheLabelsWeThinkItDoes() throws IOException {
        Set<String> keys = keysUsed();

        assertTrue(keys.contains("lmStudioUninstall.confirm.message"));
        assertTrue(keys.contains("lmStudioUninstall.done.message"));
        assertEquals(7, keys.size(), "the uninstall's full label set: " + keys);
    }

    private static Set<String> keysUsed() throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        Matcher matcher = KEY_LITERAL.matcher(Files.readString(panelSource()));
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return keys;
    }

    /**
     * Tests run from the module directory, but tolerate being run from the repository root.
     */
    private static Path panelSource() {
        String relative = "src/main/java/elite/intel/ui/screen/services/AiServicesPanel.java";
        Path fromModule = Path.of(relative);
        return Files.exists(fromModule) ? fromModule : Path.of("app").resolve(relative);
    }
}
