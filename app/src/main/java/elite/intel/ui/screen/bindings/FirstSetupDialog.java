package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devicefiles.DeviceDivergence;
import elite.intel.bindforge.devicefiles.FirstSetupPlan;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Option;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Row;
import elite.intel.bindforge.devicefiles.LabelMerge;
import elite.intel.bindforge.storage.ConfigurationFiles;
import elite.intel.bindforge.storage.FirstSetupFiles;
import elite.intel.bindforge.storage.FirstSetupReading;
import elite.intel.bindforge.storage.InstallationFiles;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudTable;
import elite.intel.ui.widget.StatusBadge;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeButton;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;
import static elite.intel.ui.theme.AppTheme.transparentPanel;

/**
 * First-time startup's reconcile, when the installations' device files differ (storage.md rule 2; alias-designer.md,
 * <em>The flow</em>): what the installations hold, one row per device, and the two routes through - <strong>use one
 * installation for all</strong>, its files whole, or <strong>merge</strong> and answer the few questions. Colliding
 * labels are answered per input in {@link LabelMergeDialog}, or for the whole device by one installation.
 * <p>
 * <strong>Nothing is written here.</strong> The dialog ends with a summary the user confirms, and hands back the
 * working copy to write; the caller writes it.
 * <p>
 * RESTORE reconciles a backup's installations the same way (storage.md rule 10), in its own {@link Wording}: there the
 * summary is RESTORE's one confirmation (Alan, 2026-10-25).
 */
final class FirstSetupDialog extends JDialog {

    private static final Logger log = LogManager.getLogger(FirstSetupDialog.class);
    private static final int MAX_COLUMN_WIDTH = 360;

    /**
     * The working copy the user confirmed.
     *
     * @param files the working copy to write
     * @param moved how many bindings a merge moved off a losing name; 0 for "use one installation for all"
     */
    record Choice(ConfigurationFiles files, int moved) {
    }

    /**
     * What the dialog says, as text already in the user's language.
     *
     * @param title        the dialog's title
     * @param intro        what the user is asked to do
     * @param backupBefore the line saying what is backed up first
     * @param summaryTitle the summary's title
     * @param confirm      the summary's button
     * @param trailer      the summary's last paragraph - where it is written
     */
    record Wording(String title, String intro, String backupBefore, String summaryTitle, String confirm,
                   String trailer) {

        /** First-time startup's words. */
        static Wording firstSetup(int installations) {
            return new Wording(getText("bindings.firstTimeStartup.title"),
                    getText("bindings.firstTimeStartup.reconcile.intro"),
                    getText("bindings.aliasDesigner.firstSetup.backupBefore"),
                    getText("bindings.firstTimeStartup.summary.title"),
                    getText("bindings.firstTimeStartup.summary.create"),
                    getText("bindings.firstTimeStartup.summary.installations", installations));
        }
    }

    private final FirstSetupReading reading;
    private final Map<String, String> installNames;
    private final Wording wording;
    private final List<Row> rows;
    private final Map<String, String> answers = new LinkedHashMap<>();

    private DefaultTableModel model;
    private JTable table;
    private JPanel choicePanel;
    private Choice choice;

    /** @param installNames what each installation is called, keyed by its id as text */
    FirstSetupDialog(Component parent, FirstSetupReading reading, Map<String, String> installNames) {
        this(parent, reading, installNames, Wording.firstSetup(reading.installations().size()));
    }

