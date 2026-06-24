package elite.intel.ui.dialog;

import elite.intel.ai.hands.*;
import elite.intel.ui.support.AssignKeyboardBindingSelection;
import elite.intel.ui.support.BindingSlotDisplayFormatter;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.KeyChordCaptureField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.*;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * Modal selector for one keyboard binding edit.
 * <p>
 * The chord is captured by pressing the actual key combination (any number of
 * supported keyboard modifiers, left/right distinct) rather than picking from
 * drop-downs. The dialog only returns the selected slot, key token, and modifier
 * list (or a clear request); all file validation and XML writing remain in
 * {@code BindingsWriter}.
 */
public class AssignKeyboardBindingDialog extends JDialog {
    private static final Logger log = LogManager.getLogger(AssignKeyboardBindingDialog.class);

    private final KeyboardKeyAvailabilityService availabilityService;
    private final BindingSlotDisplayFormatter slotFormatter = new BindingSlotDisplayFormatter();
    private final Path bindingsFile;
    private final String bindingId;
    private final BindingSlotType slotType;
    private final KeyBindingsParser.ReadOnlyBindingSlot currentSlot;
    private final String originalKey;
    private final List<BindingModifier> originalModifiers;
    private final boolean alreadyCleared;
    private final KeyChordCaptureField captureField;
    private final JButton clearButton;
    private final JButton saveButton;
    private final JLabel invalidKeyLabel;
    private final JLabel alreadyInUseLabel;

    // Pending selection (mirrors what Save would persist).
    private String selectedKey;
    private List<BindingModifier> selectedModifiers;
    private boolean cleared;

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
        this.originalModifiers = currentSupportedModifiers(currentSlot);
        this.alreadyCleared = isClearedSlot(currentSlot);

        this.selectedKey = originalKey;
        this.selectedModifiers = new ArrayList<>(originalModifiers);
        this.cleared = alreadyCleared;

        this.captureField = new KeyChordCaptureField(
                currentChordText(),
                getText("bindings.assign.capture.prompt"),
                slotFormatter::formatBindingToken,
                this::onChordCaptured
        );
        this.clearButton = makeButtonSubtle(getText("bindings.assign.capture.clear"));
        this.saveButton = makeButton(getText("button.save"));
        this.invalidKeyLabel = new JLabel(getText("bindings.assign.unknownKey"));
        this.alreadyInUseLabel = new JLabel(getText("bindings.assign.alreadyInUse"));
        buildUi();
        updateSaveState();
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
        addField(content, captureField, gbc, 1, 1.0);

        nextRow(gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        clearButton.addActionListener(e -> onClear());
        JPanel clearRow = transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        clearRow.add(clearButton);
        content.add(clearRow, gbc);

        nextRow(gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel hint = new JLabel(getText("bindings.assign.capture.hint"));
        hint.setForeground(HUD_COLOR_ROLE_SECONDARY_TEXT);
        content.add(hint, gbc);

        nextRow(gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        invalidKeyLabel.setForeground(HUD_COLOR_ROLE_DANGER);
        invalidKeyLabel.setVisible(false);
        content.add(invalidKeyLabel, gbc);

        nextRow(gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        alreadyInUseLabel.setForeground(HUD_COLOR_ROLE_DANGER);
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

        // The capture field consumes Enter while armed; otherwise Save is the default.
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
        JLabel field = hudReadoutValue(value, HUD_COLOR_ROLE_PRIMARY_TEXT);
        panel.add(field, gbc);
    }

    private void onChordCaptured(KeyChordCaptureField.CapturedChord chord) {
        cleared = false;
        selectedKey = chord.key();
        selectedModifiers = new ArrayList<>(chord.modifiers());
        updateSaveState();
    }

    private void onClear() {
        cleared = true;
        selectedKey = null;
        selectedModifiers = new ArrayList<>();
        captureField.showIdleText(getText("bindings.status.notDefined"));
        updateSaveState();
    }

    private void saveSelection() {
        if (!isChanged() || !isValidKey() || isSelectedCombinationOccupied()) {
            return;
        }
        selection = cleared
                ? new AssignKeyboardBindingSelection(slotType, null, List.of())
                : new AssignKeyboardBindingSelection(slotType, selectedKey, selectedModifiers);
        dispose();
    }

    private void updateSaveState() {
        boolean invalidKey = !cleared && selectedKey != null && !isValidKey();
        boolean occupied = !cleared && selectedKey != null && isSelectedCombinationOccupied();
        invalidKeyLabel.setVisible(invalidKey);
        alreadyInUseLabel.setVisible(occupied);
        saveButton.setEnabled(isChanged() && isValidKey() && !occupied);
    }

    private boolean isChanged() {
        if (cleared) {
            return !alreadyCleared;
        }
        if (selectedKey == null) {
            return false;
        }
        boolean keyChanged = !selectedKey.equals(originalKey);
        boolean modifiersChanged = !new HashSet<>(selectedModifiers).equals(new HashSet<>(originalModifiers));
        return keyChanged || modifiersChanged;
    }

    private boolean isValidKey() {
        return cleared || (selectedKey != null && EliteKeyboardKeys.isAssignable(selectedKey));
    }

    private boolean isSelectedCombinationOccupied() {
        if (cleared || selectedKey == null) {
            return false;
        }
        try {
            return availabilityService.isKeyOccupiedByOtherSlot(
                    bindingsFile,
                    bindingId,
                    slotType,
                    selectedKey,
                    selectedModifiers
            );
        } catch (Exception e) {
            // WHY: an unreadable/unparseable .binds must never be reported as free,
            // or we would offer a save that could silently clobber another binding.
            log.warn("Could not check key availability for binding '{}' slot {}: {}",
                    bindingId, slotType, e.getMessage());
            return true;
        }
    }

    private String currentChordText() {
        if (originalKey == null) {
            return getText("bindings.status.notDefined");
        }
        return slotFormatter.formatChord(originalModifiers, originalKey);
    }

    private String currentKeyboardKey(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        if (slot == null || !"Keyboard".equals(slot.device()) || slot.key() == null
                || slot.key().isBlank() || "{NoDevice}".equals(slot.key()) || "Key_".equals(slot.key())) {
            return null;
        }
        return slot.key();
    }

    private List<BindingModifier> currentSupportedModifiers(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        if (slot == null) {
            return List.of();
        }
        return slot.bindingModifiers().stream()
                .filter(BindingModifier::isSupportedKeyboardModifier)
                .toList();
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
}
