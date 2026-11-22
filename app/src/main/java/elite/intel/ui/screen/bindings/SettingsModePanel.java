package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.io.BindsValues;
import elite.intel.bindforge.model.SettingNumbers;
import elite.intel.bindforge.storage.ReadOnlyMode;
import elite.intel.ui.widget.HudSlider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.BooleanSupplier;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.sizeFieldLabel;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * The Bind Editor's Settings mode: the sensitivities, dead zones and power curves the binds file holds, as the game
 * groups them.
 * <p>
 * <strong>A slider and the number beside it</strong> (bind-editor.md, <em>The three value shapes</em>). The slider
 * covers what the game's own slider covers, measured in {@link SettingNumbers}; the number is the exact value, and the
 * only way past those stops, which players' files do go. A setting with no measured range has the number alone. A
 * value outside the range pins the thumb to the end and is left as it is until someone moves it.
 * <p>
 * Letting go of a slider, Enter, or leaving the field keeps a change as an edit not applied; Esc puts the number back.
 * Nothing here writes a file (storage.md rule 6); the list, the search and the edit are {@link SettingRowsPanel}'s.
 */
class SettingsModePanel extends SettingRowsPanel<SettingsModePanel.Row> {

    private static final int NAME_WIDTH = 340;
    private static final int FIELD_COLUMNS = 10;
    private static final Dimension SLIDER_SIZE = new Dimension(300, HUD_SLIDER_HEIGHT);

    SettingsModePanel(GameModePanel.EditRunner editRunner) {
        this(editRunner, () -> ReadOnlyMode.stored().activeIfKnown());
    }

    /**
     * @param readOnly whether BindForge is read only - the shared {@link ReadOnlyMode} but in a test
     */
    SettingsModePanel(GameModePanel.EditRunner editRunner, BooleanSupplier readOnly) {
        this(editRunner, readOnly, THROUGH_EDITS);
    }

    /**
     * @param write how a change is kept - through the edits but in a test
     */
    SettingsModePanel(GameModePanel.EditRunner editRunner, BooleanSupplier readOnly, SettingWrite write) {
        super(editRunner, readOnly, write, SettingNumbers.SETTINGS,
                "bindings.settings.hint", "bindings.settings.none", "bindings.mode.settings");
    }

    @Override
    protected Row newRow(String id, String name) {
        return new Row(id, name);
    }

    /**
     * Keeps a typed number as an edit, or says why it cannot be.
     */
    private void commit(Row row) {
        BindsValues.Setting setting = settings.get(row.id);
        if (setting == null || !changeable(setting)) return;
        SettingNumbers.Verdict verdict = SettingNumbers.judge(row.id, row.field.getText());
        if (!verdict.valid()) {
            row.problem.setText(switch (verdict.refusal()) {
                case NOT_A_NUMBER -> getText("bindings.settings.refused.number");
                case MUST_BE_ABOVE_ZERO -> getText("bindings.settings.refused.aboveZero");
                case MUST_BE_ZERO_TO_ONE -> getText("bindings.settings.refused.zeroToOne");
            });
            return;
        }
        row.showRangeNote(setting);
        // WHY compared as a float: 1.00000000 in the file and 1.0 in the field are one value, and the file keeps its text.
        if (SettingNumbers.judge(row.id, setting.value()).value() instanceof Float held && held.equals(verdict.value())) {
            return;
        }
        submit(row, SettingNumbers.written(verdict.value()),
                () -> row.problem.setText(getText("bindings.settings.notEditable")));
    }

    /**
     * One setting: its name, its slider when its range is known, its number and what to say about that number.
     */
    final class Row implements SettingRowsPanel.SettingRow {
        final String id;
        final JPanel panel = transparentPanel(new GridBagLayout());
        final JTextField field = makeTextField();
        final JLabel problem = hudReadoutValue(" ", HUD_COLOR_ROLE_WARNING);
        final SettingNumbers.Range range;
        final HudSlider slider;
        /**
         * Where the held value put the thumb. Back here, the slider has changed nothing: its scale must not rewrite a
         * value it merely shows.
         */
        int heldPosition;
        boolean placingSlider;

