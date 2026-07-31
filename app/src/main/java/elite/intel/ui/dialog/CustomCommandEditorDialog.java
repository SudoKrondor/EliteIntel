package elite.intel.ui.dialog;

import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandDefinition;
import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandStep;
import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandValidator;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ai.brain.vega.llm.CustomCommandKeyGenerator;
import elite.intel.ui.support.BindingSlotDisplayFormatter;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudForms;
import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Modal CRUD editor for one custom command definition. It returns a validated customCommand but does not persist it.
 */
public final class CustomCommandEditorDialog extends JDialog {

    private static final Logger log = LogManager.getLogger(CustomCommandEditorDialog.class);

    private final List<CustomCommandDefinition> existingCustomCommands;
    /** Immutable UUID carried through edits; {@code null} for new customCommands until saved. */
    private final String originalId;
    /** Action key before editing; used by the validator for uniqueness self-check. */
    private final String originalActionKey;
    /** Read-only diagnostic field showing the internal UUID. */
    private final JTextField idField = new JTextField(36);
    private final JTextField nameField = AppTheme.makeTextField();
    private final JTextArea phrasesArea = textArea(7);
    /**
     * The LLM-generated English routing key, shown read-only. It is produced by the "Generate" button
     * (or carried from the existing command on edit) and never hand-typed: the key must be ASCII because
     * it becomes the provider tool name (see {@link CustomCommandKeyGenerator}), which manual entry could
     * violate. Its text is the value {@link #buildCandidate} persists.
     */
    private final JLabel keyLabel = new JLabel();
    private JButton generateKeyButton;
    /**
     * Description is no longer surfaced; retained empty for backward-compatible persistence.
     */
    private String description = "";
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
        buildUi();
    }

    public CustomCommandDefinition showDialog() {
        setVisible(true);
        return result;
    }

    private void populate(CustomCommandDefinition customCommand) {
        if (customCommand == null) {
            idField.setText("");
            return;
        }
        idField.setText(customCommand.getId());
        nameField.setText(customCommand.getName());
        description = customCommand.getDescription();
        phrasesArea.setText(customCommand.getPhrases());
        keyLabel.setText(customCommand.getActionKey());
        stepsModel.setSteps(customCommand.getSteps());
    }

    private void buildUi() {
        HudSection identitySection = HudSection.flat(
                getText("actions.customCommands.editor.section.identity"), new BorderLayout());
        identitySection.body().add(form(), BorderLayout.CENTER);

        JPanel leftColumn = AppTheme.transparentPanel(new GridBagLayout());
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0;
        lc.gridy = 0;
        lc.weightx = 1;
        lc.weighty = 1;
        lc.fill = GridBagConstraints.BOTH;
        leftColumn.add(identitySection, lc);

        JPanel columns = new HudTwoColumns(leftColumn, stepsPanel());

        // errors block: lives in body SOUTH (was in bottomPanel CENTER before migration)
        errorsArea.setEditable(false);
        errorsArea.setVisible(false);
        errorsScrollPane = AppTheme.hudScrollPane(errorsArea);
        errorsScrollPane.setVisible(false);

        JPanel body = AppTheme.transparentPanel(new BorderLayout(0, HudPalette.HUD_GAP));
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
        setMinimumSize(new Dimension(1000, 640));
        // Match the main window width so the full-width table toolbars have room (no label clipping);
        // force the final size before centering so the window doesn't drift.
        Window owner = getOwner();
        int targetWidth = owner != null && owner.getWidth() > 0 ? owner.getWidth() : 1000;
        int targetHeight = Math.max(getHeight(), 720);
        if (owner != null && owner.getHeight() > 0) {
            targetHeight = Math.min(targetHeight, owner.getHeight());
        }
        setSize(Math.max(targetWidth, 1000), Math.max(targetHeight, 640));
        setLocationRelativeTo(owner);
    }

    private JPanel form() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = HudForms.baseGbc();

        addExplainer(panel, gbc, getText("actions.customCommands.editor.explainer"));
        addField(panel, gbc, getText("actions.customCommands.editor.name"), nameField);
        addArea(panel, gbc, getText("actions.customCommands.editor.phrases"), phrasesArea);
        addKeyRow(panel, gbc);
        return panel;
    }

    /**
     * Adds the instructional hint spanning the whole form width, as the same blue INFO banner the keybind
     * editor uses for its capture hint.
     */
    private void addExplainer(JPanel panel, GridBagConstraints gbc, String text) {
        HudForms.addSpanComponent(panel, HudBanner.multiline(text, StatusBadge.State.INFO), gbc);
        gbc.gridx = 0;
        gbc.gridy++;
    }

    /**
     * Adds the action-key row: the read-only key display plus a "Generate" button that fills it from the
     * phrases via the LLM. The key is the English snake_case token the routing model emits; it is generated
     * (not hand-derived, not hand-edited) because it must be English even when the phrases are not - see
     * {@link CustomCommandKeyGenerator}.
     */
    private void addKeyRow(JPanel panel, GridBagConstraints gbc) {
        addLabel(panel, gbc, getText("actions.customCommands.editor.actionKey"));
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel row = AppTheme.transparentPanel(new BorderLayout(HudPalette.HUD_GAP, 0));
        keyLabel.setForeground(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        row.add(keyLabel, BorderLayout.CENTER);
        generateKeyButton = AppTheme.makeButtonSubtle(getText("actions.customCommands.editor.generateKey"));
        generateKeyButton.addActionListener(event -> generateKey());
        row.add(generateKeyButton, BorderLayout.EAST);
        panel.add(row, gbc);
        gbc.gridy++;
    }

    /**
     * Generates the action key from the current phrases via the LLM, off the EDT so the modal stays
     * responsive. Requires at least one phrase; on failure (no supported provider, timeout, unusable
     * output) it surfaces the reason in the errors block rather than writing a key.
     */
    private void generateKey() {
        String phrases = normalizePhrases(phrasesArea.getText());
        if (phrases.isBlank()) {
            showErrors(List.of(getText("actions.customCommands.editor.keyPhrasesRequired")));
            return;
        }
        List<String> taken = takenActionKeys();
        String idleLabel = generateKeyButton.getText();
        generateKeyButton.setEnabled(false);
        generateKeyButton.setText(getText("actions.customCommands.editor.generatingKey"));
        new SwingWorker<String, Void>() {
            private String error;

            @Override
            protected String doInBackground() {
                try {
                    return CustomCommandKeyGenerator.generate(phrases, taken);
                } catch (CustomCommandKeyGenerator.KeyGenerationException e) {
                    error = e.getMessage();
                    return null;
                }
            }

            @Override
            protected void done() {
                generateKeyButton.setEnabled(true);
                generateKeyButton.setText(idleLabel);
                String key = null;
                try {
                    key = get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Custom command key generation was interrupted", e);
                } catch (ExecutionException e) {
                    // An unchecked failure inside generate() (not a KeyGenerationException, which the worker
                    // already turned into an error message) surfaces here; log it so it is diagnosable.
                    log.warn("Custom command key generation failed unexpectedly", e.getCause());
                }
                if (key != null && !key.isBlank()) {
                    keyLabel.setText(key);
                    hideErrors();
                } else {
                    showErrors(List.of(error != null ? error
                            : getText("actions.customCommands.editor.keyGenerationFailed")));
                }
            }
        }.execute();
    }

    /**
     * Action keys already in use by other commands (excludes this command's own key when editing).
     */
    private List<String> takenActionKeys() {
        return existingCustomCommands.stream()
                .map(CustomCommandDefinition::getActionKey)
                .filter(key -> originalActionKey == null || !key.equalsIgnoreCase(originalActionKey))
                .collect(Collectors.toList());
    }


    private JPanel stepsPanel() {
        HudSection panel = HudSection.flat(getText("actions.customCommands.editor.steps"), new BorderLayout(0, HudPalette.HUD_GAP));

        stepsTable.setFillsViewportHeight(true);
        stepsTable.setRowHeight(30);
        stepsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        HudTable.style(stepsTable);
        stepsTable.getColumnModel().getColumn(0).setCellRenderer(new HudTable.ValueCellRenderer());
        stepsTable.getColumnModel().getColumn(1).setCellRenderer(new HudTable.ValueCellRenderer());
        stepsTable.getColumnModel().getColumn(2)
                .setCellRenderer(new HudTable.ValueCellRenderer(null, SwingConstants.RIGHT));
        // Duration is a short numeric column - cap it so Type/Value take the remaining width.
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
        bg.insets = new Insets(0, 0, 0, HudPalette.HUD_GAP);
        buttons.add(stepTextButton("actions.customCommands.editor.step.add", this::addStep), bg);
        buttons.add(stepTextButton("actions.customCommands.editor.step.edit", this::editStep), bg);
        buttons.add(stepTextButton("actions.customCommands.editor.step.remove", this::removeStep), bg);
        bg.fill = GridBagConstraints.NONE;
        bg.weightx = 0;
        buttons.add(stepArrowButton(HudGlyphs.arrowUpIcon(16), "actions.customCommands.editor.step.up", () -> moveSelected(-1)), bg);
        bg.insets = new Insets(0, 0, 0, 0);
        buttons.add(stepArrowButton(HudGlyphs.arrowDownIcon(16), "actions.customCommands.editor.step.down", () -> moveSelected(1)), bg);
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
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        JScrollPane sp = AppTheme.hudScrollPane(area);
        sp.setBorder(AppTheme.hudFieldBorder());
        sp.getViewport().setBackground(HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        area.setBackground(HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        // Pin a sensible height so the row can't be squashed by GridBag under layout pressure from the
        // other rows: at least 4 lines visible, with a roomier preferred height.
        int lineHeight = area.getFontMetrics(area.getFont()).getHeight();
        sp.setMinimumSize(new Dimension(10, lineHeight * 4 + 16));
        sp.setPreferredSize(new Dimension(10, lineHeight * Math.max(4, area.getRows()) + 16));
        panel.add(sp, gbc);
        gbc.gridy++;
        // Reset so later rows (e.g. the key preview) lay out normally.
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
    }

    private void addLabel(JPanel panel, GridBagConstraints gbc, String labelText) {
        // Delegate to the canonical label builder (owns dim-aware styling + height); 170 is this dialog's column width.
        HudForms.addLabel(panel, labelText, gbc, 170);
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
        button.setSquareSide(HudPalette.HUD_BUTTON_HEIGHT);   // square, ignores the min text-button width
        button.setIcon(icon);
        button.setToolTipText(getText(tooltipKey));
        button.addActionListener(event -> action.run());
        return button;
    }

    private void addStep() {
        CustomCommandStep step = new CustomCommandStepEditorDialog(this, null).showDialog();
        if (step != null) {
            stepsModel.addStep(step);
        }
    }

    private void editStep() {
        int row = selectedStepRow();
        if (row < 0) {
            return;
        }
        CustomCommandStep edited = new CustomCommandStepEditorDialog(this, stepsModel.getStep(row)).showDialog();
        if (edited != null) {
            stepsModel.setStep(row, edited);
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
        // The action key is authored by the "Generate" button (LLM-produced English key), read from the
        // read-only label: the model output is not reproducible from the phrases and must stay stable across
        // edits. Validation catches a blank/malformed/duplicate key. The immutable UUID carries identity.
        String phrases = normalizePhrases(phrasesArea.getText());
        String actionKey = keyLabel.getText().trim();
        return new CustomCommandDefinition(
                id,
                actionKey,
                name,
                description,
                phrases,
                stepsModel.steps()
        );
    }

    /**
     * Normalizes a phrases string entered in the editor into the canonical comma-separated storage form.
     * Users may separate phrases with newlines, commas, or both; either way the result never contains an
     * empty phrase (no {@code ",,"}, no leading/trailing/stray comma). It does this by folding both
     * separators into one and round-tripping through {@link AiActionLocalizations#splitPhraseGroup} - the
     * same splitter the routing layer uses - so the stored string always matches how it will later be
     * split, and commas inside parameter templates like {@code {lat:X, lon:Y}} are preserved.
     * <p>
     * Package-private for direct testing of the no-{@code ",,"} guarantee.
     */
    static String normalizePhrases(String raw) {
        if (raw == null) return "";
        String unified = raw.replace("\r\n", "\n").replace('\r', '\n').replace('\n', ',');
        return String.join(", ", AiActionLocalizations.splitPhraseGroup(unified));
    }

    private void showErrors(List<String> errors) {
        errorsArea.setText(String.join(System.lineSeparator(), errors));
        errorsArea.setVisible(true);
        if (errorsScrollPane != null) {
            errorsScrollPane.setVisible(true);
        }
        pack();
    }

    /**
     * Hides the errors block (e.g. after a successful key generation clears a prior warning).
     */
    private void hideErrors() {
        errorsArea.setText("");
        errorsArea.setVisible(false);
        if (errorsScrollPane != null) {
            errorsScrollPane.setVisible(false);
        }
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
