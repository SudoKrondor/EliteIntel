package elite.intel.ui.theme;

import java.awt.*;

/**
 * HUD visual design tokens: colours, spacing/metric constants and semantic font roles.
 * Single source of truth for the dark cockpit palette, split out of {@link AppTheme} so the
 * token table is scannable on its own, apart from component factories and palette-apply logic.
 * Reference roles (e.g. {@link #HUD_FONT_TABLE_ROW}) in code, not raw literals.
 */
public final class HudPalette {

    private HudPalette() {
    }

    // -- HUD semantic color roles ---------------------------------------------
    // 60-30-10: dark blue-grey surfaces, cool grey lines and text, and one cyan accent.
    // Each role takes its colour from the active theme (ActiveTheme), once, when this class loads. The shipped
    // colours, and the names a theme file uses, are in ThemeRole: that is the only place a colour literal lives.

    public static final Color HUD_COLOR_ROLE_APPLICATION_BACKGROUND = ActiveTheme.color(ThemeRole.APPLICATION_BACKGROUND);
    public static final Color HUD_COLOR_ROLE_PANEL_BACKGROUND = ActiveTheme.color(ThemeRole.PANEL_BACKGROUND);
    public static final Color HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND = ActiveTheme.color(ThemeRole.TABLE_CELL_BACKGROUND);
    /**
     * Text cell of an input outside a grid: text fields, text areas, combos. A grid's combo stays on the table cell's.
     */
    public static final Color HUD_COLOR_ROLE_FIELD_BACKGROUND = ActiveTheme.color(ThemeRole.FIELD_BACKGROUND);
    public static final Color HUD_COLOR_ROLE_LOG_BACKGROUND = ActiveTheme.color(ThemeRole.LOG_BACKGROUND);
    public static final Color HUD_COLOR_ROLE_PRIMARY_TEXT = ActiveTheme.color(ThemeRole.PRIMARY_TEXT);
    /** Ship name readout in the top status bar. */
    public static final Color HUD_COLOR_ROLE_SHIP_NAME_TEXT = ActiveTheme.color(ThemeRole.SHIP_NAME_TEXT);
    public static final Color HUD_COLOR_ROLE_DIALOG_TITLE_TEXT = ActiveTheme.color(ThemeRole.DIALOG_TITLE_TEXT);
    public static final Color HUD_COLOR_ROLE_BUTTON_TEXT = ActiveTheme.color(ThemeRole.BUTTON_TEXT);
    public static final Color HUD_COLOR_ROLE_SECONDARY_TEXT = ActiveTheme.color(ThemeRole.SECONDARY_TEXT);
    /**
     * Muted key label in key→value readouts and telemetry blocks (§7). A step quieter than the secondary text.
     */
    public static final Color HUD_COLOR_ROLE_READOUT_LABEL = ActiveTheme.color(ThemeRole.READOUT_LABEL);
    /**
     * CMDR credit-balance readout (e.g. commander block, §7).
     */
    public static final Color HUD_COLOR_ROLE_CREDITS_TEXT = ActiveTheme.color(ThemeRole.CREDITS_TEXT);
    /** The accent as text, lines, frames and marks. Where the accent fills an area, that is {@link #HUD_COLOR_ROLE_ACCENT_FILL}. */
    public static final Color HUD_COLOR_ROLE_PRIMARY_ACTION = ActiveTheme.color(ThemeRole.PRIMARY_ACTION);
    /**
     * The accent as a fill under {@link #HUD_COLOR_ROLE_SELECTED_TEXT}: the selected segment, selected rows and items
     * in tables, lists and combos, selected text in fields, pressed subtle buttons and steppers, selected toggles,
     * held keys. A theme that leaves it unset shows the accent here ({@link ThemeRole#follows()}).
     */
    public static final Color HUD_COLOR_ROLE_ACCENT_FILL = ActiveTheme.color(ThemeRole.ACCENT_FILL);
    /**
     * A value at rest: field and combo text, table cells, read-only readouts. Neutral so the accent stays for what is active or selected.
     */
    public static final Color HUD_COLOR_ROLE_VALUE_TEXT = ActiveTheme.color(ThemeRole.VALUE_TEXT);
    public static final Color HUD_COLOR_ROLE_MONOSPACE_TEXT = ActiveTheme.color(ThemeRole.MONOSPACE_TEXT);
    public static final Color HUD_COLOR_ROLE_SELECTED_TEXT = ActiveTheme.color(ThemeRole.SELECTED_TEXT);
    /** Semi-transparent dark veil placed on the owner window glass pane while a modal dialog is open. */
    public static final Color HUD_COLOR_ROLE_MODAL_SCRIM = ActiveTheme.color(ThemeRole.MODAL_SCRIM);
    public static final Color HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND = ActiveTheme.color(ThemeRole.SECONDARY_PANEL_BACKGROUND);
    public static final Color HUD_COLOR_ROLE_DIALOG_BODY_BACKGROUND = ActiveTheme.color(ThemeRole.DIALOG_BODY_BACKGROUND);
    public static final Color HUD_COLOR_ROLE_DIALOG_HEADER_BACKGROUND = ActiveTheme.color(ThemeRole.DIALOG_HEADER_BACKGROUND);
    /**
     * Frame around an undecorated modal dialog window (section 7.2).
     */
    public static final Color HUD_COLOR_ROLE_DIALOG_FRAME_BORDER = ActiveTheme.color(ThemeRole.DIALOG_FRAME_BORDER);
    public static final Color HUD_COLOR_ROLE_FRAME_BORDER = ActiveTheme.color(ThemeRole.FRAME_BORDER);
    public static final Color HUD_COLOR_ROLE_SECONDARY_BORDER = ActiveTheme.color(ThemeRole.SECONDARY_BORDER);
    public static final Color HUD_COLOR_ROLE_CONTROL_DECORATION = ActiveTheme.color(ThemeRole.CONTROL_DECORATION);
    /** Semi-transparent selection surface: the standard HUD frame blue at half alpha. */
    public static final Color HUD_COLOR_ROLE_SELECTION_BACKGROUND = ActiveTheme.color(ThemeRole.SELECTION_BACKGROUND);
    /** Bright accent painted on an input frame and its corner marks while it holds keyboard focus. */
    public static final Color HUD_COLOR_ROLE_INPUT_FOCUS = ActiveTheme.color(ThemeRole.INPUT_FOCUS);
    public static final Color HUD_COLOR_ROLE_PANEL_SEPARATOR = ActiveTheme.color(ThemeRole.PANEL_SEPARATOR);
    public static final Color HUD_COLOR_ROLE_PRIMARY_BUTTON_BACKGROUND = ActiveTheme.color(ThemeRole.PRIMARY_BUTTON_BACKGROUND);
    public static final Color HUD_COLOR_ROLE_PRIMARY_BUTTON_HOVER_BACKGROUND = ActiveTheme.color(ThemeRole.PRIMARY_BUTTON_HOVER_BACKGROUND);
    public static final Color HUD_COLOR_ROLE_PRIMARY_BUTTON_PRESSED_BACKGROUND = ActiveTheme.color(ThemeRole.PRIMARY_BUTTON_PRESSED_BACKGROUND);
    public static final Color HUD_COLOR_ROLE_INFORMATION = ActiveTheme.color(ThemeRole.INFORMATION);
    /**
     * Background of an input a binding holds right now, in the bindings capture dialog: the HUD frame blue.
     */
    public static final Color HUD_COLOR_ROLE_ASSIGNED_INPUT_BACKGROUND = ActiveTheme.color(ThemeRole.ASSIGNED_INPUT_BACKGROUND);
    /*
     * The three state colours are tuned for colour-blind commanders, not only picked by eye. Red-green colour
     * blindness turns any green and red into two olives that differ only in brightness, and an ageing eye loses
     * blue-yellow contrast, so each pair is kept apart by lightness as well as hue: a light mint green, a
     * mid periwinkle blue, and a vermilion red that keeps its brightness for protanopes, who see a pure red
     * as dark. They stay distinct from each other and from the cyan accent under simulated deuteranopia,
     * protanopia and tritanopia, and each reads at 5:1 or better on the application background.
     * Retune them as a set, re-checking all three simulations.
     */
    public static final Color HUD_COLOR_ROLE_SUCCESS = ActiveTheme.color(ThemeRole.SUCCESS);
    public static final Color HUD_COLOR_ROLE_WARNING = ActiveTheme.color(ThemeRole.WARNING);
    public static final Color HUD_COLOR_ROLE_DANGER = ActiveTheme.color(ThemeRole.DANGER);
    public static final Color HUD_COLOR_ROLE_DISABLED = ActiveTheme.color(ThemeRole.DISABLED);
    /** More receded disabled tint for icon affordances, derived from the shared disabled role. */
    public static final Color HUD_COLOR_ROLE_DISABLED_ICON = new Color(
            HUD_COLOR_ROLE_DISABLED.getRed(), HUD_COLOR_ROLE_DISABLED.getGreen(), HUD_COLOR_ROLE_DISABLED.getBlue(), 128);
    public static final Color HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND = ActiveTheme.color(ThemeRole.TABLE_CELL_HOVER_BACKGROUND);
    public static final Color HUD_COLOR_ROLE_INFORMATION_MARK = ActiveTheme.color(ThemeRole.INFORMATION_MARK);
    public static final Color HUD_COLOR_ROLE_MAIN_TAB_ACTIVE_BACKGROUND = ActiveTheme.color(ThemeRole.MAIN_TAB_ACTIVE_BACKGROUND);
    public static final Color HUD_COLOR_ROLE_SECTION_TAB_ACTIVE_BACKGROUND = ActiveTheme.color(ThemeRole.SECTION_TAB_ACTIVE_BACKGROUND);
    public static final Color HUD_COLOR_ROLE_SECTION_TAB_ACTIVE_UNDERLINE = ActiveTheme.color(ThemeRole.SECTION_TAB_ACTIVE_UNDERLINE);
    /** Active left-of-thumb portion of a HudSlider track; this is a level indicator, not a danger signal. */
    public static final Color HUD_COLOR_ROLE_SLIDER_VALUE_TRACK = ActiveTheme.color(ThemeRole.SLIDER_VALUE_TRACK);
    /**
     * Plaque behind a HudSlider track, on which the rail and value fill are drawn.
     */
    public static final Color HUD_COLOR_ROLE_SLIDER_TRACK_BACKGROUND = ActiveTheme.color(ThemeRole.SLIDER_TRACK_BACKGROUND);
    /** Commander chat lane text (left cards + input caret). Green, distinct from the AI (blue) lane. */
    public static final Color HUD_COLOR_ROLE_USER_INPUT_LOG_TEXT = ActiveTheme.color(ThemeRole.USER_INPUT_LOG_TEXT);
    /** Commander chat lane marker/rail (left rail + bottom prompt glyph); own role so it can diverge from text. */
    public static final Color HUD_COLOR_ROLE_COMMANDER_MARKER = ActiveTheme.color(ThemeRole.COMMANDER_MARKER);
    public static final Color HUD_COLOR_ROLE_ASSISTANT_RESPONSE_LOG_TEXT = ActiveTheme.color(ThemeRole.ASSISTANT_RESPONSE_LOG_TEXT);
    /** Quiet, but still readable text: 5:1 on the application background, the floor for body text being 4.5:1. */
    public static final Color HUD_COLOR_ROLE_SYSTEM_LOG_TEXT = ActiveTheme.color(ThemeRole.SYSTEM_LOG_TEXT);
    public static final Color HUD_COLOR_ROLE_SYSTEM_LOG_TIMESTAMP_TEXT = ActiveTheme.color(ThemeRole.SYSTEM_LOG_TIMESTAMP_TEXT);

