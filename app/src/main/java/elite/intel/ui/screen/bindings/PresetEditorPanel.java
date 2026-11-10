package elite.intel.ui.screen.bindings;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.io.FileNames;
import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.preseteditor.BindNameEntry;
import elite.intel.bindforge.preseteditor.BindNameList;
import elite.intel.bindforge.preseteditor.BindsFileOrigin;
import elite.intel.bindforge.preseteditor.NotOfferedFile;
import elite.intel.bindforge.storage.Apply;
import elite.intel.bindforge.storage.ConfigurationFiles;
import elite.intel.bindforge.storage.Edits;
import elite.intel.bindforge.storage.PresetEdits;
import elite.intel.bindforge.storage.ReadOnlyMode;
import elite.intel.bindforge.storage.WorkingCopy;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.BindForgeEditsChangedEvent;
import elite.intel.ui.event.BindForgeReadOnlyChangedEvent;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudPanel;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.StatusBadge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static elite.intel.ui.dialog.HudConfirmDialog.info;
import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.hudReadoutLabel;
import static elite.intel.ui.theme.AppTheme.hudSubtabContentBorder;
import static elite.intel.ui.theme.AppTheme.makeButton;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND;

/**
 * The <strong>Preset Editor</strong> tab of BindForge: which binds file the game loads for each of the four sections,
 * the four lines of the preset file (preset-editor.md; P4, 2026-11-10). A tab beside the Bind Editor, Binding Management
 * and Alias Designer (Alan, 2026-11-10).
 * <p>
 * Every change is an edit not applied, through {@link PresetEdits#change}: the one APPLY / DISCARD bar above the tabs
 * writes or drops it, and this tab has no buttons of its own for that. The dropdowns offer the BindName list APPLY
 * checks against ({@link Apply#offered(ConfigurationFiles)}), so a line chosen here is a line APPLY accepts.
 * <p>
 * <strong>A line naming nothing the list offers</strong> - empty, or a binds file that is gone - stays in its dropdown,
 * marked, so the user sees what the file says and can choose another; APPLY refuses it until they do.
 */
public final class PresetEditorPanel extends JPanel {

    private static final Logger log = LogManager.getLogger(PresetEditorPanel.class);
    private static final int DROPDOWN_WIDTH = 300;

    /** What a dropdown item is: a binds file of the user's, a factory one, or a line naming nothing offered. */
    enum Kind {USER, FACTORY, MISSING}

    /** One item of a dropdown: the BindName a line would hold, and what it is. */
    record Choice(String bindName, Kind kind) {
        Choice {
            Objects.requireNonNull(bindName, "bindName");
            Objects.requireNonNull(kind, "kind");
        }
    }

    /** What one read found: the four lines, and what they may name. */
    record Read(StartPresetSections sections, BindNameList list, List<String> unreachable) {
    }

    private final Map<BindingSection, HudComboBox<Choice>> dropdowns = new EnumMap<>(BindingSection.class);
    private final HudComboBox<Choice> setAllBox = dropdown();
    private final JButton setAllButton = makeButton(getText("bindings.presetEditor.setAll"));
    /** The notices above the four rows: lines naming nothing, the sections disagreeing. */
    private final JPanel topNotices = noticeColumn();
    /** The notices below: installations not reached, files not offered. */
    private final JPanel bottomNotices = noticeColumn();
    private final RefreshCoalescer refreshes = new RefreshCoalescer();

    /** The four lines as last read, or {@code null} before a read or when there is nothing to read. EDT only. */
    private StartPresetSections shown;
    /** The dropdowns are being filled, so their selection events are not the user's. EDT only. */
    private boolean filling;
    /** A change is running; the dropdowns wait for it. EDT only. */
    private boolean changing;
    /** False while CREATE has the editors greyed. EDT only. */
    private boolean editorsUsable = true;

