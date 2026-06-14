package elite.intel.ui.view;

import elite.intel.ai.hands.BindingModifier;
import elite.intel.ai.hands.BindingSlotType;
import elite.intel.ai.hands.KeyBindingsParser;
import elite.intel.ai.hands.KeyboardKeyAvailabilityService;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.view.AppTheme.*;

/**
 * Modal selector for one keyboard binding edit.
 * <p>
 * The dialog only returns the selected slot and key token (or clear request);
 * all file validation and XML writing remain in {@code BindingsWriter}.
 */
public class AssignKeyboardBindingDialog extends JDialog {
    private final KeyboardKeyAvailabilityService availabilityService;
    private final BindingSlotDisplayFormatter slotFormatter = new BindingSlotDisplayFormatter();
    private final Path bindingsFile;
    private final String bindingId;
    private final BindingSlotType slotType;
    private final KeyBindingsParser.ReadOnlyBindingSlot currentSlot;
    private final String originalKey;
    private final BindingModifier originalModifier;
    private final boolean alreadyCleared;
    private final HudComboBox<KeyOption> keyCombo;
    private final HudComboBox<ModifierOption> modifierCombo;
    private final JButton saveButton;
    private final JLabel noFreeKeysLabel;
    private final JLabel alreadyInUseLabel;
    private boolean refreshingOptions;
    private AssignKeyboardBindingSelection selection;