    public static final int HUD_GAP = 8;
    /** Tight gap between adjacent compact controls (e.g. icon actions in a section header). */
    public static final int HUD_GAP_TIGHT = HUD_GAP / 2; // =4
    /** Smaller gap between adjacent section-header icon actions. */
    public static final int HUD_GAP_HEADER_ACTION = HUD_GAP_TIGHT / 2; // =2
    /** Width and height of the shared HUD scrollbars. */
    public static final int HUD_SCROLLBAR_WIDTH = 7;
    /** Unified side inset for body and footer of all modals (see HudModalScaffold). */
    public static final int HUD_DIALOG_BODY_INSET = HUD_GAP * 2; // =16
    public static final int SHELL_GAP = 10;
    public static final int SCREEN_TOP_GAP = 12;
    public static final int HUD_PADDING = 10;
    public static final int HUD_PADDING_SMALL = 6;
    /** Width of the application-background separator stripe used between info-zone and content (checkbox, text field). */
    public static final int HUD_SEP_W = 3;
    /** Vertical inset for HUD combo box dropdown list cells (list-level padding, not field border). */
    public static final int HUD_COMBO_ITEM_INSET_V = 4;
    /** Horizontal inset for HUD combo box dropdown list cells (list-level padding, not field border). */
    public static final int HUD_COMBO_ITEM_INSET_H = 8;
    public static final int SUBTAB_CONTENT_GAP = HUD_GAP;
    public static final int HUD_BORDER_THICKNESS = 1;
    /** Thickness for high-visibility accent borders - modal dialogs and similar prominent frames. */
    public static final int HUD_BORDER_THICKNESS_ACCENT = 2;
    /** Width of the blinking typewriter caret shared by the conversation view and stream overlay. */
    public static final int HUD_CARET_WIDTH = HUD_BORDER_THICKNESS_ACCENT;
    /** Milliseconds over which a completed Vega response fades from its active conversation treatment. */
    public static final int HUD_CHAT_ACTIVE_HOLD_MS = 1_200;
    /** Repaint cadence for the short active-Vega fade in the conversation view. */
    public static final int HUD_CHAT_ACTIVE_FADE_FRAME_MS = 33;
    public static final int HUD_PANEL_ARC = 0;
    public static final int HUD_TOP_BAR_HEIGHT = 44;
    public static final int HUD_BADGE_HEIGHT = 20;
    public static final int HUD_FIELD_HEIGHT = 30;
    /** Preferred width for searchable editable picker combo boxes; height uses {@link #HUD_FIELD_HEIGHT}. */
    public static final int HUD_PICKER_FIELD_WIDTH  = 500;
    public static final int HUD_BUTTON_HEIGHT = 34;
    public static final int HUD_BUTTON_HEIGHT_COMPACT = 28;
    public static final int HUD_DIALOG_HEADER_HEIGHT = 44;
    public static final int HUD_TABLE_ROW_HEIGHT = 34;
    public static final int HUD_TABLE_HEADER_HEIGHT = 30;
    public static final int HUD_TABLE_ROW_HEIGHT_COMPACT = 26;
    public static final int HUD_TABLE_HEADER_HEIGHT_COMPACT = 22;
    public static final int HUD_ICON_MAIN = 42;
    public static final int HUD_ICON_NAV = 24;
    public static final int HUD_ICON_SMALL = 28;
    public static final int HUD_ICON_TABLE = 18;
    /** Smaller glyph size for subdued section-header actions. */
    public static final int HUD_ICON_HEADER_ACTION = HUD_ICON_TABLE - 2; // =16
    public static final int HUD_FORM_ROW_HEIGHT_COMPACT = 22;
    /** Row height of a stat readout ({@link elite.intel.ui.widget.HudStatCell}); reserves the emblem, label, value and bar. */
    public static final int HUD_STAT_CELL_HEIGHT = 72;
    /** Thickness of the bright data-fill line in a stat readout ({@link elite.intel.ui.widget.HudStatCell}). */
    public static final int HUD_STAT_CELL_BAR_THICKNESS = 7;
    /** Thickness of the dim full-width track behind the data fill in a stat readout. */
    public static final int HUD_STAT_CELL_TRACK_THICKNESS = 3;
    // HudSlider metrics (ED slider form, section 4): brown track plaque, rail with edge inset,
    // red fill, tall start tick, round thumb, value floating above the thumb.
    /** Total row height reserving space for the value rendered above the thumb. */
    public static final int HUD_SLIDER_HEIGHT = 44;
    /** Height of the warm brown track plaque. */
    public static final int HUD_SLIDER_TRACK_HEIGHT = 18;
    /** Thickness of the dim full-width rail line. */
    public static final int HUD_SLIDER_RAIL_THICKNESS = 2;
    /** Thickness of the red active-fill line. */
    public static final int HUD_SLIDER_FILL_THICKNESS = 3;
    /** Horizontal inset of the rail/fill from the plaque edges (line does not touch the borders). */
    public static final int HUD_SLIDER_EDGE_INSET = 10;
    /** Inset of the start tick (origin) and the thumb travel range from the plaque edges. */
    public static final int HUD_SLIDER_RANGE_INSET = 26;
    /** Diameter of the round thumb. */
    public static final int HUD_SLIDER_THUMB_DIAMETER = 16;
    /** Ring thickness of the thumb (white outline around the accent core). */
    public static final int HUD_SLIDER_THUMB_RING = 2;
    /**
     * Height reserved above the track for the value rendered over the thumb. The track band is
     * centred in the remaining height; use this as a top inset on the row label so the label
     * aligns with the slider track rather than the row top.
     */
    public static final int HUD_SLIDER_VALUE_AREA = 20;
    // HudMicMeter metrics (segmented vertical LED-VU mic level meter, section 4): LIVE + PEAK-trail columns.
    /** Number of discrete segments in each meter column. */
    public static final int HUD_METER_SEG_COUNT = 32;
    /** Vertical gap between segments. */
    public static final int HUD_METER_SEG_GAP = 2;
    /** Width of the live-level column. */
    public static final int HUD_METER_LIVE_W = 22;
    /** Width of the slim peak-hold trail column. */
    public static final int HUD_METER_PEAK_W = 8;
    /** Horizontal gap between the live and peak columns. */
    public static final int HUD_METER_COL_GAP = 4;
    /** Left gutter width for the zone scale labels (MAX/GATE/FLOOR/0). */
    public static final int HUD_METER_SCALE_W = 72;
    /** Bottom strip height reserving room for the big current-value readout + status sub-line. */
    public static final int HUD_METER_READOUT_H = 42;
    public static final float HUD_FONT_BASE = 12f;
    public static final float HUD_FONT_XS   = HUD_FONT_BASE - 1f;  // 11
    public static final float HUD_FONT_LOG_PANEL = HUD_FONT_BASE + 4f;       // 12
    public static final float HUD_FONT_SM   = HUD_FONT_BASE;       // 12
    public static final float HUD_FONT_MD   = HUD_FONT_BASE + 2f;  // 14
    public static final float HUD_FONT_LG   = HUD_FONT_BASE + 4f;  // 16

