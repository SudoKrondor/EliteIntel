package elite.intel.ui.screen.bindings;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every reconcile wizard string, in every language, formats cleanly - the MessageFormat apostrophe trap (a single
 * {@code '} in a string with arguments swallows the rest) and choice formats nested inside each other. Each is
 * formatted with every argument 1, then every argument 3, so both sides of a choice are tried.
 */
class ReconcileTextTest {

    private static final List<String> BUNDLES = List.of("gui", "gui_de", "gui_es", "gui_fr", "gui_it", "gui_pt",
            "gui_ptbz", "gui_ru", "gui_uk");

    @Test
    void everyWizardStringFormatsWithoutAQuoteOrABraceLeft() throws IOException {
        List<String> broken = new ArrayList<>();
        int checked = 0;
        for (String bundle : BUNDLES) {
            Properties texts = load(bundle);
            for (String key : texts.stringPropertyNames()) {
                if (!key.startsWith("bindings.reconcile.")) continue;
                String pattern = texts.getProperty(key);
                if (!pattern.contains("{")) continue;
                for (int value : new int[]{1, 3}) {
                    String text = new MessageFormat(pattern, Locale.ROOT).format(args(value));
                    checked++;
                    if (text.contains("{") || text.contains("}") || text.contains("''")) {
                        broken.add(bundle + " " + key + " -> " + text);
                    }
                    boolean apostropheIn = pattern.replace("''", "").contains("'");
                    if (apostropheIn) broken.add(bundle + " " + key + " has a single quote: " + pattern);
                }
            }
        }
        assertTrue(checked > 100, "found the strings: " + checked);
        assertEquals(List.of(), broken);
    }

    @Test
    void everyLanguageHasEveryWizardString() throws IOException {
        Properties english = load("gui");
        List<String> keys = english.stringPropertyNames().stream()
                .filter(key -> key.startsWith("bindings.reconcile.")).sorted().toList();
        for (String bundle : BUNDLES) {
            Properties texts = load(bundle);
            List<String> missing = keys.stream().filter(key -> !texts.containsKey(key)).toList();
            assertEquals(List.of(), missing, bundle);
        }
    }

    private static Object[] args(int value) {
        Object[] args = new Object[4];
        java.util.Arrays.fill(args, value);
        return args;
    }

    private static Properties load(String bundle) throws IOException {
        Properties texts = new Properties();
        try (InputStream in = ReconcileTextTest.class.getResourceAsStream("/i18n/" + bundle + ".properties")) {
            if (in == null) throw new IOException("no bundle " + bundle);
            texts.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return texts;
    }
}
