package elite.intel.ui.overlay;

/**
 * Where the commander wants the HUD drawn.
 * <p>
 * This is not a question the app can answer for itself. A commander who owns a
 * headset does not always play in it, and one who plays in VR may still be
 * streaming or recording from the monitor - so the desktop overlay is not
 * automatically redundant just because a headset is connected.
 * <p>
 * Off is not a value here: switching the overlay off is what the toggle button
 * already does. This only decides where it goes when it is on.
 */
public enum HudDisplayMode {

    /**
     * Desktop window only. What every version before VR support did, and the
     * default, so a commander who never touches the setting is unaffected.
     */
    DESKTOP,

    /**
     * SteamVR overlay only. Falls back to a desktop window if VR cannot be had,
     * so the commander is never left with nothing on screen.
     */
    VR,

    /**
     * Both at once, as two child processes fed identical data. The VR child runs
     * with {@code --vr=only} so it exits rather than falling back - a fallback
     * here would put a second desktop window exactly on top of the first.
     */
    BOTH
}
