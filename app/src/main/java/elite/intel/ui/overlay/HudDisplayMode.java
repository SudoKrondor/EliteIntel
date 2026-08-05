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
    BOTH,

    /**
     * A plain window for a VR capture tool - Desktop+, OVR Toolkit, Virtual
     * Desktop - to pin into the headset itself, instead of this app talking to
     * SteamVR at all.
     * <p>
     * Exists because {@link #VR} hands the compositor a full texture per typed
     * character, and on a streamed headset that has been reported as a heavy
     * frame-rate cost. A capture tool takes the window on the GPU, on its own
     * schedule, and gives the commander placement and curvature controls this
     * app does not have.
     * <p>
     * It is not {@link #DESKTOP} pointed at a capture tool. That window leans,
     * is see-through, and is a tool window - which capture pickers filter out,
     * so it cannot even be selected. This mode is the same shell drawn flat and
     * opaque, in a window those pickers list.
     */
    CAPTURE_WINDOW
}
