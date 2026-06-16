package elite.intel.ui.dialog;

import elite.intel.ui.support.BindingSlotDisplayFormatter;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudButton;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudTable;
import elite.intel.ui.widget.HudTwoColumns;

import elite.intel.ai.brain.actions.customcommand.CustomCommandDefinition;
import elite.intel.ai.brain.actions.customcommand.CustomCommandValidator;
import elite.intel.ai.brain.actions.customcommand.CustomCommandParameterSpec;
import elite.intel.ai.brain.actions.customcommand.CustomCommandStep;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Modal CRUD editor for one custom command definition. It returns a validated customCommand but does not persist it.
 */
public final class CustomCommandEditorDialog extends JDialog {

    private final List<CustomCommandDefinition> existingCustomCommands;
    /** Immutable UUID carried through edits; {@code null} for new customCommands until saved. */
    private final String originalId;
    /** Action key before editing; used by the validator for uniqueness self-check. */
    private final String originalActionKey;
    /** Read-only diagnostic field showing the internal UUID. */
    private final JTextField idField = new JTextField(36);
    private final JTextField actionKeyField = AppTheme.makeTextField();
    private final JTextField nameField = AppTheme.makeTextField();
    private final JTextArea descriptionArea = textArea(3);
    private final JTextArea phrasesArea = textArea(4);
    private final ParamsTableModel paramsModel = new ParamsTableModel();
    private final JTable paramsTable = new JTable(paramsModel);
    private final StepsTableModel stepsModel = new StepsTableModel();
    private final JTable stepsTable = new JTable(stepsModel);
    private final JTextArea errorsArea = textArea(4);
    private JScrollPane errorsScrollPane;
    private CustomCommandDefinition result;

    public CustomCommandEditorDialog(Component parent, CustomCommandDefinition customCommand, List<CustomCommandDefinition> existingCustomCommands) {
        super(
                SwingUtilities.getWindowAncestor(parent),
                customCommand == null ? getText("actions.customCommands.editor.newTitle") : getText("actions.customCommands.editor.editTitle"),
                ModalityType.APPLICATION_MODAL
        );
        setUndecorated(true);
        this.existingCustomCommands = existingCustomCommands == null ? List.of() : List.copyOf(existingCustomCommands);
        this.originalId = customCommand == null ? null : customCommand.getId();
        this.originalActionKey = customCommand == null ? null : customCommand.getActionKey();
        populate(customCommand);
        buildUi(customCommand != null);
    }

    public CustomCommandDefinition showDialog() {
        setVisible(true);
        return result;
    }

    private void populate(CustomCommandDefinition customCommand) {
        if (customCommand == null) {
            idField.setText("");
            actionKeyField.setText("custom_command_new");
            return;
        }
        idField.setText(customCommand.getId());
        actionKeyField.setText(customCommand.getActionKey());
        nameField.setText(customCommand.getName());
        descriptionArea.setText(customCommand.getDescription());
        phrasesArea.setText(customCommand.getPhrases());
        paramsModel.setParameters(customCommand.getParameters());
        stepsModel.setSteps(customCommand.getSteps());
    }

