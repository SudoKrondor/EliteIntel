package elite.intel.ui.dialog;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.devicefiles.SavedDeviceLabels;
import elite.intel.bindforge.devices.CaptureControllers;
import elite.intel.bindforge.devices.ControllerCapture;
import elite.intel.bindforge.io.KeyBindingsParser;
import elite.intel.bindforge.model.*;
import elite.intel.bindforge.rules.BindingConflictScanner;
import elite.intel.bindforge.rules.KeyboardKeyAvailabilityService;
import elite.intel.bindforge.rules.ReservedKeyChords;
import elite.intel.bindforge.rules.SlotInputConflicts;
import elite.intel.devices.events.DeviceButtonEvent;
import elite.intel.devices.events.DeviceDisconnectedEvent;
import elite.intel.devices.events.DeviceHatEvent;
import elite.intel.eventbus.DeviceBus;
import elite.intel.ui.support.AssignKeyboardBindingSelection;
import elite.intel.ui.support.AssignSelection;
import elite.intel.ui.support.AssignSlotSelection;
import elite.intel.ui.support.BindingSlotDisplayFormatter;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.*;
import elite.intel.util.KeyCaptureMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.*;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * Assign Control: the editor for one control's two slots, opened by clicking either of them in the grid.
 * <p>
 * <strong>The dialog belongs to the row, not the cell</strong> (Alan, 2026-10-15). Users read a grid row as one
 * control, so the dialog shows both slots as cards across the top - what each holds now, and what it will hold.
 * The orange card is the one being filled; clicking the other card switches to it. The clicked cell decides which
 * card starts orange.
 * <p>
 * <strong>One picker, which belongs to the orange card</strong> (Alan, 2026-10-15): KEYBOARD and a tab per
 * connected controller, opening on the device that card's slot holds. Two pickers side by side read as wired to
 * the two cards, and the game puts any device in either slot. A key pressed in the capture field, a button or hat
 * pressed on a controller - buttons already held become its modifiers - or an input clicked in a list goes into
 * the orange card, and its tab comes forward (bind-editor.md, <em>Capture Dialog</em>).
 * <p>
 * <strong>An input another control holds cannot be assigned</strong>, from either source (Alan, 2026-10-15): the
 * card names that control in red and SAVE stays dimmed until the commander has freed it there. A warning that can
 * be clicked past is a warning nobody reads.
 * <p>
 * The dialog only returns what changed; all file validation and XML writing remain in {@code BindingsWriter} -
 * the keyboard path for a keyboard edit of a keyboard or empty slot, the user-edit path for anything else.
 */
public class AssignKeyboardBindingDialog extends JDialog {
    private static final Logger log = LogManager.getLogger(AssignKeyboardBindingDialog.class);
    private static final int MIN_WIDTH = 760;
    /**
     * Room left around the dialog on a screen it would otherwise fill.
     */
    private static final int SCREEN_MARGIN = 40;

    /**
     * A slot's existing keyboard clash, as the grid found it: the chord and the controls sharing it.
     */
    public record ExistingConflict(String chord, Set<String> partners) {
    }

    private enum Kind {UNCHANGED, CLEAR, KEYBOARD, CONTROLLER}

    /**
     * One slot: what it holds, and what the commander has chosen for it so far.
     */
    private static final class SlotEdit {
        final BindingSlotType type;
        final KeyBindingsParser.ReadOnlyBindingSlot original;
        final ExistingConflict existingConflict;
        Kind kind = Kind.UNCHANGED;
        String key;
        List<BindingModifier> keyboardModifiers = List.of();
        ControllerCapture.Captured control;
        /** Why the last pick for this slot was turned away, shown on its card until the next pick. */
        String rejected;

        SlotEdit(BindingSlotType type, KeyBindingsParser.ReadOnlyBindingSlot original, ExistingConflict existingConflict) {
            this.type = type;
            this.original = original;
            this.existingConflict = existingConflict;
        }

        boolean hold() {
            return original != null && original.hold();
        }

        boolean originallyEmpty() {
            return isClearedSlot(original);
        }

        /** True when the slot holds something the keyboard path will not write over - a controller, say. */
        boolean holdsController() {
            return original != null && !original.editable() && !originallyEmpty();
        }

        /**
         * The input the slot will hold after SAVE, or {@code null} for none.
         */
        BindingInput inputAfter() {
            return switch (kind) {
                case UNCHANGED -> originallyEmpty() ? null : new BindingInput(original.device(), original.key());
                case CLEAR -> null;
                case KEYBOARD -> new BindingInput(BindingInput.KEYBOARD, key);
                case CONTROLLER -> control.input();
            };
        }

