package elite.intel.i18n;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A bundle value with a {@code {0}} placeholder may not contain a bare ASCII apostrophe.
 * <p>
 * <b>The trap.</b> {@link MessageFormat} reads {@code '} as the start of a quoted run: everything up to the
 * next apostrophe is emitted verbatim, placeholders included. So the Italian
 * {@code ... a bordo l'ultima volta ..., {0} ore fa} rendered with the hour count MISSING and the word
 * spelled {@code lultima} - the number the sentence exists to say never appeared at all. Where a second
 * apostrophe closes the run it is milder and stranger: the placeholders inside vanish while later ones leak
 * out as the literal text {@code {1}}. The fix is always the same, {@code ''} for a literal apostrophe.
 * <p>
 * <b>Why a test and not care.</b> It is invisible three times over. Nothing throws, nothing logs, the key
 * exists and parity passes; French, Italian and Portuguese prose is full of apostrophes, and the damage
 * only appears in a language the reviewer probably does not read, in a line only spoken when some game
 * event fires. It reached production twice - once when a {@code {0}} was added to prose that had been
 * correct for a year, and once in an English string ({@code Vega's memory dumped to {0}}) - which is what a
 * cheap mechanical guard is for.
 * <p>
 * <b>Values with no placeholder are deliberately exempt.</b> Every text provider short-circuits -
 * {@code args.length == 0 ? pattern : MessageFormat.format(pattern, args)} - so a pattern that takes no
 * arguments is handed back untouched, and doubling ITS apostrophes would put a literal {@code ''} on the
 * screen. 326 values are in that state, correctly.
 */
class BundleQuotingTest {

    private static final List<String> FAMILIES =
            List.of("gui", "responses", "ed_events", "ai_action_aliases");

    /**
     * A placeholder proper - {@code {0}} - rather than any brace, which prose uses on its own.
     */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\d");

    private static List<String> bundlePaths() {
        List<String> paths = new ArrayList<>();
        for (String family : FAMILIES) {
            paths.add("/i18n/" + family + ".properties");
            Arrays.stream(Language.values())
                    .filter(language -> language != Language.EN)
                    .map(language -> language.name().toLowerCase(Locale.ROOT))
                    .forEach(suffix -> paths.add("/i18n/" + family + "_" + suffix + ".properties"));
        }
        return paths;
    }

    private static Properties load(String path) {
        Properties props = new Properties();
        try (InputStream in = BundleQuotingTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing bundle on classpath: " + path);
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AssertionError("could not read " + path, e);
        }
        return props;
    }

    /**
     * True when an apostrophe stands alone rather than as the {@code ''} MessageFormat reads as a literal
     * one. Doubled pairs are blanked out first, so what is left is unpaired by definition.
     */
    private static boolean hasBareApostrophe(String value) {
        return value.replace("''", "  ").indexOf('\'') >= 0;
    }

    @Test
    void noTranslatableStringLosesAPlaceholderToAnApostrophe() {
        List<String> offenders = new ArrayList<>();
        for (String path : bundlePaths()) {
            Properties props = load(path);
            props.stringPropertyNames().stream().sorted().forEach(key -> {
                String value = props.getProperty(key);
                if (!PLACEHOLDER.matcher(value).find() || !hasBareApostrophe(value)) return;
                offenders.add(path + " :: " + key + "\n      is:  " + value
                        + "\n      was: " + MessageFormat.format(value, "<0>", "<1>", "<2>", "<3>"));
            });
        }

        assertTrue(offenders.isEmpty(), () -> """
                %d bundle value(s) carry a placeholder and a bare apostrophe, so MessageFormat quotes the
                rest of the sentence instead: the "was" line is what the commander actually gets.
                
                Fix by doubling the apostrophe - l'ultima becomes l''ultima - in the bundle, NOT by
                removing the placeholder.
                
                %s"""
                .formatted(offenders.size(), String.join("\n\n", offenders)));
    }

    /**
     * The other half of the rule, and the reason the test above cannot simply demand {@code ''} everywhere:
     * a pattern nobody passes arguments to never reaches MessageFormat, so a doubled apostrophe in one
     * would be shown to the commander exactly as written.
     */
    @Test
    void aValueWithNoPlaceholderKeepsItsApostropheSingle() {
        List<String> offenders = new ArrayList<>();
        for (String path : bundlePaths()) {
            Properties props = load(path);
            props.stringPropertyNames().stream().sorted().forEach(key -> {
                String value = props.getProperty(key);
                if (PLACEHOLDER.matcher(value).find() || !value.contains("''")) return;
                offenders.add(path + " :: " + key + "\n      " + value);
            });
        }

        assertTrue(offenders.isEmpty(), () -> String.format(
                "%d value(s) take no arguments, so they are never formatted and the doubled apostrophe is "
                        + "printed literally. Use a single ':%n%n%s",
                offenders.size(), String.join("\n\n", offenders)));
    }
}