    public AssignKeyboardBindingDialog(
            Component parent,
            Path bindingsFile,
            String bindingId,
            BindingSlotType slotType,
            KeyBindingsParser.ReadOnlyBindingSlot currentSlot,
            KeyboardKeyAvailabilityService availabilityService
    ) {
        super(SwingUtilities.getWindowAncestor(parent), getText("bindings.assign.dialogTitle"), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.bindingsFile = bindingsFile;
        this.bindingId = bindingId;
        this.slotType = slotType;
        this.currentSlot = currentSlot;
        this.availabilityService = availabilityService;
        this.originalKey = currentKeyboardKey(currentSlot);
        this.originalModifier = currentSupportedModifier(currentSlot);
        this.alreadyCleared = isClearedSlot(currentSlot);
        this.keyCombo = new HudComboBox<>(new DefaultComboBoxModel<KeyOption>(), KeyOption::label, KeyOption::placeholder);
        this.modifierCombo = new HudComboBox<>(new DefaultComboBoxModel<ModifierOption>(), ModifierOption::label);
        this.saveButton = makeButton(getText("button.save"));
        this.noFreeKeysLabel = new JLabel(getText("bindings.assign.noFreeKeys"));
        this.alreadyInUseLabel = new JLabel(getText("bindings.assign.alreadyInUse"));
        buildUi();
        refreshAvailableKeys();
    }

    public Optional<AssignKeyboardBindingSelection> showDialog() {
        setVisible(true);
        return Optional.ofNullable(selection);
    }

    private void buildUi() {
        JPanel content = transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = baseGbc();
        gbc.insets = new Insets(3, 6, 3, 6);

        addLabel(content, getText("bindings.assign.selectedBinding"), gbc);
        addValue(content, bindingId, gbc);

        nextRow(gbc);
        addLabel(content, getText("bindings.assign.slot"), gbc);
        addValue(content, slotLabel(), gbc);

        nextRow(gbc);
        addLabel(content, getText("bindings.assign.currentValue"), gbc);
        addValue(content, slotFormatter.formatSlot(currentSlot), gbc);

        nextRow(gbc);
        addLabel(content, getText("bindings.assign.newKey"), gbc);
        keyCombo.addActionListener(e -> {
            if (!refreshingOptions) {
                resetModifierWhenClearing();
            }
            updateSaveState();
        });
        addField(content, keyCombo, gbc, 1, 1.0);

        nextRow(gbc);
        addLabel(content, getText("bindings.assign.modifier"), gbc);
        modifierCombo.addActionListener(e -> {
            if (!refreshingOptions) {
                KeyOption selectedKey = (KeyOption) keyCombo.getSelectedItem();
                refreshAvailableKeys(selectedKey == null || selectedKey.clear() ? null : selectedKey.rawKey());
            }
        });
        addField(content, modifierCombo, gbc, 1, 1.0);

        nextRow(gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        noFreeKeysLabel.setForeground(FG_MUTED);
        content.add(noFreeKeysLabel, gbc);

        nextRow(gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        alreadyInUseLabel.setForeground(HUD_DANGER);
        alreadyInUseLabel.setFont(alreadyInUseLabel.getFont().deriveFont(Font.BOLD));
        alreadyInUseLabel.setVisible(false);
        content.add(alreadyInUseLabel, gbc);

        HudSection bindingSection = HudSection.flat(
                getText("bindings.assign.section.assignment"), new BorderLayout());
        bindingSection.body().add(content, BorderLayout.CENTER);

        JButton cancelButton = makeButtonSubtle(getText("button.back"));
        cancelButton.addActionListener(e -> dispose());
        saveButton.addActionListener(e -> saveSelection());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("bindings.assign.dialogTitle"))
                .onClose(this::dispose)
                .body(bindingSection)
                .scrollBody(false)
                .primary(saveButton)          // right side
                .dismiss(cancelButton)        // left side
                .build();

        setContentPane(AppTheme.hudModalScaffold(spec));

        getRootPane().setDefaultButton(saveButton);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        if (getWidth() < 620) {
            setSize(620, getHeight());
        }
        setMinimumSize(new Dimension(620, getHeight()));
        setLocationRelativeTo(getOwner());
        setResizable(false);
    }

    private void addValue(JPanel panel, String value, GridBagConstraints gbc) {
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel field = hudReadoutValue(value, FG);
        panel.add(field, gbc);
    }

    private void refreshAvailableKeys() {
        refreshAvailableKeys(originalKey);
    }

    private void refreshAvailableKeys(String preferredKey) {
        refreshingOptions = true;
        keyCombo.removeAllItems();
        try {
            if (modifierCombo.getItemCount() == 0) {
                populateModifierOptions();
                selectOriginalModifier();
            }
            if (originalKey == null && !alreadyCleared) {
                keyCombo.addItem(KeyOption.placeholder(slotFormatter.formatSlot(currentSlot)));
            }
            keyCombo.addItem(KeyOption.clear(getText("bindings.status.notDefined")));

            List<String> keys = availabilityService.availableKeys(
                    bindingsFile,
                    bindingId,
                    slotType,
                    selectedModifier()
            );
            if (preferredKey != null && !keys.contains(preferredKey)) {
                // Keep an occupied selected chord visible; the disabled Save button explains the conflict.
                keyCombo.addItem(KeyOption.key(preferredKey, formatKeyToken(preferredKey)));
            }
            for (String key : keys) {
                keyCombo.addItem(KeyOption.key(key, formatKeyToken(key)));
            }

            selectKey(preferredKey);
            boolean canChange = containsDifferentKey();
            keyCombo.setEnabled(canChange || originalKey != null);
            modifierCombo.setEnabled(keyCombo.isEnabled());
            noFreeKeysLabel.setVisible(!canChange);
        } catch (Exception e) {
            keyCombo.setEnabled(false);
            modifierCombo.setEnabled(false);
            noFreeKeysLabel.setVisible(true);
        } finally {
            refreshingOptions = false;
        }
        updateSaveState();
        revalidate();
        repaint();
    }

    private void populateModifierOptions() {
        modifierCombo.removeAllItems();
        modifierCombo.addItem(ModifierOption.none());
            BindingModifier.supportedKeyboardModifiers().forEach(modifier ->
                modifierCombo.addItem(ModifierOption.modifier(modifier, slotFormatter.formatBindingToken(modifier.key()))));
    }

    private void saveSelection() {
        KeyOption selectedKey = (KeyOption) keyCombo.getSelectedItem();
        if (selectedKey == null || selectedKey.placeholder() || !isChanged(selectedKey)) {
            return;
        }

        ModifierOption selectedModifier = (ModifierOption) modifierCombo.getSelectedItem();
        BindingModifier modifier = selectedKey.clear() || selectedModifier == null ? null : selectedModifier.modifier();
        selection = new AssignKeyboardBindingSelection(slotType, selectedKey.rawKey(), modifier);
        dispose();
    }

    private void updateSaveState() {
        KeyOption selectedKey = (KeyOption) keyCombo.getSelectedItem();
        modifierCombo.setEnabled(keyCombo.isEnabled());
        boolean occupied = selectedKey != null && isSelectedCombinationOccupied(selectedKey);
        alreadyInUseLabel.setVisible(occupied);
        saveButton.setEnabled(selectedKey != null
                && !selectedKey.placeholder()
                && isChanged(selectedKey)
                && !occupied);
    }

    private boolean containsDifferentKey() {
        for (int i = 0; i < keyCombo.getItemCount(); i++) {
            KeyOption option = keyCombo.getItemAt(i);
            if (!option.placeholder() && isChanged(option)) {
                return true;
            }
        }
        return false;
    }

    private boolean isChanged(KeyOption selectedKey) {
        if (selectedKey.clear()) {
            return !alreadyCleared;
        }
        String key = selectedKey.rawKey();
        boolean keyChanged = originalKey == null ? key != null : !originalKey.equals(key);
        ModifierOption selectedModifier = (ModifierOption) modifierCombo.getSelectedItem();
        BindingModifier modifier = selectedModifier == null ? null : selectedModifier.modifier();
        boolean modifierChanged = originalModifier == null ? modifier != null : !originalModifier.equals(modifier);
        return keyChanged || modifierChanged;
    }

    private boolean isSelectedCombinationOccupied(KeyOption selectedKey) {
        if (selectedKey.clear() || selectedKey.rawKey() == null) {
            return false;
        }
        try {
            return availabilityService.isKeyOccupiedByOtherSlot(
                    bindingsFile,
                    bindingId,
                    slotType,
                    selectedKey.rawKey(),
                    selectedModifier()
            );
        } catch (Exception e) {
            return true;
        }
    }

    private void resetModifierWhenClearing() {
        KeyOption selectedKey = (KeyOption) keyCombo.getSelectedItem();
        if (selectedKey == null || !selectedKey.clear()) {
            return;
        }
        for (int i = 0; i < modifierCombo.getItemCount(); i++) {
            ModifierOption option = modifierCombo.getItemAt(i);
            if (option.modifier() == null) {
                modifierCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectKey(String key) {
        for (int i = 0; i < keyCombo.getItemCount(); i++) {
            KeyOption option = keyCombo.getItemAt(i);
            if ((key == null && option.rawKey() == null)
                    || (key != null && key.equals(option.rawKey()))) {
                keyCombo.setSelectedIndex(i);
                return;
            }
        }
        if (keyCombo.getItemCount() > 0) {
            keyCombo.setSelectedIndex(0);
        }
    }

    private void selectOriginalModifier() {
        for (int i = 0; i < modifierCombo.getItemCount(); i++) {
            ModifierOption option = modifierCombo.getItemAt(i);
            if ((originalModifier == null && option.modifier() == null)
                    || (originalModifier != null && originalModifier.equals(option.modifier()))) {
                modifierCombo.setSelectedIndex(i);
                return;
            }
        }
        if (modifierCombo.getItemCount() > 0) {
            modifierCombo.setSelectedIndex(0);
        }
    }

    private String currentKeyboardKey(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        if (slot == null || !"Keyboard".equals(slot.device()) || slot.key() == null
                || slot.key().isBlank() || "{NoDevice}".equals(slot.key()) || "Key_".equals(slot.key())) {
            return null;
        }
        return slot.key();
    }

    private BindingModifier currentSupportedModifier(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        if (slot == null || slot.bindingModifiers().size() != 1) {
            return null;
        }
        BindingModifier modifier = slot.bindingModifiers().get(0);
        return modifier.isSupportedKeyboardModifier() ? modifier : null;
    }

    private BindingModifier selectedModifier() {
        ModifierOption option = (ModifierOption) modifierCombo.getSelectedItem();
        return option == null ? null : option.modifier();
    }

    private boolean isClearedSlot(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        return slot == null || ("{NoDevice}".equals(slot.device())
                && (slot.key() == null || slot.key().isBlank()));
    }

    private String slotLabel() {
        return slotType == BindingSlotType.PRIMARY
                ? getText("bindings.column.primary")
                : getText("bindings.column.secondary");
    }

    private String formatKeyToken(String token) {
        if (token.startsWith("Key_") && token.length() > "Key_".length()) {
            return getText("bindings.assign.keyDisplay", slotFormatter.formatBindingToken(token), token);
        }
        return token;
    }

    private record KeyOption(String rawKey, String label, boolean placeholder, boolean clear) {
        private static KeyOption placeholder(String label) {
            return new KeyOption(null, label, true, false);
        }

        private static KeyOption clear(String label) {
            return new KeyOption(null, label, false, true);
        }

        private static KeyOption key(String rawKey, String label) {
            return new KeyOption(rawKey, label, false, false);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record ModifierOption(BindingModifier modifier, String label) {
        private static ModifierOption none() {
            return new ModifierOption(null, getText("bindings.assign.modifier.none"));
        }

        private static ModifierOption modifier(BindingModifier modifier, String label) {
            return new ModifierOption(modifier, label);
        }

        @Override
        public String toString() {
            return label;
        }
    }

}
