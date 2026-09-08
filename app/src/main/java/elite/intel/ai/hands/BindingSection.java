package elite.intel.ai.hands;

/**
 * The four headings down the left of the game's OPTIONS &gt; CONTROLS screen, plus a fallback
 * for controls whose in-game row has not been read off the screen yet.
 * <p>
 * Declaration order is the order the game lists them in, and the Binding Profile tables are
 * rendered in that order so the app reads top-to-bottom like the screen it mirrors.
 */
public enum BindingSection {
    GENERAL("bindings.section.general"),
    SHIP("bindings.section.ship"),
    SRV("bindings.section.srv"),
    ON_FOOT("bindings.section.onFoot"),
    /**
     * Controls with no confirmed in-game name; shown under their raw XML tag, humanized.
     */
    OTHER("bindings.section.other");

    private final String labelKey;

    BindingSection(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getLabelKey() {
        return labelKey;
    }
}
