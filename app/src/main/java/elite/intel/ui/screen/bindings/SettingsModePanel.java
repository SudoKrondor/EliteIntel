package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.io.BindingSaveResult;
import elite.intel.bindforge.io.BindingsWriter;
import elite.intel.bindforge.io.BindsValues;
import elite.intel.bindforge.model.BindingDisplayNames;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.model.SettingNumbers;
import elite.intel.bindforge.storage.BindsEdits;
import elite.intel.bindforge.storage.Edits;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.StatusBadge;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.sizeFieldLabel;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * The Bind Editor's Settings mode: the sensitivities, dead zones and power curves the binds file holds, as the game
 * groups them.
 * <p>
 * <strong>A typed number, not a slider</strong> (bind-editor.md, <em>The three value shapes</em>): a slider has to know
 * its range and the game's limits are written down nowhere - see {@link SettingNumbers}. A typed value is written as
 * typed, the same contract the rest of BindForge keeps with the file.
 * <p>
 * Enter or leaving the field keeps a change as an edit not applied; Esc puts the number back. Nothing here writes a
 * file (storage.md rule 6).
 */
class SettingsModePanel extends JPanel {

    private static final int NAME_WIDTH = 340;
    private static final int FIELD_COLUMNS = 10;

    private final BindingsWriter writer = new BindingsWriter();
    private final GameModePanel.EditRunner editRunner;
    private final JPanel list = transparentPanel(null);
    private final Map<String, Row> rows = new LinkedHashMap<>();

    private String file;
    private Map<String, BindsValues.Setting> settings = Map.of();
    private String query = "";

    SettingsModePanel(GameModePanel.EditRunner editRunner) {
        this.editRunner = editRunner;
        setOpaque(false);
        setLayout(new BorderLayout(0, 4));
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(2, 6, 2, 6));
        add(HudBanner.multiline(getText("bindings.settings.hint"), StatusBadge.State.INFO), BorderLayout.NORTH);
        JPanel top = transparentPanel(new BorderLayout());
        top.add(list, BorderLayout.NORTH);
        JScrollPane scroll = hudApplicationScrollPane(top);
        scroll.getVerticalScrollBar().setUnitIncrement(HUD_FIELD_HEIGHT);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(hudDataPlaneBorder());
        add(scroll, BorderLayout.CENTER);
    }

    /**
     * Shows what the shell's read found. On the EDT.
     * <p>
     * WHY it does not always rebuild: a read also follows an edit anywhere else in BindForge, and a rebuild would take
     * the number out from under someone typing it.
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
     * Narrows the rows to {@code query}, as the shell's search field is typed in. On the EDT.
     */
    void search(String query) {
        this.query = query;
        rebuild();
    }

    private void rebuild() {
        rows.clear();
        list.removeAll();
        List<String> ids = shownIds();
        if (ids.isEmpty()) {
            list.add(hudReadoutLabel(getText("bindings.settings.none")));
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

    private List<String> shownIds() {
        if (file == null) return List.of();
        String typed = query.trim().toLowerCase();
        Set<String> present = settings.keySet();
        return SettingNumbers.SETTINGS.stream()
                .filter(present::contains)
                .filter(id -> typed.isEmpty() || BindingDisplayNames.searchText(id,
                        getText(BindingDisplayNames.lookup(id).section().getLabelKey())).contains(typed))
                .sorted(Comparator.comparingInt(id -> BindingDisplayNames.lookup(id).order()))
                .toList();
    }

    /**
     * Keeps a typed number as an edit, or says why it cannot be. Off the EDT through the shell's runner.
     */
    private void commit(Row row) {
        BindsValues.Setting setting = settings.get(row.id);
        if (setting == null || !setting.editable()) return;
        SettingNumbers.Verdict verdict = SettingNumbers.judge(row.id, row.field.getText());
        if (!verdict.valid()) {
            row.problem.setText(switch (verdict.refusal()) {
                case NOT_A_NUMBER -> getText("bindings.settings.refused.number");
                case MUST_BE_ABOVE_ZERO -> getText("bindings.settings.refused.aboveZero");
                case MUST_BE_ZERO_TO_ONE -> getText("bindings.settings.refused.zeroToOne");
            });
            return;
        }
        row.problem.setText(" ");
        // WHY compared as a float: 1.00000000 in the file and 1.0 in the field are one value, and the file keeps its text.
        if (SettingNumbers.judge(row.id, setting.value()).value() instanceof Float held && held.equals(verdict.value())) {
            return;
        }
        if (editRunner.running()) return;
        String target = file;
        String written = SettingNumbers.written(verdict.value());
        AtomicReference<BindingSaveResult> result = new AtomicReference<>();
        editRunner.run(getText("bindings.mode.settings"),
                () -> Edits.stored().change(current -> {
                    BindsEdits.Changed changed = BindsEdits.change(current, target,
                            List.of(binds -> writer.assignSetting(binds, row.id, written)));
                    result.set(changed.result());
                    return changed.files();
                }),
                () -> {
                    BindingSaveResult saved = result.get();
                    if (saved != BindingSaveResult.SAVED && saved != BindingSaveResult.NO_CHANGE) {
                        row.problem.setText(getText("bindings.settings.notEditable"));
                    }
                }, () -> {
                });
    }

    /**
     * One setting: its name, its number and why a typed number was refused.
     */
    private final class Row {
        final String id;
        final JPanel panel = transparentPanel(new GridBagLayout());
        final JTextField field = makeTextField();
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
                }
            });
        }

        void showValue(BindsValues.Setting setting) {
            field.setEnabled(setting.editable());
            field.setToolTipText(setting.editable() ? null : getText("bindings.settings.notEditable"));
            if (!field.isFocusOwner()) {
                field.setText(SettingNumbers.shown(setting.value()));
            }
        }
    }
}
