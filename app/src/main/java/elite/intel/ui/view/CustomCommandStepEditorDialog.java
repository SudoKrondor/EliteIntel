package elite.intel.ui.view;

import elite.intel.ai.brain.actions.customcommand.CustomCommandParameterSpec;
import elite.intel.ai.brain.actions.customcommand.CustomCommandStep;
import elite.intel.ai.brain.commons.ActionParameterKeyExtractor;
import elite.intel.ai.hands.BindingModifier;
import elite.intel.ai.hands.KeyBindingExecutor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Modal editor for one custom command step.
 */
final class CustomCommandStepEditorDialog extends JDialog {

    private static final int DIALOG_MIN_WIDTH = 760;

    // typeCombo: labelFn drives localized step-type display text via stepTypeLabel().
    private final HudComboBox<CustomCommandStep.Type> typeCombo =
            new HudComboBox<>(CustomCommandStep.Type.values(),
                    CustomCommandStepEditorDialog::stepTypeLabel);

    private final JLabel valueLabel   = AppTheme.hudReadoutLabel("");
    private final HudTextField valueField = new HudTextField();

    private final JLabel commandLabel = AppTheme.hudReadoutLabel(getText("actions.customCommands.editor.step.commandId"));
    private final List<CustomCommandStepPickerItem> commandItems = new ArrayList<>(CustomCommandStepPickerItem.builtInCommandItems());
    private final HudComboBox<CustomCommandStepPickerItem> commandCombo = HudComboBox.picker(
            commandItems.toArray(CustomCommandStepPickerItem[]::new),
            CustomCommandStepPickerItem::toString,
            CustomCommandStepPickerItem::matches);

    private final JLabel bindingLabel = AppTheme.hudReadoutLabel(getText("actions.customCommands.editor.step.bindingId"));
    private final List<CustomCommandStepPickerItem> bindingItems = new ArrayList<>(CustomCommandStepPickerItem.bindingItems());
    private final HudComboBox<CustomCommandStepPickerItem> bindingCombo = HudComboBox.picker(
            bindingItems.toArray(CustomCommandStepPickerItem[]::new),
            CustomCommandStepPickerItem::toString,
            CustomCommandStepPickerItem::matches);

    private final JLabel rawKeyLabel  = AppTheme.hudReadoutLabel(getText("actions.customCommands.editor.step.rawKey"));
    private final List<CustomCommandStepPickerItem> rawKeyItems = buildRawKeyPickerItems();
    private final HudComboBox<CustomCommandStepPickerItem> rawKeyCombo = HudComboBox.picker(
            rawKeyItems.toArray(CustomCommandStepPickerItem[]::new),
            CustomCommandStepPickerItem::toString,
            CustomCommandStepPickerItem::matches);

    private final JLabel rawModLabel  = AppTheme.hudReadoutLabel(getText("actions.customCommands.editor.step.rawKeyModifier"));
    private final HudComboBox<RawModOption> rawModCombo = buildRawModCombo();

