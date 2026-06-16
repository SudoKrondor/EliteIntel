package elite.intel.ui.theme;

import java.awt.Color;

/**
 * HUD visual design tokens: colours, spacing/metric constants and semantic font roles.
 * Single source of truth for the dark cockpit palette, split out of {@link AppTheme} so the
 * token table is scannable on its own, apart from component factories and palette-apply logic.
 * Reference roles (e.g. {@link #HUD_FONT_TABLE_ROW}) in code, not raw literals.
 */
public final class HudPalette {

    private HudPalette() {
    }

    // -- Raw colors ------------------------------------------------------------

    public static final Color HUD_COLOR_000000 = new Color(0x000000);
    public static final Color HUD_COLOR_8C000000 = new Color(0x8C000000, true);
    public static final Color HUD_COLOR_090D12 = new Color(0x090D12);
    public static final Color HUD_COLOR_101721 = new Color(0x101721);
    public static final Color HUD_COLOR_151519 = new Color(0x151519);
    public static final Color HUD_COLOR_1A1206 = new Color(0x1A1206);
    public static final Color HUD_COLOR_24313A = new Color(0x24313A);
    public static final Color HUD_COLOR_2D5C66 = new Color(0x2D5C66);
    public static final Color HUD_COLOR_33D7E8 = new Color(0x33D7E8);
    public static final Color HUD_COLOR_4FC56B = new Color(0x4FC56B);
    public static final Color HUD_COLOR_5A6368 = new Color(0x5A6368);
    public static final Color HUD_COLOR_6E4A28 = new Color(0x6E4A28);
    public static final Color HUD_COLOR_72A2B4 = new Color(0x72A2B4);
    public static final Color HUD_COLOR_8B0101 = new Color(0x8B0101);
    public static final Color HUD_COLOR_9A6A3C = new Color(0x9A6A3C);
    public static final Color HUD_COLOR_B85A14 = new Color(0xB85A14);
    public static final Color HUD_COLOR_BB7A32 = new Color(0xBB7A32);
    public static final Color HUD_COLOR_C2C2C2 = new Color(0xC2C2C2);
    public static final Color HUD_COLOR_D94F4F = new Color(0xD94F4F);
    public static final Color HUD_COLOR_E6E6E6 = new Color(0xE6E6E6);
    public static final Color HUD_COLOR_FF2E00 = new Color(0xFF2E00);
    public static final Color HUD_COLOR_FF7100 = new Color(0xFF7100);
    public static final Color HUD_COLOR_FFB000 = new Color(0xE39E00);

    // -- HUD semantic color roles ---------------------------------------------