    /** @param installNames what each installation is called, keyed by its id as text */
    FirstSetupDialog(Component parent, FirstSetupReading reading, Map<String, String> installNames, Wording wording) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.reading = reading;
        this.installNames = installNames;
        this.wording = wording;
        this.rows = reading.plan().rows();
        buildUi();
    }

    /** @return the working copy the user confirmed, or empty when they cancelled */
    Optional<Choice> showDialog() {
        AppTheme.runWithModalScrim(getOwner(), () -> setVisible(true));
        return Optional.ofNullable(choice);
    }

    private void buildUi() {
        JPanel body = transparentPanel(new BorderLayout(0, 8));
        JPanel top = transparentPanel(new GridLayout(0, 1, 0, 4));
        // WHY: said first, because it is what makes the rest safe to touch.
        top.add(HudBanner.multiline(wording.backupBefore(), StatusBadge.State.INFO));
        top.add(HudBanner.multiline(wording.intro(), StatusBadge.State.IDLE));
        if (!reading.unreachable().isEmpty()) {
            top.add(HudBanner.multiline(getText("bindings.aliasDesigner.firstSetup.unreachable",
                    reading.unreachable().stream().map(row -> nameOf(row.id(), row.storefront()))
                            .collect(Collectors.joining(", "))),
                    StatusBadge.State.STANDBY));
        }
        body.add(top, BorderLayout.NORTH);

        JButton merge = makeButton(getText("bindings.firstTimeStartup.reconcile.merge"));
        merge.addActionListener(e -> onMerge());
        JButton cancel = makeButtonSubtle(getText("bindings.aliasDesigner.firstSetup.cancel"));
        cancel.addActionListener(e -> dispose());

        JPanel bottom = transparentPanel(new GridLayout(0, 1, 0, 6));
        if (reading.plan().isEmpty()) {
            body.add(HudBanner.multiline(getText("bindings.aliasDesigner.firstSetup.nothing"),
                    StatusBadge.State.IDLE), BorderLayout.CENTER);
        } else {
            body.add(HudTable.dataPlaneScrollPane(buildTable()), BorderLayout.CENTER);
            choicePanel = transparentPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            bottom.add(choicePanel);
        }
        bottom.add(useOneButtons());
        body.add(bottom, BorderLayout.SOUTH);

        HudModalSpec spec = HudModalSpec.builder()
                .title(wording.title())
                .onClose(this::dispose)
                .body(body)
                .scrollBody(false)
                .primary(merge)
                .dismiss(cancel)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setSize(Math.max(getWidth(), 820), Math.max(getHeight(), 520));
        setLocationRelativeTo(getOwner());
        showChoices();
    }

    private JTable buildTable() {
        model = new DefaultTableModel(new Object[]{
                getText("bindings.aliasDesigner.firstSetup.column.device"),
                getText("bindings.aliasDesigner.firstSetup.column.found"),
                getText("bindings.aliasDesigner.firstSetup.column.installations"),
                getText("bindings.aliasDesigner.firstSetup.column.answer")}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        HudTable.style(table);
        // WHY: severity is the row's text colour, never a fill or a pill - the HUD canon's rule for state.
        table.setDefaultRenderer(Object.class, new SeverityRenderer());
        table.putClientProperty(AppTheme.HUD_TABLE_STYLE_LOCKED, Boolean.TRUE);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showChoices();
        });
        refreshRows();
        return table;
    }

    private void refreshRows() {
        int selected = table.getSelectedRow();
        model.setRowCount(0);
        for (Row row : rows) {
            model.addRow(new Object[]{row.deviceName(), getText(kindKey(row.kind())),
                    row.presentIn().stream().map(id -> installNames.getOrDefault(String.valueOf(id),
                            String.valueOf(id))).sorted().collect(Collectors.joining(", ")),
                    answerText(row)});
        }
        HudTable.fitColumnsToContent(table, MAX_COLUMN_WIDTH);
        if (selected >= 0 && selected < rows.size()) table.setRowSelectionInterval(selected, selected);
    }

    private String answerText(Row row) {
        if (!row.isQuestion()) return "";
        if (row.kind() == FirstSetupPlan.Kind.LABELS_DIFFER) return labelsAnswerText(row.deviceName());
        if (!reading.plan().unanswered(answers).contains(row.id())) {
            String key = answers.get(row.id());
            if (key == null) return getText("bindings.aliasDesigner.firstSetup.answer.notNeeded");
            return row.options().stream().filter(option -> option.key().equals(key))
                    .map(this::optionText).findFirst().orElse(key);
        }
        return getText("bindings.aliasDesigner.firstSetup.answer.choose");
    }

    /** How many of the device's colliding inputs are answered - or not needed, when the device lost its name. */
    private String labelsAnswerText(String device) {
        FirstSetupPlan plan = reading.plan();
        LabelMerge merge = plan.labelMerge(device);
        int total = merge.collisions().size();
        int left = merge.unanswered(plan.labelAnswers(device, answers)).size();
        boolean asked = merge.collisions().stream()
                .anyMatch(collision -> plan.unanswered(answers)
                        .contains(FirstSetupPlan.labelAnswerKey(device, collision.input())));
        if (left > 0 && !asked) return getText("bindings.aliasDesigner.firstSetup.answer.notNeeded");
        return getText("bindings.aliasDesigner.labelMerge.chosen", total - left, total);
    }

    /** The answers for the selected row, one button each. */
    private void showChoices() {
        if (choicePanel == null) return;
        choicePanel.removeAll();
        int index = table == null ? -1 : table.getSelectedRow();
        if (index >= 0 && rows.get(index).isQuestion()) {
            Row row = rows.get(index);
            choicePanel.add(AppTheme.hudReadoutValue(
                    getText("bindings.aliasDesigner.firstSetup.chooseFor", row.deviceName()),
                    HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT));
            if (row.kind() == FirstSetupPlan.Kind.LABELS_DIFFER) {
                addLabelChoices(row);
            } else {
                addOptionChoices(row);
            }
        }
        choicePanel.revalidate();
        choicePanel.repaint();
    }

    private void addOptionChoices(Row row) {
        for (Option option : row.options()) {
            JButton button = makeButtonSubtle(optionText(option));
            button.addActionListener(e -> {
                answers.put(row.id(), option.key());
                refreshRows();
            });
            choicePanel.add(button);
        }
    }

    /**
     * The merge view for the device, and the whole-file route beside it - <em>use Steam's labels for this
     * device</em> - for a user who would rather not walk the list.
     */
    private void addLabelChoices(Row row) {
        String device = row.deviceName();
        FirstSetupPlan plan = reading.plan();
        JButton compare = makeButtonSubtle(getText("bindings.aliasDesigner.labelMerge.compare",
                plan.labelMerge(device).collisions().size()));
        compare.addActionListener(e -> new LabelMergeDialog(this, device, plan.labelMerge(device), installNames,
                plan.labelAnswers(device, answers)).showDialog().ifPresent(chosen -> {
                    chosen.forEach((input, label) -> answers.put(FirstSetupPlan.labelAnswerKey(device, input), label));
                    refreshRows();
                }));
        choicePanel.add(compare);
        for (Option option : row.options()) {
            JButton button = makeButtonSubtle(getText("bindings.aliasDesigner.labelMerge.useLabels",
                    optionText(option)));
            button.addActionListener(e -> {
                answers.putAll(plan.labelAnswersFavouring(device, Long.parseLong(option.key())));
                refreshRows();
            });
            choicePanel.add(button);
        }
    }

    private String optionText(Option option) {
        // WHY: a labels question is answered by an installation, and its option text is only the storefront -
        // which is ambiguous on a machine holding two of one. The screen's own name for it is not.
        if (option.installs().size() == 1 && option.key().equals(String.valueOf(option.installs().iterator().next()))) {
            return installNames.getOrDefault(option.key(), option.text());
        }
        String holders = option.installs().stream()
                .map(id -> installNames.getOrDefault(String.valueOf(id), String.valueOf(id)))
                .sorted().collect(Collectors.joining(", "));
        return option.text() + " (" + holders + ")";
    }

    /**
     * "Use one installation for all", one button per installation - not offered for one with no
     * {@code DeviceMappings.xml}, since the working copy would then hold none (Alan, 2026-10-25).
     */
    private JPanel useOneButtons() {
        JPanel panel = transparentPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (InstallationFiles installation : reading.installations()) {
            if (!installation.hasDeviceMappings()) continue;
            String name = nameOf(installation.installId(), installation.storefront());
            JButton button = makeButtonSubtle(getText("bindings.firstTimeStartup.reconcile.useOne", name));
            button.addActionListener(e -> onUseOne(installation, name));
            panel.add(button);
        }
        return panel;
    }

    /**
     * Its confirmation says, before anything is written, which entries the bindings use that it would remove - the
     * binds files and the counts (alias-designer.md, <em>Reconciliation is per device element</em>).
     */
    private void onUseOne(InstallationFiles installation, String name) {
        StringBuilder text = new StringBuilder(getText("bindings.firstTimeStartup.reconcile.useOne.confirm", name));
        for (FirstSetupFiles.Loss loss : FirstSetupFiles.lossesUsingOne(reading, installation.installId())) {
            String files = loss.bindings().entrySet().stream()
                    .map(file -> getText("bindings.firstTimeStartup.reconcile.useOne.inFile", file.getValue(),
                            file.getKey()))
                    .collect(Collectors.joining(", "));
            text.append("\n\n").append(getText("bindings.firstTimeStartup.reconcile.useOne.loss",
                    String.join(", ", loss.holders()), name, loss.name(), files));
        }
        text.append("\n\n").append(wording.trailer());
        if (!HudConfirmDialog.confirm(this, wording.summaryTitle(), text.toString(), wording.confirm(),
                getText("bindings.aliasDesigner.firstSetup.cancel"))) {
            return;
        }
        choice = new Choice(FirstSetupFiles.useOne(reading, installation.installId()), 0);
        dispose();
    }

    /** Merge: every question answered, then a summary of what the working copy will hold, then the choice. */
    private void onMerge() {
        List<String> missing = reading.plan().unanswered(answers);
        if (!missing.isEmpty()) {
            HudConfirmDialog.info(this, wording.title(),
                    getText("bindings.aliasDesigner.firstSetup.unanswered", missing.size()),
                    getText("bindings.aliasDesigner.firstSetup.close"));
            return;
        }
        FirstSetupFiles.Merged merged;
        try {
            merged = FirstSetupFiles.merge(reading, Map.copyOf(answers));
        } catch (IOException e) {
            // WHY: nothing was written - the working copy could not be made from these files, and the file is named.
            log.warn("First-time startup could not merge the installations", e);
            HudConfirmDialog.info(this, wording.title(),
                    getText("bindings.firstTimeStartup.mergeFailed", StringUtls.messageOf(e)),
                    getText("bindings.aliasDesigner.firstSetup.close"));
            return;
        }
        StringBuilder summary = new StringBuilder(getText("bindings.firstTimeStartup.summary.merge",
                merged.devices(), merged.files().buttonMaps().size()));
        merged.moves().forEach((from, to) -> summary.append('\n')
                .append(getText("bindings.aliasDesigner.firstSetup.summary.moves", from, to)));
        summary.append("\n\n").append(wording.trailer());
        if (!HudConfirmDialog.confirm(this, wording.summaryTitle(), summary.toString(), wording.confirm(),
                getText("bindings.aliasDesigner.firstSetup.cancel"))) {
            return;
        }
        choice = new Choice(merged.files(), merged.bindingsMoved());
        dispose();
    }

    private String nameOf(long installId, String storefront) {
        return installNames.getOrDefault(String.valueOf(installId), storefront);
    }

    private static String kindKey(FirstSetupPlan.Kind kind) {
        return switch (kind) {
            case IDENTICAL -> "bindings.aliasDesigner.firstSetup.kind.identical";
            case ONLY_IN_SOME -> "bindings.aliasDesigner.firstSetup.kind.onlyInSome";
            case HARDWARE_DIFFERS -> "bindings.aliasDesigner.firstSetup.kind.hardwareDiffers";
            case NAME_DIFFERS -> "bindings.aliasDesigner.firstSetup.kind.nameDiffers";
            case LABELS_DIFFER -> "bindings.aliasDesigner.firstSetup.kind.labelsDiffer";
            case BUILT_IN_LABELS -> "bindings.aliasDesigner.firstSetup.kind.builtInLabels";
            case SHADOWED -> "bindings.aliasDesigner.firstSetup.kind.shadowed";
        };
    }

    /** The row's severity as its text colour; a selected row keeps the selection colour. */
    private final class SeverityRenderer extends HudTable.CellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (!selected && row < rows.size()) cell.setForeground(colourOf(rows.get(row).severity()));
            return cell;
        }

        private Color colourOf(DeviceDivergence.Severity severity) {
            return switch (severity) {
                case RED -> HudPalette.HUD_COLOR_ROLE_DANGER;
                case YELLOW -> HudPalette.HUD_COLOR_ROLE_WARNING;
                case GREEN -> HudPalette.HUD_COLOR_ROLE_SUCCESS;
            };
        }
    }
}
