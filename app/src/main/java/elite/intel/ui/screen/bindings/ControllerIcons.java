package elite.intel.ui.screen.bindings;

import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The picture beside a controller on the reconcile's screens (multi-install-reconcile.md): a gamepad, joystick,
 * throttle, yoke or pedals, guessed from what Windows calls it and the names the player gave it. A guess, so it only
 * ever decorates - the name beside it is what the player reads. Anything not recognised is a joystick, the commonest
 * flight controller.
 * <p>
 * The images are white silhouettes, tinted to the theme's secondary text colour so they follow the active theme.
 */
final class ControllerIcons {

    /** What a controller looks like. */
    enum Kind {
        CONTROLLER("/images/controller.png"),
        JOYSTICK("/images/joystick.png"),
        THROTTLE("/images/throttle.png"),
        YOKE("/images/yoke.png"),
        PEDALS("/images/pedals.png");

        private final String resource;

        Kind(String resource) {
            this.resource = resource;
        }
    }

    /** The size of the picture in a table row. */
    static final int TABLE = 28;
    /** The size of the picture beside a question's controller. */
    static final int LARGE = 30;
    /** The box the large picture sits in. */
    static final int BOX = 46;

    // WHY: checked in this order, so "throttle pedals" is pedals and a "flight yoke system" is a yoke.
    private static final Pattern PEDALS = Pattern.compile("pedal|rudder|\\btpr\\b|\\bmfg\\b|\\bcrosswind\\b");
    private static final Pattern YOKE = Pattern.compile("yoke");
    private static final Pattern THROTTLE = Pattern.compile("throttle|\\bthr\\b|\\btqs\\b|\\bcm[23]\\b|\\btwcs\\b");
    private static final Pattern GAMEPAD = Pattern.compile(
            "gamepad|game ?pad|xbox|dualshock|dualsense|wireless controller|game controller|\\bcontroller\\b");

    private static final Map<Kind, Map<Integer, Icon>> CACHE = new EnumMap<>(Kind.class);

    private ControllerIcons() {
    }

    /**
     * The picture for a controller called by any of {@code names} - Windows' product name and the player's aliases.
     * Pure.
     */
    static Kind kindOf(Collection<String> names) {
        String all = String.join(" ", names).toLowerCase(Locale.ROOT);
        if (PEDALS.matcher(all).find()) return Kind.PEDALS;
        if (YOKE.matcher(all).find()) return Kind.YOKE;
        if (THROTTLE.matcher(all).find()) return Kind.THROTTLE;
        if (GAMEPAD.matcher(all).find()) return Kind.CONTROLLER;
        return Kind.JOYSTICK;
    }

    /** The picture at {@code size} pixels, tinted to the theme; {@code null} if the image is missing. On the EDT. */
    static Icon icon(Kind kind, int size) {
        return CACHE.computeIfAbsent(kind, k -> new java.util.HashMap<>()).computeIfAbsent(size, s -> {
            if (ControllerIcons.class.getResource(kind.resource) == null) return null;
            ImageIcon source = HudGlyphs.scaledIcon(ControllerIcons.class, kind.resource, s);
            return HudGlyphs.tintIcon(source, s, s, HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        });
    }

    /** The large picture in a bordered box, as the mockups show it beside a question's controller. */
    static JComponent boxed(Kind kind) {
        JLabel label = new JLabel(icon(kind, LARGE));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(HudPalette.HUD_COLOR_ROLE_PANEL_BACKGROUND);
        label.setBorder(BorderFactory.createLineBorder(HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER));
        Dimension size = new Dimension(BOX, BOX);
        label.setPreferredSize(size);
        label.setMinimumSize(size);
        label.setMaximumSize(size);
        return label;
    }
}
