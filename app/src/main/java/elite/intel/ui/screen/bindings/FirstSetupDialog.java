package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devicefiles.DeviceDivergence;
import elite.intel.bindforge.devicefiles.DeviceFilesPush;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.bindforge.devicefiles.FirstSetup;
import elite.intel.bindforge.devicefiles.FirstSetupPlan;
import elite.intel.bindforge.devicefiles.LabelMerge;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Option;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Row;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudTable;
import elite.intel.ui.widget.StatusBadge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeButton;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;
import static elite.intel.ui.theme.AppTheme.transparentPanel;

/**
 * First setup: what the installations hold, one row per device, and the few questions that need an answer
 * (alias-designer.md, <em>The flow</em>).
 * <p>
 * Nothing is written until the summary is confirmed, so the dialog can be opened and abandoned. Two routes
 * through: answer the questions one by one, or use one installation as the master. Colliding labels are answered
 * per input in {@link LabelMergeDialog}, or for the whole device by one installation.
 */
final class FirstSetupDialog extends JDialog {

    private static final Logger log = LogManager.getLogger(FirstSetupDialog.class);
    private static final int MAX_COLUMN_WIDTH = 360;

    private final FirstSetup setup;
    private final FirstSetup.Reading reading;
    private final Map<String, String> installNames;
    private final Runnable afterApply;
    private final List<Row> rows;
    private final Map<String, String> answers = new LinkedHashMap<>();

    private DefaultTableModel model;
    private JTable table;
    private JPanel choicePanel;
    private JButton continueButton;