    private void buildUi(boolean existing) {
        HudSection identitySection = HudSection.flat(
                getText("actions.customCommands.editor.section.identity"), new BorderLayout());
        identitySection.body().add(form(existing), BorderLayout.CENTER);

        // Two columns: left = identity (top) + parameters (fills); right = steps (fills).
        JPanel leftColumn = AppTheme.transparentPanel(new BorderLayout(0, AppTheme.HUD_GAP));
        leftColumn.add(identitySection, BorderLayout.NORTH);
        leftColumn.add(paramsPanel(), BorderLayout.CENTER);

        JPanel columns = new HudTwoColumns(leftColumn, stepsPanel());

        // errors block: lives in body SOUTH (was in bottomPanel CENTER before migration)
        errorsArea.setEditable(false);
        errorsArea.setVisible(false);
        errorsScrollPane = AppTheme.hudScrollPane(errorsArea);
        errorsScrollPane.setVisible(false);

        JPanel body = AppTheme.transparentPanel(new BorderLayout(0, AppTheme.HUD_GAP));
        body.add(columns, BorderLayout.CENTER);
        body.add(errorsScrollPane, BorderLayout.SOUTH);

        JButton save = AppTheme.makeButton(getText("button.save"));
        save.addActionListener(event -> save());
        JButton back = AppTheme.makeButtonSubtle(getText("button.back"));
        back.addActionListener(event -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getTitle())
                .onClose(this::dispose)
                .body(body)
                .scrollBody(false)            // sections manage their own scroll; body not scrolled
                .primary(save)                // right side
                .dismiss(back)                // left side
                .build();

        setContentPane(AppTheme.hudModalScaffold(spec));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(save);
        pack();
        setMinimumSize(new Dimension(1000, 560));
        // Match the main window width so the full-width table toolbars have room (no label clipping);
        // force the final size before centering so the window doesn't drift.
        Window owner = getOwner();
        int targetWidth = owner != null && owner.getWidth() > 0 ? owner.getWidth() : 1000;
        setSize(Math.max(targetWidth, 1000), Math.max(getHeight(), 560));
        setLocationRelativeTo(owner);
    }

    private JPanel form(boolean existing) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = AppTheme.baseGbc();
        idField.setEditable(false);
        idField.setForeground(AppTheme.FG_MUTED);

