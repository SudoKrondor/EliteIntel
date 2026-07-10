package elite.intel.i18n;

import elite.intel.ai.brain.i18n.LlmTextProvider;
import elite.intel.gameapi.i18n.EventsTextProvider;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Language#PT} (European) and {@link Language#PTBZ} (Brazilian) are maintained as two independent
 * languages, not as a language and its country variant. Each owns a complete set of bundles and falls back only
 * to English, exactly like every other language the app supports.
 * <p>
 * This is a deliberate choice against the Java-idiomatic {@code pt_BR -> pt -> root} chain. Inheritance would
 * save duplicated keys, but the two files are edited by two different localizers, and a key silently answered by
 * the other dialect's bundle is very hard to debug from a bug report that just says "this string is wrong".
 * The cost of the choice is that a new key must be added to both bundles; the tests below make forgetting loud.
 */
class PortugueseVariantsIndependenceTest {

    private static final List<String> BUNDLES = List.of("gui", "llm", "ed_events", "ai_action_aliases");

    /**
     * Keys the Brazilian bundles carry that the European ones do not yet. {@code *_pt.properties} is owned by the
     * European localizer and is not edited here, so this is expected debt rather than a defect: until the key
     * lands there, a European commander sees the English label. Shrink this list; never grow it.
     */
    private static final List<String> PENDING_IN_EUROPEAN_BUNDLES = List.of("language.portugueseBrazilian");

    private static Properties load(String baseName, String suffix) {
        String path = "/i18n/" + baseName + suffix + ".properties";
        Properties props = new Properties();
        try (InputStream in = PortugueseVariantsIndependenceTest.class.getResourceAsStream(path)) {
            assertTrue(in != null, "missing bundle on classpath: " + path);
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AssertionError("could not read " + path, e);
        }
        return props;
    }

    /**
     * The mechanical guarantee of independence: {@code "ptbz"} is its own language subtag, so its only parent is
     * the English root. Were {@code PTBZ} ever mapped to {@code Locale.of("pt","BR")}, {@code pt} would slip into
     * this chain and European Portuguese would start answering for missing Brazilian keys.
     */
    @Test
    void theBrazilianLanguageTagHasNoParentButTheEnglishRoot() {
        ResourceBundle.Control control =
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT);
        Locale brazilian = Locale.forLanguageTag("ptbz");