    public static final Color HUD_COLOR_ROLE_APPLICATION_BACKGROUND = HUD_COLOR_090D12;
    public static final Color HUD_COLOR_ROLE_PANEL_BACKGROUND = HUD_COLOR_101721;
    public static final Color HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND = HUD_COLOR_1A1206;
    public static final Color HUD_COLOR_ROLE_LOG_BACKGROUND = HUD_COLOR_101721;
    public static final Color HUD_COLOR_ROLE_PRIMARY_TEXT = HUD_COLOR_E6E6E6;
    public static final Color HUD_COLOR_ROLE_DIALOG_TITLE_TEXT = HUD_COLOR_C2C2C2;
    public static final Color HUD_COLOR_ROLE_BUTTON_TEXT = HUD_COLOR_E6E6E6;
    public static final Color HUD_COLOR_ROLE_SECONDARY_TEXT = HUD_COLOR_9A6A3C;
    public static final Color HUD_COLOR_ROLE_PRIMARY_ACTION = HUD_COLOR_FF7100;
    public static final Color HUD_COLOR_ROLE_MONOSPACE_TEXT = HUD_COLOR_E6E6E6;
    public static final Color HUD_COLOR_ROLE_SELECTED_TEXT = HUD_COLOR_101721;
    public static final Color HUD_COLOR_ROLE_STREAM_OVERLAY_BACKGROUND = HUD_COLOR_000000;
    /** Semi-transparent dark veil placed on the owner window glass pane while a modal dialog is open. */
    public static final Color HUD_COLOR_ROLE_MODAL_SCRIM = HUD_COLOR_8C000000;
    public static final Color HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND = HUD_COLOR_101721;
    public static final Color HUD_COLOR_ROLE_DIALOG_BODY_BACKGROUND = HUD_COLOR_090D12;
    public static final Color HUD_COLOR_ROLE_DIALOG_HEADER_BACKGROUND = HUD_COLOR_101721;
    /** Accent frame around an undecorated modal dialog window (section 7.2). */
    public static final Color HUD_COLOR_ROLE_DIALOG_FRAME_BORDER = HUD_COLOR_FF7100;
    public static final Color HUD_COLOR_ROLE_FRAME_BORDER = HUD_COLOR_2D5C66;
    public static final Color HUD_COLOR_ROLE_SECONDARY_BORDER = HUD_COLOR_24313A;
    public static final Color HUD_COLOR_ROLE_CONTROL_DECORATION = HUD_COLOR_B85A14;
    public static final Color HUD_COLOR_ROLE_PANEL_SEPARATOR = HUD_COLOR_1A1206;
    public static final Color HUD_COLOR_ROLE_PRIMARY_BUTTON_BACKGROUND = HUD_COLOR_B85A14;
    public static final Color HUD_COLOR_ROLE_PRIMARY_BUTTON_HOVER_BACKGROUND = HUD_COLOR_B85A14;
    public static final Color HUD_COLOR_ROLE_PRIMARY_BUTTON_PRESSED_BACKGROUND = HUD_COLOR_FF7100;
    public static final Color HUD_COLOR_ROLE_INFORMATION = HUD_COLOR_33D7E8;
    public static final Color HUD_COLOR_ROLE_SUCCESS = HUD_COLOR_4FC56B;
    public static final Color HUD_COLOR_ROLE_WARNING = HUD_COLOR_FFB000;
    public static final Color HUD_COLOR_ROLE_DANGER = HUD_COLOR_D94F4F;
    public static final Color HUD_COLOR_ROLE_DISABLED = HUD_COLOR_6E4A28;
    public static final Color HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND = HUD_COLOR_1A1206;
    public static final Color HUD_COLOR_ROLE_INFORMATION_MARK = HUD_COLOR_33D7E8;
    public static final Color HUD_COLOR_ROLE_MAIN_TAB_ACTIVE_BACKGROUND = HUD_COLOR_FFB000;
    public static final Color HUD_COLOR_ROLE_SECTION_TAB_ACTIVE_BACKGROUND = HUD_COLOR_FF7100;
    public static final Color HUD_COLOR_ROLE_SECTION_TAB_ACTIVE_UNDERLINE = HUD_COLOR_FF7100;
    public static final Color HUD_COLOR_ROLE_WARNING_PANEL_BACKGROUND = HUD_COLOR_1A1206;
    /** Active left-of-thumb portion of a HudSlider track; this is a level indicator, not a danger signal. */
    public static final Color HUD_COLOR_ROLE_SLIDER_VALUE_TRACK = HUD_COLOR_FF2E00;
    public static final Color HUD_COLOR_ROLE_USER_INPUT_LOG_TEXT = HUD_COLOR_BB7A32;
    public static final Color HUD_COLOR_ROLE_ASSISTANT_RESPONSE_LOG_TEXT = HUD_COLOR_72A2B4;
    public static final Color HUD_COLOR_ROLE_SYSTEM_LOG_TEXT = HUD_COLOR_5A6368;
    public static final Color HUD_COLOR_ROLE_SYSTEM_LOG_TIMESTAMP_TEXT = HUD_COLOR_5A6368;

    public static final int HUD_GAP = 8;
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
    public static final int HUD_FORM_ROW_HEIGHT_COMPACT = 22;
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
    public static final float HUD_FONT_SM   = HUD_FONT_BASE;       // 12
    public static final float HUD_FONT_MD   = HUD_FONT_BASE + 2f;  // 14
    public static final float HUD_FONT_LG   = HUD_FONT_BASE + 4f;  // 16

    /**
     * Semantic font roles. Reference roles in code, not steps or literals.
     * To change a specific element's size - update the role here; to shift all UI globally - change {@link #HUD_FONT_BASE}.
     */
    public static final float HUD_FONT_TABLE_ROW      = HUD_FONT_MD;   // 14
    public static final float HUD_FONT_TABLE_HEADER   = HUD_FONT_SM;   // 12
    public static final float HUD_FONT_FIELD_VALUE    = HUD_FONT_SM;   // 12
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
    public static final float HUD_FONT_COMMANDER_NAME = HUD_FONT_MD;   // 14
    public static final float HUD_FONT_APP_TITLE      = HUD_FONT_LG;   // 16 - app title in top bar
    public static final float HUD_FONT_BANNER         = HUD_FONT_XS;   // 11 - banner message text
    // Out-of-scale display sizes:
    public static final float HUD_FONT_CLOCK          = 26f;
    public static final float HUD_FONT_STAT_LG        = 16f;
    /** LAF defaultFont - base font size for UI labels outside HUD roles (AppView). */
    public static final float HUD_FONT_UI_DEFAULT = 18f;
    /** LAF monospaceFont - base monospace font of the application (AppView). */
    public static final float HUD_FONT_MONO_BASE  = 20f;
    /** OBS overlay text (Electrolize), separate size from MONO_BASE. */
    public static final float HUD_FONT_OVERLAY    = 20f;
}
