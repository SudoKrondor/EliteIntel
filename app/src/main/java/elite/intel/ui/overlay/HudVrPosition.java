package elite.intel.ui.overlay;

import java.util.Locale;

/**
 * Where the HUD hangs in the headset, as a point of the compass on the
 * commander's forward view.
 * <p>
 * In VR there is no window to drag: the card is drawn by the SteamVR compositor
 * in world space, and a commander wearing a headset cannot reach the app's
 * settings window to nudge it. So placement is a small fixed set of directions
 * chosen up front, and the distance and size are the overlay's business.
 * <p>
 * Named for the view rather than for the compass (NORTH/EAST/...) because Elite
 * already has a compass, pointing at planetary north, and a HUD setting that
 * said NORTH would be read as pointing at that.
 * <p>
 * This decides direction only. Which way is "ahead" is SteamVR's seated origin,
 * so the commander re-places the whole card - and the cockpit with it - using
 * the recentre they already use.
 */
public enum HudVrPosition {

    TOP,
    TOP_RIGHT,
    RIGHT,
    BOTTOM_RIGHT,
    /**
     * Below centre, and the default: it is where the VR overlay sat before the
     * setting existed, and it is the one direction that does not cover either
     * the reticle or the scanner.
     */
    BOTTOM,
    BOTTOM_LEFT,
    LEFT,
    TOP_LEFT;

    public static final HudVrPosition DEFAULT = BOTTOM;

    /**
     * The name the overlay's {@code CFG vrpos=} field carries.
     */
    public String wireName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Reads a stored value leniently: anything unrecognised, including null from
     * a row written before the column existed, means the default placement.
     * <p>
     * A bad value must not cost the commander their HUD, and every value here
     * leaves it somewhere they can see.
     */
    public static HudVrPosition parse(String stored) {
        if (stored == null) return DEFAULT;
        try {
            return valueOf(stored.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
    }
}