        assertEquals(List.of(brazilian, Locale.ROOT),
                control.getCandidateLocales("i18n.gui", brazilian),
                "PTBZ must not inherit from the European Portuguese bundle");
    }

    /**
     * The providers must actually read the Brazilian bundle. Wherever the two dialects have already diverged,
     * {@code PTBZ} has to return the Brazilian string - if its locale mapping ever regressed to {@code pt_BR},
     * these keys would quietly answer with European Portuguese instead. The guard strengthens on its own: every
     * key the two localizers translate differently becomes another case here.
     */
    @Test
    void brazilianTextIsServedFromTheBrazilianBundle() {
        int checked = 0;
        for (String baseName : List.of("gui", "llm", "ed_events")) {
            Properties european = load(baseName, "_pt");
            Properties brazilian = load(baseName, "_ptbz");

            for (String key : brazilian.stringPropertyNames()) {
                String brazilianValue = brazilian.getProperty(key);
                if (brazilianValue.equals(european.getProperty(key))) continue;
                // A '|' value is a random-variant pick, so the provider need not return this exact string.
                if (brazilianValue.contains("|")) continue;

                assertEquals(brazilianValue, resolve(baseName, key),
                        baseName + "_ptbz key '" + key + "' was not served from the Brazilian bundle");
                checked++;
            }
        }
        assertTrue(checked > 0, "no diverged keys found - this guard has nothing to check");
    }

    private static String resolve(String baseName, String key) {
        return switch (baseName) {
            case "gui" -> MultiLingualTextProvider.getText(Language.PTBZ, key);
            case "llm" -> LlmTextProvider.getText(Language.PTBZ, key);
            case "ed_events" -> EventsTextProvider.getText(Language.PTBZ, key);
            default -> throw new IllegalArgumentException("no provider wired for bundle " + baseName);
        };
    }

    /**
     * Independence means each bundle answers for itself. A key the European bundle defines but the Brazilian one
     * does not would drop to English for Brazilian commanders - almost always an oversight when a key was added
     * to only one of the two files.
     */
    @ParameterizedTest(name = "{0}_ptbz defines every key {0}_pt defines")
    @ValueSource(strings = {"gui", "llm", "ed_events", "ai_action_aliases"})
    void neitherVariantIsMissingKeysTheOtherDefines(String baseName) {
        Properties european = load(baseName, "_pt");
        Properties brazilian = load(baseName, "_ptbz");

        List<String> missingFromBrazilian = new ArrayList<>(european.stringPropertyNames());
        missingFromBrazilian.removeAll(brazilian.stringPropertyNames());

        List<String> missingFromEuropean = new ArrayList<>(brazilian.stringPropertyNames());
        missingFromEuropean.removeAll(european.stringPropertyNames());
        missingFromEuropean.removeAll(PENDING_IN_EUROPEAN_BUNDLES);

        assertTrue(missingFromBrazilian.isEmpty(),
                baseName + "_ptbz.properties is missing " + missingFromBrazilian.size()
                        + " key(s) held by _pt, so they fall back to English: " + missingFromBrazilian);
        assertTrue(missingFromEuropean.isEmpty(),
                baseName + "_pt.properties is missing " + missingFromEuropean.size()
                        + " key(s) held by _ptbz, so they fall back to English: " + missingFromEuropean);
    }

    /**
     * A duplicated key silently discards one of its two values, which is invisible in a diff.
     */
    @ParameterizedTest(name = "{0}_ptbz has no duplicate keys")
    @ValueSource(strings = {"gui", "llm", "ed_events", "ai_action_aliases"})
    void brazilianBundlesDeclareEachKeyOnce(String baseName) {
        List<String> keys = new ArrayList<>();
        String path = "/i18n/" + baseName + "_ptbz.properties";
        try (InputStream in = PortugueseVariantsIndependenceTest.class.getResourceAsStream(path)) {
            assertTrue(in != null, "missing bundle: " + path);
            new java.io.BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#") && line.contains("="))
                    .map(line -> line.substring(0, line.indexOf('=')).strip())
                    .forEach(keys::add);
        } catch (IOException e) {
            throw new AssertionError("could not read " + path, e);
        }

        List<String> duplicates = keys.stream().distinct()
                .filter(key -> keys.indexOf(key) != keys.lastIndexOf(key))
                .toList();
        assertTrue(duplicates.isEmpty(), path + " declares these keys more than once: " + duplicates);
    }

    /**
     * Every provider that reads a bundle must resolve both variants rather than echoing the key name back.
     */
    @Test
    void everyBundleFamilyResolvesForBothVariants() {
        for (Language language : List.of(Language.PT, Language.PTBZ)) {
            assertFalse(MultiLingualTextProvider.getText(language, "tab.commander").isBlank());
            assertFalse(LlmTextProvider.getText(language, "carrier.squadronStems").isBlank());
            assertFalse(EventsTextProvider.getText(language, "event.srv.welcomeBack", "CMDR").isBlank());
        }
    }

    /**
     * The picker lists both variants, so each must name its country rather than both reading "Portuguese".
     */
    @Test
    void bothVariantsAreDistinguishableInTheLanguagePicker() {
        assertEquals("Português (Brasil)",
                MultiLingualTextProvider.getText(Language.PTBZ, "language.portugueseBrazilian"));
        assertEquals("Português (Portugal)",
                MultiLingualTextProvider.getText(Language.PTBZ, "language.portuguese"));
    }

    /**
     * Brazilian bundles must actually be reached - a missing file would silently serve English throughout.
     */
    @Test
    void brazilianBundlesAreOnTheClasspath() {
        BUNDLES.forEach(baseName -> assertFalse(load(baseName, "_ptbz").isEmpty(),
                baseName + "_ptbz.properties is empty or absent"));
    }
}
