package elite.intel.ui.screen;

import elite.intel.ai.brain.actions.catalog.CommandCatalog;
import elite.intel.ai.brain.actions.catalog.CommandCatalogEntry;
import elite.intel.ai.brain.actions.customcommand.*;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ui.dialog.*;
import elite.intel.ui.render.HudCommandNameCellRenderer;
import elite.intel.ui.support.BindingSlotDisplayFormatter;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudSearchToolbar;
import elite.intel.ui.widget.HudTable;
import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Read-only customCommand catalog view backed by the loaded customCommand
 * registry.
 */
public class CustomCommandsTabPanel extends JPanel {

    private static final Logger log = LogManager.getLogger(CustomCommandsTabPanel.class);
    /**
     * How many timestamped pre-import backups to retain; older ones are pruned.
     */
    private static final int MAX_BACKUPS = 10;
    private static final DateTimeFormatter BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    private final CommandCatalog commandCatalog = new CommandCatalog();
    private final CustomCommandRepository customCommandRepository = new CustomCommandRepository();
    private List<CustomCommandRow> visibleRows = List.of();
    private JTextField searchField;
    private JTable table;
    private DefaultTableModel tableModel;

    public CustomCommandsTabPanel() {
        buildUi();
        initData();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> styleTable(table));
    }

    private void buildUi() {
        setLayout(new BorderLayout(0, 0));
        setBorder(AppTheme.hudSubtabContentBorder());
        setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

        tableModel = new ReadOnlyTableModel(columnNames(), 0);
        table = new JTable(tableModel);
        styleTable(table);
        installRowHover(table);
        table.addMouseListener(new CustomCommandTableMouseListener());
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("ENTER"), "openCustomCommandDetails");
        table.getActionMap().put("openCustomCommandDetails", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                openCustomCommandDetailsAtViewRow(table.getSelectedRow());
            }
        });

        JScrollPane scrollPane = HudTable.dataPlaneScrollPane(table);

        add(controlsToolbar(), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Builds the connected toolbar row: search field on the left, action buttons on
     * the right.
     */
    private JPanel controlsToolbar() {
        // Shared full-width filter bar (search box owns its TABLE_FILTER frame) with the
        // import/export/new buttons as trailing actions beside it, outside the frame. The shared
        // bar owns the field height so it matches the buttons and the other command tab.
        HudSearchToolbar toolbar = HudSearchToolbar.tableFilter(
                getText("actions.customCommands.search.placeholder"),
                getText("actions.commands.search.clearTooltip"),
                actionPanel());
        searchField = toolbar.textField();
        searchField.getDocument().addDocumentListener(new SearchDocumentListener());
        return toolbar;
    }

    public void initData() {
        tableModel.setRowCount(0);
        visibleRows = filteredRows();
        for (CustomCommandRow row : visibleRows) {
            tableModel.addRow(new Object[] {
                    new CustomCommandNameCell(row.entry().name(), row.entry().id()),
                    row.phrasesText()
            });
        }
    }

    private JPanel actionPanel() {
        JPanel panel = AppTheme.transparentPanel(new FlowLayout(FlowLayout.RIGHT, HudPalette.HUD_GAP, 0));
        panel.add(secondaryButton("actions.customCommands.action.restore", this::restoreFromBackup));
        panel.add(secondaryButton("actions.customCommands.action.import", this::importCustomCommands));
        panel.add(secondaryButton("actions.customCommands.action.export", this::exportCustomCommands));
        panel.add(primaryButton("actions.customCommands.action.new", this::newCustomCommand));
        return panel;
    }

    private JButton primaryButton(String key, Runnable action) {
        JButton btn = AppTheme.makeButton(getText(key));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private JButton secondaryButton(String key, Runnable action) {
        JButton btn = AppTheme.makeButtonSubtle(getText(key));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private String[] columnNames() {
        return new String[] {
                getText("actions.customCommands.column.name"),
                getText("actions.customCommands.column.phrases")
        };
    }

    private List<CustomCommandRow> sortedRows() {
        List<CustomCommandDefinition> customCommands = CustomCommandRegistry.getInstance().getCustomCommands();
        Map<String, CustomCommandDefinition> customCommandsById = customCommands.stream()
                .collect(Collectors.toMap(
                        CustomCommandDefinition::getActionKey,
                        Function.identity(),
                        (first, second) -> second,
                        LinkedHashMap::new));

        return commandCatalog.entries(customCommands).stream()
                .filter(CommandCatalogEntry::isCustomCommand)
                .map(entry -> new CustomCommandRow(entry, customCommandsById.get(entry.id())))
                .filter(row -> row.customCommand() != null)
                .sorted(Comparator
                        .comparing((CustomCommandRow row) -> row.entry().name(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(row -> row.entry().id()))
                .toList();
    }

    private List<CustomCommandRow> filteredRows() {
        String query = normalizedSearchText();
        return sortedRows().stream()
                .filter(row -> query.isBlank() || matchesSearch(row, query))
                .toList();
    }

    private boolean matchesSearch(CustomCommandRow row, String query) {
        return row.entry().name().toLowerCase(Locale.ROOT).contains(query)
                || row.entry().id().toLowerCase(Locale.ROOT).contains(query)
                || row.entry().description().toLowerCase(Locale.ROOT).contains(query)
                || row.phrasesText().toLowerCase(Locale.ROOT).contains(query);
    }

    private String normalizedSearchText() {
        if (searchField == null) {
            return "";
        }
        return searchField.getText().trim().toLowerCase(Locale.ROOT);
    }

    private void resetSearch() {
        searchField.setText("");
        initData();
    }

    private void openCustomCommandDetailsAtViewRow(int viewRow) {
        if (viewRow < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= visibleRows.size()) {
            return;
        }

        CustomCommandRow row = visibleRows.get(modelRow);
        AppTheme.runWithModalScrim(
                SwingUtilities.getWindowAncestor(this),
                () -> new CommandDetailsDialog(
                        this,
                        row.entry(),
                        row.phrases(),
                        false,
                        row.sequenceText(),
                        row.customCommand().getParameters(),
                        () -> editCustomCommand(row),
                        () -> deleteCustomCommand(row)).showDialog());
    }

    private void exportCustomCommands() {
        List<CustomCommandDefinition> commands = CustomCommandRegistry.getInstance().getCustomCommands();
        if (commands.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    getText("actions.customCommands.export.noSelection"),
                    getText("actions.customCommands.export.title"),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new CustomCommandExportDialog(this, commands).showDialog();
    }

    private void importCustomCommands() {
        importFrom(null);
    }

    /**
     * Restores a previously backed-up set: the import flow with the file picker landed in the backups folder.
     */
    private void restoreFromBackup() {
        Path startDir = null;
        try {
            startDir = AppPaths.getCustomCommandsBackupDir();
        } catch (IOException e) {
            log.warn("Could not resolve custom command backups folder for restore", e);
        }
        importFrom(startDir);
    }

    /**
     * Imports a custom command set, <em>replacing</em> the current one (not merging). The current set
     * is written to a durable timestamped backup first so a regretted import can be undone via
     * "Restore from backup". {@code startDir} optionally lands the file picker in a specific folder.
     */
    private void importFrom(Path startDir) {
        List<CustomCommandDefinition> existing = CustomCommandRegistry.getInstance().getCustomCommands();
        // Replace semantics: the chosen file becomes the whole set, so per-command conflicts against
        // the existing set are irrelevant - pass no existing-set for conflict classification.
        List<CustomCommandDefinition> toImport = CustomCommandImportDialog.showImportFlow(this, List.of(), startDir);
        if (toImport == null || toImport.isEmpty()) {
            return;
        }
        if (!confirmReplace(existing.size(), toImport.size())) {
            return;
        }
        Path backup = backupCurrentSet(existing);
        persistAndRefresh(toImport);
        showImportSuccess(toImport.size(), backup);
    }

    /**
     * Confirms a destructive replace, stating the counts. Skips the prompt when there is nothing to
     * lose (no existing commands).
     */
    private boolean confirmReplace(int currentCount, int importCount) {
        if (currentCount == 0) {
            return true;
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                getText("actions.customCommands.import.replaceConfirm", currentCount, importCount),
                getText("actions.customCommands.import.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.OK_OPTION;
    }

    /**
     * Writes a durable, timestamped copy of {@code current} to the backups folder and prunes old ones.
     * Returns the backup file, or {@code null} when there was nothing to back up or the write failed
     * (import still proceeds - the backup is best-effort safety, not a hard dependency).
     */
    private Path backupCurrentSet(List<CustomCommandDefinition> current) {
        if (current == null || current.isEmpty()) {
            return null;
        }
        try {
            Path dir = AppPaths.getCustomCommandsBackupDir();
            Path file = dir.resolve("custom_commands_" + LocalDateTime.now().format(BACKUP_STAMP) + ".json");
            Files.writeString(file, CustomCommandExportImportService.toJson(current), StandardCharsets.UTF_8);
            pruneBackups(dir);
            log.info("Backed up {} custom command(s) to {} before import", current.size(), file);
            return file;
        } catch (Exception e) {
            log.warn("Could not back up custom commands before import - proceeding anyway: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Keeps only the newest {@link #MAX_BACKUPS} backups (timestamped names sort chronologically).
     */
    private void pruneBackups(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            List<Path> backups = stream
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith("custom_commands_") && name.endsWith(".json");
                    })
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
            for (int i = 0; i < backups.size() - MAX_BACKUPS; i++) {
                Files.deleteIfExists(backups.get(i));
            }
        }
    }

    private void showImportSuccess(int count, Path backup) {
        if (backup == null) {
            JOptionPane.showMessageDialog(this,
                    getText("actions.customCommands.import.success", count),
                    getText("actions.customCommands.import.title"),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Object[] options = {
                getText("button.ok"),
                getText("actions.customCommands.backup.openFolder")
        };
        int choice = JOptionPane.showOptionDialog(
                this,
                getText("actions.customCommands.import.successBackup", count),
                getText("actions.customCommands.import.title"),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);
        if (choice == 1) {
            openBackupsFolder();
        }
    }

    private void openBackupsFolder() {
        try {
            Path dir = AppPaths.getCustomCommandsBackupDir();
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(dir.toFile());
            }
        } catch (Exception e) {
            log.warn("Could not open custom command backups folder", e);
        }
    }

    private void newCustomCommand() {
        CustomCommandDefinition saved = new CustomCommandEditorDialog(this, null,
                CustomCommandRegistry.getInstance().getCustomCommands()).showDialog();
        if (saved == null) {
            return;
        }
        List<CustomCommandDefinition> customCommands = new java.util.ArrayList<>(
                CustomCommandRegistry.getInstance().getCustomCommands());
        customCommands.add(saved);
        persistAndRefresh(customCommands);
    }

    private void editCustomCommand(CustomCommandRow row) {
        CustomCommandDefinition saved = new CustomCommandEditorDialog(this, row.customCommand(),
                CustomCommandRegistry.getInstance().getCustomCommands()).showDialog();
        if (saved == null) {
            return;
        }
        List<CustomCommandDefinition> customCommands = CustomCommandRegistry.getInstance().getCustomCommands().stream()
                .map(customCommand -> customCommand.getId().equalsIgnoreCase(row.customCommand().getId()) ? saved
                        : customCommand)
                .toList();
        persistAndRefresh(customCommands);
    }

    private void deleteCustomCommand(CustomCommandRow row) {
        int result = JOptionPane.showConfirmDialog(
                this,
                getText("actions.customCommands.delete.confirm", row.entry().name()),
                getText("actions.customCommands.delete.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        List<CustomCommandDefinition> customCommands = CustomCommandRegistry.getInstance().getCustomCommands().stream()
                .filter(customCommand -> !customCommand.getId().equalsIgnoreCase(row.customCommand().getId()))
                .toList();
        persistAndRefresh(customCommands);
    }

    private void persistAndRefresh(List<CustomCommandDefinition> customCommands) {
        if (!customCommandRepository.trySave(customCommands)) {
            JOptionPane.showMessageDialog(
                    this,
                    getText("actions.customCommands.save.error", AppPaths.CUSTOM_COMMANDS_FILE_NAME),
                    getText("actions.customCommands.title"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        CustomCommandRegistry.getInstance().replaceCustomCommands(customCommands);
        CommandHandlerFactory.getInstance().refreshCustomCommandHandlers();
        initData();
    }

    private void styleTable(JTable table) {
        HudTable.style(table);
        table.setRowHeight(48);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setDefaultRenderer(Object.class, new CellRenderer());
        table.putClientProperty(AppTheme.HUD_TABLE_STYLE_LOCKED, Boolean.TRUE);

        table.getColumnModel().getColumn(0).setPreferredWidth(220);
        table.getColumnModel().getColumn(1).setPreferredWidth(520);
    }

    private record CustomCommandRow(CommandCatalogEntry entry, CustomCommandDefinition customCommand) {
        private List<String> phrases() {
            return AiActionLocalizations.splitPhraseGroup(customCommand.getPhrases());
        }

        private String phrasesText() {
            return String.join(", ", phrases());
        }

        private String sequenceText() {
            if (customCommand.getSteps() == null || customCommand.getSteps().isEmpty()) {
                return "";
            }

            StringBuilder text = new StringBuilder();
            for (int i = 0; i < customCommand.getSteps().size(); i++) {
                if (!text.isEmpty()) {
                    text.append(System.lineSeparator());
                }
                text.append(i + 1)
                        .append(". ")
                        .append(stepText(customCommand.getSteps().get(i)));
            }
            return text.toString();
        }

        private static String stepText(CustomCommandStep step) {
            if (step == null || step.getType() == null) {
                return "INVALID_STEP";
            }
            String label = CustomCommandStepEditorDialog.stepTypeLabel(step.getType()) + ": ";
            return switch (step.getType()) {
                case SPEAK -> label + "\"" + step.getText() + "\"";
                case DELAY -> label + step.getDurationMs() + " ms";
                case BINDING_TAP -> label + step.getBindingId();
                case BINDING_HOLD -> label + step.getBindingId() + " (" + step.getDurationMs() + " ms)";
                case RUN_COMMAND -> label + step.getActionId();
                case RAW_KEY -> {
                    String combo = new BindingSlotDisplayFormatter().formatRawKeyStep(step.getRawKey(),
                            step.getRawKeyModifier());
                    yield label + combo + (step.getDurationMs() > 0 ? " (" + step.getDurationMs() + " ms)" : "");
                }
            };
        }

    }

    private void installRowHover(JTable table) {
        table.putClientProperty(HudTable.HOVER_ROW_PROPERTY, -1);
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                setHoveredRow(table, rowUnderPoint(table, event.getPoint()));
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                setHoveredRow(table, -1);
            }
        });
    }

    private int rowUnderPoint(JTable table, Point point) {
        int row = table.rowAtPoint(point);
        if (row >= 0) {
            return row;
        }
        Point up = new Point(point.x, point.y - 2);
        row = table.rowAtPoint(up);
        if (row >= 0) {
            return row;
        }
        Point down = new Point(point.x, point.y + 2);
        return table.rowAtPoint(down);
    }

    private void setHoveredRow(JTable table, int row) {
        Object current = table.getClientProperty(HudTable.HOVER_ROW_PROPERTY);
        int currentRow = current instanceof Integer value ? value : -1;
        if (currentRow == row) {
            return;
        }
        table.putClientProperty(HudTable.HOVER_ROW_PROPERTY, row);
        repaintTableRow(table, currentRow);
        repaintTableRow(table, row);
    }

    private void repaintTableRow(JTable table, int row) {
        if (row < 0 || row >= table.getRowCount()) {
            return;
        }
        Rectangle rect = table.getCellRect(row, 0, true);
        rect.width = table.getWidth();
        table.repaint(rect);
    }

    private static final class ReadOnlyTableModel extends DefaultTableModel {
        private ReadOnlyTableModel(Object[] columnNames, int rowCount) {
            super(columnNames, rowCount);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    private static final class CellRenderer extends HudTable.CellRenderer {
        private final HudCommandNameCellRenderer commandNameRenderer = new HudCommandNameCellRenderer();

        private CellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            if (value instanceof CustomCommandNameCell customCommandNameCell) {
                Component component = commandNameRenderer.getTableCellRendererComponent(
                        table, customCommandNameCell, isSelected, hasFocus, row, column);
                if (component instanceof JComponent jComponent) {
                    jComponent.setToolTipText(customCommandNameCell.toString());
                }
                return component;
            }

            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);
            String text = String.valueOf(value);
            label.setText(text);
            label.setToolTipText(text);
            return label;
        }
    }

    private record CustomCommandNameCell(String name, String id) implements HudCommandNameCellRenderer.Value {
        @Override
        public String toString() {
            return name + " " + id;
        }
    }

    private final class SearchDocumentListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            initData();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            initData();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            initData();
        }
    }

    private final class CustomCommandTableMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent event) {
            if (!SwingUtilities.isLeftMouseButton(event)) {
                return;
            }
            int row = table.rowAtPoint(event.getPoint());
            if (row < 0) {
                return;
            }
            table.setRowSelectionInterval(row, row);
            table.requestFocusInWindow();
        }

        @Override
        public void mouseClicked(MouseEvent event) {
            if (!SwingUtilities.isLeftMouseButton(event)) {
                return;
            }
            int row = table.rowAtPoint(event.getPoint());
            if (row >= 0) {
                openCustomCommandDetailsAtViewRow(row);
            }
        }
    }
}