    /**
     * @param installNames what each installation is called, keyed by its id as text
     * @param afterApply   run on the EDT once something was written, to refresh the screen behind
     */
    FirstSetupDialog(Component parent, FirstSetup setup, FirstSetup.Reading reading,
                     Map<String, String> installNames, Runnable afterApply) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.setup = setup;
        this.reading = reading;
        this.installNames = installNames;
        this.afterApply = afterApply;
        this.rows = reading.plan().rows();
        buildUi();
    }

    void showDialog() {
        AppTheme.runWithModalScrim(getOwner(), () -> setVisible(true));
    }

    private void buildUi() {
        JPanel body = transparentPanel(new BorderLayout(0, 8));
        JPanel top = transparentPanel(new GridLayout(0, 1, 0, 4));
        // WHY: said first, because it is what makes the rest safe to touch.
        top.add(HudBanner.multiline(getText("bindings.aliasDesigner.firstSetup.backupBefore"),
                StatusBadge.State.INFO));
        if (!reading.unreachable().isEmpty()) {
            top.add(HudBanner.multiline(getText("bindings.aliasDesigner.firstSetup.unreachable",
                    reading.unreachable().stream().map(this::nameOf).collect(Collectors.joining(", "))),
                    StatusBadge.State.STANDBY));
        }
        body.add(top, BorderLayout.NORTH);

        continueButton = makeButton(getText("bindings.aliasDesigner.firstSetup.continue"));
        continueButton.addActionListener(e -> onContinue());
        JButton cancel = makeButtonSubtle(getText("bindings.aliasDesigner.firstSetup.cancel"));
        cancel.addActionListener(e -> dispose());

        if (reading.plan().isEmpty()) {
            body.add(HudBanner.multiline(getText("bindings.aliasDesigner.firstSetup.nothing"),
                    StatusBadge.State.IDLE), BorderLayout.CENTER);
            continueButton.setEnabled(false);
        } else {
            body.add(HudTable.dataPlaneScrollPane(buildTable()), BorderLayout.CENTER);
            JPanel bottom = transparentPanel(new GridLayout(0, 1, 0, 6));
            choicePanel = transparentPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            bottom.add(choicePanel);
            bottom.add(useAsMasterButtons());
            body.add(bottom, BorderLayout.SOUTH);
        }

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("bindings.aliasDesigner.firstSetup.title"))
                .onClose(this::dispose)
                .body(body)
                .scrollBody(false)
                .primary(continueButton)
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
            JButton button = makeButtonSubtle(option.available()
                    ? optionText(option)
                    : getText("bindings.aliasDesigner.firstSetup.option.needsRename", optionText(option)));
            button.setEnabled(option.available());
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

    private JPanel useAsMasterButtons() {
        JPanel panel = transparentPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (FirstSetupPlan.Installation installation : reading.plan().installations()) {
            String name = installNames.getOrDefault(String.valueOf(installation.installId()),
                    installation.storefront());
            JButton button = makeButtonSubtle(getText("bindings.aliasDesigner.firstSetup.useAsMaster", name));
            button.addActionListener(e -> {
                answers.putAll(reading.plan().answersFavouring(installation.installId()));
                refreshRows();
            });
            panel.add(button);
        }
        return panel;
    }

    private void onContinue() {
        List<String> missing = reading.plan().unanswered(answers);
        if (!missing.isEmpty()) {
            HudConfirmDialog.info(this, getText("bindings.aliasDesigner.firstSetup.title"),
                    getText("bindings.aliasDesigner.firstSetup.unanswered", missing.size()),
                    getText("bindings.aliasDesigner.firstSetup.close"));
            return;
        }
        FirstSetupPlan.Resolution resolution = reading.plan().resolve(answers);
        long reachable = reading.targets().size() - reading.unreachable().size();
        boolean go = HudConfirmDialog.confirm(this, getText("bindings.aliasDesigner.firstSetup.summary.title"),
                getText("bindings.aliasDesigner.firstSetup.summary.text",
                        resolution.devices().size(), resolution.buttonMapCount(), reachable),
                getText("bindings.aliasDesigner.firstSetup.summary.apply"),
                getText("bindings.aliasDesigner.firstSetup.cancel"));
        if (!go) return;

        continueButton.setEnabled(false);
        Map<String, String> chosen = Map.copyOf(answers);
        new Thread(() -> {
            FirstSetup.Result result;
            try {
                result = setup.apply(reading, chosen);
            } catch (RuntimeException e) {
                // WHY: broad on purpose - a thread boundary, and an escaping exception would leave the dialog
                // waiting with nothing said.
                log.error("First setup failed", e);
                result = FirstSetup.Result.failedBeforeWriting(e);
            }
            FirstSetup.Result finished = result;
            SwingUtilities.invokeLater(() -> showResult(finished));
        }, "BindForge-FirstSetup").start();
    }

    private void showResult(FirstSetup.Result result) {
        String message = switch (result.outcome()) {
            case APPLIED -> appliedText(result);
            case ALREADY_SET_UP -> getText("bindings.aliasDesigner.firstSetup.result.alreadySetUp");
            case BACKUP_FAILED -> getText("bindings.aliasDesigner.firstSetup.result.backupFailed", result.reason());
            case FAILED -> getText("bindings.aliasDesigner.firstSetup.result.failed", result.reason());
        };
        HudConfirmDialog.info(this, getText("bindings.aliasDesigner.firstSetup.title"), message,
                getText("bindings.aliasDesigner.firstSetup.close"));
        // WHY: closed whenever the master holds anything, whatever else failed. First setup is then over -
        // continuing again could only say so - and the screen behind must stop offering it.
        if (result.masterFilled()) {
            dispose();
            afterApply.run();
        } else {
            continueButton.setEnabled(true);
        }
    }

    private String appliedText(FirstSetup.Result result) {
        DeviceFilesPush.Report report = result.report();
        StringBuilder text = new StringBuilder(getText("bindings.aliasDesigner.firstSetup.result.applied",
                String.valueOf(result.backup()), report.matchingCount(), report.installations().size()));
        for (DeviceFilesPush.InstallationResult installation : report.installations()) {
            if (installation.reason() == null) continue;
            text.append('\n').append(getText("bindings.aliasDesigner.firstSetup.result.installation",
                    nameOf(installation.target()), installation.reason()));
        }
        if (!result.resolution().pendingRemovals().isEmpty()) {
            text.append('\n').append(getText("bindings.aliasDesigner.firstSetup.result.leftOnDisk",
                    String.join(", ", result.resolution().pendingRemovals())));
        }
        return text.toString();
    }

    private String nameOf(Target target) {
        return installNames.getOrDefault(String.valueOf(target.installId()), target.storefront());
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
