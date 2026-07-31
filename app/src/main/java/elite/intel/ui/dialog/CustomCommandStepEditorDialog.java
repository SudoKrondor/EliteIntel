package elite.intel.ui.dialog;

import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandStep;
import elite.intel.ai.hands.BindingModifier;
import elite.intel.ai.hands.KeyBindingExecutor;
import elite.intel.ui.support.BindingSlotDisplayFormatter;
import elite.intel.ui.support.CustomCommandStepPickerItem;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudForms;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Modal editor for one custom command step.
 */
public final class CustomCommandStepEditorDialog extends JDialog {

    private static final int DIALOG_MIN_WIDTH = 760;

    // typeCombo: labelFn drives localized step-type display text via stepTypeLabel().
    private final HudComboBox<CustomCommandStep.Type> typeCombo =
            new HudComboBox<>(CustomCommandStep.Type.values(),
                    CustomCommandStepEditorDialog::stepTypeLabel);

    private final JLabel valueLabel   = AppTheme.hudReadoutLabel("");
    private final HudTextField valueField = new HudTextField();

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
    private final HudStepper durationStepper = new HudStepper(0, Integer.MAX_VALUE, 50, 250);
    private CustomCommandStep result;

    CustomCommandStepEditorDialog(Component parent, CustomCommandStep step) {
        super(SwingUtilities.getWindowAncestor(parent), getText("actions.customCommands.editor.step.title"), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        populate(step);
        buildUi();
    }

    CustomCommandStep showDialog() {
        setVisible(true);
        return result;
    }

    private void populate(CustomCommandStep step) {
        if (step == null) {
            typeCombo.setSelectedItem(CustomCommandStep.Type.SPEAK);
            return;
        }
        typeCombo.setSelectedItem(step.getType());
        durationStepper.setValue(Math.max(0, step.getDurationMs()));   // HudStepper.setValue(int)
        switch (step.getType()) {
            case SPEAK -> valueField.setText(step.getText());
            case BINDING_TAP, BINDING_HOLD ->
                    selectPickerItem(bindingCombo, bindingItems, step.getBindingId(), getText("actions.customCommands.editor.step.unknownBinding"));
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
                .scrollBody(false)
                .primary(save)                // right side
                .dismiss(back)                // left side
                .build();

        setContentPane(AppTheme.hudModalScaffold(spec));

        typeCombo.addActionListener(event -> updateFieldsForType());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(save);
        updateFieldsForType();   // apply type-specific fields before measuring so centering is final
        pack();
        setMinimumSize(new Dimension(DIALOG_MIN_WIDTH, 260));
        setSize(Math.max(getWidth(), DIALOG_MIN_WIDTH), getHeight());
        setLocationRelativeTo(getOwner());
    }

    private JPanel form() {
        JPanel panel = AppTheme.transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = HudForms.baseGbc();

        addRow(panel, gbc, getText("actions.customCommands.editor.step.type"), typeCombo);
        addRow(panel, gbc, valueLabel, valueField);
        addRow(panel, gbc, bindingLabel, bindingCombo);
        addRow(panel, gbc, rawKeyLabel, rawKeyCombo);
        addRow(panel, gbc, rawModLabel, rawModCombo);
        addRow(panel, gbc, durationLabel, durationStepper);
        AppTheme.applyDarkPalette(panel);
        // HudTextField and HudComboBox self-style via their constructors - no manual styleComboBox calls needed.
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
        HudForms.sizeFieldLabel(label, 160);
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        // Dialog owns only the uniform field WIDTH (so every row's right edge aligns); HEIGHT comes from
        // the control itself (HUD layer) - HudTextField/HudStepper/HudComboBox all size to HUD_FIELD_HEIGHT.
        Dimension fieldSize = new Dimension(HudPalette.HUD_PICKER_FIELD_WIDTH, field.getPreferredSize().height);
        field.setPreferredSize(fieldSize);
        field.setMinimumSize(fieldSize);
        panel.add(field, gbc);
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.CENTER;
    }

    private void updateFieldsForType() {
        CustomCommandStep.Type type = selectedType();
        boolean hasText    = type == CustomCommandStep.Type.SPEAK;
        boolean hasBinding = type == CustomCommandStep.Type.BINDING_TAP || type == CustomCommandStep.Type.BINDING_HOLD;
        boolean isRawKey   = type == CustomCommandStep.Type.RAW_KEY;
        boolean hasDuration = type == CustomCommandStep.Type.BINDING_HOLD || type == CustomCommandStep.Type.DELAY || isRawKey;

        valueLabel.setVisible(hasText);
        valueField.setVisible(hasText);
        bindingLabel.setVisible(hasBinding);
        bindingCombo.setVisible(hasBinding);
        rawKeyLabel.setVisible(isRawKey);
        rawKeyCombo.setVisible(isRawKey);
        rawModLabel.setVisible(isRawKey);
        rawModCombo.setVisible(isRawKey);
        durationLabel.setVisible(hasDuration);
        durationStepper.setVisible(hasDuration);

        // hudReadoutLabel caps only at construction time; setText must uppercase explicitly.
        valueLabel.setText(getText("actions.customCommands.editor.step.text").toUpperCase());
        packPreservingWidth();
    }

    private void save() {
        CustomCommandStep step = buildStep();
        try {
            step.validate(0);
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return;
        }
        result = step;
        dispose();
    }

    private CustomCommandStep buildStep() {
        CustomCommandStep.Type type = selectedType();
        String value = valueField.getText().trim();
        int duration = durationStepper.getValue();
        return switch (type) {
            case SPEAK -> new CustomCommandStep(type, null, 0, value);
            case BINDING_TAP -> new CustomCommandStep(type, selectedPickerId(bindingCombo), 0, null);
            case BINDING_HOLD -> new CustomCommandStep(type, selectedPickerId(bindingCombo), duration, null);
            case DELAY -> new CustomCommandStep(type, null, duration, null);
            case RAW_KEY -> {
                String rawKey = selectedPickerId(rawKeyCombo);
                RawModOption modOption = (RawModOption) rawModCombo.getSelectedItem();
                String rawMod = (modOption != null && !modOption.key().isBlank()) ? modOption.key() : null;
                yield new CustomCommandStep(type, null, duration, null, rawKey, rawMod);
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
    public static String stepTypeLabel(CustomCommandStep.Type type) {
        if (type == null) return "";
        return switch (type) {
            case BINDING_TAP  -> getText("actions.customCommands.editor.step.type.bindingTap");
            case BINDING_HOLD -> getText("actions.customCommands.editor.step.type.bindingHold");
            case DELAY        -> getText("actions.customCommands.editor.step.type.delay");
            case SPEAK        -> getText("actions.customCommands.editor.step.type.speak");
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

    private void packPreservingWidth() {
        int width = getWidth() > 0 ? getWidth() : DIALOG_MIN_WIDTH;
        // Keep the dialog's center fixed across runtime resizes so it doesn't drift off-centre.
        Point center = isShowing()
                ? new Point(getX() + getWidth() / 2, getY() + getHeight() / 2)
                : null;
        pack();
        setSize(Math.max(width, DIALOG_MIN_WIDTH), getHeight());
        if (center != null) {
            setLocation(center.x - getWidth() / 2, center.y - getHeight() / 2);
        }
        revalidate();
        repaint();
    }

    /** Carries the stored uppercase key name and a human-readable display label for the modifier combo. */
    private record RawModOption(String key, String label) {
        @Override
        public String toString() { return label; }
    }
}
