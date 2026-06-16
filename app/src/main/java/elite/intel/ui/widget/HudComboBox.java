package elite.intel.ui.widget;
import static elite.intel.ui.theme.HudPalette.*;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * HUD-styled combo box with shared dark input colours and configurable cell rendering.
 *
 * <p>Constructors accepting {@code labelFn} control dropdown text extraction; constructors
 * accepting {@code mutedWhen} dim placeholder or disabled items with {@link AppTheme#HUD_COLOR_ROLE_SECONDARY_TEXT}.
 * All constructors install {@link HudComboBoxUI} and the built-in {@link HudComboRenderer}.
 *
 * <p>Use {@link #picker} to create an editable, searchable variant.
 *
 * @param <E> option type
 */
public class HudComboBox<E> extends JComboBox<E> {

    // -- Constructors ---------------------------------------------------------

    /** Creates a HUD combo box from an array; items rendered via {@link Object#toString}. */
    public HudComboBox(E[] values) {
        super(values);
        init(null, null);
    }

    /** Creates a HUD combo box from an array with a custom display-text extractor. */
    public HudComboBox(E[] values, Function<? super E, String> labelFn) {
        super(values);
        init(labelFn, null);
    }

    /**
     * Creates a HUD combo box from an array with a custom display-text extractor and a
     * predicate that dims non-selected items matching the condition with {@link AppTheme#HUD_COLOR_ROLE_SECONDARY_TEXT}.
     */
    public HudComboBox(E[] values, Function<? super E, String> labelFn,
                       Predicate<? super E> mutedWhen) {
        super(values);
        init(labelFn, mutedWhen);
    }

    /**
     * Creates a HUD combo box from a {@link ComboBoxModel}; useful for dynamic or pre-built models.
     * Items rendered via {@link Object#toString}.
     */
    public HudComboBox(ComboBoxModel<E> model) {
        super(model);
        init(null, null);
    }

    /** Creates a HUD combo box from a {@link ComboBoxModel} with a custom display-text extractor. */
    public HudComboBox(ComboBoxModel<E> model, Function<? super E, String> labelFn) {
        super(model);
        init(labelFn, null);
    }

    /**
     * Creates a HUD combo box from a {@link ComboBoxModel} with a custom display-text extractor
     * and a muted-item predicate.
     */
    public HudComboBox(ComboBoxModel<E> model, Function<? super E, String> labelFn,
                       Predicate<? super E> mutedWhen) {
        super(model);
        init(labelFn, mutedWhen);
    }

    private void init(Function<? super E, String> labelFn, Predicate<? super E> mutedWhen) {
        // Install the HUD UI delegate here (not in AppTheme) so the theme layer stays independent
        // of the combo UI. Guard by instanceof for idempotency when the tree is rebuilt: setUI()
        // unconditionally recreates the editor (uninstall/installComponents), which would orphan
        // an editable combo's editor DocumentListener.
        if (!(getUI() instanceof HudComboBoxUI)) {
            setUI(new HudComboBoxUI());
        }
        AppTheme.styleComboBox(this);
        setRenderer(new HudComboRenderer<>(labelFn, mutedWhen));
    }

    /**
     * Caps a non-editable combo to the shared field height ({@link AppTheme#HUD_FIELD_HEIGHT}, the same
     * token {@link HudTextField} uses) so combos line up with text fields in form rows. FlatLaf otherwise
     * makes combos a few px taller than fields. Editable pickers and any explicit {@code setPreferredSize}
     * (e.g. {@code addField}, {@link #picker}) are honoured untouched; width stays dynamic.
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (!isPreferredSizeSet() && !isEditable()) {
            return new Dimension(d.width, HudPalette.HUD_FIELD_HEIGHT);
        }
        return d;
    }

    // -- Renderer -------------------------------------------------------------

    /**
     * HUD dropdown cell renderer.
     *
     * <ul>
     *   <li>Selected: {@link AppTheme#HUD_COLOR_ROLE_PRIMARY_ACTION} background, {@link AppTheme#HUD_COLOR_ROLE_SELECTED_TEXT} foreground.</li>
     *   <li>Non-selected: {@link AppTheme#HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND} background;
     *       {@link AppTheme#HUD_COLOR_ROLE_SECONDARY_TEXT} when {@code mutedWhen} matches, otherwise {@link AppTheme#HUD_COLOR_ROLE_PRIMARY_TEXT}.</li>
     *   <li>Font: {@link AppTheme#HUD_FONT_FIELD_VALUE}.</li>
     *   <li>Border: {@link AppTheme#HUD_COMBO_ITEM_INSET_V} / {@link AppTheme#HUD_COMBO_ITEM_INSET_H}.</li>
     * </ul>
     */
    static final class HudComboRenderer<E> extends DefaultListCellRenderer {
        private final Function<? super E, String> labelFn;
        private final Predicate<? super E> mutedWhen;

        HudComboRenderer(Function<? super E, String> labelFn, Predicate<? super E> mutedWhen) {
            this.labelFn = labelFn;
            this.mutedWhen = mutedWhen;
        }

        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            String text;
            if (value == null) {
                text = "";
            } else if (labelFn != null) {
                @SuppressWarnings("unchecked") E item = (E) value;
                text = labelFn.apply(item);
            } else {
                text = String.valueOf(value);
            }
            label.setText(text);

            if (isSelected) {
                label.setBackground(HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
                label.setForeground(HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT);
            } else {
                label.setBackground(HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
                boolean muted = false;
                if (mutedWhen != null && value != null) {
                    @SuppressWarnings("unchecked") E item = (E) value;
                    muted = mutedWhen.test(item);
                }
                label.setForeground(muted ? HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT : HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
            }

            label.setBorder(new EmptyBorder(
                    HudPalette.HUD_COMBO_ITEM_INSET_V, HudPalette.HUD_COMBO_ITEM_INSET_H,
                    HudPalette.HUD_COMBO_ITEM_INSET_V, HudPalette.HUD_COMBO_ITEM_INSET_H));
            label.setFont(label.getFont().deriveFont(HudPalette.HUD_FONT_FIELD_VALUE));
            return label;
        }
    }

    // -- Picker factory -------------------------------------------------------

    /**
     * Creates an editable HUD combo box for searchable pickers.
     *
     * <p>The dropdown list is filtered live as the user types: {@code matches.test(item, query)}
     * decides which items remain visible. Selecting an item from the list or typing its exact
     * display text (as produced by {@code labelFn}) restores the full list and commits the item.
     *
     * <p>When the popup re-opens after a filtered session the full item list is restored so the
     * user can see all options again.
     *
     * @param items    full item array; used as the restore model when the popup re-opens
     * @param labelFn  produces display text per item; {@code null} falls back to
     *                 {@link Object#toString}; also used for exact-match detection
     * @param matches  {@code (item, query) -> true} when the item should stay visible
     * @param <E>      item type
     * @return configured editable {@link HudComboBox} with live search
     */
    public static <E> HudComboBox<E> picker(
            E[] items,
            Function<? super E, String> labelFn,
            BiPredicate<? super E, String> matches) {

        HudComboBox<E> combo = new HudComboBox<>(items, labelFn);
        // setEditable(true) triggers FlatLaf's addEditor()/configureEditor(); HudComboBoxUI.configureEditor()
        // subdues the editor to the HUD canon (colour, inset, locks). No manual editor styling needed here.
        combo.setEditable(true);
        // Picker keeps the wide picker field width but the standard field height, so it lines up with
        // text fields and non-editable combos (height is owned by the HUD layer, not each call site).
        combo.setPreferredSize(new Dimension(HudPalette.HUD_PICKER_FIELD_WIDTH, HudPalette.HUD_FIELD_HEIGHT));
        combo.setMinimumSize(new Dimension(HudPalette.HUD_PICKER_FIELD_WIDTH, HudPalette.HUD_FIELD_HEIGHT));

        configureSearch(combo, items, labelFn, matches);
        return combo;
    }

    /**
     * Wires live search/filter on an editable combo.
     *
     * <ul>
     *   <li>A {@link PopupMenuListener} restores the full model when the popup re-opens after
     *       a filtered session.</li>
     *   <li>A {@link DocumentListener} on the editor field rebuilds the visible model on every
     *       keystroke. An exact display-text match selects the item directly.</li>
     * </ul>
     */
    private static <E> void configureSearch(
            HudComboBox<E> combo,
            E[] sourceItems,
            Function<? super E, String> labelFn,
            BiPredicate<? super E, String> matches) {

        Component editorComponent = combo.getEditor().getEditorComponent();
        if (!(editorComponent instanceof JTextComponent editor)) {
            return;
        }

        final boolean[] updating = {false};

        combo.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // Restore the full list only on a genuine user-initiated open (empty/selected
                // field), NOT during programmatic re-open from filter() — that would wipe the
                // filtered model. updating[] marks programmatic model changes.
                if (!updating[0] && combo.getModel().getSize() != sourceItems.length) {
                    Object prev = combo.getSelectedItem();
                    updating[0] = true;
                    combo.setModel(new DefaultComboBoxModel<>(sourceItems));
                    if (prev != null) combo.setSelectedItem(prev);
                    updating[0] = false;
                }
            }
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filter(); }
            @Override public void removeUpdate(DocumentEvent e)  { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter(); }

            private void filter() {
                // updating[] guards against re-entrancy from programmatic setModel/setSelectedItem;
                // hasFocus() check absent — FlatLaf editable combos report focus on the container.
                if (updating[0]) return;
                SwingUtilities.invokeLater(() -> {
                    String query = editor.getText();

                    // Exact display-text match: restore full model and select the matched item.
                    for (E item : sourceItems) {
                        String display = labelFn != null ? labelFn.apply(item) : String.valueOf(item);
                        if (display.equals(query)) {
                            updating[0] = true;
                            combo.setModel(new DefaultComboBoxModel<>(sourceItems));
                            combo.setSelectedItem(item);
                            updating[0] = false;
                            return;
                        }
                    }

                    // Partial match: build a filtered model.
                    DefaultComboBoxModel<E> model = new DefaultComboBoxModel<>();
                    for (E item : sourceItems) {
                        if (matches.test(item, query)) model.addElement(item);
                    }
                    updating[0] = true;
                    combo.setModel(model);
                    combo.setSelectedItem(query);
                    // An already-open popup ignores showPopup() (no-op) and keeps painting the
                    // stale model. Re-open it (hide+show) so it recomputes size from the new model.
                    // Both calls run under updating[0]=true so popupMenuWillBecomeVisible skips
                    // the full-list restore and the filtered model survives.
                    if (combo.isShowing() && model.getSize() > 0) {
                        if (combo.isPopupVisible()) combo.hidePopup();
                        combo.showPopup();
                    }
                    updating[0] = false;
                });
            }
        });
    }
}
