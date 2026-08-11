package elite.intel.ui.overlay;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import elite.intel.ui.overlay.MiningObjectiveSource.TargetYield;
import elite.intel.util.StringUtls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static elite.intel.ui.overlay.ExobiologyFixtures.bodyWith;
import static elite.intel.ui.overlay.ExobiologyFixtures.genus;
import static elite.intel.ui.overlay.HudCards.labels;
import static elite.intel.ui.overlay.HudCards.valueOf;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The card is the one surface a commander reads mid-flight, so it speaks their language: the words
 * this app writes are looked up, while the words the game gave us - system, commodity, faction and
 * genus names - are passed through as they arrived.
 * <p>
 * The other card tests in this package assert English labels and are the guard that the English text
 * is unchanged; these are the guard that it is not the only text there is. Language is DB-backed and
 * shared across the whole test fork, so each test restores English.
 */
class HudCardLocalizationTest {

    private static final String KEY_PREFIX = "overlay.card.";

    /**
     * The labels drawn on a row that carries a progress bar, which is the tightest column on the card.
     */
    private static final Set<String> PROGRESS_ROW_KEYS = Set.of(
            KEY_PREFIX + "row.genus",
            KEY_PREFIX + "row.pirates",
            KEY_PREFIX + "row.piratesEstimated",
            KEY_PREFIX + "row.hold",
            KEY_PREFIX + "row.legs");

    @AfterEach
    void restoreLanguage() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    /**
     * A key the code asks for but no bundle defines renders as the key itself - "overlay.card.row.hold"
     * across the middle of the card - which no test asserting English would ever catch.
     */
    @Test
    void everyCardKeyResolvesInEveryLanguage() {
        Set<String> keys = cardKeys();
        assertFalse(keys.isEmpty(), "no overlay.card.* keys in the English bundle");

        for (Language language : Language.values()) {
            for (String key : keys) {
                String text = MultiLingualTextProvider.getText(language, key);
                assertFalse(text.isBlank(), key + " is blank in " + language);
                assertFalse(text.equals(key), key + " is undefined in " + language);
            }
        }
    }

    /**
     * A counted key is not one key but one per plural category, and which categories exist is the
     * language's business: English declares {@code .one} and {@code .many}, Russian and Ukrainian also
     * declare {@code .few} for 2 to 4. The base bundle therefore cannot list what a Slavic bundle owes,
     * so {@link #everyCardKeyResolvesInEveryLanguage}, which reads the English base, is blind to exactly
     * those keys.
     * <p>
     * Read from the bundle files rather than through {@link HudText}, deliberately: the lookup degrades a
     * missing category to the {@code .many} form so the card never shows a raw key, and going through it
     * would let this test pass on the very gap it exists to catch.
     */
    @Test
    void everyLanguageDeclaresEveryPluralCategoryItsOwnRuleCanSelect() {
        for (String base : pluralKeyBases()) {
            for (Language language : Language.values()) {
                Set<String> declared = bundleKeys(language);
                for (String suffix : categoriesUsedBy(language)) {
                    assertTrue(declared.contains(base + suffix),
                            language + " never declares " + base + suffix
                                    + ", which its own plural rule selects");
                }
            }
        }
    }