        List<BindingInput> modifiersAfter() {
            return switch (kind) {
                case UNCHANGED -> originallyEmpty() ? List.of() : original.bindingModifiers().stream()
                        .filter(m -> m.key() != null && !m.key().isBlank())
                        .map(m -> new BindingInput(m.device(), m.key())).toList();
                case CLEAR -> List.of();
                case KEYBOARD -> keyboardModifiers.stream().map(m -> new BindingInput(m.device(), m.key())).toList();
                case CONTROLLER -> control.modifiers();
            };
        }

        /**
         * Whether Vega can press what the slot will hold.
         */
        boolean keyboardAfter() {
            return switch (kind) {
                case UNCHANGED -> original != null && original.keyboardUsable();
                case KEYBOARD -> true;
                case CLEAR, CONTROLLER -> false;
            };
        }

        String keyboardKeyAfter() {
            BindingInput after = inputAfter();
            return after != null && BindingInput.KEYBOARD.equals(after.device()) ? after.key() : null;
        }

        boolean changed() {
            if (kind == Kind.UNCHANGED) return false;
            if (kind == Kind.CLEAR) return !originallyEmpty();
            if (originallyEmpty()) return true;
            BindingInput was = new BindingInput(original.device(), original.key());
            if (!was.sameInputAs(inputAfter())) return true;
            Set<String> before = new HashSet<>();
            original.bindingModifiers().stream().filter(m -> m.key() != null && !m.key().isBlank())
                    .forEach(m -> before.add(identity(m.device(), m.key())));
            Set<String> now = new HashSet<>();
            modifiersAfter().forEach(m -> now.add(identity(m.device(), m.key())));
            return !before.equals(now);
        }

        AssignSelection selection() {
            return switch (kind) {
                case CLEAR -> holdsController()
                        ? new AssignSlotSelection(type, null, List.of())
                        : new AssignKeyboardBindingSelection(type, null, List.of());
                case KEYBOARD -> holdsController()
                        // WHY: the keyboard path never writes over a controller slot, so a key on one goes
                        // through the user-edit path - after every keyboard check, exactly as on any other slot.
                        ? new AssignSlotSelection(type, inputAfter(), modifiersAfter())
                        : new AssignKeyboardBindingSelection(type, key, keyboardModifiers);
                case CONTROLLER -> new AssignSlotSelection(type, control.input(), control.modifiers());
                case UNCHANGED -> throw new IllegalStateException("An unchanged slot has nothing to save");
            };
        }

        /**
         * Back to what the slot held when the dialog opened.
         */
        void undo() {
            kind = Kind.UNCHANGED;
            key = null;
            keyboardModifiers = List.of();
            control = null;
            rejected = null;
        }

        private static String identity(String device, String key) {
            return (device + "|" + key).toLowerCase(Locale.ROOT);
        }
    }

    private final BindingSlotDisplayFormatter slotFormatter = BindingSlotDisplayFormatter.withSavedLabels();
    private final Path bindingsFile;
    private final String bindingId;
    private final KeyboardKeyAvailabilityService availabilityService;
    private final Map<String, KeyBindingsParser.BindingSlots> existingSlots;
    private final Map<String, KeyBindingsParser.ReadOnlyBindingSlots> readOnlySlots;
    /**
     * The keys the commander has on the game menu, which no other control may use. Empty when this dialog
     * is editing the game-menu control itself - that is the one binding its own key belongs to.
     */
    private final Set<String> gameMenuKeys;
    private final List<CaptureControllers.Controller> controllers;
    private final boolean vegaUsesIt;
    private final Map<BindingSlotType, SlotEdit> edits = new EnumMap<>(BindingSlotType.class);
    private final Map<BindingSlotType, SlotCard> cards = new EnumMap<>(BindingSlotType.class);
    /**
     * Every controller's panel, connected or not.
     */
    private final List<ControllerInputsPanel> panels = new ArrayList<>();
    /**
     * The connected ones, by the Device Service's id - the only ones a press can reach.
     */
    private final Map<Integer, ControllerInputsPanel> livePanels = new HashMap<>();
    private final ControllerCapture capture = new ControllerCapture();
    private final KeyChordCaptureField captureField;
    private final KeyboardAvailabilityView keyboardView;
    private final HudBanner aiAccessBanner;
    private final JButton saveButton;
    private final Set<String> dialogHeldModifiers = new LinkedHashSet<>();
    private KeyEventDispatcher modifierTracker;
    /**
     * The one picker: KEYBOARD, then a tab per connected controller. It belongs to the orange card.
     */
    private JTabbedPane sourceTabs;
    private final Map<ControllerInputsPanel, JComponent> tabOf = new HashMap<>();
    private JComponent keyboardTab;
    private BindingSlotType active;
    private List<AssignSelection> result = List.of();

