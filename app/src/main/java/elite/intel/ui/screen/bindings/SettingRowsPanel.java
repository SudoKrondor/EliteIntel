package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.io.BindingSaveResult;
import elite.intel.bindforge.io.BindingsWriter;
import elite.intel.bindforge.io.BindsValues;
import elite.intel.bindforge.model.BindingDisplayNames;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.storage.BindsEdits;
import elite.intel.bindforge.storage.Edits;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.StatusBadge;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * What the Bind Editor's two lists of settings share - Dead Zones &amp; Sensitivity and Toggles (bind-editor.md,
 * <em>Settings</em> and <em>Toggles</em>): the list under its hint, grouped by the game's sections and groups,
 * narrowed by the shell's search, greyed while read only, and a change kept as an edit not applied. Each tab supplies
 * only its settings, its words and its row (E10R, from the E10 / S19 review's D1, 2026-11-11).
 * <p>
 * Nothing here writes a file (storage.md rule 6).
 */
abstract class SettingRowsPanel<R extends SettingRowsPanel.SettingRow> extends JPanel {

    /**
     * One setting's row, as a tab draws it.
     */
    interface SettingRow {
        String id();

        JComponent panel();

        /**
         * Shows what the file holds, greyed when it cannot change. On the EDT.
         */
        void showValue(BindsValues.Setting setting);
    }

    /**
     * Keeps one setting's new value as an edit not applied, and says how the edit took it. Off the EDT.
     */
    @FunctionalInterface
    interface SettingWrite {
        BindingSaveResult write(String file, String setting, String value) throws IOException;
    }

    /**
     * The write every tab uses but in a test: through the edits, never a file.
     */
    static final SettingWrite THROUGH_EDITS = SettingRowsPanel::writeThroughEdits;

    private static final BindingsWriter WRITER = new BindingsWriter();

    private final GameModePanel.EditRunner editRunner;
    /** Whether BindForge is read only, an installation missing (Storage S14b). Asked on the EDT. */
    private final BooleanSupplier readOnly;
    private final SettingWrite write;
    private final Set<String> listed;
    private final String noneKey;
    private final String titleKey;
    private final JPanel list = transparentPanel(null);
    private final Map<String, R> rows = new LinkedHashMap<>();

    private String file;
    /** What the last read found, by setting. Read by the rows. */
    protected Map<String, BindsValues.Setting> settings = Map.of();
    private String query = "";

    /**
     * @param listed   the settings this tab lists, when the file holds them
     * @param hintKey  the banner above the list
     * @param noneKey  what the list says when nothing is shown
     * @param titleKey the tab's name, which an edit is titled by
     */
    SettingRowsPanel(GameModePanel.EditRunner editRunner, BooleanSupplier readOnly, SettingWrite write,
                     Set<String> listed, String hintKey, String noneKey, String titleKey) {
        this.editRunner = editRunner;
        this.readOnly = readOnly;
        this.write = write;
        this.listed = listed;
        this.noneKey = noneKey;
        this.titleKey = titleKey;
        setOpaque(false);
        setLayout(new BorderLayout(0, 4));
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(2, 6, 2, 6));
        add(HudBanner.multiline(getText(hintKey), StatusBadge.State.INFO), BorderLayout.NORTH);
        JPanel top = transparentPanel(new BorderLayout());
        top.add(list, BorderLayout.NORTH);
        JScrollPane scroll = hudApplicationScrollPane(top);
        scroll.getVerticalScrollBar().setUnitIncrement(HUD_FIELD_HEIGHT);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(hudDataPlaneBorder());
        add(scroll, BorderLayout.CENTER);
    }

    /**
     * A row for {@code id}, under the game's {@code name} for it.
     */
    protected abstract R newRow(String id, String name);

    /**
     * Shows what the shell's read found. On the EDT.
     * <p>
     * WHY it does not always rebuild: a read also follows an edit anywhere else in BindForge, and a rebuild would take
     * a number out from under someone typing it, and the list away from where it was scrolled.
     */
    void show(String file, BindsValues values) {
        boolean sameRows = Objects.equals(this.file, file) && settings.keySet().equals(values.settings().keySet());
        this.file = file;
        settings = values.settings();
        if (!sameRows) {
            rebuild();
            return;
        }
        rows.values().forEach(row -> row.showValue(settings.get(row.id())));
    }

    /**
     * BindForge went read only, or stopped being (Storage S14b): every row follows. On the EDT.
     */
    void readOnlyChanged() {
        rows.values().forEach(row -> row.showValue(settings.get(row.id())));
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
    protected final boolean changeable(BindsValues.Setting setting) {
        return setting.editable() && !readOnly.getAsBoolean();
    }

    /**
     * Keeps {@code written} as an edit of the row's setting, off the EDT through the shell's runner. While another edit
     * runs, and after a change that failed, the row is put back to what the file holds; {@code refused} runs on the EDT
     * when the edit could not take the value.
     */
    protected final void submit(R row, String written, Runnable refused) {
        String id = row.id();
        if (editRunner.running()) {
            row.showValue(settings.get(id));
            return;
        }
        String target = file;
        AtomicReference<BindingSaveResult> result = new AtomicReference<>();
        editRunner.run(getText(titleKey),
                () -> result.set(write.write(target, id, written)),
                () -> {
                    BindingSaveResult saved = result.get();
                    if (saved != BindingSaveResult.SAVED && saved != BindingSaveResult.NO_CHANGE) {
                        refused.run();
                    }
                },
                () -> row.showValue(settings.get(id)));
    }

    private static BindingSaveResult writeThroughEdits(String file, String setting, String value) throws IOException {
        AtomicReference<BindingSaveResult> result = new AtomicReference<>();
        Edits.stored().change(current -> {
            BindsEdits.Changed changed = BindsEdits.change(current, file,
                    List.of(binds -> WRITER.assignSetting(binds, setting, value)));
            result.set(changed.result());
            return changed.files();
        });
        return result.get();
    }

    private void rebuild() {
        rows.clear();
        list.removeAll();
        List<String> ids = shownIds();
        if (ids.isEmpty()) {
            list.add(hudReadoutLabel(getText(noneKey)));
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
            R row = newRow(id, control.name());
            row.showValue(settings.get(id));
            rows.put(id, row);
            list.add(row.panel());
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
     * Every listed setting the file holds, whatever its value (E10 Q3), in the game's order.
     * <p>
     * WHY the tie-break: every setting the name table does not place shares one order, and {@code Set.of} iterates
     * differently from one run to the next, so without it the <em>Other</em> rows would move between launches.
     */
    private List<String> shownIds() {
        if (file == null) return List.of();
        String typed = query.trim().toLowerCase();
        Set<String> present = settings.keySet();
        return listed.stream()
                .filter(present::contains)
                .filter(id -> typed.isEmpty() || BindingDisplayNames.searchText(id,
                        getText(BindingDisplayNames.lookup(id).section().getLabelKey())).contains(typed))
                .sorted(Comparator.comparingInt((String id) -> BindingDisplayNames.lookup(id).order())
                        .thenComparing(Comparator.naturalOrder()))
                .toList();
    }
}
