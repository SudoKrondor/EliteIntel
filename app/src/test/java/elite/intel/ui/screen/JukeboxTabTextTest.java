package elite.intel.ui.screen;

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
 * Every piece of text on the Jukebox tab has to exist in every language.
 *
 * <p>{@code BundleKeyParityTest} guards the bundles against each other, which catches a key added to eight
 * files and forgotten in the ninth. It cannot catch the other half: a key the Java asks for that no bundle
 * defines at all. {@code MultiLingualTextProvider} answers that by handing back the key itself, so the
 * commander reads "jukebox.menu.playNow" off a menu while nothing fails or logs.
 *
 * <p>The keys are read out of the panel's own source rather than listed here, so this cannot drift away
 * from what the panel actually asks for - a hand-kept list would go stale the first time someone adds a
 * button.
 *
 * <p>The panel itself is not built here: the build runs every test headless on purpose, so that a test
 * touching AWT fails on a developer's machine instead of only on the build server.
 */
class JukeboxTabTextTest {

    private static final Pattern GET_TEXT = Pattern.compile("getText\\(\"([^\"]+)\"");

    @ParameterizedTest
    @EnumSource(Language.class)
    void everyLabelOnTheTabResolvesInEveryLanguage(Language language) throws IOException {
        for (String key : keysUsedByTheTab()) {
            String text = MultiLingualTextProvider.getText(language, key);
            assertNotEquals(key, text,
                    "no bundle defines " + key + ", so " + language + " would show the raw key");
            assertFalse(text.isBlank(), key + " is defined but empty in " + language);
        }
    }

    @Test
    void theTabAsksForTheLabelsWeThinkItDoes() throws IOException {
        Set<String> keys = keysUsedByTheTab();

        assertTrue(keys.size() > 30, "expected the tab's full label set, found " + keys.size());
        assertTrue(keys.contains("jukebox.section.playlist"));
        assertTrue(keys.contains("jukebox.menu.playNext"));
        assertTrue(keys.contains("jukebox.confirm.clear"));
    }

    @Test
    void theTabItselfIsNamedInEveryLanguage() {
        for (Language language : Language.values()) {
            String label = MultiLingualTextProvider.getText(language, "tab.jukebox");
            assertNotEquals("tab.jukebox", label, "the tab has no name in " + language);
            assertFalse(label.isBlank());
        }
    }

    private static Set<String> keysUsedByTheTab() throws IOException {
        String source = Files.readString(panelSource());
        Set<String> keys = new LinkedHashSet<>();
        Matcher matcher = GET_TEXT.matcher(source);
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return keys;
    }

    /**
     * Tests run from the module directory, but tolerate being run from the repository root.
     */
    private static Path panelSource() {
        String relative = "src/main/java/elite/intel/ui/screen/JukeboxTabPanel.java";
        Path fromModule = Path.of(relative);
        return Files.exists(fromModule) ? fromModule : Path.of("app").resolve(relative);
    }
}
