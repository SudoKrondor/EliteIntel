package elite.intel.ui.dialog;

import elite.intel.ai.hands.*;
import elite.intel.ui.support.AssignKeyboardBindingSelection;
import elite.intel.ui.support.BindingSlotDisplayFormatter;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.*;
import elite.intel.util.KeyCaptureMapper;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.*;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_DANGER;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT;

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
    private final JLabel conflictLabel;
    private final JLabel reservedLabel;
    private final Map<String, KeyBindingsParser.KeyBinding> existingBindings;
    /**
     * Bindings this one already collides with on load (its current chord), shown as a red banner. Empty if none.
     */
    private final Set<String> existingConflicts;
    /**
     * The shared chord that puts this binding into its current conflict; only meaningful when {@link #existingConflicts} is non-empty.
     */
    private final String existingConflictChord;
    private final KeyboardAvailabilityView keyboardView;
    private final Set<String> dialogHeldModifiers = new LinkedHashSet<>();
    private KeyEventDispatcher modifierTracker;

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
            KeyboardKeyAvailabilityService availabilityService,
            Map<String, KeyBindingsParser.KeyBinding> existingBindings,
            Set<String> existingConflicts,
            String existingConflictChord
    ) {
        super(SwingUtilities.getWindowAncestor(parent), getText("bindings.assign.dialogTitle"), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.bindingsFile = bindingsFile;
        this.bindingId = bindingId;
        this.slotType = slotType;
        this.currentSlot = currentSlot;
        this.availabilityService = availabilityService;
        this.existingBindings = existingBindings == null ? Map.of() : existingBindings;
        this.existingConflicts = existingConflicts == null ? Set.of() : existingConflicts;
        this.existingConflictChord = existingConflictChord == null ? "" : existingConflictChord;
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
        this.conflictLabel = new JLabel(getText("bindings.assign.conflict"));
        this.reservedLabel = new JLabel(getText("bindings.assign.reserved"));
        this.keyboardView = new KeyboardAvailabilityView(bindingId, this.existingBindings);
        this.keyboardView.setCurrentKey(originalKey);
        buildUi();
        updateSaveState();
    }

    public Optional<AssignKeyboardBindingSelection> showDialog() {
        installModifierTracker();
        try {
            setVisible(true);
        } finally {
            removeModifierTracker();
        }
        return Optional.ofNullable(selection);
    }

    /**
     * Tracks modifier key presses for the whole time the dialog is open — not just while the
     * capture field is armed — so the keyboard map lights up the moment the user holds a modifier,
     * helping them decide before committing. Never consumes events, so capture and dialog keys work.
     */
    private void installModifierTracker() {
        if (modifierTracker != null) {
            return;
        }
        modifierTracker = e -> {
            if (KeyCaptureMapper.isModifierOnly(e)) {
                KeyCaptureMapper.fromKeyEvent(e).ifPresent(token -> {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        dialogHeldModifiers.add(token);
                    } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                        dialogHeldModifiers.remove(token);
                    }
                });
                keyboardView.setHeldModifiers(dialogHeldModifiers);
            }
            return false;
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(modifierTracker);
    }

    private void removeModifierTracker() {
        if (modifierTracker != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(modifierTracker);
            modifierTracker = null;
            dialogHeldModifiers.clear();
        }
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
        // Natural size, left-aligned, with vertical insets so the button's painted border is not
        // clipped (a zero-gap FlowLayout wrapper used to cut it off top/bottom).
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 6, 5, 6);
        clearButton.addActionListener(e -> onClear());
        content.add(clearButton, gbc);
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        nextRow(gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        HudBanner hint = HudBanner.multiline(getText("bindings.assign.capture.hint"), StatusBadge.State.INFO);
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

        nextRow(gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        conflictLabel.setVisible(false);
        content.add(conflictLabel, gbc);

        nextRow(gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        reservedLabel.setForeground(HUD_COLOR_ROLE_DANGER);
        reservedLabel.setFont(reservedLabel.getFont().deriveFont(Font.BOLD));
        reservedLabel.setVisible(false);
        content.add(reservedLabel, gbc);

        // Live QWERTY availability map: spans both columns, recolors as modifiers are held.
        nextRow(gbc);
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 6, 0, 6);
        HudBanner keyboardHint = HudBanner.multiline(getText("bindings.assign.keyboard.hint"), StatusBadge.State.INFO);
        content.add(keyboardHint, gbc);

        nextRow(gbc);
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 6, 3, 6);
        content.add(keyboardView, gbc);
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 6, 3, 6);

        // This binding already collides with another: state it inline, in red, rather than relying on
        // the table's hover callout (which is unreachable while this modal is open). Same banner theme
        // as the blue hints above, only the danger colour.
        if (!existingConflicts.isEmpty()) {
            nextRow(gbc);
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(8, 6, 3, 6);
            String partners = String.join(", ",
                    existingConflicts.stream().map(StringUtls::humanizeBindingName).toList());
            HudBanner existingConflictBanner = HudBanner.multiline(
                    getText("bindings.conflict.popup.title", existingConflictChord) + " " + partners,
                    StatusBadge.State.OFFLINE);
            content.add(existingConflictBanner, gbc);
            gbc.gridwidth = 1;
            gbc.insets = new Insets(3, 6, 3, 6);
        }

        // Absorb extra vertical space at the bottom so content top-aligns (removes the centred
        // top gap), and a conflict message appearing shrinks this filler rather than the keyboard.
        nextRow(gbc);
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        content.add(Box.createGlue(), gbc);
        gbc.weighty = 0;
        gbc.gridwidth = 1;

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
        boolean blockingConflict = selectedConflict() != null;
        if (!isChanged() || !isValidKey() || isSelectedCombinationOccupied() || blockingConflict || isReservedChord()) {
            return;
        }
        selection = cleared
                ? new AssignKeyboardBindingSelection(slotType, null, List.of())
                : new AssignKeyboardBindingSelection(slotType, selectedKey, selectedModifiers);
        dispose();
    }

    private void updateSaveState() {
        boolean invalidKey = !cleared && selectedKey != null && !isValidKey();
        boolean reserved = isReservedChord();
        // A reserved chord is unassignable on principle; an exact occupancy shows its own message.
        // Suppress the lesser warnings when a higher-priority one already explains the block.
        boolean occupied = !reserved && !cleared && selectedKey != null && isSelectedCombinationOccupied();
        BindingConflictScanner.CandidateConflict conflict = (reserved || occupied) ? null : selectedConflict();
        invalidKeyLabel.setVisible(invalidKey);
        reservedLabel.setVisible(reserved);
        alreadyInUseLabel.setVisible(occupied);
        updateConflictLabel(conflict);
        saveButton.setEnabled(isChanged() && isValidKey() && !reserved && !occupied && conflict == null);
    }

    /**
     * True when the pending chord is reserved by the OS (e.g. Alt+F4) and must not be assigned.
     */
    private boolean isReservedChord() {
        if (cleared || selectedKey == null || !isValidKey()) {
            return false;
        }
        List<String> modifierKeys = selectedModifiers.stream().map(BindingModifier::key).toList();
        return ReservedKeyChords.isReserved(selectedKey, modifierKeys);
    }

    private void updateConflictLabel(BindingConflictScanner.CandidateConflict conflict) {
        if (conflict == null) {
            conflictLabel.setVisible(false);
            return;
        }
        // Name the binding it collides with so the user knows what the conflict is.
        conflictLabel.setText(getText("bindings.assign.conflict",
                StringUtls.humanizeBindingName(conflict.otherBinding())));
        conflictLabel.setForeground(HUD_COLOR_ROLE_DANGER);
        conflictLabel.setVisible(true);
    }

    /**
     * The conflict the pending chord would create against another binding (its own slot excluded),
     * naming the binding it collides with, or {@code null} if clean. Drives the label and save gating.
     */
    private BindingConflictScanner.CandidateConflict selectedConflict() {
        if (cleared || selectedKey == null || !isValidKey()) {
            return null;
        }
        List<String> modifierKeys = selectedModifiers.stream().map(BindingModifier::key).toList();
        return BindingConflictScanner.candidateConflict(bindingId, selectedKey, modifierKeys, existingBindings);
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
