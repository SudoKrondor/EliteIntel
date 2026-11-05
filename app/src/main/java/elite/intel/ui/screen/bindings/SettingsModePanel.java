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
import elite.intel.ui.widget.HudSlider;
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
 * <strong>A slider and the number beside it</strong> (bind-editor.md, <em>The three value shapes</em>). The slider
 * covers what the game's own slider covers, measured in {@link SettingNumbers}; the number is the exact value, and the
 * only way past those stops, which players' files do go. A setting with no measured range has the number alone. A
 * value outside the range pins the thumb to the end and is left as it is until someone moves it.
 * <p>
 * Letting go of a slider, Enter, or leaving the field keeps a change as an edit not applied; Esc puts the number back.
 * Nothing here writes a file (storage.md rule 6).
 */
class SettingsModePanel extends JPanel {

    private static final int NAME_WIDTH = 340;
    private static final int FIELD_COLUMNS = 10;
    private static final Dimension SLIDER_SIZE = new Dimension(300, HUD_SLIDER_HEIGHT);

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
        row.showRangeNote(setting);
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
     * One setting: its name, its slider when its range is known, its number and what to say about that number.
     */
    private final class Row {
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

        void showValue(BindsValues.Setting setting) {
            field.setEnabled(setting.editable());
            field.setToolTipText(setting.editable() ? null : getText("bindings.settings.notEditable"));
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
            slider.setEnabled(setting.editable());
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
