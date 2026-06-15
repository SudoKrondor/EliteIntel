package elite.intel.ui.view;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Shared colors, component factories, and layout helpers for the dark UI theme.
 */
public class AppTheme {
    /** Client-property key: set to {@link Boolean#TRUE} on a {@link javax.swing.JComponent} to prevent
     *  {@link #applyDarkPalette} from overriding its foreground colour. */
    public static final String HUD_LOCKED_FOREGROUND = "eliteIntel.hud.lockedForeground";
    static final String HUD_TABLE_STYLE_LOCKED = "eliteIntel.hud.tableStyleLocked";
    /**
     * Opt-out client property for JScrollPane: when TRUE, applyDarkPalette does
     * NOT restyle this scroll pane (keeps its own viewport bg / border). Use for
     * data-plane table scrolls that must keep the warm HUD_BG viewport instead of
     * the cold HUD_PANEL_BG that styleScrollPane applies. Mirrors
     * HUD_TABLE_STYLE_LOCKED for tables (ED_HUD_REFERENCE §8.6).
     */
    public static final String HUD_SCROLL_STYLE_LOCKED = "eliteIntel.hud.scrollStyleLocked";
    /**
     * Marks a combo box editor text field as fully styled by its factory (e.g. {@link HudComboBox#picker}).
     * {@link #applyDarkPalette} / {@link #styleTextComponent} must skip components carrying this flag;
     * otherwise they overwrite the editor's tight {@link javax.swing.border.EmptyBorder} with
     * {@link #hudFieldBorder()} (a {@link javax.swing.border.LineBorder}), whose right edge appears
     * as a visible vertical line inside the field next to the arrow button.
     */
    public static final String HUD_COMBO_EDITOR_LOCKED = "eliteIntel.hud.comboEditorLocked";
    public static final String HUD_CARD_BORDER_COLOR = "eliteIntel.hud.cardBorderColor";

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
    /** Preferred width for searchable editable picker combo boxes. */
    public static final int HUD_PICKER_FIELD_WIDTH  = 500;
    /** Preferred height for searchable editable picker combo boxes. */
    public static final int HUD_PICKER_FIELD_HEIGHT = 42;
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

    // -- Button factories ------------------------------------------------------

    public static JButton makeButton(String label) {
        return new HudButton(label, true);
    }

    public static JButton makeButtonSubtle(String label) {
        return new HudButton(label, false);
    }

    /**
     * Creates a compact square HUD button for a trailing field action (e.g. a directory/file
     * picker placed at the end of a form field). The button is sized
     * {@code fieldHeight}×{@code fieldHeight} so it aligns with the neighbouring field height
     * while staying narrow.
     *
     * @param glyph       the button glyph/label (e.g. the picker ellipsis)
     * @param fieldHeight height of the adjacent field, used as the square side
     */
    public static JButton makeFieldButton(String glyph, int fieldHeight) {
        HudButton button = new HudButton(glyph, true);
        button.setSquareSide(fieldHeight);
        return button;
    }

    /**
     * Icon variant of {@link #makeFieldButton(String, int)} for glyph affordances painted as
     * HUD primitives (e.g. {@link #verticalEllipsisIcon(int)}) rather than Unicode text.
     *
     * @param icon        glyph icon, painted in the button's state-driven foreground colour
     * @param fieldHeight height of the adjacent field, used as the square side
     */
    public static JButton makeFieldButton(Icon icon, int fieldHeight) {
        HudButton button = new HudButton(null, true);
        button.setSquareSide(fieldHeight);
        // Dark glyph on the bright primary fill (HUD inversion, §0.4); the icon reads getForeground().
        button.setForeground(SEL_FG);
        button.setIcon(icon);
        return button;
    }

    /**
     * Creates a HUD-styled toggle button for on/off controls.
     */
    public static JToggleButton makeToggleButton(String label) {
        return new HudToggleButton(label);
    }

    /**
     * Creates a HUD-styled checkbox preserving standard Swing checkbox behaviour.
     */
    public static JCheckBox makeCheckBox(String label, boolean selected) {
        return new HudCheckBox(label, selected);
    }

    /**
     * Creates a HUD-styled checkbox with an optional info-zone that runs {@code infoAction}
     * on click without toggling the checkbox state.
     * Pass {@code null} for {@code infoAction} to get identical behaviour to
     * {@link #makeCheckBox(String, boolean)}.
     */
    public static HudCheckBox makeCheckBox(String label, boolean selected, Runnable infoAction) {
        HudCheckBox cb = new HudCheckBox(label, selected);
        cb.setInfoAction(infoAction);
        return cb;
    }

    /**
     * Creates a HUD-styled single-line text field.
     */
    public static JTextField makeTextField() {
        return new HudTextField();
    }

    /**
     * Creates a HUD-styled text field with an info-zone on the right that runs
     * {@code infoAction} on click without interfering with text editing.
     */
    public static HudTextField makeTextField(Runnable infoAction) {
        HudTextField tf = new HudTextField();
        tf.setInfoAction(infoAction);
        return tf;
    }

    /**
     * Creates a compact read-only HUD field for metadata values.
     */
    public static JTextField makeMetadataField() {
        return new HudMetadataField();
    }