        addField(panel, gbc, getText("actions.customCommands.editor.actionKey"), actionKeyField);
        addField(panel, gbc, getText("actions.customCommands.editor.name"), nameField);
        addArea(panel, gbc, getText("actions.customCommands.editor.description"), descriptionArea);
        addArea(panel, gbc, getText("actions.customCommands.editor.phrases"), phrasesArea);
        return panel;
    }

    private JPanel paramsPanel() {
        HudSection panel = HudSection.flat(getText("actions.customCommands.editor.parameters"), new BorderLayout(0, AppTheme.HUD_GAP));

        paramsTable.setFillsViewportHeight(true);
        paramsTable.setRowHeight(26);
        paramsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        HudTable.style(paramsTable);
        paramsTable.getColumnModel().getColumn(0)
                .setCellRenderer(new HudTable.ValueCellRenderer());
        for (int i = 1; i <= 3; i++) {
            paramsTable.getColumnModel().getColumn(i)
                    .setCellRenderer(new HudTable.ValueCellRenderer());
        }
        paramsTable.getColumnModel().getColumn(2)
                .setCellRenderer(new RequiredCellRenderer());
        paramsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editParam();
            }
        });
        JScrollPane scroll = HudTable.scrollPane(paramsTable);
        scroll.setPreferredSize(new Dimension(0, 130));
        panel.body().add(scroll, BorderLayout.CENTER);

        // Table toolbar: buttons stretch evenly across the full table width.
        JPanel buttons = AppTheme.transparentPanel(new GridLayout(1, 0, AppTheme.HUD_GAP, 0));
        addStepButton(buttons, "actions.customCommands.editor.param.add", this::addParam);
        addStepButton(buttons, "actions.customCommands.editor.param.edit", this::editParam);
        addStepButton(buttons, "actions.customCommands.editor.param.remove", this::removeParam);
        panel.body().add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void addParam() {
        CustomCommandParameterSpec spec = new CustomCommandParamSpecEditorDialog(this, null).showDialog();
        if (spec != null) {
            paramsModel.addParameter(spec);
        }
    }

    private void editParam() {
        int row = selectedParamRow();
        if (row < 0) return;
        CustomCommandParameterSpec edited = new CustomCommandParamSpecEditorDialog(this, paramsModel.getParameter(row)).showDialog();
        if (edited != null) {
            paramsModel.setParameter(row, edited);
        }
    }

    private void removeParam() {
        int row = selectedParamRow();
        if (row >= 0) {
            paramsModel.removeParameter(row);
        }
    }

    private int selectedParamRow() {
        int viewRow = paramsTable.getSelectedRow();
        return viewRow < 0 ? -1 : paramsTable.convertRowIndexToModel(viewRow);
    }

    private JPanel stepsPanel() {
        HudSection panel = HudSection.flat(getText("actions.customCommands.editor.steps"), new BorderLayout(0, AppTheme.HUD_GAP));

        stepsTable.setFillsViewportHeight(true);
        stepsTable.setRowHeight(30);
        stepsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        HudTable.style(stepsTable);
        stepsTable.getColumnModel().getColumn(0).setCellRenderer(new HudTable.ValueCellRenderer());
        stepsTable.getColumnModel().getColumn(1).setCellRenderer(new HudTable.ValueCellRenderer());
        stepsTable.getColumnModel().getColumn(2)
                .setCellRenderer(new HudTable.ValueCellRenderer(null, SwingConstants.RIGHT));
        // Duration is a short numeric column — cap it so Type/Value take the remaining width.
        stepsTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        stepsTable.getColumnModel().getColumn(2).setMaxWidth(150);
        stepsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editStep();
            }
        });
        panel.body().add(HudTable.scrollPane(stepsTable), BorderLayout.CENTER);

        // Table toolbar: text actions stretch to fill the width; the move arrows stay compact (square).
        JPanel buttons = AppTheme.transparentPanel(new GridBagLayout());
        GridBagConstraints bg = new GridBagConstraints();
        bg.gridy = 0;
        bg.fill = GridBagConstraints.HORIZONTAL;
        bg.weightx = 1;
        bg.insets = new Insets(0, 0, 0, AppTheme.HUD_GAP);
        buttons.add(stepTextButton("actions.customCommands.editor.step.add", this::addStep), bg);
        buttons.add(stepTextButton("actions.customCommands.editor.step.edit", this::editStep), bg);
        buttons.add(stepTextButton("actions.customCommands.editor.step.remove", this::removeStep), bg);
        bg.fill = GridBagConstraints.NONE;
        bg.weightx = 0;
        buttons.add(stepArrowButton(AppTheme.arrowUpIcon(16), "actions.customCommands.editor.step.up", () -> moveSelected(-1)), bg);
        bg.insets = new Insets(0, 0, 0, 0);
        buttons.add(stepArrowButton(AppTheme.arrowDownIcon(16), "actions.customCommands.editor.step.down", () -> moveSelected(1)), bg);
        panel.body().add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void addField(JPanel panel, GridBagConstraints gbc, String labelText, JTextField field) {
        addLabel(panel, gbc, labelText);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, gbc);
        gbc.gridy++;
    }

    private void addArea(JPanel panel, GridBagConstraints gbc, String labelText, JTextArea area) {
        addLabel(panel, gbc, labelText);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JScrollPane sp = AppTheme.hudScrollPane(area);
        sp.setBorder(AppTheme.hudFieldBorder());
        sp.getViewport().setBackground(AppTheme.HUD_TABLE_ROW);
        area.setBackground(AppTheme.HUD_TABLE_ROW);
        panel.add(sp, gbc);
        gbc.gridy++;
    }

    private void addLabel(JPanel panel, GridBagConstraints gbc, String labelText) {
        // Delegate to the canonical label builder (owns dim-aware styling + height); 170 is this dialog's column width.
        AppTheme.addLabel(panel, labelText, gbc, 170);
    }

    private void addStepButton(JPanel panel, String key, Runnable action) {
        JButton button = AppTheme.makeButtonSubtle(getText(key));
        button.addActionListener(event -> action.run());
        panel.add(button);
    }

    /** Subtle stretchable text toolbar button. */
    private JButton stepTextButton(String key, Runnable action) {
        JButton button = AppTheme.makeButtonSubtle(getText(key));
        button.addActionListener(event -> action.run());
        return button;
    }

    /** Compact square subtle toolbar button with a glyph icon; {@code tooltipKey} labels it. */
    private JButton stepArrowButton(Icon icon, String tooltipKey, Runnable action) {
        HudButton button = (HudButton) AppTheme.makeButtonSubtle("");
        button.setSquareSide(AppTheme.HUD_BUTTON_HEIGHT);   // square, ignores the min text-button width
        button.setIcon(icon);
        button.setToolTipText(getText(tooltipKey));
        button.addActionListener(event -> action.run());
        return button;
    }

    private void addStep() {
        CustomCommandStep step = new CustomCommandStepEditorDialog(
                this,
                null,
                paramsModel.parameters(),
                this::addMissingCustomCommandParameters
        ).showDialog();
        if (step != null) {
            stepsModel.addStep(step);
        }
    }

    private void editStep() {
        int row = selectedStepRow();
        if (row < 0) {
            return;
        }
        CustomCommandStep edited = new CustomCommandStepEditorDialog(
                this,
                stepsModel.getStep(row),
                paramsModel.parameters(),
                this::addMissingCustomCommandParameters
        ).showDialog();
        if (edited != null) {
            stepsModel.setStep(row, edited);
        }
    }

    private void addMissingCustomCommandParameters(List<CustomCommandParameterSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return;
        }
        for (CustomCommandParameterSpec spec : specs) {
            String name = spec == null ? null : spec.getName();
            if (name == null || name.isBlank() || paramsModel.hasParameter(name)) {
                continue;
            }
            paramsModel.addParameter(spec);
        }
    }

    private void removeStep() {
        int row = selectedStepRow();
        if (row >= 0) {
            stepsModel.removeStep(row);
        }
    }

    private void moveSelected(int delta) {
        int row = selectedStepRow();
        if (stepsModel.move(row, delta)) {
            stepsTable.setRowSelectionInterval(row + delta, row + delta);
        }
    }

    private int selectedStepRow() {
        int viewRow = stepsTable.getSelectedRow();
        return viewRow < 0 ? -1 : stepsTable.convertRowIndexToModel(viewRow);
    }

    private void save() {
        CustomCommandDefinition candidate = buildCandidate();
        List<String> errors = CustomCommandValidator.validate(candidate, existingCustomCommands, originalActionKey);
        if (!errors.isEmpty()) {
            showErrors(errors);
            return;
        }
        result = candidate;
        dispose();
    }

    private CustomCommandDefinition buildCandidate() {
        String name = nameField.getText().trim();
        // Preserve the existing UUID on edit; generate a new one for new customCommands.
        String id = (originalId != null && !originalId.isBlank()) ? originalId : UUID.randomUUID().toString();
        idField.setText(id);
        String actionKey = actionKeyField.getText().trim();
        if (actionKey.isBlank()) {
            actionKey = uniqueGeneratedActionKey(name);
            actionKeyField.setText(actionKey);
        }
        return new CustomCommandDefinition(
                id,
                actionKey,
                name,
                descriptionArea.getText().trim(),
                normalizePhrases(phrasesArea.getText()),
                paramsModel.parameters(),
                stepsModel.steps()
        );
    }

    private String uniqueGeneratedActionKey(String name) {
        String base = sanitizeId(name);
        if (base.isBlank()) {
            base = "new_custom_command";
        }
        List<String> existingKeys = existingCustomCommands.stream()
                .filter(customCommand -> !sameId(customCommand.getActionKey(), originalActionKey))
                .map(CustomCommandDefinition::getActionKey)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
        String candidate = base;
        int suffix = 2;
        while (existingKeys.contains(candidate.toLowerCase(Locale.ROOT))) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    /**
     * Normalizes a phrases string entered in the editor: treats newlines as phrase separators
     * so users can type one phrase per line instead of comma-separating them manually.
     * The result is always a comma-separated string, matching the storage format.
     * Commas within a single line are preserved as-is so parameter templates like
     * {@code {lat:X, lon:Y}} are not broken.
     */
    private static String normalizePhrases(String raw) {
        if (raw == null) return "";
        return Arrays.stream(raw.replace("\r\n", "\n").replace('\r', '\n').split("\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
    }

    private static String sanitizeId(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return normalized;
    }

    private static boolean sameId(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.equalsIgnoreCase(right);
    }

    private void showErrors(List<String> errors) {
        errorsArea.setText(String.join(System.lineSeparator(), errors));
        errorsArea.setVisible(true);
        if (errorsScrollPane != null) {
            errorsScrollPane.setVisible(true);
        }
        pack();
    }

    private static JTextArea textArea(int rows) {
        JTextArea area = AppTheme.makeTextArea(rows, 36);
        area.setBorder(new EmptyBorder(8, 8, 8, 8));
        installPlainTextPaste(area);
        return area;
    }

    private static void installPlainTextPaste(JTextComponent component) {
        Action paste = new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                pastePlainText(component);
            }
        };
        component.getActionMap().put(DefaultEditorKit.pasteAction, paste);
        component.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                DefaultEditorKit.pasteAction
        );
        component.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.SHIFT_DOWN_MASK),
                DefaultEditorKit.pasteAction
        );
    }

    private static void pastePlainText(JTextComponent component) {
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(new ClipboardFlavorNoiseFilter(originalErr), true));
        try {
            Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (contents == null) {
                return;
            }
            Object data = contents.getTransferData(DataFlavor.stringFlavor);
            if (data instanceof String text) {
                component.replaceSelection(text);
            }
        } catch (UnsupportedFlavorException | IOException | IllegalStateException e) {
            Toolkit.getDefaultToolkit().beep();
        } finally {
            System.setErr(originalErr);
        }
    }

    private static final class ClipboardFlavorNoiseFilter extends OutputStream {
        private final PrintStream delegate;
        private final StringBuilder line = new StringBuilder();

        private ClipboardFlavorNoiseFilter(PrintStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public synchronized void write(int b) {
            char c = (char) b;
            line.append(c);
            if (c == '\n') {
                flushLine();
            }
        }

        @Override
        public synchronized void flush() {
            if (!line.isEmpty()) {
                flushLine();
            }
            delegate.flush();
        }

        private void flushLine() {
            String text = line.toString();
            line.setLength(0);
            if (!isIntelliJClipboardFlavorNoise(text)) {
                delegate.print(text);
            }
        }

        private static boolean isIntelliJClipboardFlavorNoise(String text) {
            return text.contains("while constructing DataFlavor")
                    && (text.contains("com/intellij/openapi/editor/RawText")
                    || text.contains("com/intellij/codeInsight/editorActions/FoldingData")
                    || text.contains("com/intellij/openapi/editor/impl/EditorCopyPasteHelperImpl$CopyPasteOptionsTransferableData"));
        }
    }

    private static final class ParamsTableModel extends AbstractTableModel {
        private final List<CustomCommandParameterSpec> params = new ArrayList<>();
        private final String[] columns = {
                getText("actions.customCommands.editor.param.column.name"),
                getText("actions.customCommands.editor.param.column.type"),
                getText("actions.customCommands.editor.param.column.required"),
                getText("actions.customCommands.editor.param.column.description")
        };

        void setParameters(List<CustomCommandParameterSpec> newParams) {
            params.clear();
            if (newParams != null) params.addAll(newParams);
            fireTableDataChanged();
        }

        List<CustomCommandParameterSpec> parameters() { return List.copyOf(params); }

        CustomCommandParameterSpec getParameter(int row) { return params.get(row); }

        void addParameter(CustomCommandParameterSpec spec) {
            params.add(spec);
            fireTableRowsInserted(params.size() - 1, params.size() - 1);
        }

        void setParameter(int row, CustomCommandParameterSpec spec) {
            params.set(row, spec);
            fireTableRowsUpdated(row, row);
        }

        void removeParameter(int row) {
            params.remove(row);
            fireTableRowsDeleted(row, row);
        }

        boolean hasParameter(String name) {
            return params.stream().anyMatch(param -> param.getName().equalsIgnoreCase(name));
        }

        @Override public int getRowCount() { return params.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            CustomCommandParameterSpec spec = params.get(row);
            return switch (col) {
                case 0 -> spec.getName();
                case 1 -> spec.getType();
                case 2 -> spec.isRequired();
                case 3 -> spec.getDescription();
                default -> "";
            };
        }
    }

    private static final class RequiredCellRenderer extends HudTable.ValueCellRenderer {
        private boolean checked;

        @Override
        public java.awt.Component getTableCellRendererComponent(
                javax.swing.JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            java.awt.Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            this.checked = Boolean.TRUE.equals(value);
            if (c instanceof javax.swing.JLabel l) {
                l.setText("");
            }
            return c;
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            try {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                int pad = getVerticalPadding();
                int size = (int) Math.round((getHeight() - 2 * pad) * 0.8);  // smaller, with air
                int x = (getWidth() - size) / 2;            // center horizontally
                int y = (getHeight() - size) / 2;
                java.awt.Color tint = getForeground();
                AppTheme.paintHudCheckMarker(g2, x, y, size, tint, checked);
            } finally {
                g2.dispose();
            }
        }
    }

    private static final class StepsTableModel extends AbstractTableModel {
        private final List<CustomCommandStep> steps = new ArrayList<>();
        private final String[] columns = {
                getText("actions.customCommands.editor.step.type"),
                getText("actions.customCommands.editor.step.column.value"),
                getText("actions.customCommands.editor.step.durationMs")
        };

        private void setSteps(List<CustomCommandStep> newSteps) {
            steps.clear();
            if (newSteps != null) {
                steps.addAll(newSteps);
            }
            fireTableDataChanged();
        }

        private List<CustomCommandStep> steps() {
            return List.copyOf(steps);
        }

        private CustomCommandStep getStep(int row) {
            return steps.get(row);
        }

        private void addStep(CustomCommandStep step) {
            steps.add(step);
            fireTableRowsInserted(steps.size() - 1, steps.size() - 1);
        }

        private void setStep(int row, CustomCommandStep step) {
            steps.set(row, step);
            fireTableRowsUpdated(row, row);
        }

        private void removeStep(int row) {
            steps.remove(row);
            fireTableRowsDeleted(row, row);
        }

        private boolean move(int row, int delta) {
            int target = row + delta;
            if (row < 0 || target < 0 || target >= steps.size()) {
                return false;
            }
            CustomCommandStep step = steps.remove(row);
            steps.add(target, step);
            fireTableDataChanged();
            return true;
        }

        @Override
        public int getRowCount() {
            return steps.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            CustomCommandStep step = steps.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> CustomCommandStepEditorDialog.stepTypeLabel(step.getType());
                case 1 -> stepValue(step);
                case 2 -> durationValue(step);
                default -> "";
            };
        }

        private static String stepValue(CustomCommandStep step) {
            return switch (step.getType()) {
                case SPEAK -> step.getText();
                case BINDING_TAP, BINDING_HOLD -> step.getBindingId();
                case RUN_COMMAND -> step.getActionId();
                case DELAY -> "";
                case RAW_KEY -> new BindingSlotDisplayFormatter().formatRawKeyStep(step.getRawKey(), step.getRawKeyModifier());
            };
        }

        private static String durationValue(CustomCommandStep step) {
            return switch (step.getType()) {
                case BINDING_HOLD, DELAY, RAW_KEY -> Integer.toString(step.getDurationMs());
                default -> "";
            };
        }
    }
}
