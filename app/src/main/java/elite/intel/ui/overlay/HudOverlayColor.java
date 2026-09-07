package elite.intel.ui.overlay;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The text colours of the HUD overlay card, one per role the renderer asks for.
 * <p>
 * Mirrors {@code ColorRole} in {@code overlay/src/hud.h}: the {@link #wireName()}
 * is what goes on the wire as {@code CFG col_<name>=RRGGBB}, and the defaults
 * here are the same hex values as the {@code HUD_COL_DEFAULT_*} table there. The
 * two tables are duplicated rather than generated because the binary has to draw
 * something sane when it is driven by hand ({@code overlay/test-feed.sh}) or by
 * an app too old to send colours at all - so both ends need their own copy.
 * <p>
 * The card's background is deliberately not in this list. Its colour is the one
 * surface the commander already controls, through the transparency slider, and
 * offering a picker as well would let a card be made opaque and bright enough to
 * hide the cockpit behind it.
 */
public enum HudOverlayColor {

    /**
     * Objective title, and any row value with no state of its own.
     */
    PRIMARY("primary", 0xFF7100, "overlay.settings.color.primary"),
    /**
     * A row reading "good" - a target met, a hold filled.
     */
    SUCCESS("success", 0x4FC56B, "overlay.settings.color.success"),
    /**
     * A row reading "warn" - a deadline closing in.
     */
    WARNING("warning", 0xFFB000, "overlay.settings.color.warning"),
    /**
     * A row reading "critical".
     */
    DANGER("danger", 0xD94F4F, "overlay.settings.color.danger"),
    /**
     * Subtitle, row labels, and the unfilled part of a progress bar.
     */
    DISABLED("disabled", 0x6E4A28, "overlay.settings.color.disabled"),
    /**
     * The commander's own words in the conversation.
     */
    USER("user", 0x4FC56B, "overlay.settings.color.user"),
    /**
     * The ship AI's reply.
     */
    AI("ai", 0x72A2B4, "overlay.settings.color.ai"),
    /**
     * Radio traffic: station control, carriers, other commanders.
     */
    RADIO("radio", 0xB78CD9, "overlay.settings.color.radio");

    private static final Logger log = LogManager.getLogger(HudOverlayColor.class);

    /**
     * Separators for the stored form, {@code primary=FF7100;radio=B78CD9}.
     * <p>
     * A string in one column rather than a column per role: the palette is read
     * and written whole, never a role at a time, and eight more columns on
     * {@code game_session} would need a migration every time a role is added.
     */
    private static final String ENTRY_SEPARATOR = ";";
    private static final String VALUE_SEPARATOR = "=";

    private final String wireName;
    private final int defaultRgb;
    private final String labelKey;

    HudOverlayColor(String wireName, int defaultRgb, String labelKey) {
        this.wireName = wireName;
        this.defaultRgb = defaultRgb;
        this.labelKey = labelKey;
    }

    public String wireName() {
        return wireName;
    }

    public Color defaultColor() {
        return new Color(defaultRgb);
    }

    /**
     * Key in the gui bundle for the row label in the settings dialog.
     */
    public String labelKey() {
        return labelKey;
    }

    /**
     * {@code RRGGBB}, upper case - the form both the wire and the stored string use.
     */
    public static String hex(Color color) {
        return String.format(Locale.ROOT, "%06X", color.getRGB() & 0xFFFFFF);
    }

    /**
     * Reads the stored overrides, keeping only what it understands.
     * <p>
     * Only roles the commander actually changed are stored, so a default that is
     * retuned in a later release reaches everyone who never overrode it. A role
     * this build does not know - a palette written by a newer version, then rolled
     * back - is dropped rather than refused, for the same reason the overlay
     * ignores an unknown CFG key: a settings string is not worth losing an
     * overlay over.
     */
    public static Map<HudOverlayColor, Color> parseOverrides(String stored) {
        Map<HudOverlayColor, Color> overrides = new LinkedHashMap<>();
        if (stored == null || stored.isBlank()) return overrides;

        for (String entry : stored.split(ENTRY_SEPARATOR)) {
            if (entry.isBlank()) continue;
            String[] parts = entry.split(VALUE_SEPARATOR, 2);
            if (parts.length != 2) {
                log.warn("Ignoring malformed HUD overlay colour entry: {}", entry);
                continue;
            }
            HudOverlayColor role = byWireName(parts[0].trim());
            Color color = parseHex(parts[1].trim());
            if (role != null && color != null) overrides.put(role, color);
        }
        return overrides;
    }

    /**
     * The inverse of {@link #parseOverrides}. An entry equal to the role's own
     * default is dropped, so "reset" and "chose the default colour by hand" are
     * the same stored state - there is no difference the commander could see.
     */
    public static String formatOverrides(Map<HudOverlayColor, Color> overrides) {
        StringBuilder out = new StringBuilder();
        for (HudOverlayColor role : values()) {
            Color color = overrides.get(role);
            if (color == null || color.equals(role.defaultColor())) continue;
            if (!out.isEmpty()) out.append(ENTRY_SEPARATOR);
            out.append(role.wireName()).append(VALUE_SEPARATOR).append(hex(color));
        }
        return out.toString();
    }

    private static HudOverlayColor byWireName(String name) {
        for (HudOverlayColor role : values()) {
            if (role.wireName.equals(name)) return role;
        }
        log.warn("Ignoring unknown HUD overlay colour role: {}", name);
        return null;
    }

    /**
     * {@code RRGGBB} only: six hex digits, no {@code #} and no alpha. The overlay
     * draws its text fully opaque - transparency there is the background's job -
     * so an alpha channel would be a value with nowhere to go.
     */
    private static Color parseHex(String value) {
        if (value.length() != 6) {
            log.warn("Ignoring HUD overlay colour that is not RRGGBB: {}", value);
            return null;
        }
        try {
            return new Color(Integer.parseInt(value, 16));
        } catch (NumberFormatException e) {
            log.warn("Ignoring HUD overlay colour that is not hex: {}", value);
            return null;
        }
    }
}
