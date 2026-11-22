package elite.intel.ui.theme;

import java.awt.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static elite.intel.ui.theme.ThemeRole.Kind.BACKGROUND;
import static elite.intel.ui.theme.ThemeRole.Kind.FOREGROUND;

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
 * <p>
 * Each role is a {@link Kind#BACKGROUND} or a {@link Kind#FOREGROUND}. The Color Themes editor never changes the
 * one kind along with the other, even where both show the same colour: changing text and finding a background
 * changed with it surprised the user (2026-11-14).
 */
public enum ThemeRole {
    APPLICATION_BACKGROUND("applicationBackground", 0xFF0B0F14, BACKGROUND),
    PANEL_BACKGROUND("panelBackground", 0xFF111820, BACKGROUND),
    TABLE_CELL_BACKGROUND("tableCellBackground", 0xFF161F29, BACKGROUND),
    FIELD_BACKGROUND("fieldBackground", 0xFF0B0F14, BACKGROUND),
    LOG_BACKGROUND("logBackground", 0xFF111820, BACKGROUND),
    PRIMARY_TEXT("primaryText", 0xFFE6E6E6, FOREGROUND),
    SHIP_NAME_TEXT("shipNameText", 0xA633D7E8, FOREGROUND),
    DIALOG_TITLE_TEXT("dialogTitleText", 0xFFA9B8C2, FOREGROUND),
    BUTTON_TEXT("buttonText", 0xFFE6E6E6, FOREGROUND),
    SECONDARY_TEXT("secondaryText", 0xFFA9B8C2, FOREGROUND),
    READOUT_LABEL("readoutLabel", 0xFF7F909B, FOREGROUND),
    CREDITS_TEXT("creditsText", 0xA633D7E8, FOREGROUND),
    PRIMARY_ACTION("primaryAction", 0xA633D7E8, FOREGROUND),
    VALUE_TEXT("valueText", 0xFFE6E6E6, FOREGROUND),
    MONOSPACE_TEXT("monospaceText", 0xFFE6E6E6, FOREGROUND),
    SELECTED_TEXT("selectedText", 0xFF0B0F14, FOREGROUND),
    MODAL_SCRIM("modalScrim", 0x8C000000, BACKGROUND),
    SECONDARY_PANEL_BACKGROUND("secondaryPanelBackground", 0xFF111820, BACKGROUND),
    DIALOG_BODY_BACKGROUND("dialogBodyBackground", 0xFF0B0F14, BACKGROUND),
    DIALOG_HEADER_BACKGROUND("dialogHeaderBackground", 0xFF111820, BACKGROUND),
    DIALOG_FRAME_BORDER("dialogFrameBorder", 0xFF3A4C58, FOREGROUND),
    FRAME_BORDER("frameBorder", 0xFF3A4C58, FOREGROUND),
    SECONDARY_BORDER("secondaryBorder", 0xFF24313A, FOREGROUND),
    CONTROL_DECORATION("controlDecoration", 0xFF5F7480, FOREGROUND),
    SELECTION_BACKGROUND("selectionBackground", 0x802D5C66, BACKGROUND),
    INPUT_FOCUS("inputFocus", 0xFF5CE1EE, FOREGROUND),
    PANEL_SEPARATOR("panelSeparator", 0xFF3A4C58, FOREGROUND),
    PRIMARY_BUTTON_BACKGROUND("primaryButtonBackground", 0xFF1B6F7B, BACKGROUND),
    PRIMARY_BUTTON_HOVER_BACKGROUND("primaryButtonHoverBackground", 0xFF1B6F7B, BACKGROUND),
    PRIMARY_BUTTON_PRESSED_BACKGROUND("primaryButtonPressedBackground", 0xA633D7E8, BACKGROUND),
    INFORMATION("information", 0xA633D7E8, FOREGROUND),
    ASSIGNED_INPUT_BACKGROUND("assignedInputBackground", 0xFF2D5C66, BACKGROUND),
    SUCCESS("success", 0xFF7CE8A8, FOREGROUND),
    WARNING("warning", 0xFF8FA6FF, FOREGROUND),
    DANGER("danger", 0xFFE8602C, FOREGROUND),
    DISABLED("disabled", 0xFF566874, FOREGROUND),
    TABLE_CELL_HOVER_BACKGROUND("tableCellHoverBackground", 0xFF161F29, BACKGROUND),
    INFORMATION_MARK("informationMark", 0xA633D7E8, FOREGROUND),
    MAIN_TAB_ACTIVE_BACKGROUND("mainTabActiveBackground", 0xBF6EA7FF, BACKGROUND),
    SECTION_TAB_ACTIVE_BACKGROUND("sectionTabActiveBackground", 0xA633D7E8, BACKGROUND),
    SECTION_TAB_ACTIVE_UNDERLINE("sectionTabActiveUnderline", 0xA633D7E8, FOREGROUND),
    SLIDER_VALUE_TRACK("sliderValueTrack", 0xA633D7E8, BACKGROUND),
    SLIDER_TRACK_BACKGROUND("sliderTrackBackground", 0xFF161F29, BACKGROUND),
    USER_INPUT_LOG_TEXT("userInputLogText", 0xFF7CE8A8, FOREGROUND),
    COMMANDER_MARKER("commanderMarker", 0xFF7CE8A8, FOREGROUND),
    ASSISTANT_RESPONSE_LOG_TEXT("assistantResponseLogText", 0xFF72A2B4, FOREGROUND),
    SYSTEM_LOG_TEXT("systemLogText", 0xFF76848D, FOREGROUND),
    SYSTEM_LOG_TIMESTAMP_TEXT("systemLogTimestampText", 0xFF76848D, FOREGROUND),
    ACCENT_FILL("accentFill", 0xA633D7E8, BACKGROUND);

    private static final Map<String, ThemeRole> BY_WIRE_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ThemeRole::wireName, Function.identity()));

    /** What a role paints: an area other parts are drawn on, or the parts drawn on it. */
    public enum Kind {
        /** A fill other parts sit on: panels, fields, selections, pressed buttons, tracks, the dialog shade. */
        BACKGROUND,
        /** Text, lines, frames and marks. */
        FOREGROUND
    }

    private final String wireName;
    private final Color defaultColor;
    private final Kind kind;

    ThemeRole(String wireName, int defaultArgb, Kind kind) {
        this.wireName = wireName;
        this.defaultColor = new Color(defaultArgb, true);
        this.kind = kind;
    }

    /** Whether the role is a background or text, lines and marks. */
    public Kind kind() {
        return kind;
    }

    /**
     * The role whose colour this one shows in a theme that leaves it unset, or empty for a role that falls back to its
     * own default. WHY: {@link #ACCENT_FILL} was split from {@link #PRIMARY_ACTION} after themes were shared, and an
     * orange theme with no {@code accentFill} must keep orange selections rather than gain the Default's cyan.
     */
    public Optional<ThemeRole> follows() {
        return this == ACCENT_FILL ? Optional.of(PRIMARY_ACTION) : Optional.empty();
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
