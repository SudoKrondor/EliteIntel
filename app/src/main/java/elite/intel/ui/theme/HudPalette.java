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

    public static final Color BG = new Color(0x151519);
    public static final Color LOG_BG = new Color(0x171927);
    public static final Color BG_PANEL = new Color(0x1F2032);
    public static final Color FG = new Color(0xE6E6E6);
    public static final Color HUD_DIALOG_TITLE_FG = new Color(0xC2C2C2);
    public static final Color BUTTON_FG = new Color(0xFFFFFF);
    public static final Color BUTTON_BG = new Color(0x03529F);
    public static final Color FG_MUTED = new Color(0x9A6A3C);
    public static final Color ACCENT = new Color(0xFF7100);
    public static final Color CONSOLE_FG = new Color(0xE0FFEF);
    public static final Color SEL_BG = new Color(0xE0FFEF);
    public static final Color SEL_FG = new Color(0x13181D);
    public static final Color TAB_UNSELECTED = new Color(0x151519);
    public static final Color TAB_SELECTED = new Color(0x151519);
    public static final Color DISABLED_FG = new Color(0x8B0101);

    // -- HUD design tokens -----------------------------------------------------

    public static final Color HUD_BG = new Color(0x090D12);
    /** Подложка OBS-оверлея: ЧИСТЫЙ чёрный (контраст/luma-key на стриме),
     *  НЕ путать с HUD_BG — это не фон тела HUD-окна. */
    public static final Color HUD_OVERLAY_BG = new Color(0, 0, 0);
    /** Semi-transparent dark veil placed on the owner window's glass pane while a modal dialog is open (~55 % alpha). */
    public static final Color HUD_SCRIM = new Color(0, 0, 0, 140);
    public static final Color HUD_SHELL_BACKGROUND = HUD_BG;
    public static final Color HUD_CONTENT_BACKGROUND = HUD_BG;
    public static final Color HUD_PANEL_BG = new Color(0x101721);
    public static final Color HUD_PANEL_BG_ALT = new Color(0x151E2B);
    /** Warm-dark canvas for HUD modal dialog bodies; warmer and slightly lighter than cold HUD_BG, darker than panel BG. */
    public static final Color HUD_DIALOG_BODY = new Color(0x100E0C);
    /** Cool saturated blue for HUD modal dialog header strip; lighter and more saturated than HUD_PANEL_BG. */
    public static final Color HUD_DIALOG_HEADER_BG = new Color(0x16223A);
    public static final Color HUD_BORDER = new Color(0x2D5C66);
    public static final Color HUD_BORDER_DIM = new Color(0x24313A);
    public static final Color HUD_ORANGE_SOFT = new Color(0xB85A14);
    public static final Color HUD_ORANGE_FILL = new Color(0x3A1E0A);
    public static final Color HUD_ORANGE_FILL_HOVER = new Color(0x532A0D);
    /** Заливка primary-кнопки HudButton: покой / hover / нажатие (§4). */
    public static final Color HUD_BUTTON_FILL         = new Color(0xB04000);
    public static final Color HUD_BUTTON_FILL_HOVER   = new Color(0xCC4D00);
    public static final Color HUD_BUTTON_FILL_PRESSED = new Color(0xFF6000);
    /** Active MAIN_NAV tab box fill (§11.1). Slightly softer/warmer than the global {@code ACCENT}
     *  used for rails and other accents, so the navbar box reads as the source the SECTION tone shifts from. */
    public static final Color HUD_TAB_MAIN_FILL = new Color(0xF5820E);
    /** Active SECTION tab box fill — orange nudged slightly toward red (22°), kept bright so the
     *  active tab reads as live, while the warmer hue separates it from the MAIN_NAV box (§11). */
    public static final Color HUD_TAB_SECTION_FILL = new Color(0xF26412);
    /** Section tab-row underline rail — a touch redder again (20°), pairs with the box fill. */
    public static final Color HUD_TAB_SECTION_RAIL = new Color(0xF25D12);
    public static final Color HUD_CYAN = new Color(0x33D7E8);
    public static final Color HUD_CYAN_SOFT = new Color(0x49AFC7);
    public static final Color HUD_OK = new Color(0x4FC56B);
    public static final Color HUD_WARN = new Color(0xFFB000);
    public static final Color HUD_WARN_BG = new Color(0x2A2114);
    public static final Color HUD_DANGER = new Color(0xD94F4F);
    public static final Color HUD_DISABLED = new Color(0x6E4A28);
    public static final Color HUD_TABLE_ROW = new Color(0x1A1206);
    public static final Color HUD_TABLE_ROW_HOVER = new Color(0x2A1B08);
    /**
     * Saturated red fill for the active (left-of-thumb) portion of a {@link HudSlider} track.
     * Conscious exception to §1 (red = danger): here red is a level indicator mirroring the
     * in-game ED slider, chosen for legibility against the warm track — not a danger signal.
     */
    public static final Color HUD_SLIDER_FILL = new Color(0xFF2E00);
    public static final Color HUD_HOVER = new Color(0x182838);

    /** Muted amber/warm-orange body text for USER_INPUT log readouts. */
    public static final Color HUD_USER_INPUT_TEXT = new Color(0xBB7A32);
    /** Soft blue-grey body text for AI_RESPONSE log readouts. */
    public static final Color HUD_AI_RESPONSE_TEXT = new Color(0x72A2B4);
    /** Dim neutral grey body text for SYSTEM_LOG readout messages. */
    public static final Color HUD_SYSTEM_LOG_TEXT = new Color(0x5A6368);
    /** Subdued grey-blue for HH:mm:ss timestamp prefixes in SYSTEM_LOG entries. */
    public static final Color HUD_SYSTEM_LOG_TIMESTAMP = new Color(0x4A6270);

    public static final int HUD_GAP = 8;
    /** Unified side inset for body and footer of all modals (see HudModalScaffold). */
    public static final int HUD_DIALOG_BODY_INSET = HUD_GAP * 2; // =16
    public static final int SHELL_GAP = 10;
    public static final int SCREEN_TOP_GAP = 12;
    public static final int HUD_PADDING = 10;
    public static final int HUD_PADDING_SMALL = 6;
    /** Width of the HUD_BG separator stripe used between info-zone and content (checkbox, text field). */
    public static final int HUD_SEP_W = 3;
    /** Vertical inset for HUD combo box dropdown list cells (list-level padding, not field border). */
    public static final int HUD_COMBO_ITEM_INSET_V = 4;
    /** Horizontal inset for HUD combo box dropdown list cells (list-level padding, not field border). */
    public static final int HUD_COMBO_ITEM_INSET_H = 8;
    public static final int SUBTAB_CONTENT_GAP = HUD_GAP;
    public static final int HUD_BORDER_THICKNESS = 1;
    /** Thickness for high-visibility accent borders — modal dialogs and similar prominent frames. */
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
    // HudSlider metrics (ED slider form, §4): brown track plaque, rail with edge inset,
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
    // HudMicMeter metrics (segmented vertical LED-VU mic level meter, §4): LIVE + PEAK-trail columns.
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
     * To change a specific element's size — update the role here; to shift all UI globally — change {@link #HUD_FONT_BASE}.
     */
    public static final float HUD_FONT_TABLE_ROW      = HUD_FONT_MD;   // 14
    public static final float HUD_FONT_TABLE_HEADER   = HUD_FONT_SM;   // 12
    public static final float HUD_FONT_FIELD_VALUE    = HUD_FONT_SM;   // 12
    public static final float HUD_FONT_FIELD_LABEL    = HUD_FONT_SM + 1f; // 13 — form/readout key labels (§5.1)
    public static final float HUD_FONT_READOUT_KEY    = HUD_FONT_XS;   // 11
    public static final float HUD_FONT_READOUT_VALUE  = HUD_FONT_SM;   // 12
    public static final float HUD_FONT_SECTION_TITLE  = HUD_FONT_SM;   // 12
    public static final float HUD_FONT_TAB_MAIN       = HUD_FONT_LG;   // 16
    public static final float HUD_FONT_TAB_SECTION    = HUD_FONT_MD;   // 14 — second-level section tabs
    public static final float HUD_FONT_TAB_COMPACT    = HUD_FONT_SM;   // 12 — dense inner (compact) tabs
    public static final float HUD_FONT_BUTTON         = HUD_FONT_SM;   // 12
    public static final float HUD_FONT_CHECKBOX       = HUD_FONT_SM;   // 12f — форм-контрол чекбокса (§4.1)
    public static final float HUD_FONT_ICON_BUTTON    = HUD_FONT_LG;  // 16f — символ-кнопки (ⓘ и т.п.) в боксе 24×24
    public static final float HUD_FONT_BADGE_ROLE     = HUD_FONT_XS;   // 11
    public static final float HUD_FONT_COMMANDER_NAME = HUD_FONT_MD;   // 14
    public static final float HUD_FONT_APP_TITLE      = HUD_FONT_LG;   // 16 — app title in top bar
    public static final float HUD_FONT_BANNER         = HUD_FONT_XS;   // 11 — banner message text
    // Out-of-scale display sizes:
    public static final float HUD_FONT_CLOCK          = 26f;
    public static final float HUD_FONT_STAT_LG        = 16f;
    /** LAF defaultFont — базовый кегль UI-меток вне HUD-ролей (AppView). */
    public static final float HUD_FONT_UI_DEFAULT = 18f;
    /** LAF monospaceFont — базовый монокегль приложения (AppView). */
    public static final float HUD_FONT_MONO_BASE  = 20f;
    /** Текст OBS-оверлея (Electrolize), отдельный сайт от MONO_BASE. */
    public static final float HUD_FONT_OVERLAY    = 20f;
}
