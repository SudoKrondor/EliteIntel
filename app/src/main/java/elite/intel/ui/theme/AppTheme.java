package elite.intel.ui.theme;

import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudButton;
import elite.intel.ui.widget.HudCheckBox;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudFooter;
import elite.intel.ui.widget.HudMetadataField;
import elite.intel.ui.widget.HudModalScaffold;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudPanel;
import elite.intel.ui.widget.HudPasswordField;
import elite.intel.ui.widget.HudScrollPane;
import elite.intel.ui.widget.HudSearchField;
import elite.intel.ui.widget.HudSlider;
import elite.intel.ui.widget.HudStepper;
import elite.intel.ui.widget.HudTabbedPane;
import elite.intel.ui.widget.HudTable;
import elite.intel.ui.widget.HudTextArea;
import elite.intel.ui.widget.HudTextField;
import elite.intel.ui.widget.HudToggleButton;
import elite.intel.ui.widget.StatusBadge;
import elite.intel.ui.widget.TopStatusBar;

import com.formdev.flatlaf.FlatClientProperties;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.image.BufferedImage;

import static elite.intel.ui.theme.HudPalette.*;

/**
 * HUD component factories, border factories, text/scroll/table stylers, and the
 * {@link #applyDarkPalette} treatment pass for the dark UI theme. The pieces split out of this
 * class: visual tokens live in {@link HudPalette}, glyph/icon primitives in {@link HudGlyphs},
 * and GridBag form-layout helpers in {@link HudForms}.
 */