    /**
     * Creates a HUD-styled password field for secret inputs.
     */
    public static JPasswordField makePasswordField() {
        return new HudPasswordField();
    }

    /**
     * Creates a HUD-styled multi-line text area for logs and details.
     */
    public static JTextArea makeTextArea(int rows, int columns) {
        return new HudTextArea(rows, columns);
    }

    /**
     * Creates a HUD-styled combo box with readable dropdown cells.
     */
    public static <E> JComboBox<E> makeComboBox(E[] values) {
        return new HudComboBox<>(values);
    }

    public static void styleButton(AbstractButton b) {
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setForeground(FG);
        b.setBackground(BUTTON_BG);
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BUTTON_BG, HUD_BORDER_THICKNESS, true),
                new EmptyBorder(6, 12, 6, 12)
        ));
    }

    /**
     * Creates the standard border used by reusable cockpit/HUD panels.
     */
    public static Border hudBorder() {
        return BorderFactory.createCompoundBorder(
                new LineBorder(HUD_BORDER_DIM, HUD_BORDER_THICKNESS, true),
                new EmptyBorder(HUD_PADDING, HUD_PADDING, HUD_PADDING, HUD_PADDING)
        );
    }

    /**
     * Creates a quiet border for nested HUD surfaces that should not add another visible frame.
     */
    public static Border hudFlatBorder() {
        return new EmptyBorder(HUD_PADDING_SMALL, HUD_PADDING_SMALL, HUD_PADDING_SMALL, HUD_PADDING_SMALL);
    }

    /**
     * Creates the compact outer spacing used between the main navigation and screen content.
     */
    public static Border hudScreenBorder() {
        return new EmptyBorder(SCREEN_TOP_GAP, SHELL_GAP, SHELL_GAP, SHELL_GAP);
    }


    /**
     * Creates the compact spacing between a screen sub-tab row and its first content surface.
     */
    public static Border hudSubtabContentBorder() {
        return new EmptyBorder(SUBTAB_CONTENT_GAP, 0, 0, 0);
    }

    /**
     * Creates a restrained structural border for compact HUD cards and major modules.
     */
    public static Border hudMajorPanelBorder() {
        return BorderFactory.createCompoundBorder(
                new LineBorder(HUD_BORDER_DIM, HUD_BORDER_THICKNESS, true),
                new EmptyBorder(4, 6, 5, 6)
        );
    }

    /**
     * Creates a subtle frame for dense table/data-plane surfaces.
     */
    public static Border hudDataPlaneBorder() {
        return BorderFactory.createEmptyBorder(1, 1, 1, 1);
    }

    /**
     * Creates a left/right/bottom border for a data table that sits directly below a
     * {@link HudSearchField.Variant#TABLE_FILTER_CONNECTED} filter bar.
     * The top edge is omitted because the filter bar provides the shared top border line.
     */
    public static Border hudConnectedScrollPaneBorder() {
        return BorderFactory.createMatteBorder(0, 1, 1, 1, HUD_BORDER);
    }

    /**
     * Canonical footer rule (§10) shared by modal dialogs and non-modal screen/tab footers:
     * a full-width warm {@link #HUD_ORANGE_FILL_HOVER} rule of {@link #HUD_BORDER_THICKNESS}
     * at the top, with {@link #HUD_GAP} padding above (after the rule) and below, zero side inset.
     * Apply to the footer strip built by {@link HudFooter}.
     */
    public static Border hudFooterBorder() {
        final int gap = HUD_GAP;
        final int th  = HUD_BORDER_THICKNESS;
        return new AbstractBorder() {
            @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                g.setColor(HUD_ORANGE_FILL_HOVER);
                g.fillRect(x, y, w, th);            // full-width rule, zero side inset
            }
            @Override public Insets getBorderInsets(Component c) { return new Insets(th + gap, 0, gap, 0); }
            @Override public Insets getBorderInsets(Component c, Insets i) { i.set(th + gap, 0, gap, 0); return i; }
        };
    }

    /**
     * Creates a compact label for HUD section titles without changing the global UI font.
     */
    public static JLabel hudSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(ACCENT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, HUD_FONT_SECTION_TITLE));
        label.putClientProperty(HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        return label;
    }

    /**
     * Creates a muted cyan group-separator label for list sections within a data panel.
     * Use for group/category titles inside scrollable lists, not for screen-level section titles.
     */
    public static JLabel hudGroupLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(FG);
        label.setFont(label.getFont().deriveFont(Font.BOLD, HUD_FONT_SECTION_TITLE));
        label.putClientProperty(HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        return label;
    }

    /**
     * Creates a muted key-label for readout rows (FG_MUTED, XS caps).
     * Use for field labels inside settings rows, telemetry keys, and form prompts.
     */
    public static JLabel hudReadoutLabel(String text) {
        JLabel lbl = new JLabel(text != null ? text.toUpperCase() : "");
        lbl.setForeground(FG_MUTED);
        lbl.setFont(lbl.getFont().deriveFont(HUD_FONT_READOUT_KEY));
        lbl.putClientProperty(HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        return lbl;
    }

    /**
     * Creates a plain value label for readout rows at {@link #HUD_FONT_READOUT_VALUE} size.
     * Pair with {@link #hudReadoutLabel} for the key column. No border or background is set.
     *
     * @param value initial text
     * @param color foreground colour — e.g. {@link #HUD_CYAN} for command names, {@link #FG} for plain values
     */
    public static JLabel hudReadoutValue(String value, Color color) {
        JLabel l = new JLabel(value);
        l.setForeground(color);
        l.setFont(l.getFont().deriveFont(HUD_FONT_READOUT_VALUE));
        l.putClientProperty(HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        return l;
    }

    /**
     * Creates the standard HUD command title block: name in bold cyan at app-title size
     * with the command id beneath in a muted readout-key font.
     * Use above a details section in undecorated command dialogs.
     *
     * @param name command display name; converted to upper case
     * @param id   command action key; rendered as-is
     */
    public static JPanel commandTitleBlock(String name, String id) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        JLabel nameLabel = new JLabel(name.toUpperCase());
        nameLabel.setForeground(HUD_CYAN);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, HUD_FONT_APP_TITLE));
        JLabel idLabel = new JLabel(id);
        idLabel.setForeground(FG_MUTED);
        idLabel.setFont(idLabel.getFont().deriveFont(HUD_FONT_READOUT_KEY));
        idLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
        panel.add(nameLabel);
        panel.add(idLabel);
        return panel;
    }

    /** Vertical inset inside the HUD field border (top and bottom padding). */
    private static final int HUD_FIELD_INSET_V = 5;
    /** Horizontal inset inside the HUD field border (left and right padding). */
    private static final int HUD_FIELD_INSET_H = 8;

    /**
     * Creates the standard HUD input border used by text fields and combo boxes.
     */
    /**
     * Outer field line that follows the component's enabled state: warm {@code HUD_ORANGE_SOFT}
     * when enabled, dimmed {@code HUD_DISABLED} when disabled (§0.6). Shared by all HUD fields and
     * combo boxes so the disabled look is consistent app-wide.
     */
    private static Border hudFieldLine() {
        return new javax.swing.border.AbstractBorder() {
            @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                g.setColor(c.isEnabled() ? HUD_ORANGE_SOFT : HUD_DISABLED);
                for (int i = 0; i < HUD_BORDER_THICKNESS; i++) {
                    g.drawRect(x + i, y + i, width - 1 - 2 * i, height - 1 - 2 * i);
                }
            }
            @Override public Insets getBorderInsets(Component c) {
                int t = HUD_BORDER_THICKNESS;
                return new Insets(t, t, t, t);
            }
            @Override public Insets getBorderInsets(Component c, Insets insets) {
                int t = HUD_BORDER_THICKNESS;
                insets.set(t, t, t, t);
                return insets;
            }
        };
    }

    public static Border hudFieldBorder() {
        return BorderFactory.createCompoundBorder(
                hudFieldLine(),
                new EmptyBorder(HUD_FIELD_INSET_V, HUD_FIELD_INSET_H,
                        HUD_FIELD_INSET_V, HUD_FIELD_INSET_H)
        );
    }

    /**
     * Creates a HUD input border with an enlarged right inset that reserves space for the
     * info-zone glyph ({@link #HUD_SEP_W} separator + {@link #HUD_TABLE_ROW_HEIGHT_COMPACT} zone).
     * The outer {@code LineBorder} and all other insets are identical to {@link #hudFieldBorder()}.
     */
    public static Border hudFieldBorderWithInfo() {
        return BorderFactory.createCompoundBorder(
                hudFieldLine(),
                new EmptyBorder(HUD_FIELD_INSET_V, HUD_FIELD_INSET_H,
                        HUD_FIELD_INSET_V, HUD_FIELD_INSET_H + HUD_SEP_W + HUD_TABLE_ROW_HEIGHT_COMPACT)
        );
    }

    /**
     * Creates a transparent panel with the supplied layout for HUD composition.
     */
    public static JPanel transparentPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Wraps a component in the standard HUD scroll pane.
     */
    public static JScrollPane hudScrollPane(Component view) {
        return new HudScrollPane(view);
    }

    /**
     * Builds and returns the wrapper JPanel for an undecorated modal dialog using the HUD canon (§7.2).
     * Call {@code dialog.setContentPane(AppTheme.hudModalScaffold(spec))} after constructing the dialog.
     */
    public static JPanel hudModalScaffold(HudModalSpec spec) {
        return HudModalScaffold.build(spec);
    }

    /**
     * Applies the standard HUD treatment to text components without replacing the component instance.
     */
    public static void styleTextComponent(JTextComponent tc) {
        if (tc instanceof JComponent jc
                && Boolean.TRUE.equals(jc.getClientProperty(HudSearchField.HUD_SEARCH_INNER_FIELD))) {
            styleSearchInnerField(tc);
            return;
        }
        if (tc instanceof HudMetadataField) {
            styleMetadataField(tc);
            return;
        }
        if (tc instanceof JComponent jce
                && Boolean.TRUE.equals(jce.getClientProperty(HUD_COMBO_EDITOR_LOCKED))) {
            return;   // combo editor is fully styled by picker(); palette must not re-border it
        }
        tc.setBackground(HUD_TABLE_ROW);
        tc.setForeground(FG);
        tc.setDisabledTextColor(HUD_DISABLED); // §0.6: disabled text dims to the warm muted tone
        tc.setCaretColor(ACCENT);
        tc.setSelectionColor(ACCENT);
        tc.setSelectedTextColor(SEL_FG);
        // Preserve the wider info border so the palette does not clobber the reserved info-«i» zone.
        tc.setBorder(tc instanceof HudTextField htf && htf.hasInfoZone()
                ? hudFieldBorderWithInfo() : hudFieldBorder());
        tc.setFont(tc.getFont().deriveFont(HUD_FONT_FIELD_VALUE));
    }

    /**
     * Applies borderless text styling for fields embedded inside composite HUD controls.
     * Uses {@link #HUD_FONT_SM} so search/filter inputs read as secondary controls, not headings.
     */
    public static void styleSearchInnerField(JTextComponent tc) {
        tc.setOpaque(false);
        tc.setBackground(HUD_PANEL_BG_ALT);
        tc.setForeground(FG);
        tc.setCaretColor(HUD_CYAN);
        tc.setSelectionColor(HUD_CYAN);
        tc.setSelectedTextColor(SEL_FG);
        tc.setBorder(BorderFactory.createEmptyBorder());
        tc.setFont(tc.getFont().deriveFont(HUD_FONT_SM));
    }

    /**
     * Applies the compact borderless HUD treatment for read-only metadata fields.
     */
    public static void styleMetadataField(JTextComponent tc) {
        tc.setOpaque(true);
        tc.setBackground(HUD_PANEL_BG_ALT);
        tc.setForeground(FG);
        tc.setCaretColor(FG_MUTED);
        tc.setSelectionColor(HUD_CYAN);
        tc.setSelectedTextColor(SEL_FG);
        tc.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        tc.setFont(tc.getFont().deriveFont(HUD_FONT_FIELD_VALUE));
    }

    /**
     * Applies the standard HUD treatment to combo boxes without replacing the model or renderer.
     * Installs {@link HudComboBoxUI} for the flat ▼ arrow and warm field background.
     */
    public static void styleComboBox(JComboBox<?> comboBox) {
        // setUI() unconditionally recreates the editor (uninstall/installComponents),
        // orphaning any DocumentListener on an editable combo's editor. Only install the
        // HUD UI when it is not already present, so repeat calls (e.g. applyDarkPalette
        // re-walking an already-styled picker) are no-ops for the editor.
        if (!(comboBox.getUI() instanceof HudComboBoxUI)) {
            comboBox.setUI(new HudComboBoxUI());
        }
        comboBox.setBackground(HUD_TABLE_ROW);
        comboBox.setForeground(FG);
        comboBox.setBorder(hudFieldBorder());
        comboBox.setFocusable(true);
        comboBox.setFont(comboBox.getFont().deriveFont(HUD_FONT_FIELD_VALUE));
    }

    /**
     * Applies the standard HUD treatment to checkbox-like buttons.
     */
    public static void styleCheckBox(AbstractButton checkBox) {
        checkBox.setOpaque(false);
        checkBox.setForeground(FG);
        checkBox.setFocusPainted(false);
    }

    /**
     * Applies the standard HUD treatment to scroll panes and their viewport.
     */
    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBackground(HUD_BG);
        scrollPane.getViewport().setBackground(HUD_PANEL_BG);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        styleScrollBar(scrollPane.getVerticalScrollBar());
        styleScrollBar(scrollPane.getHorizontalScrollBar());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    }

    static void styleScrollBar(JScrollBar scrollBar) {
        scrollBar.setPreferredSize(new Dimension(9, 9));
        scrollBar.setBackground(HUD_BG);
        scrollBar.setUnitIncrement(16);
        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = HUD_DISABLED;
                thumbDarkShadowColor = HUD_BORDER_DIM;
                thumbHighlightColor = HUD_DISABLED;
                trackColor = HUD_PANEL_BG;
                trackHighlightColor = HUD_PANEL_BG_ALT;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return zeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return zeroButton();
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setColor(HUD_DISABLED);
                    g2.fillRect(thumbBounds.x + 1, thumbBounds.y + 1,
                            Math.max(1, thumbBounds.width - 2),
                            Math.max(1, thumbBounds.height - 2));
                } finally {
                    g2.dispose();
                }
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                g.setColor(HUD_BG);
                g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            }

            private JButton zeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });
    }

    /**
     * Applies shared HUD table metrics and renderers to an existing table.
     */
    public static void styleTable(JTable table) {
        HudTable.style(table);
    }

    /**
     * Loads and scales an image resource relative to the supplied owner class.
     */
    public static ImageIcon scaledIcon(Class<?> owner, String resource, int size) {
        return scaledIcon(owner, resource, size, size);
    }

    /**
     * Loads and scales an image resource relative to the supplied owner class.
     */
    public static ImageIcon scaledIcon(Class<?> owner, String resource, int width, int height) {
        return new ImageIcon(
                new ImageIcon(java.util.Objects.requireNonNull(owner.getResource(resource)))
                        .getImage()
                        .getScaledInstance(width, height, Image.SCALE_SMOOTH)
        );
    }

    /**
     * Tints a monochrome glyph icon to the given colour using {@link AlphaComposite#SRC_IN},
     * preserving per-pixel alpha. Only correct for single-colour masks on a transparent background.
     *
     * @param src   source icon (any size)
     * @param w     output width in pixels
     * @param h     output height in pixels
     * @param color replacement colour
     * @return new {@link ImageIcon} backed by a {@link BufferedImage}
     */
    public static ImageIcon tintIcon(ImageIcon src, int w, int h, Color color) {
        BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buf.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(src.getImage(), 0, 0, w, h, null);
            g2.setComposite(AlphaComposite.SrcIn);
            g2.setColor(color);
            g2.fillRect(0, 0, w, h);
        } finally {
            g2.dispose();
        }
        return new ImageIcon(buf);
    }

    /**
     * Returns a copy of {@code src} composited at the given alpha (0 = transparent, 1 = opaque).
     * Use to produce a visually receded version of a colourful icon without recolouring it.
     *
     * @param src   source icon (not modified)
     * @param alpha opacity, clamped to [0, 1] by the compositing pipeline
     * @return new {@link ImageIcon} backed by a {@link BufferedImage}
     */
    public static ImageIcon dimIcon(ImageIcon src, float alpha) {
        int w = src.getIconWidth();
        int h = src.getIconHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        try {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.drawImage(src.getImage(), 0, 0, w, h, null);
        } finally {
            g2.dispose();
        }
        return new ImageIcon(result);
    }

    /**
     * Draws the flat ▼ triangle used by HUD combo boxes, centred within (x, y, w, h).
     * Shared between {@link HudComboBoxUI} arrow button and table cell renderers.
     *
     * @param g2    graphics context (not disposed by this method)
     * @param x     left edge of the available area
     * @param y     top edge of the available area
     * @param w     width of the available area
     * @param h     height of the available area
     * @param color fill colour for the triangle
     */
    public static void paintHudArrowDown(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Local geometry — not a colour/font/height token.
        int aw = 8;
        int ah = 5;
        int ax = x + (w - aw) / 2;
        int ay = y + (h - ah) / 2;
        g2.setColor(color);
        g2.fillPolygon(
                new int[]{ax, ax + aw, ax + aw / 2},
                new int[]{ay, ay,      ay + ah},
                3);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Draws a left-pointing filled triangle centred within the box (x, y, w, h).
     * Pair to {@link #paintHudArrowDown}; used by the discrete stepper ({@link HudStepper}).
     */
    public static void paintHudArrowLeft(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int aw = 5;
        int ah = 8;
        int ax = x + (w - aw) / 2;
        int ay = y + (h - ah) / 2;
        g2.setColor(color);
        g2.fillPolygon(
                new int[]{ax, ax + aw, ax + aw},
                new int[]{ay + ah / 2, ay, ay + ah},
                3);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Draws a right-pointing filled triangle centred within the box (x, y, w, h).
     * Pair to {@link #paintHudArrowDown}; used by the discrete stepper ({@link HudStepper}).
     */
    public static void paintHudArrowRight(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int aw = 5;
        int ah = 8;
        int ax = x + (w - aw) / 2;
        int ay = y + (h - ah) / 2;
        g2.setColor(color);
        g2.fillPolygon(
                new int[]{ax, ax + aw, ax},
                new int[]{ay, ay + ah / 2, ay + ah},
                3);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Draws an up-pointing filled triangle centred within the box (x, y, w, h).
     * Pair to {@link #paintHudArrowDown}; used for move-up affordances.
     */
    public static void paintHudArrowUp(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int aw = 8;
        int ah = 5;
        int ax = x + (w - aw) / 2;
        int ay = y + (h - ah) / 2;
        g2.setColor(color);
        g2.fillPolygon(
                new int[]{ax, ax + aw, ax + aw / 2},
                new int[]{ay + ah, ay + ah, ay},
                3);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Draws a lowercase «i» glyph (dot + stem) centred within the box (x, y, w, h).
     * All geometry is relative — no hardcoded pixel sizes except proportional formulas.
     * Suitable for info-affording controls; caller chooses colour based on component state.
     *
     * @param g2    graphics context (not disposed by this method)
     * @param x     left edge of the available area
     * @param y     top edge of the available area
     * @param w     width of the available area
     * @param h     height of the available area
     * @param color fill colour for both dot and stem
     */
    public static void paintHudInfoGlyph(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int stemW = Math.max(2, w / 5);
        int stemH = (int) Math.round(h * 0.55);
        int gap   = Math.max(1, stemW / 2);
        int totalH = stemW + gap + stemH;
        int gx = x + (w - stemW) / 2;
        int gy = y + (h - totalH) / 2;
        g2.setColor(color);
        g2.fillRect(gx, gy,              stemW, stemW); // dot
        g2.fillRect(gx, gy + stemW + gap, stemW, stemH); // stem
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Draws an × glyph (two crossing diagonals) centred within the box (x, y, w, h).
     * All geometry is proportional — no hardcoded pixel sizes except the proportional formulas.
     * Suitable for close/dismiss affordances; caller chooses colour based on hover state.
     *
     * @param g2    graphics context (not disposed by this method)
     * @param x     left edge of the available area
     * @param y     top edge of the available area
     * @param w     width of the available area
     * @param h     height of the available area
     * @param color fill colour for both diagonals
     */
    public static void paintHudCloseGlyph(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Stroke oldStroke = g2.getStroke();
        float strokeW = Math.max(2f, w / 8f);
        g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        int pad = (int) (w * 0.28);
        int x1 = x + pad,     y1 = y + pad;
        int x2 = x + w - pad, y2 = y + h - pad;
        g2.setColor(color);
        g2.drawLine(x1, y1, x2, y2); // top-left → bottom-right
        g2.drawLine(x1, y2, x2, y1); // bottom-left → top-right
        g2.setStroke(oldStroke);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Draws a vertical three-dot «more/options» glyph (three stacked squares) centred within
     * the box (x, y, w, h). All geometry is proportional. Flat squares (not rounded dots) per
     * the HUD flat-style rule; caller chooses colour based on component state.
     *
     * @param g2    graphics context (not disposed by this method)
     * @param x     left edge of the available area
     * @param y     top edge of the available area
     * @param w     width of the available area
     * @param h     height of the available area
     * @param color fill colour for the three dots
     */
    public static void paintHudVerticalEllipsis(Graphics2D g2, int x, int y, int w, int h, Color color) {
        // Small dots with generous vertical breathing room — proportional so they scale with the box.
        int dot = Math.max(3, Math.round(h / 7f));
        int gap = Math.max(2, Math.round(h / 12f));
        int totalH = dot * 3 + gap * 2;
        int dx = x + (w - dot) / 2;
        int dy = y + (h - totalH) / 2;
        g2.setColor(color);
        for (int i = 0; i < 3; i++) {
            g2.fillRect(dx, dy + i * (dot + gap), dot, dot);
        }
    }

    /**
     * Returns an icon that paints a vertical three-dot glyph using the host component's
     * foreground colour, so it follows the button's state-driven colour. Use for compact
     * field-trailing «more/options/pick» buttons instead of a Unicode «⋮» (see HUD §13).
     *
     * @param boxSize square icon side in px (typically the field/button height)
     */
    public static Icon verticalEllipsisIcon(int boxSize) {
        return new Icon() {
            @Override public int getIconWidth() { return boxSize; }
            @Override public int getIconHeight() { return boxSize; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                paintHudVerticalEllipsis((Graphics2D) g, x, y, boxSize, boxSize, c.getForeground());
            }
        };
    }

    /**
     * Returns an icon that paints an up-pointing triangle in the host component's foreground
     * colour (so it follows button state). Use for move-up affordances on buttons (see HUD §13).
     */
    public static Icon arrowUpIcon(int boxSize) {
        return new Icon() {
            @Override public int getIconWidth() { return boxSize; }
            @Override public int getIconHeight() { return boxSize; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                paintHudArrowUp((Graphics2D) g, x, y, boxSize, boxSize, c.getForeground());
            }
        };
    }

    /**
     * Returns an icon that paints a down-pointing triangle in the host component's foreground
     * colour (so it follows button state). Use for move-down affordances on buttons (see HUD §13).
     */
    public static Icon arrowDownIcon(int boxSize) {
        return new Icon() {
            @Override public int getIconWidth() { return boxSize; }
            @Override public int getIconHeight() { return boxSize; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                paintHudArrowDown((Graphics2D) g, x, y, boxSize, boxSize, c.getForeground());
            }
        };
    }

    /**
     * Draws the HUD warning glyph — a triangle outline with a centred exclamation — within
     * (x, y, w, h). Primitive replacement for the Unicode "⚠" (HUD §13); the caller chooses the
     * colour (state-driven, typically {@link #HUD_WARN}).
     *
     * @param g2    graphics context (not disposed by this method)
     * @param x     left edge of the available area
     * @param y     top edge of the available area
     * @param w     width of the available area
     * @param h     height of the available area
     * @param color stroke/fill colour for the glyph
     */
    public static void paintHudWarningGlyph(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Stroke oldStroke = g2.getStroke();
        int size = Math.min(w, h);
        int inset = Math.max(1, Math.round(size * 0.10f));
        int left = x + (w - size) / 2 + inset;
        int right = x + (w + size) / 2 - inset;
        int top = y + (h - size) / 2 + inset;
        int bottom = y + (h + size) / 2 - inset;
        int cx = (left + right) / 2;
        float stroke = Math.max(1.5f, size / 11f);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawPolygon(new int[]{left, right, cx}, new int[]{bottom, bottom, top}, 3);
        // Exclamation mark: vertical bar + dot, centred in the triangle.
        int barTop = top + Math.round((bottom - top) * 0.40f);
        int barBot = top + Math.round((bottom - top) * 0.66f);
        g2.drawLine(cx, barTop, cx, barBot);
        int gap = Math.max(2, Math.round((bottom - top) * 0.08f));
        int dot = Math.max(2, Math.round(stroke));
        g2.fillRect(cx - dot / 2, barBot + gap, dot, dot);
        g2.setStroke(oldStroke);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Returns an icon that paints the HUD warning glyph using the host component's foreground
     * colour. Use as a leading glyph on {@link HudBanner} warnings instead of a Unicode "⚠" (§13).
     *
     * @param boxSize square icon side in px
     */
    public static Icon warningGlyphIcon(int boxSize) {
        return new Icon() {
            @Override public int getIconWidth() { return boxSize; }
            @Override public int getIconHeight() { return boxSize; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                paintHudWarningGlyph((Graphics2D) g, x, y, boxSize, boxSize, c.getForeground());
            }
        };
    }

    /**
     * Paints the HUD checkbox marker: a 2-px double-outline square box, with a centred
     * filled inner square when {@code filled} is true. Geometry matches the legacy inline
     * drawing in {@link HudCheckBox}. Caller is responsible for antialiasing hints.
     *
     * @param g2          graphics context (not disposed by this method)
     * @param x           left edge of the marker box
     * @param y           top edge of the marker box
     * @param size        outer marker size in px (e.g. {@code HUD_TABLE_ROW_HEIGHT_COMPACT - 2*HUD_PADDING_SMALL})
     * @param markerColor colour of outline and inner fill
     * @param filled      draw the inner filled square (ON state) when true
     */
    public static void paintHudCheckMarker(Graphics2D g2, int x, int y, int size,
                                           Color markerColor, boolean filled) {
        g2.setColor(markerColor);
        g2.drawRect(x,     y,     size - 1, size - 1);
        g2.drawRect(x + 1, y + 1, size - 3, size - 3);
        if (filled) {
            int innerSize = size / 2;
            int innerX = x + (size - innerSize) / 2;
            int innerY = y + (size - innerSize) / 2;
            g2.fillRect(innerX, innerY, innerSize, innerSize);
        }
    }

    // -- Tabbed pane -----------------------------------------------------------

    /** @deprecated Use {@link #makeStandardTabs()} or {@code new HudTabbedPane(HudTabbedPane.Level.STANDARD)}. */
    @Deprecated
    public static void styleTabbedPane(JTabbedPane tp) {
        HudTabbedPane.applyStyle(tp, HudTabbedPane.Level.STANDARD);
    }

    /** @deprecated Use {@link #makeSectionTabs()} or {@code new HudTabbedPane(HudTabbedPane.Level.SECTION)}. */
    @Deprecated
    public static void styleFlatTabbedPane(JTabbedPane tp) {
        HudTabbedPane.applyStyle(tp, HudTabbedPane.Level.SECTION);
    }

    /** @deprecated Use {@link #makeSectionTabs()} or {@code new HudTabbedPane(HudTabbedPane.Level.SECTION)}. */
    @Deprecated
    public static void styleCompactFlatTabbedPane(JTabbedPane tp) {
        HudTabbedPane.applyStyle(tp, HudTabbedPane.Level.SECTION);
    }

    /** @deprecated Use {@link #makeMainNavTabs()} or {@code new HudTabbedPane(HudTabbedPane.Level.MAIN_NAV)}. */
    @Deprecated
    public static void styleMainNavigationTabbedPane(JTabbedPane tp) {
        HudTabbedPane.applyStyle(tp, HudTabbedPane.Level.MAIN_NAV);
    }

    /** Creates a HUD-styled application-level navigation tabbed pane. */
    public static JTabbedPane makeMainNavTabs() {
        return new HudTabbedPane(HudTabbedPane.Level.MAIN_NAV);
    }

    /** Creates a HUD-styled sub-navigation tabbed pane for screen sections. */
    public static JTabbedPane makeSectionTabs() {
        return new HudTabbedPane(HudTabbedPane.Level.SECTION);
    }

    /** Creates a HUD-styled compact bold tabbed pane for data panels. */
    public static JTabbedPane makeCompactTabs() {
        return new HudTabbedPane(HudTabbedPane.Level.COMPACT);
    }

    /** Creates a HUD-styled settings-style tabbed pane with a visible content frame. */
    public static JTabbedPane makeStandardTabs() {
        return new HudTabbedPane(HudTabbedPane.Level.STANDARD);
    }

    // -- Dark palette ----------------------------------------------------------

    public static void applyDarkPalette(Component c) {
        if (c == null) return;
        boolean lockForeground = c instanceof JComponent jc
                && Boolean.TRUE.equals(jc.getClientProperty(HUD_LOCKED_FOREGROUND));
        boolean lockScroll = c instanceof JComponent jcs
                && Boolean.TRUE.equals(jcs.getClientProperty(HUD_SCROLL_STYLE_LOCKED));

        if (c instanceof TopStatusBar || c instanceof HudPanel || c instanceof StatusBadge) {
            // HUD primitives own their painting and state colours.
        } else if (c instanceof JScrollPane && lockScroll) {
            // data-plane scroll owns its viewport bg/border — leave it untouched
        } else if (c instanceof JPanel || c instanceof JTabbedPane || c instanceof JScrollPane) {
            c.setBackground(HUD_CONTENT_BACKGROUND);
            if (!lockForeground) c.setForeground(FG);
        } else {
            c.setBackground(c instanceof JTextComponent ? BG_PANEL : BG);
            if (!lockForeground) c.setForeground(FG);
        }

        if (c instanceof JTextArea) {
            c.setBackground(LOG_BG);
            c.setForeground(CONSOLE_FG);
        }

        if (c instanceof JTextComponent tc) {
            styleTextComponent(tc);
        }

        boolean searchClearButton = c instanceof JComponent jc
                && Boolean.TRUE.equals(jc.getClientProperty(HudSearchField.HUD_SEARCH_CLEAR_BUTTON));

        if (c instanceof HudButton || c instanceof HudToggleButton || searchClearButton) {
            // HUD buttons own their border and paint state.
        } else if (c instanceof JButton b) {
            styleButton(b);
        }

        if (c instanceof JCheckBox cb && !(cb instanceof HudCheckBox)) {
            styleCheckBox(cb);
        }

        if (c instanceof JComboBox<?> comboBox) {
            styleComboBox(comboBox);
        }

        if (c instanceof JTable table
                && !Boolean.TRUE.equals(table.getClientProperty(HUD_TABLE_STYLE_LOCKED))) {
            styleTable(table);
        }

        if (c instanceof JTabbedPane tp) {
            tp.setBackground(HUD_CONTENT_BACKGROUND);
            tp.setForeground(FG);
            tp.setOpaque(true);
        }

        if (c instanceof JScrollPane sp && !lockScroll) {
            styleScrollPane(sp);
        }

        if (c instanceof JEditorPane ep) {
            ep.setBackground(Color.WHITE);
            ep.setForeground(Color.BLACK);
        }

        // TopStatusBar and HudBanner own all colours of their children — do not recurse into them
        // (HudBanner's inner label/JTextArea must keep its banner styling, not get the field border).
        if (c instanceof Container cont && !(c instanceof TopStatusBar) && !(c instanceof HudBanner)) {
            for (Component child : cont.getComponents()) {
                applyDarkPalette(child);
            }
        }
    }

    // -- Modal scrim -----------------------------------------------------------

    /**
     * Shows a {@link #HUD_SCRIM} veil on the owner's glass pane for the duration of a modal dialog.
     * {@code showModal} must call {@code setVisible(true)} on an {@code APPLICATION_MODAL} dialog,
     * which blocks until the dialog is closed. The scrim is guaranteed to be removed in a
     * {@code finally} block even if {@code showModal} throws.
     * Falls back to plain {@code showModal.run()} if {@code owner} is not a {@link RootPaneContainer}.
     */
    public static void runWithModalScrim(Window owner, Runnable showModal) {
        if (!(owner instanceof RootPaneContainer rpc)) {
            showModal.run();
            return;
        }
        Component prevGlass = rpc.getGlassPane();
        JComponent scrim = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(HUD_SCRIM);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        scrim.setOpaque(false);
        try {
            rpc.setGlassPane(scrim);
            scrim.setVisible(true);
            showModal.run();
        } finally {
            scrim.setVisible(false);
            if (prevGlass != null) rpc.setGlassPane(prevGlass);
        }
    }

    // -- GridBagLayout helpers -------------------------------------------------

    public static GridBagConstraints baseGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        return gbc;
    }

    public static void nextRow(GridBagConstraints gbc) {
        gbc.gridy++;
    }

    public static void addLabel(JPanel panel, String text, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        // Dim-aware field label: follows its enabled state (§0.6) so a disabled row's
        // label greys out together with its field, centrally for every form.
        JLabel label = new JLabel(text.toUpperCase()) {
            @Override public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                setForeground(enabled ? FG_MUTED : HUD_DISABLED);
            }
        };
        label.setForeground(FG_MUTED);
        label.setFont(label.getFont().deriveFont(HUD_FONT_SM));
        label.setPreferredSize(new Dimension(220, HUD_TABLE_ROW_HEIGHT_COMPACT));
        panel.add(label, gbc);
    }

    public static void addLabel(JPanel panel, String text, GridBagConstraints gbc, int col, double weightX) {
        gbc.gridx = col;
        gbc.weightx = weightX;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel comp = new JLabel(text);
        comp.setFont(comp.getFont().deriveFont(HUD_FONT_SM));
        comp.setPreferredSize(new Dimension(0, comp.getPreferredSize().height));
        panel.add(comp, gbc);
    }

    public static void addField(JPanel panel, JComponent comp, GridBagConstraints gbc, int col, double weightX) {
        gbc.gridx = col;
        gbc.weightx = weightX;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        comp.setPreferredSize(new Dimension(0, comp.getPreferredSize().height));
        panel.add(comp, gbc);
    }

    public static void addCheck(JPanel panel, JCheckBox check, GridBagConstraints gbc) {
        gbc.gridx = 2;
        gbc.weightx = 0.2;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(check, gbc);
    }

    public static void addNestedPanel(JPanel parent, JPanel child, String title) {
        parent.add(new JLabel(title));
        parent.add(child);
    }

    public static void bindLock(JCheckBox lockCheck, JComponent field) {
        Runnable apply = () -> {
            boolean locked = lockCheck.isSelected();
            if (field instanceof JTextComponent tc) {
                tc.setEnabled(!locked);
            } else {
                field.setEnabled(!locked);
            }
        };
        lockCheck.addItemListener(e -> apply.run());
        apply.run();
    }

}