    /**
     * Semantic font roles. Reference roles in code, not steps or literals.
     * To change a specific element's size - update the role here; to shift all UI globally - change {@link #HUD_FONT_BASE}.
     */
    public static final float HUD_FONT_TABLE_ROW      = HUD_FONT_MD;   // 14
    public static final float HUD_FONT_TABLE_HEADER   = HUD_FONT_SM;   // 12
    public static final float HUD_FONT_FIELD_VALUE    = HUD_FONT_MD;   // 14
    public static final float HUD_FONT_FIELD_LABEL    = HUD_FONT_SM + 1f; // 13 - form/readout key labels (section 5.1)
    public static final float HUD_FONT_READOUT_KEY    = HUD_FONT_XS;   // 11
    public static final float HUD_FONT_READOUT_VALUE  = HUD_FONT_SM;   // 12
    public static final float HUD_FONT_SECTION_TITLE  = HUD_FONT_SM;   // 12
    public static final float HUD_FONT_TAB_MAIN       = HUD_FONT_LG;   // 16
    public static final float HUD_FONT_TAB_SECTION    = HUD_FONT_MD;   // 14 - second-level section tabs
    public static final float HUD_FONT_TAB_COMPACT    = HUD_FONT_SM;   // 12 - dense inner (compact) tabs
    public static final float HUD_FONT_BUTTON         = HUD_FONT_SM;   // 12
    public static final float HUD_FONT_CHECKBOX       = HUD_FONT_SM;   // 12f - checkbox form-control (section 4.1)
    public static final float HUD_FONT_ICON_BUTTON    = HUD_FONT_LG;  // 16f - icon buttons (info, etc.) in a 24x24 box
    public static final float HUD_FONT_BADGE_ROLE     = HUD_FONT_XS;   // 11
    public static final float HUD_FONT_COMMANDER_NAME = HUD_FONT_LG;   // 16 - CMDR/ship values in top bar, level with the app title
    public static final float HUD_FONT_APP_TITLE      = HUD_FONT_LG;   // 16 - app title in top bar
    public static final float HUD_FONT_BANNER         = HUD_FONT_MD;   // 14 - banner message text
    public static final float HUD_FONT_TOOLTIP        = HUD_FONT_SM;   // 12 - hover tooltip text (global ToolTip.font)
    // The Bind Editor's binding grid, larger than the compact tables (Alan, 2026-11-22):
    public static final float HUD_FONT_BIND_GRID_TAB          = HUD_FONT_LG;       // 16 - the tab row above the grid (ALL, GENERAL, ...)
    public static final float HUD_FONT_BIND_GRID_ROW          = HUD_FONT_MD;       // 14 - a binding's row
    public static final float HUD_FONT_BIND_GRID_GROUP        = HUD_FONT_LG;       // 16 - a group's sub-heading
    public static final float HUD_FONT_BIND_GRID_COLUMN_TITLE = HUD_FONT_LG + 2f;  // 18 - the pinned column titles
    public static final float HUD_FONT_BIND_GRID_SLOT_TITLE   = HUD_FONT_LG;       // 16 - Primary and Secondary, over their two columns
    public static final float HUD_FONT_BIND_GRID_SUBTITLE     = HUD_FONT_MD;       // 14 - Device and Input, under them
    public static final float HUD_FONT_BIND_GRID_SECTION      = HUD_FONT_LG;       // 16 - a section's header (GENERAL CONTROLS)
    // Out-of-scale display sizes:
    public static final float HUD_FONT_CLOCK          = 26f;
    public static final float HUD_FONT_STAT_LG        = 16f;
    public static final float HUD_FONT_STAT_VALUE     = 22f;  // prominent value in a stat cell (HudStatCell)
    /** LAF defaultFont - base font size for UI labels outside HUD roles (AppView). */
    public static final float HUD_FONT_UI_DEFAULT = 18f;
    /** LAF monospaceFont - base monospace font of the application (AppView). */
    public static final float HUD_FONT_MONO_BASE  = 20f;
    /** OBS overlay text (Electrolize), separate size from MONO_BASE. */
    public static final float HUD_FONT_OVERLAY    = 20f;
}