    /**
     * The expiry countdown carries its units from the bundle but pads the hours and minutes itself, and a
     * bare {@code String.format} would take that pad from the JVM default locale, which is the operating
     * system's rather than the one chosen here. On a machine set to Arabic or Hindi that put Arabic-Indic
     * digits in this one row while every other figure on the card stayed Western.
     * <p>
     * WHY the restore is not optional: {@link Locale#setDefault} is process-wide, not test-scoped. This is
     * safe only because the {@code test} task forks one JVM and runs sequentially (no
     * {@code maxParallelForks} in {@code app/build.gradle}). Turning on JUnit parallel execution would let
     * this test change the locale under any test running beside it.
     */
    @Test
    void theExpiryCountdownKeepsItsDigitsWhateverTheMachineLocaleIs() {
        SystemSession.getInstance().setLanguage(Language.EN);
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"));

            assertEquals("2d 04h", MissionObjectiveSource.humanDuration(Duration.ofHours(52)));
            assertEquals("5h 30m", MissionObjectiveSource.humanDuration(Duration.ofMinutes(330)));
            assertEquals("45m", MissionObjectiveSource.humanDuration(Duration.ofMinutes(45)));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void theExpiryCountdownTakesItsUnitsFromTheCommandersLanguage() {
        SystemSession.getInstance().setLanguage(Language.RU);

        assertEquals("2д 04ч", MissionObjectiveSource.humanDuration(Duration.ofHours(52)));
        assertEquals("45м", MissionObjectiveSource.humanDuration(Duration.ofMinutes(45)));
    }

    /**
     * A top-level {@code |} in a bundle value is variant syntax: the provider splits on it and picks a
     * side at random on every single lookup. That is harmless for a greeting spoken once, and not at all
     * harmless here. A card is re-derived on every poll and {@code NativeHudOverlay} writes only when the
     * card differs from the last one, so a variant in card text would make the card differ from itself
     * once a second: a permanent rewrite down the pipe and a label visibly flickering between wordings.
     * <p>
     * A translator has no way to know the character is special, so this is a guard rather than a rule
     * anyone is expected to remember.
     */
    @Test
    void noCardValueCarriesVariantSyntax() {
        for (Language language : Language.values()) {
            Properties bundle = bundle(language);
            for (String key : bundle.stringPropertyNames()) {
                if (!key.startsWith(KEY_PREFIX)) continue;
                assertFalse(bundle.getProperty(key).contains("|"),
                        language + " " + key + " carries variant syntax: \""
                                + bundle.getProperty(key) + "\"");
            }
        }
    }

    @Test
    void theExobiologyCardIsWrittenInTheCommandersLanguage() {
        SystemSession.getInstance().setLanguage(Language.IT);

        HudObjective card = ExobiologyObjectiveSource.card(
                bodyWith(genus("Bacterium", "$Codex_Ent_Bacterial_Genus_Name;")),
                List.of(), "exobiology:1:2").orElseThrow();

        assertEquals("ESOBIOLOGIA", card.title());
        assertEquals("GENERE", card.rows().getFirst().label());
        assertEquals("BACTERIUM", card.rows().get(1).label(),
                "the genus name came from the game and is not ours to translate");
    }

    @Test
    void theMiningCardIsWrittenInTheCommandersLanguage() {
        SystemSession.getInstance().setLanguage(Language.DE);

        HudObjective card = MiningObjectiveSource.card(
                List.of(new TargetYield("Platinum", 89)), 137, 226, 512, "HYADES SECTOR").orElseThrow();

        assertEquals("BERGBAU", card.title());
        assertEquals(List.of("LADERAUM", "PLATINUM", "LIMPETS"), labels(card));
        assertEquals("89 T", valueOf(card, "PLATINUM"));
    }

    /**
     * Russian counts in three: two missions and five missions are not the same word. English has no
     * such category, so the shape of the key set differs per language and only the count decides.
     */
    @Test
    void aCountedRowTakesThePluralFormItsLanguageCallsFor() {
        SystemSession.getInstance().setLanguage(Language.RU);

        assertEquals("1 МИССИЯ", HudText.plural("overlay.card.value.missionCount", 1));
        assertEquals("3 МИССИИ", HudText.plural("overlay.card.value.missionCount", 3));
        assertEquals("8 МИССИЙ", HudText.plural("overlay.card.value.missionCount", 8));
    }

    @Test
    void anEnglishCountFallsBackToTheTwoFormsEnglishHas() {
        SystemSession.getInstance().setLanguage(Language.EN);

        assertEquals("1 MISSION", HudText.plural("overlay.card.value.missionCount", 1));
        assertEquals("10 MISSIONS", HudText.plural("overlay.card.value.missionCount", 10));
    }

    /**
     * A label on a progress row shares its line with the bar, which starts at a fixed offset
     * (150px + padding at scale 1.0). Nothing wraps or clips, so a label past that offset runs into
     * the bar. Measured against the real renderer, 16 upper-case characters is what fits - the German
     * "PIRATEN (GESCH.)" ends within a few pixels of the bar and is the widest we ship.
     */
    @Test
    void noProgressRowLabelReachesTheBar() {
        for (Language language : Language.values()) {
            for (String key : PROGRESS_ROW_KEYS) {
                String label = MultiLingualTextProvider.getText(language, key);
                assertTrue(label.length() <= 16,
                        language + " " + key + " is " + label.length() + " characters: \"" + label + "\"");
            }
        }
    }

    /**
     * A value row has no bar, so its label only has to leave room for the value drawn from the right
     * edge - a credit figure is the longest of those.
     */
    @Test
    void noValueRowLabelCrowdsItsValue() {
        for (Language language : Language.values()) {
            for (String key : cardKeys()) {
                if (!key.startsWith(KEY_PREFIX + "row.") || PROGRESS_ROW_KEYS.contains(key)) continue;
                String label = MultiLingualTextProvider.getText(language, key);
                assertTrue(label.length() <= 24,
                        language + " " + key + " is " + label.length() + " characters: \"" + label + "\"");
            }
        }
    }

    private static Set<String> cardKeys() {
        return new TreeSet<>(bundleKeys(Language.EN).stream()
                .filter(key -> key.startsWith(KEY_PREFIX))
                .toList());
    }

    /**
     * Card key stems that are counted, taken from the {@code .one} form the base always declares.
     */
    private static Set<String> pluralKeyBases() {
        Set<String> bases = new TreeSet<>(cardKeys().stream()
                .filter(key -> key.endsWith(".one"))
                .map(key -> key.substring(0, key.length() - ".one".length()))
                .toList());
        assertFalse(bases.isEmpty(), "no counted overlay.card.* keys found to check");
        return bases;
    }

    /**
     * The categories a language's plural rule can actually select, taken from the rule itself so a
     * language added later is covered without editing this test.
     */
    private static Set<String> categoriesUsedBy(Language language) {
        Set<String> categories = new TreeSet<>();
        for (int count = 0; count <= 120; count++) {
            categories.add(StringUtls.pluralSuffix(language, count));
        }
        return categories;
    }

    private static Set<String> bundleKeys(Language language) {
        return bundle(language).stringPropertyNames();
    }

    /**
     * A language's own bundle file, with no fallback to English: a key English covers for is exactly
     * what these tests are looking for.
     */
    private static Properties bundle(Language language) {
        String path = language == Language.EN
                ? "/i18n/gui.properties"
                : "/i18n/gui_" + language.name().toLowerCase(Locale.ROOT) + ".properties";
        Properties props = new Properties();
        try (InputStream in = HudCardLocalizationTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing bundle on the classpath: " + path);
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AssertionError("could not read " + path, e);
        }
        return props;
    }

}
