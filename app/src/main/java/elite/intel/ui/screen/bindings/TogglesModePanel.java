package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.io.BindsValues;
import elite.intel.bindforge.model.SettingToggles;
import elite.intel.bindforge.storage.ReadOnlyMode;
import elite.intel.ui.widget.HudToggleButton;

import javax.swing.*;
import java.awt.*;
import java.util.function.BooleanSupplier;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.sizeFieldLabel;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * The Bind Editor's Toggles mode: every yes/no setting the binds file holds, grouped as Dead Zones &amp; Sensitivity
 * groups its rows (bind-editor.md, <em>Toggles</em>).
 * <p>
 * A row is the setting's name and a button reading ON or OFF, as the game's own options screen does (E10 Q1). Only
 * {@code 1} reads as on; a value the game would not write reads as off and stays in the file until the button is
 * pressed (Q5). A press keeps the change as an edit not applied - nothing here writes a file (storage.md rule 6) - and
 * a setting the file holds more than once, as every file the game saves holds {@code MouseGUI}, has every copy written
 * (Q4). The list, the search and the edit are {@link SettingRowsPanel}'s.
 */
class TogglesModePanel extends SettingRowsPanel<TogglesModePanel.Row> {

    private static final int NAME_WIDTH = 340;
    private static final int BUTTON_TEXT_MARGIN = 28;

    TogglesModePanel(GameModePanel.EditRunner editRunner) {
        this(editRunner, () -> ReadOnlyMode.stored().activeIfKnown());
    }

    /**
     * @param readOnly whether BindForge is read only - the shared {@link ReadOnlyMode} but in a test
     */
    TogglesModePanel(GameModePanel.EditRunner editRunner, BooleanSupplier readOnly) {
        this(editRunner, readOnly, THROUGH_EDITS);
    }

    /**
     * @param write how a change is kept - through the edits but in a test
     */
    TogglesModePanel(GameModePanel.EditRunner editRunner, BooleanSupplier readOnly, SettingWrite write) {
        super(editRunner, readOnly, write, SettingToggles.SETTINGS,
                "bindings.toggles.hint", "bindings.toggles.none", "bindings.mode.toggles");
    }

    @Override
    protected Row newRow(String id, String name) {
        return new Row(id, name);
    }

    /**
     * Keeps the button's new state as an edit, or puts it back and says why it cannot be.
     */
    private void commit(Row row) {
        BindsValues.Setting setting = settings.get(row.id);
        if (!changeable(setting)) {
            row.showValue(setting);
            return;
        }
        boolean on = row.button.isSelected();
        row.showText(on);
        submit(row, SettingToggles.written(on), () -> {
            row.showValue(settings.get(row.id));
            row.problem.setText(getText("bindings.settings.notEditable"));
        });
    }

    /**
     * One setting: its name, its ON/OFF button and what to say when it could not be changed.
     */
    final class Row implements SettingRowsPanel.SettingRow {
        final String id;
        final JPanel panel = transparentPanel(new GridBagLayout());
        final HudToggleButton button = new HudToggleButton("");
        final JLabel problem = hudReadoutValue(" ", HUD_COLOR_ROLE_WARNING);

        Row(String id, String name) {
            this.id = id;
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 0, 2, 8);
            gbc.anchor = GridBagConstraints.WEST;
            JLabel label = hudReadoutLabel(name);
            label.setToolTipText(id);
            sizeFieldLabel(label, NAME_WIDTH);
            panel.add(label, gbc);
            sizeButton();
            panel.add(button, gbc);
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(problem, gbc);
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
            button.addActionListener(e -> commit(this));
        }

        /**
         * WHY one width for both words: the button keeps its place in the row when it turns over.
         */
        private void sizeButton() {
            FontMetrics metrics = button.getFontMetrics(button.getFont());
            int width = Math.max(metrics.stringWidth(getText("bindings.toggles.on").toUpperCase()),
                    metrics.stringWidth(getText("bindings.toggles.off").toUpperCase())) + BUTTON_TEXT_MARGIN;
            Dimension size = new Dimension(Math.max(width, button.getPreferredSize().width), HUD_BUTTON_HEIGHT);
            button.setPreferredSize(size);
            button.setMinimumSize(size);
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
            boolean on = SettingToggles.on(setting.value());
            button.setSelected(on);
            showText(on);
            button.setEnabled(changeable(setting));
            button.setToolTipText(changeable(setting) ? null : getText(setting.editable()
                    ? "bindings.readOnly.tooltip" : "bindings.settings.notEditable"));
            problem.setText(" ");
        }

        void showText(boolean on) {
            button.setText(getText(on ? "bindings.toggles.on" : "bindings.toggles.off").toUpperCase());
        }
    }
}
