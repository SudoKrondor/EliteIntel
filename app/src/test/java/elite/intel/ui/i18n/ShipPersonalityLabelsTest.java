package elite.intel.ui.i18n;

import elite.intel.ai.brain.ShipPersonality;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every {@link ShipPersonality} needs a {@code ship.personality.<lowercased enum name>} label in every gui
 * bundle, because that is the key {@code CommanderTabPanel#personalityLabel} builds.
 * <p>
 * WHY this is a test and not a code review item: nothing fails when the key is missing.
 * {@code MultiLingualTextProvider} returns the key itself for an unknown key, so the fleet grid's dropdown
 * quietly renders "SHIP.PERSONALITY.MOUTHY_MERC" and the app carries on. Adding a personality is a one-line
 * enum change that looks complete on its own, which is exactly how MOUTHY_MERC shipped without a label.
 */
class ShipPersonalityLabelsTest {

    /**
     * The one place the key shape is written down outside the panel that builds it.
     */
    private static String labelKey(ShipPersonality personality) {
        return "ship.personality." + personality.name().toLowerCase(Locale.ROOT);
    }

    private static Properties bundle(String suffix) {
        Properties props = new Properties();
        String path = "/i18n/gui" + suffix + ".properties";
        try (InputStream in = ShipPersonalityLabelsTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing bundle " + path);
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new AssertionError("could not read " + path, e);
        }
        return props;
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "_de", "_es", "_fr", "_it", "_pt", "_ptbz", "_ru", "_uk"})
    void everyPersonalityHasALabelInEveryBundle(String suffix) {
        Properties props = bundle(suffix);

        List<String> missing = new ArrayList<>();
        for (ShipPersonality personality : ShipPersonality.values()) {
            String key = labelKey(personality);
            if (!props.containsKey(key) || props.getProperty(key).isBlank()) {
                missing.add(key);
            }
        }

        assertTrue(missing.isEmpty(),
                "gui" + suffix + ".properties has no label for " + missing
                        + " - the fleet grid would show the raw key instead");
    }

    /**
     * A label left as a copy of its key would pass the presence check while still showing a raw key to the
     * commander, so the value is checked for the key shape too.
     */
    @ParameterizedTest
    @ValueSource(strings = {"", "_de", "_es", "_fr", "_it", "_pt", "_ptbz", "_ru", "_uk"})
    void noLabelIsJustTheKey(String suffix) {
        Properties props = bundle(suffix);

        for (ShipPersonality personality : ShipPersonality.values()) {
            String key = labelKey(personality);
            String value = props.getProperty(key, "");
            assertFalse(value.startsWith("ship.personality."),
                    "gui" + suffix + ".properties gives " + key + " the value " + value);
        }
    }

    /**
     * Guards the derivation itself: an enum name with characters the key shape cannot carry would break it.
     */
    @Test
    void everyEnumNameProducesAUsableKey() {
        for (ShipPersonality personality : ShipPersonality.values()) {
            assertTrue(labelKey(personality).matches("ship\\.personality\\.[a-z0-9_]+"),
                    personality + " does not produce a usable i18n key");
        }
    }
}
