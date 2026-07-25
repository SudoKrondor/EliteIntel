package elite.intel.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every language bundle must define the same keys as the English base.
 * <p>
 * A missing key is invisible: the provider falls back to English, so the app keeps working and the
 * commander simply hears one English phrase in the middle of an otherwise translated sentence. Nothing
 * fails, nothing logs, and it survives review easily because adding a key means touching nine files and
 * missing the ninth looks exactly like success.
 * <p>
 * The reverse case matters just as much. A key a translation defines but the base does not is dead: it is
 * never looked up, so the translator's work has no effect. Writing this test surfaced three such cases,
 * all since fixed: the Nomad command's ID was misspelled {@code lauch_deploy_nomad} in the code while the
 * Italian bundle spelled it correctly, the Italian bundle keyed the central panel by its synonym
 * {@code show_commander_panel}, and {@code transfer_power_to_ship_systems} had Russian and Ukrainian
 * aliases but none in the English base.
 * <p>
 * {@link PortugueseVariantsIndependenceTest} already guards PT against PTBZ in both directions. This test
 * covers what that one does not: every language against the English base.
 */
class BundleKeyParityTest {

    private static final List<String> FAMILIES = List.of("gui", "llm", "ed_events", "ai_action_aliases");

    private static final String BASELINE = "/i18n-parity-baseline.txt";

    /**
     * Plural category suffixes. Russian and Ukrainian need a {@code .few} form that English has no word
     * for, so those keys legitimately exist only in the Slavic bundles and must not read as dead entries.
     * A key is judged against its stem: {@code event.time.hours.few} is fine because the base defines
     * {@code event.time.hours.one}.
     */
    private static final Pattern PLURAL_CATEGORY = Pattern.compile("\\.(zero|one|two|few|many|other)$");

    private record Gap(String family, String language, String kind, String key) {
        static final String MISSING = "MISSING";
        static final String ORPHAN = "ORPHAN";

        @Override
        public String toString() {
            return family + "|" + language + "|" + kind + "|" + key;
        }
    }

    /**
     * Language suffixes are derived from the enum rather than listed, so adding a language to
     * {@link Language} puts its bundles under this guard automatically instead of silently skipping them.
     */
    private static List<String> translatedSuffixes() {
        return Arrays.stream(Language.values())
                .filter(language -> language != Language.EN)
                .map(language -> language.name().toLowerCase(Locale.ROOT))
                .toList();
    }

    private static Set<String> keysOf(String family, String suffix) {
        String path = "/i18n/" + family + (suffix.isEmpty() ? "" : "_" + suffix) + ".properties";
        Properties props = new Properties();
        try (InputStream in = BundleKeyParityTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing bundle on classpath: " + path);
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AssertionError("could not read " + path, e);
        }
        return props.stringPropertyNames();
    }

    private static List<Gap> gapsIn(String family) {
        Set<String> base = keysOf(family, "");
        Set<String> baseStems = new HashSet<>();
        base.forEach(key -> baseStems.add(PLURAL_CATEGORY.matcher(key).replaceAll("")));

        List<Gap> gaps = new ArrayList<>();
        for (String suffix : translatedSuffixes()) {
            Set<String> translated = keysOf(family, suffix);

            base.stream().filter(key -> !translated.contains(key)).sorted()
                    .forEach(key -> gaps.add(new Gap(family, suffix, Gap.MISSING, key)));

            translated.stream()
                    .filter(key -> !base.contains(key))
                    .filter(key -> !baseStems.contains(PLURAL_CATEGORY.matcher(key).replaceAll("")))
                    .sorted()
                    .forEach(key -> gaps.add(new Gap(family, suffix, Gap.ORPHAN, key)));
        }
        return gaps;
    }

    private static Set<String> baseline() {
        Set<String> entries = new HashSet<>();
        try (InputStream in = BundleKeyParityTest.class.getResourceAsStream(BASELINE)) {
            assertNotNull(in, "missing baseline on test classpath: " + BASELINE);
            new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(entries::add);
        } catch (IOException e) {
            throw new AssertionError("could not read " + BASELINE, e);
        }
        return entries;
    }

    /**
     * The guard that matters day to day: a key added to the base but not to every translation, or added to
     * a translation the base never declares. Known gaps are carried in the baseline file so this test is
     * green on a backlog it did not create; anything not listed there is new and fails here.
     */
    @ParameterizedTest(name = "{0}: no new key gaps against the English base")
    @ValueSource(strings = {"gui", "llm", "ed_events", "ai_action_aliases"})
    void translationsDefineTheSameKeysAsTheBase(String family) {
        Set<String> known = baseline();
        List<Gap> unexpected = gapsIn(family).stream()
                .filter(gap -> !known.contains(gap.toString()))
                .toList();

        assertTrue(unexpected.isEmpty(), () -> """
                %d new key gap(s) in the %s bundles.
                
                MISSING = the base declares it, this translation does not, so it falls back to English.
                ORPHAN  = this translation declares it, the base does not, so it is never looked up.
                
                Add the key to the bundles listed, or if the gap is deliberate, add these lines to
                src/test/resources%s:
                
                %s"""
                .formatted(unexpected.size(), family, BASELINE,
                        String.join("\n", unexpected.stream().map(Gap::toString).toList())));
    }

    /**
     * Keeps the baseline honest. Once a gap is filled its line must go, otherwise the file grows into a
     * permanent rug to sweep drift under and stops meaning anything.
     */
    @Test
    void theBaselineListsNoGapThatHasSinceBeenFixed() {
        Set<String> actual = new HashSet<>();
        FAMILIES.forEach(family -> gapsIn(family).forEach(gap -> actual.add(gap.toString())));

        List<String> stale = baseline().stream().filter(entry -> !actual.contains(entry)).sorted().toList();

        assertTrue(stale.isEmpty(), () -> String.format(
                "%d baseline entr(ies) no longer describe a real gap. Delete them from src/test/resources%s:%n%s",
                stale.size(), BASELINE, String.join("\n", stale)));
    }

    /**
     * A baseline entry naming a family or language that no longer exists would silently excuse nothing,
     * and would hide a renamed bundle.
     */
    @Test
    void everyBaselineEntryIsWellFormed() {
        List<String> suffixes = translatedSuffixes();
        List<String> malformed = baseline().stream().filter(entry -> {
            String[] parts = entry.split("\\|", 4);
            return parts.length != 4
                    || !FAMILIES.contains(parts[0])
                    || !suffixes.contains(parts[1])
                    || !(Gap.MISSING.equals(parts[2]) || Gap.ORPHAN.equals(parts[2]));
        }).sorted().toList();

        assertTrue(malformed.isEmpty(), () -> "malformed baseline entr(ies): " + malformed);
    }
}
