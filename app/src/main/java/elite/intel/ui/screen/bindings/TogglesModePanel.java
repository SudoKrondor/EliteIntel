package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.io.BindingSaveResult;
import elite.intel.bindforge.io.BindingsWriter;
import elite.intel.bindforge.io.BindsValues;
import elite.intel.bindforge.model.BindingDisplayNames;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.model.SettingToggles;
import elite.intel.bindforge.storage.BindsEdits;
import elite.intel.bindforge.storage.Edits;
import elite.intel.bindforge.storage.ReadOnlyMode;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudToggleButton;
import elite.intel.ui.widget.StatusBadge;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
 * (Q4).
 */
class TogglesModePanel extends JPanel {

    private static final int NAME_WIDTH = 340;
    private static final int BUTTON_TEXT_MARGIN = 28;

    private final BindingsWriter writer = new BindingsWriter();
    private final GameModePanel.EditRunner editRunner;
    /** Whether BindForge is read only, an installation missing (Storage S14b). Asked on the EDT. */
    private final BooleanSupplier readOnly;
    private final JPanel list = transparentPanel(null);
    private final Map<String, Row> rows = new LinkedHashMap<>();

    private String file;
    private Map<String, BindsValues.Setting> settings = Map.of();
    private String query = "";

    TogglesModePanel(GameModePanel.EditRunner editRunner) {
        this(editRunner, () -> ReadOnlyMode.stored().activeIfKnown());
    }

    /**
     * @param readOnly whether BindForge is read only - the shared {@link ReadOnlyMode} but in a test
     */
    TogglesModePanel(GameModePanel.EditRunner editRunner, BooleanSupplier readOnly) {
        this.editRunner = editRunner;
        this.readOnly = readOnly;
        setOpaque(false);
        setLayout(new BorderLayout(0, 4));
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(2, 6, 2, 6));
        add(HudBanner.multiline(getText("bindings.toggles.hint"), StatusBadge.State.INFO), BorderLayout.NORTH);
        JPanel top = transparentPanel(new BorderLayout());
        top.add(list, BorderLayout.NORTH);
        JScrollPane scroll = hudApplicationScrollPane(top);
        scroll.getVerticalScrollBar().setUnitIncrement(HUD_FIELD_HEIGHT);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(hudDataPlaneBorder());
        add(scroll, BorderLayout.CENTER);
    }

    /**
     * Shows what the shell's read found. On the EDT. Rebuilds only when the rows change, as Dead Zones &amp;
     * Sensitivity does, so a read following an edit elsewhere leaves the list where it was scrolled.
     */
    void show(String file, BindsValues values) {
        boolean sameRows = Objects.equals(this.file, file) && settings.keySet().equals(values.settings().keySet());
        this.file = file;
        settings = values.settings();
        if (!sameRows) {
            rebuild();
            return;
        }
        rows.values().forEach(row -> row.showValue(settings.get(row.id)));
    }

    /**
     * BindForge went read only, or stopped being (Storage S14b): every button follows. On the EDT.
     */
    void readOnlyChanged() {
        rows.values().forEach(row -> row.showValue(settings.get(row.id)));
    }

    /**
     * Narrows the rows to {@code query}, as the shell's search field is typed in. On the EDT.
     */
    void search(String query) {
        this.query = query;
        rebuild();
    }

    /**
     * Whether this setting may change: one the writer knows, while BindForge is not read only (S14b).
     */
    private boolean changeable(BindsValues.Setting setting) {
        return setting.editable() && !readOnly.getAsBoolean();
    }

    private void rebuild() {
        rows.clear();
        list.removeAll();
        List<String> ids = shownIds();
        if (ids.isEmpty()) {
            list.add(hudReadoutLabel(getText("bindings.toggles.none")));
        }
        BindingSection section = null;
        String group = null;
        for (String id : ids) {
            BindingDisplayNames.ControlName control = BindingDisplayNames.lookup(id);
            if (control.section() != section) {
                section = control.section();
                group = null;
                JLabel heading = hudGroupLabel(getText(section.getLabelKey()).toUpperCase());
                heading.setBorder(new EmptyBorder(10, 0, 6, 0));
                list.add(leftAligned(heading));
            }
            if (!Objects.equals(control.group(), group)) {
                group = control.group();
                if (group != null) {
                    JLabel heading = hudReadoutValue(group, HUD_COLOR_ROLE_PRIMARY_ACTION);
                    heading.setBorder(new EmptyBorder(6, 0, 2, 0));
                    list.add(leftAligned(heading));
                }
            }
            Row row = new Row(id, control.name());
            row.showValue(settings.get(id));
            rows.put(id, row);
            list.add(row.panel);
        }
        list.revalidate();
        list.repaint();
    }

    private static JComponent leftAligned(JComponent component) {
        JPanel line = transparentPanel(new BorderLayout());
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        line.add(component, BorderLayout.WEST);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, line.getPreferredSize().height));
        return line;
    }

    /**
     * Every setting on the toggles list that the file holds, whatever its value (E10 Q3), in the game's order.
     */
    private List<String> shownIds() {
        if (file == null) return List.of();
        String typed = query.trim().toLowerCase();
        Set<String> present = settings.keySet();
        return SettingToggles.SETTINGS.stream()
                .filter(present::contains)
                .filter(id -> typed.isEmpty() || BindingDisplayNames.searchText(id,
                        getText(BindingDisplayNames.lookup(id).section().getLabelKey())).contains(typed))
                .sorted(Comparator.comparingInt((String id) -> BindingDisplayNames.lookup(id).order())
                        .thenComparing(Comparator.naturalOrder()))
                .toList();
    }

    /**
     * Keeps the button's new state as an edit, or says why it cannot be. Off the EDT through the shell's runner.
     */
    private void commit(Row row) {
        BindsValues.Setting setting = settings.get(row.id);
        boolean on = row.button.isSelected();
        if (setting == null) return;
        if (!changeable(setting) || editRunner.running()) {
            row.showValue(setting);
            return;
        }
        row.showText(on);
        String target = file;
        String written = SettingToggles.written(on);
        AtomicReference<BindingSaveResult> result = new AtomicReference<>();
        editRunner.run(getText("bindings.mode.toggles"),
                () -> Edits.stored().change(current -> {
                    BindsEdits.Changed changed = BindsEdits.change(current, target,
                            List.of(binds -> writer.assignSetting(binds, row.id, written)));
                    result.set(changed.result());
                    return changed.files();
                }),
                () -> {
                    BindingSaveResult saved = result.get();
                    if (saved != BindingSaveResult.SAVED && saved != BindingSaveResult.NO_CHANGE) {
                        row.showValue(settings.get(row.id));
                        row.problem.setText(getText("bindings.settings.notEditable"));
                    }
                }, () -> row.showValue(settings.get(row.id)));
    }

    /**
     * One setting: its name, its ON/OFF button and what to say when it could not be changed.
     */
    private final class Row {
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

        void showValue(BindsValues.Setting setting) {
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