    public AssignKeyboardBindingDialog(
            Component parent,
            Path bindingsFile,
            String bindingId,
            BindingSlotType startSlot,
            KeyBindingsParser.ReadOnlyBindingSlots slots,
            KeyboardKeyAvailabilityService availabilityService,
            Map<String, KeyBindingsParser.BindingSlots> existingSlots,
            Map<String, KeyBindingsParser.ReadOnlyBindingSlots> readOnlySlots,
            Map<BindingSlotType, ExistingConflict> existingConflicts,
            List<CaptureControllers.Controller> controllers,
            boolean vegaUsesIt
    ) {
        super(SwingUtilities.getWindowAncestor(parent), getText("bindings.assign.dialogTitle"), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.bindingsFile = bindingsFile;
        this.bindingId = bindingId;
        this.availabilityService = availabilityService;
        this.existingSlots = existingSlots == null ? Map.of() : existingSlots;
        this.readOnlySlots = readOnlySlots == null ? Map.of() : readOnlySlots;
        this.gameMenuKeys = ReservedKeyChords.GAME_MENU_ACTION.equals(bindingId)
                ? Set.of()
                : ReservedKeyChords.gameMenuKeysFromExecutableSlots(this.existingSlots);
        this.controllers = controllers == null ? List.of() : List.copyOf(controllers);
        this.vegaUsesIt = vegaUsesIt;
        Map<BindingSlotType, ExistingConflict> conflicts = existingConflicts == null ? Map.of() : existingConflicts;
        edits.put(BindingSlotType.PRIMARY, new SlotEdit(BindingSlotType.PRIMARY,
                slots == null ? null : slots.primary(), conflicts.get(BindingSlotType.PRIMARY)));
        edits.put(BindingSlotType.SECONDARY, new SlotEdit(BindingSlotType.SECONDARY,
                slots == null ? null : slots.secondary(), conflicts.get(BindingSlotType.SECONDARY)));
        this.active = startSlot == null ? BindingSlotType.PRIMARY : startSlot;

        this.captureField = new KeyChordCaptureField(
                getText("bindings.assign.capture.idle"),
                getText("bindings.assign.capture.prompt"),
                slotFormatter::formatBindingToken,
                this::onChordCaptured
        );
        this.keyboardView = new KeyboardAvailabilityView(bindingId, active, this.existingSlots);
        this.aiAccessBanner = HudBanner.multiline(getText("bindings.assign.aiAccessLoss"), StatusBadge.State.STANDBY);
        this.saveButton = makeButton(getText("button.save"));
        buildUi();
        keyboardView.setOwnChords(ownKeyboardChords());
        showSourceOf(edits.get(active));
        refresh(true);
    }

    /** @return the slots to write, each once - empty when the commander left with nothing changed */
    public List<AssignSelection> showDialog() {
        installModifierTracker();
        boolean listening = !livePanels.isEmpty();
        if (listening) DeviceBus.register(this);
        try {
            setVisible(true);
        } finally {
            if (listening) DeviceBus.unregister(this);
            removeModifierTracker();
        }
        return result;
    }

    // ------------------------------------------------------------------ live controller input

    @Subscribe
    public void onButton(DeviceButtonEvent event) {
        SwingUtilities.invokeLater(() -> {
            ControllerInputsPanel panel = livePanels.get(event.deviceId());
            if (panel == null) return;
            capture.button(panel.deviceName(), event.buttonIndex(), event.pressed()).ifPresent(this::pickControl);
        });
    }

    @Subscribe
    public void onHat(DeviceHatEvent event) {
        SwingUtilities.invokeLater(() -> {
            ControllerInputsPanel panel = livePanels.get(event.deviceId());
            if (panel == null) return;
            capture.hat(panel.deviceName(), event.hatIndex(), event.directions()).ifPresent(this::pickControl);
        });
    }

    @Subscribe
    public void onDisconnected(DeviceDisconnectedEvent event) {
        SwingUtilities.invokeLater(() -> {
            ControllerInputsPanel panel = livePanels.get(event.deviceId());
            if (panel != null) capture.release(panel.deviceName());
        });
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

    // ------------------------------------------------------------------ layout

    private void buildUi() {
        JPanel content = transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = baseGbc();
        gbc.insets = new Insets(3, 6, 3, 6);

        // One line: where the control lives and what the file calls it - the game has four rows called
        // Move Forward, and the tag is what a support bundle or a forum post names.
        BindingDisplayNames.ControlName control = BindingDisplayNames.lookup(bindingId);
        addLabel(content, getText("bindings.assign.control"), gbc);
        addValue(content, controlPath(control) + "   \u00B7   " + bindingId, gbc);

        // The row's two slots, side by side as in the grid.
        JPanel cardRow = transparentPanel(new GridLayout(1, 2, 12, 0));
        for (BindingSlotType type : List.of(BindingSlotType.PRIMARY, BindingSlotType.SECONDARY)) {
            SlotCard card = new SlotCard(edits.get(type));
            cards.put(type, card);
            cardRow.add(card);
        }
        wide(content, gbc, cardRow, new Insets(10, 6, 3, 6));
        wide(content, gbc, HudBanner.multiline(getText("bindings.assign.slotHint"), StatusBadge.State.INFO), null);
        aiAccessBanner.setVisible(false);
        wide(content, gbc, aiAccessBanner, null);

        // ONE picker under the cards, following the orange card (Alan, 2026-10-15): two side-by-side pickers
        // read as wired to the two cards, and the game puts any device in either slot.
        wide(content, gbc, sources(), new Insets(10, 6, 3, 6));
        if (controllers.isEmpty()) {
            wide(content, gbc, HudBanner.multiline(getText("bindings.assign.noControllers"), StatusBadge.State.INFO), null);
        }

        // Absorb extra vertical space at the bottom so content top-aligns, and a message appearing
        // shrinks this filler rather than the input lists.
        nextRow(gbc);
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        content.add(Box.createGlue(), gbc);

        HudSection bindingSection = HudSection.flat(
                getText("bindings.assign.section.assignment"), new BorderLayout());
        bindingSection.body().add(content, BorderLayout.CENTER);

        JButton cancelButton = makeButtonSubtle(getText("button.back"));
        cancelButton.addActionListener(e -> dispose());
        saveButton.addActionListener(e -> save());

        // The title is the control as the game names it - what the commander recognises (bind-editor.md,
        // What the dialog shows); CONTROL and XML BINDING beneath it say which of the game's same-named rows.
        // WHY a scrolling body: on a short screen the footer - and SAVE - must stay reachable; the body scrolls
        // under a fixed header and footer instead of the dialog running off the bottom.
        HudModalSpec spec = HudModalSpec.builder()
                .title(control.name())
                .onClose(this::dispose)
                .body(bindingSection)
                .scrollBody(true)
                .primary(saveButton)          // right side
                .dismiss(cancelButton)        // left side
                .build();

        setContentPane(AppTheme.hudModalScaffold(spec));

        // The capture field consumes Enter while armed; otherwise Save is the default.
        getRootPane().setDefaultButton(saveButton);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        fitToScreen();
        setLocationRelativeTo(getOwner());
        setResizable(false);
    }

    /**
     * At least {@link #MIN_WIDTH} wide, and never taller than the screen it opens on, less the taskbar - the body
     * scrolls instead, so SAVE stays on screen.
     */
    private void fitToScreen() {
        GraphicsConfiguration screen = getOwner() != null ? getOwner().getGraphicsConfiguration() : getGraphicsConfiguration();
        Rectangle bounds = screen.getBounds();
        Insets taskbar = Toolkit.getDefaultToolkit().getScreenInsets(screen);
        int maxHeight = bounds.height - taskbar.top - taskbar.bottom - SCREEN_MARGIN;
        int maxWidth = bounds.width - taskbar.left - taskbar.right - SCREEN_MARGIN;
        Dimension preferred = getPreferredSize();
        int width = Math.min(Math.max(preferred.width, MIN_WIDTH), maxWidth);
        int height = Math.min(preferred.height, maxHeight);
        setMinimumSize(new Dimension(width, 0));
        setSize(width, height);
    }

    private static void wide(JPanel content, GridBagConstraints gbc, JComponent component, Insets insets) {
        nextRow(gbc);
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Insets was = gbc.insets;
        if (insets != null) gbc.insets = insets;
        content.add(component, gbc);
        gbc.insets = was;
        gbc.gridwidth = 1;
    }

    /**
     * The keyboard: the chord capture field and the live availability map.
     */
    private JComponent keyboardSource() {
        JPanel source = transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = baseGbc();
        gbc.insets = new Insets(6, 6, 3, 6);
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        source.add(captureField, gbc);
        gbc.gridy++;
        source.add(HudBanner.multiline(getText("bindings.assign.capture.hint"), StatusBadge.State.INFO), gbc);
        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 0, 0);
        // The colour legend and the hold-a-modifier hint ride on the map itself, to keep the dialog short enough
        // for a laptop screen; the map shows the colours the moment a modifier is held anyway.
        keyboardView.setToolTipText(getText("bindings.assign.keyboard.hint"));
        source.add(keyboardView, gbc);
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        source.add(Box.createGlue(), gbc);
        return source;
    }

    /**
     * The picker: KEYBOARD, then a tab per connected controller - greyed for one with no name, which cannot be
     * written. Pressing a key or a controller input works from any tab and brings its own forward.
     */
    private JComponent sources() {
        sourceTabs = AppTheme.makeSectionTabs();
        keyboardTab = keyboardSource();
        sourceTabs.addTab(getText("bindings.assign.tab.keyboard"), keyboardTab);
        SavedDeviceLabels labels = SavedDeviceLabels.load();
        for (CaptureControllers.Controller controller : controllers) {
            if (!controller.named()) {
                sourceTabs.addTab(controller.tabTitle(), transparentPanel(new BorderLayout()));
                int index = sourceTabs.getTabCount() - 1;
                sourceTabs.setEnabledAt(index, false);
                sourceTabs.setToolTipTextAt(index, getText("bindings.assign.controller.unnamed"));
                continue;
            }
            ControllerInputsPanel panel = new ControllerInputsPanel(controller, labels,
                    token -> holderOf(edits.get(active), new BindingInput(controller.name(), token), List.of()).isPresent(),
                    token -> pickControl(new ControllerCapture.Captured(new BindingInput(controller.name(), token), List.of())));
            panels.add(panel);
            if (controller.connected()) livePanels.put(controller.liveId(), panel);
            JPanel tab = transparentPanel(new BorderLayout(0, 6));
            tab.setBorder(BorderFactory.createEmptyBorder(6, 6, 3, 6));
            tab.add(HudBanner.multiline(getText(controller.connected()
                    ? "bindings.assign.controller.hint"
                    : "bindings.assign.controller.notConnected"), StatusBadge.State.INFO), BorderLayout.NORTH);
            tab.add(panel, BorderLayout.CENTER);
            tabOf.put(panel, tab);
            sourceTabs.addTab(controller.tabTitle(), tab);
        }
        return sourceTabs;
    }

    /**
     * Brings forward the tab of the device the orange card's slot holds: its controller's, or KEYBOARD for a key,
     * an empty slot, or a controller that is not plugged in.
     */
    private void showSourceOf(SlotEdit edit) {
        BindingInput after = edit.inputAfter();
        JComponent tab = keyboardTab;
        if (after != null) {
            for (ControllerInputsPanel panel : panels) {
                if (panel.deviceName().equalsIgnoreCase(after.device())) tab = tabOf.get(panel);
            }
        }
        sourceTabs.setSelectedComponent(tab);
    }

    private void addValue(JPanel panel, String value, GridBagConstraints gbc) {
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(hudReadoutValue(value, HUD_COLOR_ROLE_PRIMARY_TEXT), gbc);
    }

    private String controlPath(BindingDisplayNames.ControlName control) {
        String section = getText(control.section().getLabelKey());
        return control.group() == null ? section : section + " › " + control.group();
    }

    // ------------------------------------------------------------------ choosing

    private void setActive(BindingSlotType type) {
        if (active == type) return;
        active = type;
        showSourceOf(edits.get(type));
        refresh(true);
    }

    private void onChordCaptured(KeyChordCaptureField.CapturedChord chord) {
        sourceTabs.setSelectedComponent(keyboardTab);
        choose(edits.get(active), edit -> {
            edit.kind = Kind.KEYBOARD;
            edit.key = chord.key();
            edit.keyboardModifiers = List.copyOf(chord.modifiers());
            edit.control = null;
        });
        refresh(false);
    }

    private void onClear(BindingSlotType type) {
        setActive(type);
        SlotEdit edit = edits.get(type);
        edit.kind = Kind.CLEAR;
        edit.control = null;
        edit.rejected = null;
        refresh(false);
    }

    private void onUndo(BindingSlotType type) {
        setActive(type);
        edits.get(type).undo();
        showSourceOf(edits.get(type));
        refresh(true);
    }

    /**
     * A controller input, pressed or clicked, goes into the orange card; its controller's tab comes forward.
     */
    private void pickControl(ControllerCapture.Captured captured) {
        SlotEdit edit = edits.get(active);
        choose(edit, e -> {
            e.kind = Kind.CONTROLLER;
            e.control = captured;
        });
        showSourceOf(edit);
        refresh(true);
    }

    /**
     * Puts a pick into the slot - unless it cannot be saved, in which case the slot keeps what it had and its card
     * says why (Alan, 2026-10-15). A refused pick left in the card read as assigned, and the commander's next pick
     * then went into the other slot beside it.
     */
    private void choose(SlotEdit edit, Consumer<SlotEdit> pick) {
        Kind kind = edit.kind;
        String key = edit.key;
        List<BindingModifier> modifiers = edit.keyboardModifiers;
        ControllerCapture.Captured control = edit.control;
        pick.accept(edit);
        String reason = blockingReason(edit);
        if (reason == null) {
            edit.rejected = null;
            return;
        }
        String picked = newText(edit);
        edit.kind = kind;
        edit.key = key;
        edit.keyboardModifiers = modifiers;
        edit.control = control;
        edit.rejected = picked + " \u2014 " + reason;
    }

    // ------------------------------------------------------------------ checks

    /**
     * Why this slot's choice cannot be saved, or {@code null} when it can. Checked for every changed slot, so a
     * problem in the card that is not orange still holds SAVE back - and says so on that card.
     */
    private String blockingReason(SlotEdit edit) {
        if (!edit.changed()) return null;
        // WHY: a chord this control held when the dialog opened - in either slot - can always go back: it is what
        // the file says today, and putting it back adds no clash. Without this, clearing a slot and re-picking
        // its own key was refused whenever another control shared it.
        boolean wasOwn = isOwnChord(edit.inputAfter(), edit.modifiersAfter());
        if (edit.kind == Kind.CONTROLLER) {
            if (wasOwn) return null;
            return holderOf(edit, edit.control.input(), edit.control.modifiers())
                    .map(holder -> getText("bindings.assign.controller.inUse", BindingDisplayNames.label(holder)))
                    .orElse(null);
        }
        if (edit.kind != Kind.KEYBOARD) return null;
        if (!EliteKeyboardKeys.isAssignable(edit.key)) {
            return getText("bindings.assign.unknownKey");
        }
        // Modifiers are not consulted for the game-menu key: Elite opens the menu on that key however it is
        // modified - see ReservedKeyChords.
        if (gameMenuKeys.contains(edit.key)) {
            return getText("bindings.assign.gameMenuKey");
        }
        List<String> modifierKeys = edit.keyboardModifiers.stream().map(BindingModifier::key).toList();
        if (ReservedKeyChords.isOsReserved(edit.key, modifierKeys)) {
            return getText("bindings.assign.reserved");
        }
        if (wasOwn) {
            return null;
        }
        if (isOccupied(edit)) {
            return getText("bindings.assign.alreadyInUse");
        }
        BindingConflictScanner.CandidateConflict conflict = BindingConflictScanner.candidateConflictInSlots(
                bindingId, edit.type, edit.key, modifierKeys, existingSlots);
        return conflict == null ? null
                : getText("bindings.assign.conflict", BindingDisplayNames.label(conflict.otherBinding()));
    }

    private boolean isOccupied(SlotEdit edit) {
        try {
            return availabilityService.isKeyOccupiedByOtherSlot(bindingsFile, bindingId, edit.type, edit.key,
                    edit.keyboardModifiers);
        } catch (Exception e) {
            // WHY: an unreadable/unparseable .binds must never be reported as free,
            // or we would offer a save that could silently clobber another binding.
            log.warn("Could not check key availability for binding '{}' slot {}: {}",
                    bindingId, edit.type, e.getMessage());
            return true;
        }
    }

    /**
     * The all-keyboard chords the slots held when the dialog opened, as key sets - green for this control.
     */
    private List<Set<String>> ownKeyboardChords() {
        List<Set<String>> chords = new ArrayList<>();
        for (SlotEdit edit : edits.values()) {
            if (edit.originallyEmpty() || !BindingInput.KEYBOARD.equals(edit.original.device())) continue;
            List<BindingInput> modifiers = originalModifiers(edit);
            if (modifiers.stream().anyMatch(m -> !BindingInput.KEYBOARD.equals(m.device()))) continue;
            Set<String> chord = new HashSet<>();
            chord.add(edit.original.key());
            modifiers.forEach(m -> chord.add(m.key()));
            chords.add(chord);
        }
        return chords;
    }

    /**
     * True when these inputs are exactly what one of the control's slots held when the dialog opened.
     */
    private boolean isOwnChord(BindingInput input, List<BindingInput> modifiers) {
        if (input == null) return false;
        String chord = chordIdentity(input, modifiers);
        return edits.values().stream().anyMatch(e -> !e.originallyEmpty()
                && chordIdentity(new BindingInput(e.original.device(), e.original.key()), originalModifiers(e)).equals(chord));
    }

    private static List<BindingInput> originalModifiers(SlotEdit edit) {
        return edit.original.bindingModifiers().stream()
                .filter(m -> m.key() != null && !m.key().isBlank())
                .map(m -> new BindingInput(m.device(), m.key())).toList();
    }

    private static String chordIdentity(BindingInput input, List<BindingInput> modifiers) {
        TreeSet<String> parts = new TreeSet<>();
        parts.add((input.device() + "|" + input.key()).toLowerCase(Locale.ROOT));
        modifiers.forEach(m -> parts.add((m.device() + "|" + m.key()).toLowerCase(Locale.ROOT)));
        return String.join("+", parts);
    }

    /**
     * The other control holding exactly these inputs for this slot's Hold, if any - on any device.
     */
    private Optional<String> holderOf(SlotEdit edit, BindingInput input, List<BindingInput> modifiers) {
        return SlotInputConflicts.holderOf(bindingId, input, modifiers, edit.hold(), readOnlySlots);
    }

    /**
     * True when the choices leave this control with no keyboard key while Vega uses it and could before. A
     * warning, not a block: moving a control to the stick is the commander's to decide - the AI ACCESS column
     * shows it after.
     */
    private boolean takesControlFromVega() {
        if (!vegaUsesIt) return false;
        boolean before = edits.values().stream().anyMatch(e -> e.original != null && e.original.keyboardUsable());
        boolean after = edits.values().stream().anyMatch(SlotEdit::keyboardAfter);
        return before && !after;
    }

    // ------------------------------------------------------------------ showing

    private void refresh(boolean bringForward) {
        SlotEdit activeEdit = edits.get(active);

        boolean anyChanged = false;
        boolean anyBlocked = false;
        for (SlotEdit edit : edits.values()) {
            String reason = blockingReason(edit);
            cards.get(edit.type).show(edit.type == active, newText(edit), reason != null ? reason : edit.rejected);
            anyChanged |= edit.changed();
            anyBlocked |= reason != null;
        }

        String activeKey = activeEdit.keyboardKeyAfter();
        // WHY not "Not defined": the field speaks for the keyboard only, and under a slot bound to a stick that
        // reads as if the slot were empty. With no key in the orange slot, it invites one.
        captureField.showIdleText(activeKey == null
                ? getText("bindings.assign.capture.idle")
                : slotFormatter.formatChord(activeEdit.kind == Kind.KEYBOARD ? activeEdit.keyboardModifiers
                : keyboardModifiersOf(activeEdit.original), activeKey));
        keyboardView.setCurrentKey(activeKey);
        keyboardView.setAssigned(heldOn(BindingInput.KEYBOARD, false), heldOn(BindingInput.KEYBOARD, true));

        for (ControllerInputsPanel panel : panels) {
            Set<String> held = new HashSet<>(heldOn(panel.deviceName(), false));
            held.addAll(heldOn(panel.deviceName(), true));
            panel.show(tokenOn(activeEdit, panel.deviceName()), held, ownAloneOn(panel.deviceName()), bringForward);
        }

        aiAccessBanner.setVisible(takesControlFromVega());
        saveButton.setEnabled(anyChanged && !anyBlocked);
        // Grow when a message needs the room, up to the screen; never shrink, so the dialog does not jump while
        // choosing. Past the screen's height the body scrolls.
        if (isDisplayable() && getPreferredSize().height > getHeight()) {
            Point at = getLocation();
            pack();
            fitToScreen();
            setLocation(at);
        }
    }

    /**
     * The tokens on that device the control will hold after SAVE - its slots' main inputs, or their modifiers.
     */
    private Set<String> heldOn(String device, boolean modifiers) {
        Set<String> tokens = new HashSet<>();
        for (SlotEdit edit : edits.values()) {
            if (modifiers) {
                edit.modifiersAfter().stream().filter(m -> m.device().equalsIgnoreCase(device))
                        .forEach(m -> tokens.add(m.key()));
            } else {
                String token = tokenOn(edit, device);
                if (token != null) tokens.add(token);
            }
        }
        return tokens;
    }

    /**
     * The inputs on that device one of the slots held on its own when the dialog opened.
     */
    private Set<String> ownAloneOn(String device) {
        Set<String> tokens = new HashSet<>();
        for (SlotEdit edit : edits.values()) {
            if (!edit.originallyEmpty() && edit.original.device().equalsIgnoreCase(device) && originalModifiers(edit).isEmpty()) {
                tokens.add(edit.original.key());
            }
        }
        return tokens;
    }

    private static String tokenOn(SlotEdit edit, String device) {
        BindingInput after = edit.inputAfter();
        return after != null && after.device().equalsIgnoreCase(device) ? after.key() : null;
    }

    private static List<BindingModifier> keyboardModifiersOf(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        return slot == null ? List.of() : slot.bindingModifiers().stream()
                .filter(BindingModifier::isSupportedKeyboardModifier).toList();
    }

    /**
     * What the card's arrow line says the slot will hold, or {@code null} when unchanged.
     */
    private String newText(SlotEdit edit) {
        if (!edit.changed()) return null;
        BindingInput after = edit.inputAfter();
        if (after == null) return getText("bindings.status.notDefined");
        List<BindingModifier> modifiers = edit.modifiersAfter().stream()
                .map(m -> new BindingModifier(m.device(), m.key())).toList();
        return slotFormatter.formatInputs(after.device(), modifiers, after.key());
    }

    private void save() {
        List<AssignSelection> selections = new ArrayList<>();
        for (SlotEdit edit : edits.values()) {
            if (!edit.changed()) continue;
            if (blockingReason(edit) != null) return;
            selections.add(edit.selection());
        }
        if (selections.isEmpty()) return;
        result = List.copyOf(selections);
        dispose();
    }

    private static boolean isClearedSlot(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        return slot == null || ("{NoDevice}".equals(slot.device())
                && (slot.key() == null || slot.key().isBlank()));
    }

    // ------------------------------------------------------------------ the slot cards

    /**
     * One slot of the row: its name, what it holds now and - once changed - what it will hold, its CLEAR, and
     * why it cannot be saved when it cannot. Orange while it is the slot being filled; a click makes it so.
     */
    private final class SlotCard extends JPanel {
        private final SlotEdit edit;
        private final JLabel newValue = new JLabel();
        /**
         * Shown once the slot has changed: back to what it held, without leaving the dialog.
         */
        private final JButton undo = makeButtonSubtle(getText("bindings.assign.undo"));
        private final JPanel messages = transparentPanel(new BorderLayout(0, 4));

        SlotCard(SlotEdit edit) {
            super(new GridBagLayout());
            this.edit = edit;
            setOpaque(true);
            setBackground(HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            GridBagConstraints gbc = baseGbc();
            gbc.insets = new Insets(2, 0, 2, 0);
            gbc.gridx = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(hudGroupLabel(slotTitle(edit.type)), gbc);

            JButton clear = makeButtonSubtle(getText("bindings.assign.capture.clear"));
            clear.addActionListener(e -> onClear(edit.type));
            undo.addActionListener(e -> onUndo(edit.type));
            JPanel buttons = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            buttons.add(undo);
            buttons.add(clear);
            gbc.gridx = 1;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            add(buttons, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            add(hudReadoutValue(slotFormatter.formatSlot(edit.original), HUD_COLOR_ROLE_PRIMARY_TEXT), gbc);

            gbc.gridy = 2;
            newValue.setForeground(HUD_COLOR_ROLE_PRIMARY_ACTION);
            add(newValue, gbc);

            gbc.gridy = 3;
            add(messages, gbc);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    setActive(edit.type);
                }
            });
        }

        /**
         * @param problem why the slot cannot be saved, or why its last pick was turned away; {@code null} for none
         */
        void show(boolean isActive, String willHold, String problem) {
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(isActive ? HUD_COLOR_ROLE_PRIMARY_ACTION : HUD_COLOR_ROLE_SECONDARY_BORDER,
                            isActive ? 2 : 1),
                    BorderFactory.createEmptyBorder(isActive ? 6 : 7, isActive ? 8 : 9, isActive ? 6 : 7, isActive ? 8 : 9)));
            newValue.setText(willHold == null ? " " : "→ " + willHold);
            undo.setVisible(edit.changed());

            messages.removeAll();
            List<JComponent> shown = new ArrayList<>();
            if (problem != null) {
                shown.add(HudBanner.multiline(problem, StatusBadge.State.OFFLINE));
            } else if (!edit.changed() && edit.existingConflict != null && !edit.existingConflict.partners().isEmpty()) {
                // This slot already collides with another control: state it on the card, in red, rather than
                // relying on the grid's hover callout, which is unreachable while this modal is open.
                String partners = String.join(", ",
                        edit.existingConflict.partners().stream().map(BindingDisplayNames::label).toList());
                shown.add(HudBanner.multiline(
                        getText("bindings.conflict.popup.title", edit.existingConflict.chord()) + " " + partners,
                        StatusBadge.State.OFFLINE));
            }
            JPanel stack = transparentPanel(new GridLayout(0, 1, 0, 4));
            shown.forEach(stack::add);
            messages.add(stack, BorderLayout.CENTER);
            messages.setVisible(!shown.isEmpty());
            revalidate();
            repaint();
        }

        private String slotTitle(BindingSlotType type) {
            return (type == BindingSlotType.PRIMARY
                    ? getText("bindings.column.primary")
                    : getText("bindings.column.secondary")).toUpperCase();
        }
    }
}
