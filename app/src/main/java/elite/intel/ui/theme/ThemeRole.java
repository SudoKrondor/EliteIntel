package elite.intel.ui.theme;

import java.awt.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Every colour role a theme can set, with its shipped colour.
 * <p>
 * Each {@link HudPalette} colour role takes its value from here through the active theme
 * ({@link ActiveTheme}), so this table is the only place a literal app-window colour is written. The
 * defaults are the Default theme. {@link HudPalette} keeps the comment on what each role is for.
 * <p>
 * The <b>wire name</b> is what a theme file stores. It must never change once shipped, even when the constant is
 * renamed, or every theme a commander made or shared loses that colour. A role added later simply takes its default
 * in older files.
 * <p>
 * The defaults are {@code 0xAARRGGBB}: a role drawn see-through carries its alpha here.
 */
public enum ThemeRole {
    APPLICATION_BACKGROUND("applicationBackground", 0xFF0B0F14),
    PANEL_BACKGROUND("panelBackground", 0xFF111820),
    TABLE_CELL_BACKGROUND("tableCellBackground", 0xFF161F29),
    FIELD_BACKGROUND("fieldBackground", 0xFF0B0F14),
    LOG_BACKGROUND("logBackground", 0xFF111820),
    PRIMARY_TEXT("primaryText", 0xFFE6E6E6),
    SHIP_NAME_TEXT("shipNameText", 0xA633D7E8),
    DIALOG_TITLE_TEXT("dialogTitleText", 0xFFA9B8C2),
    BUTTON_TEXT("buttonText", 0xFFE6E6E6),
    SECONDARY_TEXT("secondaryText", 0xFFA9B8C2),
    READOUT_LABEL("readoutLabel", 0xFF7F909B),
    CREDITS_TEXT("creditsText", 0xA633D7E8),
    PRIMARY_ACTION("primaryAction", 0xA633D7E8),
    VALUE_TEXT("valueText", 0xFFE6E6E6),
    MONOSPACE_TEXT("monospaceText", 0xFFE6E6E6),
    SELECTED_TEXT("selectedText", 0xFF0B0F14),
    MODAL_SCRIM("modalScrim", 0x8C000000),
    SECONDARY_PANEL_BACKGROUND("secondaryPanelBackground", 0xFF111820),
    DIALOG_BODY_BACKGROUND("dialogBodyBackground", 0xFF0B0F14),
    DIALOG_HEADER_BACKGROUND("dialogHeaderBackground", 0xFF111820),
    DIALOG_FRAME_BORDER("dialogFrameBorder", 0xFF3A4C58),
    FRAME_BORDER("frameBorder", 0xFF3A4C58),
    SECONDARY_BORDER("secondaryBorder", 0xFF24313A),
    CONTROL_DECORATION("controlDecoration", 0xFF5F7480),
    SELECTION_BACKGROUND("selectionBackground", 0x802D5C66),
    INPUT_FOCUS("inputFocus", 0xFF5CE1EE),
    PANEL_SEPARATOR("panelSeparator", 0xFF3A4C58),
    PRIMARY_BUTTON_BACKGROUND("primaryButtonBackground", 0xFF1B6F7B),
    PRIMARY_BUTTON_HOVER_BACKGROUND("primaryButtonHoverBackground", 0xFF1B6F7B),
    PRIMARY_BUTTON_PRESSED_BACKGROUND("primaryButtonPressedBackground", 0xA633D7E8),
    INFORMATION("information", 0xA633D7E8),
    ASSIGNED_INPUT_BACKGROUND("assignedInputBackground", 0xFF2D5C66),
    SUCCESS("success", 0xFF7CE8A8),
    WARNING("warning", 0xFF8FA6FF),
    DANGER("danger", 0xFFE8602C),
    DISABLED("disabled", 0xFF566874),
    TABLE_CELL_HOVER_BACKGROUND("tableCellHoverBackground", 0xFF161F29),
    INFORMATION_MARK("informationMark", 0xA633D7E8),
    MAIN_TAB_ACTIVE_BACKGROUND("mainTabActiveBackground", 0xBF6EA7FF),
    SECTION_TAB_ACTIVE_BACKGROUND("sectionTabActiveBackground", 0xA633D7E8),
    SECTION_TAB_ACTIVE_UNDERLINE("sectionTabActiveUnderline", 0xA633D7E8),
    SLIDER_VALUE_TRACK("sliderValueTrack", 0xA633D7E8),
    SLIDER_TRACK_BACKGROUND("sliderTrackBackground", 0xFF161F29),
    USER_INPUT_LOG_TEXT("userInputLogText", 0xFF7CE8A8),
    COMMANDER_MARKER("commanderMarker", 0xFF7CE8A8),
    ASSISTANT_RESPONSE_LOG_TEXT("assistantResponseLogText", 0xFF72A2B4),
    SYSTEM_LOG_TEXT("systemLogText", 0xFF76848D),
    SYSTEM_LOG_TIMESTAMP_TEXT("systemLogTimestampText", 0xFF76848D);

    private static final Map<String, ThemeRole> BY_WIRE_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ThemeRole::wireName, Function.identity()));

    private final String wireName;
    private final Color defaultColor;

    ThemeRole(String wireName, int defaultArgb) {
        this.wireName = wireName;
        this.defaultColor = new Color(defaultArgb, true);
    }

    /** The role's name in a theme file. */
    public String wireName() {
        return wireName;
    }

    /** The role's colour in the Default theme. */
    public Color defaultColor() {
        return defaultColor;
    }

    /** The role a theme file names, or empty for a name this build does not know. */
    public static Optional<ThemeRole> byWireName(String wireName) {
        return Optional.ofNullable(BY_WIRE_NAME.get(wireName));
    }
}