    public PresetEditorPanel() {
        setLayout(new BorderLayout());
        setBorder(hudSubtabContentBorder());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        add(buildSection(), BorderLayout.NORTH);
        updateEnabled();
        // WHY: an installation can come back or go between visits, and its factory binds files with it.
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !isShowing()) return;
            refresh();
        });
        UiBus.register(this);
    }

    public void initData() {
        refresh();
    }

    public void dispose() {
        UiBus.unregister(this);
    }

    /** CREATE greyed the editors, or brought them back. On the EDT. */
    public void setEditorsUsable(boolean usable) {
        editorsUsable = usable;
        updateEnabled();
    }

    /** DISCARD, APPLY, RESTORE, CREATE or an edit - the preset file may have changed. */
    @Subscribe
    public void onEditsChanged(BindForgeEditsChangedEvent event) {
        SwingUtilities.invokeLater(this::refresh);
    }

    /** The dropdowns are locked while BindForge is read only; the lines stay shown (Storage S14a). */
    @Subscribe
    public void onReadOnlyChanged(BindForgeReadOnlyChangedEvent event) {
        SwingUtilities.invokeLater(this::updateEnabled);
    }

    // -- Layout ---------------------------------------------------------------

    private JComponent buildSection() {
        HudSection section = new HudSection(getText("bindings.presetEditor.title"), new GridBagLayout(),
                HudPanel.Variant.FLAT, 6);
        JPanel body = section.body();
        int row = 0;
        body.add(HudBanner.multiline(getText("bindings.presetEditor.intro"), StatusBadge.State.INFO),
                spanning(row++));
        body.add(topNotices, spanning(row++));
        for (BindingSection bindingSection : PresetEdits.SECTIONS) {
            HudComboBox<Choice> box = dropdown();
            box.addActionListener(e -> chosen(bindingSection, box));
            dropdowns.put(bindingSection, box);
            addRow(body, row++, hudReadoutLabel(getText(bindingSection.getLabelKey())), box);
        }
        setAllButton.setToolTipText(getText("bindings.presetEditor.setAll.tooltip"));
        setAllButton.addActionListener(e -> setAll());
        JPanel setAllRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setAllRow.setOpaque(false);
        setAllRow.add(setAllBox);
        setAllRow.add(Box.createHorizontalStrut(10));
        setAllRow.add(setAllButton);
        GridBagConstraints c = spanning(row++);
        c.insets = new Insets(12, 0, 2, 0);
        body.add(setAllRow, c);
        body.add(bottomNotices, spanning(row));
        return section;
    }

    private static HudComboBox<Choice> dropdown() {
        HudComboBox<Choice> box = new HudComboBox<>(new DefaultComboBoxModel<>(), PresetEditorPanel::label,
                choice -> choice.kind() != Kind.USER);
        box.setPreferredSize(new Dimension(DROPDOWN_WIDTH, box.getPreferredSize().height));
        return box;
    }

    private static void addRow(JPanel body, int row, JComponent label, JComponent control) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(4, 0, 4, 8);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.weightx = 1;
        body.add(label, c);
        c.gridx = 1;
        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        body.add(control, c);
    }

    private static GridBagConstraints spanning(int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 2, 0);
        return c;
    }

    private static JPanel noticeColumn() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        return column;
    }

    // -- Reading --------------------------------------------------------------

    /**
     * Reads the four lines and the BindName list off the EDT, then shows them. A read in flight is left to finish and
     * one more follows. Before first-time startup there is no working copy: nothing shown, nothing logged.
     */
    void refresh() {
        if (!refreshes.tryStart()) return;
        Function<String, Runnable> nothing = reason -> () -> show(null);
        FlowSteps.inBackground("BindForge-PresetEditor", log, "The Preset Editor's read", () -> {
            ConfigurationFiles current;
            try {
                current = Edits.stored().snapshot().current();
            } catch (WorkingCopy.NoWorkingCopyException e) {
                return () -> show(null);
            }
            StartPresetSections sections = PresetEdits.sections(current).orElse(EMPTY);
            Apply.Offered offered = Apply.offered(current);
            List<String> unreachable = offered.unreachable().stream()
                    .map(row -> getText("bindings.firstTimeStartup.place.installation", row.storefront(),
                            row.rootPath()))
                    .toList();
            Read read = new Read(sections, offered.list(), unreachable);
            return () -> show(read);
        }, nothing, nothing, FlowSteps.NOTHING);
    }

    private static final StartPresetSections EMPTY = new StartPresetSections("", "", "", "", false);

    /** Shows what {@link #refresh} read - {@code null} for nothing - then the read asked for while it ran. On the EDT. */
    private void show(Read read) {
        shown = read == null ? null : read.sections();
        filling = true;
        try {
            for (BindingSection section : PresetEdits.SECTIONS) {
                HudComboBox<Choice> box = dropdowns.get(section);
                String value = read == null ? "" : read.sections().forSection(section);
                List<Choice> choices = read == null ? List.of() : choices(read.list(), value);
                box.setModel(new DefaultComboBoxModel<>(choices.toArray(Choice[]::new)));
                box.setSelectedItem(read == null ? null : selected(choices, value));
                box.setToolTipText(read != null && unnamed(read.list(), value)
                        ? getText("bindings.presetEditor.missing.tooltip") : null);
            }
            List<Choice> all = read == null ? List.of() : choices(read.list(), null);
            setAllBox.setModel(new DefaultComboBoxModel<>(all.toArray(Choice[]::new)));
            setAllBox.setSelectedItem(read == null ? null : selected(all, read.sections().general()));
        } finally {
            filling = false;
        }
        showNotices(read);
        updateEnabled();
        if (refreshes.finish()) refresh();
    }

    private void showNotices(Read read) {
        topNotices.removeAll();
        bottomNotices.removeAll();
        if (read != null) {
            List<BindingSection> unnamed = unnamedSections(read.sections(), read.list());
            if (!unnamed.isEmpty()) {
                addNotice(topNotices, getText("bindings.presetEditor.missing", ApplyBar.sectionNames(unnamed)),
                        StatusBadge.State.OFFLINE);
            }
            if (split(read.sections())) {
                addNotice(topNotices, getText("bindings.presetEditor.split"), StatusBadge.State.INFO);
            }
            if (!read.unreachable().isEmpty()) {
                addNotice(bottomNotices, getText("bindings.presetEditor.unreachable",
                        String.join(", ", read.unreachable())), StatusBadge.State.STANDBY);
            }
            for (NotOfferedFile file : read.list().notOffered()) {
                addNotice(bottomNotices, notOfferedText(file), StatusBadge.State.INFO);
            }
        }
        revalidate();
        repaint();
    }

    private static void addNotice(JPanel column, String text, StatusBadge.State state) {
        if (column.getComponentCount() > 0) column.add(Box.createVerticalStrut(4));
        HudBanner banner = HudBanner.multiline(text, state);
        banner.setAlignmentX(LEFT_ALIGNMENT);
        column.add(banner);
    }

    private void updateEnabled() {
        boolean readOnly = ReadOnlyMode.stored().activeIfKnown();
        boolean usable = editorsUsable && shown != null && !changing && !readOnly;
        String why = readOnly ? getText("bindings.readOnly.tooltip") : null;
        for (HudComboBox<Choice> box : dropdowns.values()) {
            box.setEnabled(usable);
            if (readOnly) box.setToolTipText(why);
        }
        boolean anyToSet = setAllBox.getItemCount() > 0;
        setAllBox.setEnabled(usable && anyToSet);
        setAllButton.setEnabled(usable && anyToSet);
        setAllButton.setToolTipText(readOnly ? why : getText("bindings.presetEditor.setAll.tooltip"));
    }

    // -- Changing -------------------------------------------------------------

    /** A dropdown changed. Only the user's choice of another BindName is an edit. On the EDT. */
    private void chosen(BindingSection section, HudComboBox<Choice> box) {
        if (filling || shown == null) return;
        Choice choice = (Choice) box.getSelectedItem();
        if (choice == null || choice.kind() == Kind.MISSING) return;
        if (choice.bindName().equals(shown.forSection(section))) return;
        change(with(shown, section, choice.bindName()));
    }

    /** SET ALL TO SAME: the BindName in its dropdown on all four lines. On the EDT. */
    private void setAll() {
        Choice choice = (Choice) setAllBox.getSelectedItem();
        if (choice == null || shown == null) return;
        change(allTo(choice.bindName()));
    }

    /**
     * {@code sections} as an edit not applied, off the EDT. A refusal - a write running, or read only - is said, and
     * the dropdowns go back to what the edits hold.
     */
    private void change(StartPresetSections sections) {
        if (changing) return;
        changing = true;
        updateEnabled();
        String title = getText("bindings.tab.presetEditor");
        FlowSteps.inBackground("BindForge-PresetEditorChange", log, "The Preset Editor's change", () -> {
                    try {
                        Edits.stored().change((current, applied) -> PresetEdits.change(current, applied, sections));
                    } catch (Edits.RefusedException e) {
                        log.info("The preset file's change not made: {}", e.getMessage());
                        return () -> changeEnded(title, getText(e.messageKey()));
                    }
                    return () -> changeEnded(title, null);
                }, reason -> () -> changeEnded(title, getText("bindings.change.failed")),
                reason -> () -> changeEnded(title, getText("bindings.change.stopped", reason)),
                FlowSteps.NOTHING);
    }

    private void changeEnded(String title, String failure) {
        changing = false;
        // WHY: a refused change leaves a dropdown showing a choice the edits do not hold - read them again.
        refresh();
        if (failure != null) info(this, title, failure, getText("button.close"));
    }

    // -- Pure rules, tested ---------------------------------------------------

    /**
     * The items of a dropdown: every BindName the list offers, in its order, and first - when {@code value} names
     * nothing it offers, an empty line included - the value itself, marked. {@code null} for the list alone.
     */
    static List<Choice> choices(BindNameList list, String value) {
        List<Choice> choices = new ArrayList<>();
        if (value != null && unnamed(list, value)) choices.add(new Choice(value, Kind.MISSING));
        for (BindNameEntry entry : list.entries()) {
            choices.add(new Choice(entry.bindName(),
                    entry.origin() == BindsFileOrigin.FACTORY ? Kind.FACTORY : Kind.USER));
        }
        return choices;
    }

    /** The item showing {@code value}: the one naming the same file, as the game matches names - ignoring case. */
    static Choice selected(List<Choice> choices, String value) {
        if (value == null) return null;
        return choices.stream().filter(choice -> choice.kind() == Kind.MISSING && choice.bindName().equals(value))
                .findFirst()
                .orElseGet(() -> choices.stream()
                        .filter(choice -> FileNames.sameFile(choice.bindName(), value))
                        .findFirst().orElse(null));
    }

    /** {@code value} is empty or names no binds file the list offers - what APPLY refuses. */
    static boolean unnamed(BindNameList list, String value) {
        return value.isEmpty() || list.find(value).isEmpty();
    }

    /** The sections whose line APPLY would refuse, in the file's order. */
    static List<BindingSection> unnamedSections(StartPresetSections sections, BindNameList list) {
        return PresetEdits.SECTIONS.stream().filter(section -> unnamed(list, sections.forSection(section))).toList();
    }

    /** The four lines do not all name one file - the consistency warning (preset-editor.md). Never blocking. */
    static boolean split(StartPresetSections sections) {
        String general = sections.general();
        return PresetEdits.SECTIONS.stream().anyMatch(section -> !FileNames.sameFile(general,
                sections.forSection(section)));
    }

    /** {@code sections} with one line changed - always the four-line form, which APPLY writes. */
    static StartPresetSections with(StartPresetSections sections, BindingSection changed, String bindName) {
        Map<BindingSection, String> values = PresetEdits.SECTIONS.stream().collect(Collectors.toMap(
                Function.identity(), section -> section == changed ? bindName : sections.forSection(section)));
        return new StartPresetSections(values.get(BindingSection.GENERAL), values.get(BindingSection.SHIP),
                values.get(BindingSection.SRV), values.get(BindingSection.ON_FOOT), false);
    }

    /** All four lines naming {@code bindName} - SET ALL TO SAME. */
    static StartPresetSections allTo(String bindName) {
        return new StartPresetSections(bindName, bindName, bindName, bindName, false);
    }

    /** A dropdown item as shown: the BindName, marked when factory or naming nothing. */
    static String label(Choice choice) {
        return switch (choice.kind()) {
            case USER -> choice.bindName();
            case FACTORY -> getText("bindings.presetEditor.factory", choice.bindName());
            case MISSING -> choice.bindName().isEmpty()
                    ? getText("bindings.presetEditor.empty")
                    : getText("bindings.presetEditor.notFound", choice.bindName());
        };
    }

    /** Why a binds file of the user's is not offered, as a sentence naming it (preset-editor.md, the BindName list). */
    static String notOfferedText(NotOfferedFile file) {
        String name = file.file().getFileName().toString();
        String why = switch (file.reason()) {
            case PRESET_FILE -> getText("bindings.presetEditor.notOffered.presetFile");
            case OTHER_XML -> getText("bindings.presetEditor.notOffered.otherXml", file.detail());
            case ROOT_WITHOUT_BINDINGS -> getText("bindings.presetEditor.notOffered.noBindings");
            case NO_PRESET_NAME -> getText("bindings.presetEditor.notOffered.noPresetName");
            case NAME_MISMATCH -> getText("bindings.presetEditor.notOffered.nameMismatch", file.detail());
            case NOT_XML -> getText("bindings.presetEditor.notOffered.notXml");
            case EMPTY -> getText("bindings.presetEditor.notOffered.empty");
            case UNREADABLE -> getText("bindings.presetEditor.notOffered.unreadable");
        };
        return getText("bindings.presetEditor.notOffered", name, why);
    }
}