        Row(String id, String name) {
            this.id = id;
            range = SettingNumbers.range(id).orElse(null);
            slider = range == null ? null
                    : new HudSlider(range.sliderMin(), range.sliderMax(), range.sliderStep(), range.sliderMin());
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            GridBagConstraints gbc = new GridBagConstraints();
            // WHY the top inset: lines the label and the number up with the slider's track, below its value.
            gbc.insets = new Insets(2 + HUD_SLIDER_VALUE_AREA, 0, 2, 8);
            gbc.anchor = GridBagConstraints.WEST;
            JLabel label = hudReadoutLabel(name);
            label.setToolTipText(id);
            sizeFieldLabel(label, NAME_WIDTH);
            panel.add(label, gbc);
            gbc.insets = new Insets(2, 0, 2, 8);
            if (slider == null) {
                panel.add(Box.createRigidArea(SLIDER_SIZE), gbc);
            } else {
                slider.setPreferredSize(SLIDER_SIZE);
                slider.setMinimumSize(SLIDER_SIZE);
                slider.setValueFormatter(this::textAt);
                slider.addChangeListener(e -> sliderMoved());
                panel.add(slider, gbc);
            }
            gbc.insets = new Insets(2 + HUD_SLIDER_VALUE_AREA, 0, 2, 8);
            field.setColumns(FIELD_COLUMNS);
            field.setMinimumSize(field.getPreferredSize());
            panel.add(field, gbc);
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(problem, gbc);
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));

            field.addActionListener(e -> commit(this));
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    commit(Row.this);
                }
            });
            field.getInputMap(JComponent.WHEN_FOCUSED)
                    .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "bindforge.setting.revert");
            field.getActionMap().put("bindforge.setting.revert", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    problem.setText(" ");
                    field.setText(SettingNumbers.shown(settings.get(Row.this.id).value()));
                    showRangeNote(settings.get(Row.this.id));
                }
            });
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public JComponent panel() {
            return panel;
        }

        @Override
        public void showValue(BindsValues.Setting setting) {
            field.setEnabled(changeable(setting));
            field.setToolTipText(changeable(setting) ? null : getText(setting.editable()
                    ? "bindings.readOnly.tooltip" : "bindings.settings.notEditable"));
            if (!field.isFocusOwner()) {
                field.setText(SettingNumbers.shown(setting.value()));
            }
            placeSlider(setting);
            showRangeNote(setting);
        }

        /**
         * WHY it leaves a thumb in someone's hand alone: a read following another row's edit can land mid-drag.
         */
        private void placeSlider(BindsValues.Setting setting) {
            if (slider == null || slider.isAdjusting()) return;
            slider.setEnabled(changeable(setting));
            Float held = SettingNumbers.judge(id, setting.value()).value();
            heldPosition = held == null ? range.sliderMin() : range.toSlider(held);
            placingSlider = true;
            slider.setValue(heldPosition);
            placingSlider = false;
            heldPosition = slider.getValue();
            slider.repaint();
        }

        /**
         * Says so when the held value is past the game's stops, because the thumb at the end of its track cannot.
         * WHY it stays out of a refusal's way: this runs on every read, and the commander has not fixed that number yet.
         */
        private void showRangeNote(BindsValues.Setting setting) {
            if (range == null || !SettingNumbers.judge(id, field.getText()).valid()) return;
            Float held = SettingNumbers.judge(id, setting.value()).value();
            boolean outside = held != null && !range.contains(held);
            problem.setText(outside
                    ? getText("bindings.settings.outsideRange",
                    SettingNumbers.shown(String.valueOf(range.min())), SettingNumbers.shown(String.valueOf(range.max())))
                    : " ");
        }

        private String textAt(int position) {
            BindsValues.Setting setting = settings.get(id);
            return position == heldPosition && setting != null
                    ? SettingNumbers.shown(setting.value())
                    : SettingNumbers.shown(SettingNumbers.written(range.fromSlider(position)));
        }

        /**
         * The number follows the thumb; letting go keeps it, unless the thumb came back to where it started.
         */
        private void sliderMoved() {
            if (placingSlider) return;
            field.setText(textAt(slider.getValue()));
            if (slider.getValue() == heldPosition) {
                showRangeNote(settings.get(id));
                return;
            }
            problem.setText(" ");
            if (!slider.isAdjusting()) {
                commit(this);
            }
        }
    }
}