    private final JLabel durationLabel = AppTheme.hudReadoutLabel(getText("actions.customCommands.editor.step.durationMs"));
    private final JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(250, 0, Integer.MAX_VALUE, 50));
    private final JLabel stepParamsLabel = AppTheme.hudReadoutLabel(getText("actions.customCommands.editor.step.params"));
    private final JButton stepParamsInfoButton = new JButton("ⓘ");
    private final DefaultTableModel stepParamsModel = new DefaultTableModel(
            new String[]{getText("actions.customCommands.editor.step.params.column.key"),
                         getText("actions.customCommands.editor.step.params.column.value")}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return true; }
    };
    private final JTable stepParamsTable = new JTable(stepParamsModel);
    private final JPanel stepParamsPanel = buildStepParamsPanel();
    private final Set<String> customCommandParameterNames;
    private final Consumer<List<CustomCommandParameterSpec>> missingCustomCommandParamsConsumer;
    private String lastParamHintActionId;
    private CustomCommandStep result;

    CustomCommandStepEditorDialog(Component parent, CustomCommandStep step) {
        this(parent, step, List.of(), missingParams -> {});
    }

    CustomCommandStepEditorDialog(Component parent, CustomCommandStep step, List<CustomCommandParameterSpec> customCommandParameters) {
        this(parent, step, customCommandParameters, missingParams -> {});
    }

    CustomCommandStepEditorDialog(
            Component parent,
            CustomCommandStep step,
            List<CustomCommandParameterSpec> customCommandParameters,
            Consumer<List<CustomCommandParameterSpec>> missingCustomCommandParamsConsumer
    ) {
        super(SwingUtilities.getWindowAncestor(parent), getText("actions.customCommands.editor.step.title"), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.customCommandParameterNames = customCommandParameterNames(customCommandParameters);
        this.missingCustomCommandParamsConsumer = missingCustomCommandParamsConsumer == null ? missingParams -> {} : missingCustomCommandParamsConsumer;
        populate(step);
        buildUi();
        updateFieldsForType();
    }

    CustomCommandStep showDialog() {
        setVisible(true);
        return result;
    }

    private void populate(CustomCommandStep step) {
        if (step == null) {
            typeCombo.setSelectedItem(CustomCommandStep.Type.SPEAK);
            commandCombo.setSelectedItem("");
            return;
        }
        typeCombo.setSelectedItem(step.getType());
        durationSpinner.setValue(Math.max(0, step.getDurationMs()));
        switch (step.getType()) {
            case SPEAK -> valueField.setText(step.getText());
            case BINDING_TAP, BINDING_HOLD ->
                    selectPickerItem(bindingCombo, bindingItems, step.getBindingId(), getText("actions.customCommands.editor.step.unknownBinding"));
            case RUN_COMMAND -> {
                selectPickerItem(commandCombo, commandItems, step.getActionId(), getText("actions.customCommands.editor.step.unknownCommand"));
                stepParamsModel.setRowCount(0);
                step.getStepParams().forEach((k, v) -> stepParamsModel.addRow(new Object[]{k, v}));
            }
            case DELAY -> valueField.setText("");
            case RAW_KEY -> {
                selectPickerItem(rawKeyCombo, rawKeyItems, step.getRawKey(), getText("actions.customCommands.editor.step.unknownRawKey"));
                selectRawMod(step.getRawKeyModifier());
            }
        }
    }

    private void buildUi() {
        HudSection formSection = HudSection.flat(
                getText("actions.customCommands.editor.step.section.definition"), new BorderLayout());
        formSection.body().add(form(), BorderLayout.CENTER);

        JButton save = AppTheme.makeButton(getText("button.save"));
        save.addActionListener(event -> save());
        JButton back = AppTheme.makeButtonSubtle(getText("button.back"));
        back.addActionListener(event -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("actions.customCommands.editor.step.title"))
                .onClose(this::dispose)
                .body(formSection)
                .scrollBody(false)            // form body is not scrolled (params table has its own scroll)
                .primary(save)                // right side
                .dismiss(back)                // left side
                .build();

        setContentPane(AppTheme.hudModalScaffold(spec));

        typeCombo.addActionListener(event -> updateFieldsForType());
        commandCombo.addActionListener(event -> updateFieldsForSelectedCommand());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(save);
        pack();
        setMinimumSize(new Dimension(DIALOG_MIN_WIDTH, 260));
        setSize(Math.max(getWidth(), DIALOG_MIN_WIDTH), getHeight());
        setLocationRelativeTo(getOwner());
    }

    private JPanel form() {
        JPanel panel = AppTheme.transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = AppTheme.baseGbc();

        addRow(panel, gbc, getText("actions.customCommands.editor.step.type"), typeCombo);
        addRow(panel, gbc, valueLabel, valueField);
        addRow(panel, gbc, commandLabel, commandCombo);

        // Step params panel spans both columns for RUN_COMMAND.
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(stepParamsPanel, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;

        addRow(panel, gbc, bindingLabel, bindingCombo);
        addRow(panel, gbc, rawKeyLabel, rawKeyCombo);
        addRow(panel, gbc, rawModLabel, rawModCombo);
        addRow(panel, gbc, durationLabel, durationSpinner);
        AppTheme.applyDarkPalette(panel);
        // HudTextField and HudComboBox self-style via their constructors — no manual styleComboBox calls needed.
        styleInfoButton(stepParamsInfoButton);
        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, String label, JComponent field) {
        addRow(panel, gbc, AppTheme.hudReadoutLabel(label), field);
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, JLabel label, JComponent field) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        // Foreground already set by hudReadoutLabel; fixed-width column for label alignment.
        label.setPreferredSize(new Dimension(160, 28));
        panel.add(label, gbc);

        gbc.gridx = 1;
        boolean fixedWidthField = field instanceof JComboBox<?> || field instanceof JSpinner;
        gbc.weightx = fixedWidthField ? 0 : 1;
        gbc.fill = fixedWidthField ? GridBagConstraints.NONE : GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(field, gbc);
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.CENTER;
    }

    private void updateFieldsForType() {
        CustomCommandStep.Type type = selectedType();
        boolean hasText    = type == CustomCommandStep.Type.SPEAK;
        boolean hasCommand = type == CustomCommandStep.Type.RUN_COMMAND;
        boolean hasBinding = type == CustomCommandStep.Type.BINDING_TAP || type == CustomCommandStep.Type.BINDING_HOLD;
        boolean isRawKey   = type == CustomCommandStep.Type.RAW_KEY;
        boolean hasDuration = type == CustomCommandStep.Type.BINDING_HOLD || type == CustomCommandStep.Type.DELAY || isRawKey;
        if (hasCommand) {
            addMissingHintedStepParams();
        }

        valueLabel.setVisible(hasText);
        valueField.setVisible(hasText);
        commandLabel.setVisible(hasCommand);
        commandCombo.setVisible(hasCommand);
        stepParamsPanel.setVisible(shouldShowStepParamsPanel(hasCommand));
        bindingLabel.setVisible(hasBinding);
        bindingCombo.setVisible(hasBinding);
        rawKeyLabel.setVisible(isRawKey);
        rawKeyCombo.setVisible(isRawKey);
        rawModLabel.setVisible(isRawKey);
        rawModCombo.setVisible(isRawKey);
        durationLabel.setVisible(hasDuration);
        durationSpinner.setVisible(hasDuration);

        // hudReadoutLabel caps only at construction time; setText must uppercase explicitly.
        valueLabel.setText(getText("actions.customCommands.editor.step.text").toUpperCase());
        packPreservingWidth();
    }

    private void updateFieldsForSelectedCommand() {
        if (selectedType() != CustomCommandStep.Type.RUN_COMMAND) {
            return;
        }
        if (!(commandCombo.getSelectedItem() instanceof CustomCommandStepPickerItem)) {
            return;
        }
        syncStepParamsForSelectedCommand();
        stepParamsPanel.setVisible(shouldShowStepParamsPanel(true));
        packPreservingWidth();
    }

    private void addMissingHintedStepParams() {
        String actionId = selectedPickerId(commandCombo);
        if (actionId.isBlank() || actionId.equalsIgnoreCase(lastParamHintActionId != null ? lastParamHintActionId : "")) {
            return;
        }
        lastParamHintActionId = actionId;

        for (String key : ActionParameterKeyExtractor.getInstance().parameterKeysForAction(actionId)) {
            if (!hasStepParamKey(key)) {
                stepParamsModel.addRow(new Object[]{key, defaultStepParamValue(key)});
            }
        }
    }

    private void syncStepParamsForSelectedCommand() {
        String actionId = selectedPickerId(commandCombo);
        if (actionId.isBlank() || actionId.equalsIgnoreCase(lastParamHintActionId != null ? lastParamHintActionId : "")) {
            return;
        }
        lastParamHintActionId = actionId;

        List<String> knownKeys = ActionParameterKeyExtractor.getInstance().parameterKeysForAction(actionId);
        stepParamsModel.setRowCount(0);
        if (knownKeys.isEmpty()) { return; }

        for (String key : knownKeys) {
            stepParamsModel.addRow(new Object[]{key, defaultStepParamValue(key)});
        }
    }

    private boolean shouldShowStepParamsPanel(boolean hasCommand) {
        if (!hasCommand) { return false; }
        if (stepParamsModel.getRowCount() > 0) { return true; }
        return !ActionParameterKeyExtractor.getInstance()
                .parameterKeysForAction(selectedPickerId(commandCombo))
                .isEmpty();
    }

    private boolean hasStepParamKey(String key) {
        for (int i = 0; i < stepParamsModel.getRowCount(); i++) {
            Object existing = stepParamsModel.getValueAt(i, 0);
            if (key.equals(existing != null ? existing.toString().trim() : "")) {
                return true;
            }
        }
        return false;
    }

    private String defaultStepParamValue(String key) {
        return "${" + key + "}";
    }

    private void save() {
        CustomCommandStep step = buildStep();
        try {
            step.validate(0);
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return;
        }
        addMissingCustomCommandParams(step);
        result = step;
        dispose();
    }

    private void addMissingCustomCommandParams(CustomCommandStep step) {
        if (step.getType() != CustomCommandStep.Type.RUN_COMMAND) { return; }
        List<CustomCommandParameterSpec> missing = step.getStepParams().entrySet().stream()
                .filter(entry -> isSameNameCustomCommandTemplate(entry.getKey(), entry.getValue()))
                .map(Map.Entry::getKey)
                .filter(key -> !customCommandParameterNames.contains(key))
                .distinct()
                .map(key -> new CustomCommandParameterSpec(
                        key,
                        inferredCustomCommandParamType(step.getActionId(), key),
                        true,
                        getText("actions.customCommands.editor.step.params.autoDescription", commandNameForActionId(step.getActionId()), key),
                        List.of(),
                        null
                ))
                .toList();
        if (!missing.isEmpty()) {
            missingCustomCommandParamsConsumer.accept(missing);
        }
    }

    private static boolean isSameNameCustomCommandTemplate(String key, String value) {
        return key != null && !key.isBlank() && ("${" + key + "}").equals(value);
    }

    private String commandNameForActionId(String actionId) {
        return commandItems.stream()
                .filter(item -> item.id().equals(actionId))
                .map(CustomCommandStepPickerItem::label)
                .findFirst()
                .orElse(actionId);
    }

    private static String inferredCustomCommandParamType(String actionId, String key) {
        return ActionParameterKeyExtractor.getInstance().parameterHintsForAction(actionId).stream()
                .filter(hint -> hint.name().equals(key))
                .map(ActionParameterKeyExtractor.ActionParameterHint::type)
                .findFirst()
                .orElse("string");
    }

    private CustomCommandStep buildStep() {
        CustomCommandStep.Type type = selectedType();
        String value = valueField.getText().trim();
        int duration = ((Number) durationSpinner.getValue()).intValue();
        return switch (type) {
            case SPEAK        -> new CustomCommandStep(type, null, 0, value, null);
            case BINDING_TAP  -> new CustomCommandStep(type, selectedPickerId(bindingCombo), 0, null, null);
            case BINDING_HOLD -> new CustomCommandStep(type, selectedPickerId(bindingCombo), duration, null, null);
            case DELAY        -> new CustomCommandStep(type, null, duration, null, null);
            case RUN_COMMAND  -> {
                // Stop any in-progress cell edit so the last typed value is committed.
                if (stepParamsTable.isEditing()) {
                    stepParamsTable.getCellEditor().stopCellEditing();
                }
                Map<String, String> paramMap = new LinkedHashMap<>();
                for (int i = 0; i < stepParamsModel.getRowCount(); i++) {
                    Object k = stepParamsModel.getValueAt(i, 0);
                    Object v = stepParamsModel.getValueAt(i, 1);
                    String key = k != null ? k.toString().trim() : "";
                    String val = v != null ? v.toString().trim() : "";
                    if (!key.isBlank()) { paramMap.put(key, val); }
                }
                yield paramMap.isEmpty()
                        ? new CustomCommandStep(type, null, 0, null, selectedPickerId(commandCombo))
                        : CustomCommandStep.runCommandWithParams(selectedPickerId(commandCombo), paramMap);
            }
            case RAW_KEY -> {
                String rawKey = selectedPickerId(rawKeyCombo);
                RawModOption modOption = (RawModOption) rawModCombo.getSelectedItem();
                String rawMod = (modOption != null && !modOption.key().isBlank()) ? modOption.key() : null;
                yield new CustomCommandStep(type, null, duration, null, null, rawKey, rawMod);
            }
        };
    }

    private CustomCommandStep.Type selectedType() {
        Object selected = typeCombo.getSelectedItem();
        return selected instanceof CustomCommandStep.Type type ? type : CustomCommandStep.Type.SPEAK;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, getText("actions.customCommands.editor.validation.title"), JOptionPane.ERROR_MESSAGE);
    }

    private static void selectPickerItem(
            JComboBox<CustomCommandStepPickerItem> combo,
            List<CustomCommandStepPickerItem> items,
            String id,
            String unknownLabel
    ) {
        if (id == null || id.isBlank()) { return; }
        for (CustomCommandStepPickerItem item : items) {
            if (item.id().equalsIgnoreCase(id)) {
                combo.setSelectedItem(item);
                return;
            }
        }
        CustomCommandStepPickerItem unknown = CustomCommandStepPickerItem.unknown(id, unknownLabel);
        items.add(0, unknown);
        combo.addItem(unknown);
        combo.setSelectedItem(unknown);
    }

    private static String selectedPickerId(JComboBox<CustomCommandStepPickerItem> combo) {
        return CustomCommandStepPickerItem.resolveId(combo.getEditor().getItem()).trim();
    }

    /** Returns the localized display label for a step type. */
    static String stepTypeLabel(CustomCommandStep.Type type) {
        if (type == null) return "";
        return switch (type) {
            case BINDING_TAP  -> getText("actions.customCommands.editor.step.type.bindingTap");
            case BINDING_HOLD -> getText("actions.customCommands.editor.step.type.bindingHold");
            case DELAY        -> getText("actions.customCommands.editor.step.type.delay");
            case SPEAK        -> getText("actions.customCommands.editor.step.type.speak");
            case RUN_COMMAND  -> getText("actions.customCommands.editor.step.type.runCommand");
            case RAW_KEY      -> getText("actions.customCommands.editor.step.type.rawKey");
        };
    }

    private static List<CustomCommandStepPickerItem> buildRawKeyPickerItems() {
        BindingSlotDisplayFormatter formatter = new BindingSlotDisplayFormatter();
        return KeyBindingExecutor.knownEliteKeyNames().stream()
                .sorted()
                .map(name -> new CustomCommandStepPickerItem(name, formatter.formatBindingToken(BindingSlotDisplayFormatter.toEliteKeyFormat(name)), true))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static HudComboBox<RawModOption> buildRawModCombo() {
        List<RawModOption> items = new ArrayList<>();
        items.add(new RawModOption("", getText("actions.customCommands.editor.step.noModifier")));
        BindingModifier.supportedKeyboardModifiers().forEach(bm ->
                items.add(new RawModOption(bm.key().toUpperCase(), bm.key())));
        return new HudComboBox<>(items.toArray(RawModOption[]::new), RawModOption::label);
    }

    /** Selects the modifier combo item matching the stored key name (case-insensitive), or "(none)" if absent. */
    private void selectRawMod(String storedKeyName) {
        for (int i = 0; i < rawModCombo.getItemCount(); i++) {
            RawModOption option = rawModCombo.getItemAt(i);
            if (option.key().equalsIgnoreCase(storedKeyName != null ? storedKeyName : "")) {
                rawModCombo.setSelectedIndex(i);
                return;
            }
        }
        rawModCombo.setSelectedIndex(0); // default to "(none)"
    }

    private JPanel buildStepParamsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        header.setOpaque(false);
        // stepParamsLabel foreground already set by hudReadoutLabel.
        header.add(stepParamsLabel);
        stepParamsInfoButton.addActionListener(event -> showStepParamsInfo());
        header.add(stepParamsInfoButton);
        panel.add(header, BorderLayout.NORTH);

        HudTable.style(stepParamsTable);
        stepParamsTable.setRowHeight(24);
        JScrollPane scroll = HudTable.scrollPane(stepParamsTable);
        scroll.setPreferredSize(new Dimension(0, 90));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.setOpaque(false);
        JButton add = AppTheme.makeButtonSubtle(getText("actions.customCommands.editor.step.params.add"));
        add.addActionListener(e -> stepParamsModel.addRow(new Object[]{"", ""}));
        JButton remove = AppTheme.makeButtonSubtle(getText("actions.customCommands.editor.step.params.remove"));
        remove.addActionListener(e -> {
            int row = stepParamsTable.getSelectedRow();
            if (row >= 0) {
                if (stepParamsTable.isEditing()) stepParamsTable.getCellEditor().cancelCellEditing();
                stepParamsModel.removeRow(row);
                stepParamsPanel.setVisible(shouldShowStepParamsPanel(selectedType() == CustomCommandStep.Type.RUN_COMMAND));
                packPreservingWidth();
            }
        });
        buttons.add(add);
        buttons.add(remove);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private static void styleInfoButton(JButton button) {
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setForeground(AppTheme.FG_MUTED);
        button.setBackground(AppTheme.BG);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(button.getFont().deriveFont(Font.PLAIN, AppTheme.HUD_FONT_ICON_BUTTON));
        Dimension size = new Dimension(28, 28);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
    }

    private void showStepParamsInfo() {
        JOptionPane.showMessageDialog(
                this,
                getText("actions.customCommands.editor.step.params.hint"),
                getText("actions.customCommands.editor.step.params"),
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void packPreservingWidth() {
        int width = getWidth() > 0 ? getWidth() : DIALOG_MIN_WIDTH;
        pack();
        setSize(Math.max(width, DIALOG_MIN_WIDTH), getHeight());
        revalidate();
        repaint();
    }

    /** Carries the stored uppercase key name and a human-readable display label for the modifier combo. */
    private record RawModOption(String key, String label) {
        @Override
        public String toString() { return label; }
    }

    private static Set<String> customCommandParameterNames(List<CustomCommandParameterSpec> parameters) {
        if (parameters == null || parameters.isEmpty()) { return Set.of(); }
        Set<String> names = new LinkedHashSet<>();
        parameters.stream()
                .map(CustomCommandParameterSpec::getName)
                .filter(name -> name != null && !name.isBlank())
                .forEach(names::add);
        return Set.copyOf(names);
    }
}
