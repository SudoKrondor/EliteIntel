package elite.intel.ui.theme;

import java.awt.*;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * A colour theme for the app window: a name, who made it, and the colours it changes.
 * <p>
 * A theme holds only the roles whose colour differs from the Default theme ({@link ThemeRole#defaultColor()}). An
 * entry equal to the default is dropped on the way in, so "left alone" and "set to the default by hand" are the same
 * theme, and a default retuned in a later release reaches every theme that never changed that role.
 *
 * @param name      shown in the theme list; never blank
 * @param author    who made it, or empty
 * @param overrides the roles this theme changes, unmodifiable
 */
public record ColorTheme(String name, String author, Map<ThemeRole, Color> overrides) {

    /** The shipped colours. It changes no role. */
    public static final ColorTheme DEFAULT = new ColorTheme("Default", "", Map.of());

    public ColorTheme {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) throw new IllegalArgumentException("A theme needs a name");
        author = author == null ? "" : author;
        overrides = withoutDefaults(overrides);
    }

    /** The colour this theme gives {@code role}: its own, or the default. */
    public Color color(ThemeRole role) {
        return overrides.getOrDefault(role, role.defaultColor());
    }

    private static Map<ThemeRole, Color> withoutDefaults(Map<ThemeRole, Color> overrides) {
        Map<ThemeRole, Color> kept = new EnumMap<>(ThemeRole.class);
        overrides.forEach((role, color) -> {
            if (!color.equals(role.defaultColor())) kept.put(role, color);
        });
        return Collections.unmodifiableMap(kept);
    }
}