public class AppTheme {
    /** Client-property key: set to {@link Boolean#TRUE} on a {@link javax.swing.JComponent} to prevent
     *  {@link #applyDarkPalette} from overriding its foreground colour. */
    public static final String HUD_LOCKED_FOREGROUND = "eliteIntel.hud.lockedForeground";
    public static final String HUD_TABLE_STYLE_LOCKED = "eliteIntel.hud.tableStyleLocked";
    /**
     * Opt-out client property for JScrollPane: when TRUE, applyDarkPalette does
     * NOT restyle this scroll pane (keeps its own viewport bg / border). Use for
     * data-plane table scrolls that must keep the warm HUD_COLOR_ROLE_APPLICATION_BACKGROUND viewport instead of
     * the cold HUD_COLOR_ROLE_PANEL_BACKGROUND that styleScrollPane applies. Mirrors
     * HUD_TABLE_STYLE_LOCKED for tables (ED_HUD_REFERENCE section 8.6).
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
     * {@code fieldHeight}x{@code fieldHeight} so it aligns with the neighbouring field height
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
        // Dark glyph on the bright primary fill (HUD inversion, section 0.4); the icon reads getForeground().
        button.setForeground(HUD_COLOR_ROLE_SELECTED_TEXT);
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
        b.setForeground(HUD_COLOR_ROLE_PRIMARY_TEXT);
        b.setBackground(HUD_COLOR_ROLE_PRIMARY_BUTTON_BACKGROUND);
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(HUD_COLOR_ROLE_PRIMARY_BUTTON_BACKGROUND, HUD_BORDER_THICKNESS, true),
                new EmptyBorder(6, 12, 6, 12)
        ));
    }

    /**
     * Creates the standard border used by reusable cockpit/HUD panels.
     */
    public static Border hudBorder() {
        return BorderFactory.createCompoundBorder(
                new LineBorder(HUD_COLOR_ROLE_SECONDARY_BORDER, HUD_BORDER_THICKNESS, true),
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
                new LineBorder(HUD_COLOR_ROLE_SECONDARY_BORDER, HUD_BORDER_THICKNESS, true),
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
        return BorderFactory.createMatteBorder(0, 1, 1, 1, HUD_COLOR_ROLE_FRAME_BORDER);
    }

    /**
     * Canonical footer rule (section 10) shared by modal dialogs and non-modal screen/tab footers:
     * a full-width warm {@link #HUD_COLOR_ROLE_PANEL_SEPARATOR} rule of {@link #HUD_BORDER_THICKNESS}
     * at the top, with {@link #HUD_GAP} padding above (after the rule) and below, zero side inset.
     * Apply to the footer strip built by {@link HudFooter}.
     */
    public static Border hudFooterBorder() {
        final int gap = HUD_GAP;
        final int th  = HUD_BORDER_THICKNESS;
        return new AbstractBorder() {
            @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                g.setColor(HUD_COLOR_ROLE_PANEL_SEPARATOR);
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
        label.setForeground(HUD_COLOR_ROLE_PRIMARY_ACTION);
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
        label.setForeground(HUD_COLOR_ROLE_PRIMARY_TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, HUD_FONT_SECTION_TITLE));
        label.putClientProperty(HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        return label;
    }

    /**
     * Light form field label (section 5.1): {@code HUD_COLOR_ROLE_PRIMARY_TEXT}, SM caps - the key column next to inputs.
     * Shares {@link #styleFieldLabel} with {@link #addLabel}.
     */
    public static JLabel hudReadoutLabel(String text) {
        JLabel lbl = new JLabel(text != null ? text.toUpperCase() : "");
        styleFieldLabel(lbl);
        return lbl;
    }

    /**
     * Single source of truth for field/readout key-label styling (section 5.1): {@code HUD_COLOR_ROLE_SECONDARY_TEXT}, XS caps,
     * foreground locked against the dark palette. Shared by {@link #hudReadoutLabel} and {@link #addLabel}
     * so every form label looks identical.
     */
    static void styleFieldLabel(JLabel label) {
        // Vanilla-ED form labels are light (HUD_COLOR_ROLE_PRIMARY_TEXT), not muted, at the field-label size (section 5.1).
        label.setForeground(HUD_COLOR_ROLE_PRIMARY_TEXT);
        label.setFont(label.getFont().deriveFont(HUD_FONT_FIELD_LABEL));
        label.putClientProperty(HUD_LOCKED_FOREGROUND, Boolean.TRUE);
    }

    /**
     * Creates a plain value label for readout rows at {@link #HUD_FONT_READOUT_VALUE} size.
     * Pair with {@link #hudReadoutLabel} for the key column. No border or background is set.
     *
     * @param value initial text
     * @param color foreground colour - e.g. {@link #HUD_COLOR_ROLE_INFORMATION} for command names, {@link #HUD_COLOR_ROLE_PRIMARY_TEXT} for plain values
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
        nameLabel.setForeground(HUD_COLOR_ROLE_INFORMATION);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, HUD_FONT_APP_TITLE));
        JLabel idLabel = new JLabel(id);
        idLabel.setForeground(HUD_COLOR_ROLE_SECONDARY_TEXT);
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
     * Outer field line that follows the component's enabled state: warm {@code HUD_COLOR_ROLE_CONTROL_DECORATION}
     * when enabled, dimmed {@code HUD_COLOR_ROLE_DISABLED} when disabled (section 0.6). Shared by all HUD fields and
     * combo boxes so the disabled look is consistent app-wide.
     */
    private static Border hudFieldLine() {
        return new javax.swing.border.AbstractBorder() {
            @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                g.setColor(c.isEnabled() ? HUD_COLOR_ROLE_CONTROL_DECORATION : HUD_COLOR_ROLE_DISABLED);
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

    /**
     * Creates the standard HUD input border used by text fields and combo boxes.
     */
    public static Border hudFieldBorder() {
        return BorderFactory.createCompoundBorder(
                hudFieldLine(),
                new EmptyBorder(HUD_FIELD_INSET_V, HUD_FIELD_INSET_H,
                        HUD_FIELD_INSET_V, HUD_FIELD_INSET_H)
        );
    }

    /**
     * Combo frame border: the warm field line plus the vertical inset only. The horizontal content
     * inset is owned by the cell renderer / editor ({@link #HUD_COMBO_ITEM_INSET_H}), so the collapsed
     * value lines up with plain HUD text fields instead of double-counting the field horizontal inset.
     */
    public static Border hudComboBorder() {
        return BorderFactory.createCompoundBorder(
                hudFieldLine(),
                new EmptyBorder(HUD_FIELD_INSET_V, 0, HUD_FIELD_INSET_V, 0)
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
     * Builds and returns the wrapper JPanel for an undecorated modal dialog using the HUD canon (section 7.2).
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
        tc.setBackground(HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        // Field value text - HUD_COLOR_ROLE_PRIMARY_ACTION (vanilla-ED: light label, orange value), the same for single-line
        // and multi-line FIELD areas; only enabled/disabled differs. The live console/log is a
        // separate role (HudLogArea, a JPanel) and is not styled here.
        tc.setForeground(HUD_COLOR_ROLE_PRIMARY_ACTION);
        tc.setDisabledTextColor(HUD_COLOR_ROLE_DISABLED); // section 0.6: disabled text dims to the warm muted tone
        tc.setCaretColor(HUD_COLOR_ROLE_PRIMARY_ACTION);
        tc.setSelectionColor(HUD_COLOR_ROLE_PRIMARY_ACTION);
        tc.setSelectedTextColor(HUD_COLOR_ROLE_SELECTED_TEXT);
        // Preserve the wider info border so the palette does not clobber the reserved info-"i" zone.
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
        tc.setBackground(HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND);
        tc.setForeground(HUD_COLOR_ROLE_PRIMARY_TEXT);
        tc.setCaretColor(HUD_COLOR_ROLE_INFORMATION);
        tc.setSelectionColor(HUD_COLOR_ROLE_INFORMATION);
        tc.setSelectedTextColor(HUD_COLOR_ROLE_SELECTED_TEXT);
        tc.setBorder(BorderFactory.createEmptyBorder());
        tc.setFont(tc.getFont().deriveFont(HUD_FONT_SM));
    }

    /**
     * Applies the compact borderless HUD treatment for read-only metadata fields.
     */
    public static void styleMetadataField(JTextComponent tc) {
        tc.setOpaque(true);
        tc.setBackground(HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND);
        tc.setForeground(HUD_COLOR_ROLE_PRIMARY_TEXT);
        tc.setCaretColor(HUD_COLOR_ROLE_SECONDARY_TEXT);
        tc.setSelectionColor(HUD_COLOR_ROLE_INFORMATION);
        tc.setSelectedTextColor(HUD_COLOR_ROLE_SELECTED_TEXT);
        tc.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        tc.setFont(tc.getFont().deriveFont(HUD_FONT_FIELD_VALUE));
    }

    /**
     * Applies the warm HUD palette (background, foreground, border, font) to a combo box without
     * replacing the model or renderer. The UI delegate is installed by HudComboBox itself.
     */
    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        comboBox.setForeground(HUD_COLOR_ROLE_PRIMARY_ACTION); // collapsed value - same HUD_COLOR_ROLE_PRIMARY_ACTION as the popup items (section 5.3)
        comboBox.setBorder(hudComboBorder());
        comboBox.setFocusable(true);
        comboBox.setFont(comboBox.getFont().deriveFont(HUD_FONT_FIELD_VALUE));
        // BasicComboBoxUI.paintCurrentValue paints the collapsed value in comboBox.getForeground(),
        // so the orange value is lost if applyDarkPalette overwrites the foreground during the tree
        // walk. Lock it (same opt-out the editable picker editor uses) to keep the canon value colour.
        comboBox.putClientProperty(HUD_LOCKED_FOREGROUND, Boolean.TRUE);
    }

    /**
     * Applies the canonical HUD treatment to the live editor of an editable combo (picker).
     *
     * <p>An editable combo paints its collapsed value through a FlatLaf-managed {@link JTextField}
     * editor, not through the renderer used by non-editable combos. This is the single place that
     * subdues that editor to the HUD canon so both combo types read identically (section 5.3):
     * <ul>
     *   <li>Value colour {@link #HUD_COLOR_ROLE_PRIMARY_ACTION} (enabled) / {@link #HUD_COLOR_ROLE_DISABLED} (disabled) - matches the
     *       renderer-painted value of non-editable combos.</li>
     *   <li>Text inset {@link #HUD_COMBO_ITEM_INSET_H}/{@link #HUD_COMBO_ITEM_INSET_V}, equal to the
     *       renderer item inset. FlatLaf's own {@code JTextField.padding} is zeroed so it does not add
     *       on top of our border - FlatLaf computes that padding once, before our border exists, and
     *       would otherwise shift the value text to the right.</li>
     *   <li>{@link #HUD_COMBO_EDITOR_LOCKED} + {@link #HUD_LOCKED_FOREGROUND} so {@link #applyDarkPalette}
     *       and {@link #styleTextComponent} do not overwrite the border/foreground during the palette walk.</li>
     * </ul>
     *
     * <p>Invoked from the combo box UI delegate after every FlatLaf editor (re)configuration, so the
     * HUD border is already in place when the FlatLaf padding is neutralised.
     */
    public static void styleComboEditor(JTextComponent editor) {
        editor.setBackground(HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        editor.setForeground(HUD_COLOR_ROLE_PRIMARY_ACTION);
        editor.setDisabledTextColor(HUD_COLOR_ROLE_DISABLED);
        editor.setCaretColor(HUD_COLOR_ROLE_PRIMARY_ACTION);
        editor.setSelectionColor(HUD_COLOR_ROLE_PRIMARY_ACTION);
        editor.setSelectedTextColor(HUD_COLOR_ROLE_SELECTED_TEXT);
        // Border carries the full text inset; equals the non-editable renderer item inset.
        editor.setBorder(new EmptyBorder(
                HUD_COMBO_ITEM_INSET_V, HUD_COMBO_ITEM_INSET_H,
                HUD_COMBO_ITEM_INSET_V, HUD_COMBO_ITEM_INSET_H));
        // Neutralise FlatLaf's injected editor padding so only our border defines the inset.
        editor.putClientProperty(FlatClientProperties.TEXT_FIELD_PADDING, new Insets(0, 0, 0, 0));
        editor.setFont(editor.getFont().deriveFont(HUD_FONT_FIELD_VALUE));
        editor.putClientProperty(HUD_COMBO_EDITOR_LOCKED, Boolean.TRUE);
        editor.putClientProperty(HUD_LOCKED_FOREGROUND, Boolean.TRUE);
    }

    /**
     * Applies the standard HUD treatment to checkbox-like buttons.
     */
    public static void styleCheckBox(AbstractButton checkBox) {
        checkBox.setOpaque(false);
        checkBox.setForeground(HUD_COLOR_ROLE_PRIMARY_TEXT);
        checkBox.setFocusPainted(false);
    }

    /**
     * Applies the standard HUD treatment to scroll panes and their viewport.
     */
    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        scrollPane.getViewport().setBackground(HUD_COLOR_ROLE_PANEL_BACKGROUND);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        styleScrollBar(scrollPane.getVerticalScrollBar());
        styleScrollBar(scrollPane.getHorizontalScrollBar());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    }

    public static void styleScrollBar(JScrollBar scrollBar) {
        scrollBar.setPreferredSize(new Dimension(9, 9));
        scrollBar.setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        scrollBar.setUnitIncrement(16);
        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = HUD_COLOR_ROLE_DISABLED;
                thumbDarkShadowColor = HUD_COLOR_ROLE_SECONDARY_BORDER;
                thumbHighlightColor = HUD_COLOR_ROLE_DISABLED;
                trackColor = HUD_COLOR_ROLE_PANEL_BACKGROUND;
                trackHighlightColor = HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND;
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
                    g2.setColor(HUD_COLOR_ROLE_DISABLED);
                    g2.fillRect(thumbBounds.x + 1, thumbBounds.y + 1,
                            Math.max(1, thumbBounds.width - 2),
                            Math.max(1, thumbBounds.height - 2));
                } finally {
                    g2.dispose();
                }
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                g.setColor(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
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
            // data-plane scroll owns its viewport bg/border - leave it untouched
        } else if (c instanceof JPanel || c instanceof JTabbedPane || c instanceof JScrollPane) {
            c.setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
            if (!lockForeground) c.setForeground(HUD_COLOR_ROLE_PRIMARY_TEXT);
        } else {
            c.setBackground(c instanceof JTextComponent ? HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND : HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
            if (!lockForeground) c.setForeground(HUD_COLOR_ROLE_PRIMARY_TEXT);
        }

        if (c instanceof JTextArea) {
            c.setBackground(HUD_COLOR_ROLE_LOG_BACKGROUND);
            c.setForeground(HUD_COLOR_ROLE_MONOSPACE_TEXT);
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

        if (c instanceof JTable table
                && !Boolean.TRUE.equals(table.getClientProperty(HUD_TABLE_STYLE_LOCKED))) {
            styleTable(table);
        }

        if (c instanceof JTabbedPane tp) {
            tp.setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
            tp.setForeground(HUD_COLOR_ROLE_PRIMARY_TEXT);
            tp.setOpaque(true);
        }

        if (c instanceof JScrollPane sp && !lockScroll) {
            styleScrollPane(sp);
        }

        if (c instanceof JEditorPane ep) {
            ep.setBackground(Color.WHITE);
            ep.setForeground(Color.BLACK);
        }

        // TopStatusBar and HudBanner own all colours of their children - do not recurse into them
        // (HudBanner's inner label/JTextArea must keep its banner styling, not get the field border).
        if (c instanceof Container cont && !(c instanceof TopStatusBar) && !(c instanceof HudBanner)) {
            for (Component child : cont.getComponents()) {
                applyDarkPalette(child);
            }
        }
    }

    // -- Modal scrim -----------------------------------------------------------

    /**
     * Shows a {@link #HUD_COLOR_ROLE_MODAL_SCRIM} veil on the owner's glass pane for the duration of a modal dialog.
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
                g.setColor(HUD_COLOR_ROLE_MODAL_SCRIM);
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
}
