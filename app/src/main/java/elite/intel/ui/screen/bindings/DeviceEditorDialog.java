package elite.intel.ui.screen.bindings;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.devicefiles.FrontierStockButtonMaps;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.bindforge.devices.*;
import elite.intel.bindforge.storage.ConfigurationFiles;
import elite.intel.bindforge.storage.DeviceEdits;
import elite.intel.bindforge.storage.Edits;
import elite.intel.devices.events.DeviceAxisEvent;
import elite.intel.devices.events.DeviceButtonEvent;
import elite.intel.devices.events.DeviceDisconnectedEvent;
import elite.intel.devices.events.DeviceHatEvent;
import elite.intel.devices.model.DeviceDescription;
import elite.intel.eventbus.DeviceBus;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudTextField;
import elite.intel.ui.widget.HudUnsavedHint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;

/**
 * The Device Editor: one device's record, opened from the gear on its My Devices row (alias-designer.md, <em>the
 * Device Editor</em>, 2026-10-10).
 * <p>
 * <strong>Built from what the controller reports.</strong> DEVICE always; AXES, BUTTONS and HATS only when the
 * controller has that kind of input ({@link DeviceEditorInputs}). A device with no name yet keeps its input tabs
 * disabled - a {@code .buttonMap} is named after the device, so there is nothing to label until it has a name.
 * <p>
 * <strong>The name can be changed</strong> on one of the user's own named devices (A11) - checked as it is typed, by
 * the rule onboarding uses, against every name in use ({@link DeviceRename}). SAVE renames the element and the
 * {@code .buttonMap} and moves the bindings, all in the edits. On an attached built-in the same box shadows Frontier's
 * element (A15): the user's goes ahead of it, and typing Frontier's name again goes back.
 * <p>
 * <strong>SAVE keeps the changes as edits not applied</strong> (Alan, 2026-10-24; Storage S4a) - it closes the dialog
 * and lights the APPLY bar above BindForge's tabs; nothing is written until APPLY ({@link Edits}, {@link DeviceEdits}).
 * Leaving with unsaved changes asks Save, Discard or Keep editing.
 * <p>
 * <strong>CLEAR - RESET LABELS on a built-in - takes the device out of the edits</strong>, and APPLY out of every
 * installation ({@link DeviceEditorMiddle}, A10). A device cleared and not applied says so, cannot be edited, and offers
 * its undo in the same place.
 * <p>
 * <strong>Live highlighting</strong> listens to the Device Service for this device alone while the dialog is open
 * ({@link DeviceEditorLights}). The dialog never changes tab for an input: a box lights only where the user is
 * looking.
 */
final class DeviceEditorDialog extends JDialog {

    private static final Logger log = LogManager.getLogger(DeviceEditorDialog.class);

    /** Six axes to a column, as the spec lays them out. */
    private static final int AXES_PER_COLUMN = 6;
    /** One sub-tab per 32 buttons, numbered as the game numbers them. */
    private static final int BUTTONS_PER_GROUP = 32;
    private static final int BUTTONS_PER_COLUMN = 8;
    private static final int FIELD_COLUMNS = 14;

    /**
     * Everything the dialog shows, read off the EDT by the screen.
     *
     * @param deviceName  the name the device is under - in the edits, in the working copy when it is cleared in the
     *                    edits, or Frontier's for a built-in - or {@code null} for a device with no name yet
     * @param liveId      the Device Service's id for the controller when it is attached, else {@code null}
     * @param description which axes and hats it has when attached, else {@code null}
     * @param gamepad     whether the attached controller is a gamepad, whose triggers rest at -1; {@code false}
     *                    when it is not attached
     * @param saved       the labels the device had when the dialog opened, in the edits
     * @param gameDrawsIt whether the game draws this device's inputs itself, so untouched defaults are not written
     * @param middle      what the middle button is, and whether it can be pressed
     * @param rename      whether the name can be changed, and why not
     * @param taken       the names a new name must not clash with - empty unless {@code rename} allows it
     * @param covering    Frontier's element covering the controller's hardware, or {@code null} when none does
     */
    record Opening(MyDevice device, String deviceName, Integer liveId, DeviceDescription description,
                   boolean gamepad, DeviceEditorInputs inputs, Map<String, String> saved, boolean gameDrawsIt,
                   DeviceEditorMiddle middle, DeviceRename.Allowed rename,
                   Set<String> taken, Covering covering) {
        Opening {
            taken = Set.copyOf(taken);
        }
    }

    /**
     * The Frontier element covering a controller (A15).
     *
     * @param name   its element name - the name a shadow keeps as its old one, and the one the user may go back to
     * @param shared whether it covers other hardware too, so bindings naming it are not this controller's alone
     * @param held   whether labels of the user's are under its name while it covers other controllers too, so going
     *               back to it is refused - this controller's labels would go over theirs
     */
    record Covering(String name, boolean shared, boolean held) {
    }

    private final Opening opening;
    private final Runnable onName;
    private final Map<String, String> starting;
    private final Map<String, HudTextField> boxes = new LinkedHashMap<>();
    private final DeviceEditorLights lights;
    private final Map<String, Color[]> restingColours = new LinkedHashMap<>();
    private final HudUnsavedHint unsavedHint = new HudUnsavedHint();
    private JButton save;
    private JButton middle;
    /** The name box, editable only when the device can be renamed. */
    private HudTextField nameBox;
    private JLabel nameProblem;
    /** A change to the edits is under way - SAVE, CLEAR, RESET LABELS or an undo - so nothing else may start. */
    private boolean busy;

    /**
     * @param onName run on the EDT after the dialog closes, when the user asks to name an unnamed controller - which
     *               opens onboarding for it (Alan, 2026-10-14, A6b). The screen reads again after a save by itself,
     *               since every change to the edits is announced
     */
    DeviceEditorDialog(Component parent, Opening opening, Runnable onName) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.opening = opening;
        this.onName = onName;
        this.starting = opening.inputs().startingLabels(opening.saved());
        this.lights = new DeviceEditorLights(opening.description() == null
                ? DeviceDescription.axesUnknown(0)
                : opening.description(), opening.gamepad());
        buildUi();
    }

    void showDialog() {
        // WHY: on the bus only while open, and only once the dialog exists - a press with no dialog to light is
        // nothing, and a subscriber left registered after close would light boxes nobody sees, forever.
        if (opening.liveId() != null) DeviceBus.register(this);
        try {
            AppTheme.runWithModalScrim(getOwner(), () -> setVisible(true));
        } finally {
            if (opening.liveId() != null) DeviceBus.unregister(this);
        }
    }

    private boolean named() {
        return opening.deviceName() != null;
    }

    /** NAME, for a controller onboarding would ask about - {@link DeviceNames#needingName}, the one rule. */
    private boolean offersName() {
        return !named() && !DeviceNames.needingName(List.of(opening.device())).isEmpty();
    }

    private boolean renamable() {
        return opening.rename() == DeviceRename.Allowed.YES;
    }

    /** The name as typed, trimmed - or the name it opened with when it cannot be changed. */
    private String typedName() {
        return renamable() ? nameBox.getText().trim() : opening.deviceName();
    }

    private boolean nameChanged() {
        return renamable() && !typedName().equals(opening.deviceName());
    }

    /** {@code null} when the typed name may be saved, else why not, in the user's words. */
    private String nameRefusal() {
        if (!nameChanged()) return null;
        Covering covering = opening.covering();
        if (DeviceRename.backToFrontier(typedName(), covering == null ? null : covering.name())) {
            // WHY: refused here, where the reason is known - SAVE would find the name taken and could only say the
            // device changed (review, 2026-10-17).
            return covering.held() ? getText("bindings.aliasDesigner.editor.name.frontierHeld", covering.name()) : null;
        }
        DeviceNames.Verdict verdict = DeviceNames.validate(typedName(), opening.taken());
        return verdict.valid() ? null : OnboardingDialog.problemText(verdict);
    }

    /** Labels can be edited once the device has a name - and not while it is cleared or reset, waiting for APPLY. */
    private boolean editable() {
        return named() && !opening.middle().undoing();
    }

    private void buildUi() {
        JTabbedPane tabs = AppTheme.makeCompactTabs();
        tabs.addTab(getText("bindings.aliasDesigner.editor.tab.device"), deviceTab());
        DeviceEditorInputs inputs = opening.inputs();
        if (!inputs.axes().isEmpty()) addInputTab(tabs, "bindings.aliasDesigner.editor.tab.axes", axesTab());
        if (!inputs.buttons().isEmpty()) addInputTab(tabs, "bindings.aliasDesigner.editor.tab.buttons", buttonsTab());
        if (inputs.hats() > 0) addInputTab(tabs, "bindings.aliasDesigner.editor.tab.hats", hatsTab());

        JPanel body = transparentPanel(new BorderLayout(0, HudPalette.HUD_GAP));
        body.add(tabs, BorderLayout.CENTER);
        body.add(note(getText("bindings.aliasDesigner.editor.workingCopy"), HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT),
                BorderLayout.SOUTH);

        save = makeButton(getText("button.save"));
        save.setEnabled(false);
        save.addActionListener(e -> save());
        JButton back = makeButtonSubtle(getText("button.back"));
        back.addActionListener(e -> leave());
        // WHY: in the middle, so the destructive button is beside neither of the ones reached for by habit. On a
        // built-in, RESET LABELS takes CLEAR's place, since CLEAR would remove Frontier's entry.
        DeviceEditorMiddle rule = opening.middle();
        middle = makeButtonSubtle(getText(switch (rule.kind()) {
            case CLEAR -> "bindings.aliasDesigner.editor.button.clear";
            case RESET_LABELS -> "bindings.aliasDesigner.editor.button.resetLabels";
            case UNDO_CLEAR -> "bindings.aliasDesigner.editor.button.undoClear";
            case UNDO_RESET -> "bindings.aliasDesigner.editor.button.undoReset";
        }));
        middle.setEnabled(rule.enabled());
        String why = whyText(rule);
        if (why != null) middle.setToolTipText(why);
        middle.addActionListener(e -> middleAction());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("bindings.aliasDesigner.editor.title", titleName()))
                .onClose(this::leave)
                .body(body)
                .scrollBody(false)
                .dismiss(back)
                .middle(middle)
                .unsavedHint(unsavedHint)
                .primary(save)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        getRootPane().registerKeyboardAction(e -> leave(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        // WHY: no default button. Enter in a label box would otherwise SAVE and close the dialog mid-edit.
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        pack();
        setLocationRelativeTo(getOwner());
    }

    private String titleName() {
        return named() ? opening.deviceName() : opening.device().label();
    }

    /** An input tab, disabled while the device has no name: there is nothing yet for its labels to be saved under. */
    private void addInputTab(JTabbedPane tabs, String titleKey, JComponent content) {
        tabs.addTab(getText(titleKey), content);
        tabs.setEnabledAt(tabs.getTabCount() - 1, editable());
    }

    // ------------------------------------------------------------------ DEVICE

    private JComponent deviceTab() {
        MyDevice device = opening.device();
        JPanel panel = transparentPanel(new GridBagLayout());
        int row = 0;
        row = nameField(panel, row);
        for (String nameNote : nameNotes()) {
            row = line(panel, row, nameNote, HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        }
        nameProblem = note(" ", HudPalette.HUD_COLOR_ROLE_WARNING);
        nameProblem.setVisible(false);
        GridBagConstraints problem = cell(0, row++);
        problem.gridwidth = 3;
        panel.add(nameProblem, problem);
        DeviceEditorMiddle rule = opening.middle();
        if (rule.undoing()) {
            row = line(panel, row, getText(rule.kind() == DeviceEditorMiddle.Kind.UNDO_RESET
                    ? "bindings.aliasDesigner.editor.reset"
                    : "bindings.aliasDesigner.editor.cleared"), HudPalette.HUD_COLOR_ROLE_WARNING);
        } else if (rule.why() == DeviceEditorMiddle.Why.NAMED_IN_BINDS
                || rule.why() == DeviceEditorMiddle.Why.BINDS_UNREAD) {
            row = line(panel, row, whyText(rule), HudPalette.HUD_COLOR_ROLE_WARNING);
        }
        if (offersName()) {
            JButton name = makeButton(getText("bindings.aliasDesigner.editor.button.name"));
            name.addActionListener(e -> {
                dispose();
                onName.run();
            });
            GridBagConstraints c = cell(0, row++);
            c.gridwidth = 3;
            panel.add(name, c);
        }
        row = field(panel, row, "bindings.aliasDesigner.editor.field.vid", device.vid());
        row = field(panel, row, "bindings.aliasDesigner.editor.field.pid", device.pid());
        row = readout(panel, row, "bindings.aliasDesigner.editor.field.hardwareName",
                device.hardwareName() == null ? "-" : device.hardwareName());
        row = readout(panel, row, "bindings.aliasDesigner.editor.field.status", getText(device.attached()
                ? "bindings.aliasDesigner.status.attached"
                : "bindings.aliasDesigner.status.missing"));
        if (!device.attached()) {
            row = line(panel, row, getText(opening.inputs().isEmpty()
                    ? "bindings.aliasDesigner.editor.noInputs"
                    : "bindings.aliasDesigner.editor.notAttached"), HudPalette.HUD_COLOR_ROLE_WARNING);
        }
        filler(panel, row);
        return panel;
    }

    /**
     * The lines beneath the name box. On an attached built-in they say what renaming it does - a shadow, with Frontier's
     * element kept - and, where Frontier's covers other controllers, that every binding naming it moves to this one. On
     * a shadowed one, how to go back (A15).
     */
    private List<String> nameNotes() {
        Covering covering = opening.covering();
        return switch (opening.rename()) {
            case YES -> {
                if (covering == null) yield List.of(getText("bindings.aliasDesigner.editor.name.rename"));
                if (!covering.name().equals(opening.deviceName())) {
                    yield List.of(getText("bindings.aliasDesigner.editor.name.backToFrontier", covering.name()));
                }
                List<String> notes = new ArrayList<>();
                notes.add(getText("bindings.aliasDesigner.editor.name.shadow"));
                if (covering.shared()) {
                    notes.add(getText("bindings.aliasDesigner.editor.name.shadowShared", covering.name()));
                }
                yield notes;
            }
            case NO_NAME -> List.of(getText("bindings.aliasDesigner.editor.noName"));
            case BUILT_IN -> List.of(getText("bindings.aliasDesigner.editor.builtIn"));
            case CLEARED -> List.of();
        };
    }

    /**
     * The name: a box the user can type in when the device can be renamed, read-only otherwise. Checked as it is
     * typed; SAVE stays dimmed while the typed name is refused.
     */
    private int nameField(JPanel panel, int row) {
        panel.add(AppTheme.hudReadoutLabel(getText("bindings.aliasDesigner.editor.field.name")), cell(0, row));
        nameBox = new HudTextField();
        nameBox.setText(titleName());
        nameBox.setEditable(renamable());
        sized(nameBox, FIELD_COLUMNS * 2);
        refreshOnEdit(nameBox);
        GridBagConstraints c = cell(1, row);
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(nameBox, c);
        return row + 1;
    }

    /** A read-only field: VID and PID - the game attaches a device by them. */
    private int field(JPanel panel, int row, String labelKey, String value) {
        panel.add(AppTheme.hudReadoutLabel(getText(labelKey)), cell(0, row));
        HudTextField field = new HudTextField();
        field.setText(value);
        field.setEditable(false);
        sized(field, FIELD_COLUMNS * 2);
        GridBagConstraints c = cell(1, row);
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, c);
        return row + 1;
    }

    private int readout(JPanel panel, int row, String labelKey, String value) {
        panel.add(AppTheme.hudReadoutLabel(getText(labelKey)), cell(0, row));
        GridBagConstraints c = cell(1, row);
        c.gridwidth = 2;
        panel.add(AppTheme.hudReadoutValue(value, HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT), c);
        return row + 1;
    }

    private int line(JPanel panel, int row, String text, Color colour) {
        GridBagConstraints c = cell(0, row);
        c.gridwidth = 3;
        panel.add(note(text, colour), c);
        return row + 1;
    }

    // ------------------------------------------------------------------ inputs

    private JComponent axesTab() {
        JPanel grid = transparentPanel(new GridBagLayout());
        List<String> axes = opening.inputs().axes();
        for (int i = 0; i < axes.size(); i++) {
            addBox(grid, axes.get(i), (i / AXES_PER_COLUMN) * 2, i % AXES_PER_COLUMN);
        }
        JPanel panel = transparentPanel(new BorderLayout(0, HudPalette.HUD_GAP));
        panel.add(grid, BorderLayout.NORTH);
        JPanel notes = transparentPanel(new GridLayout(0, 1, 0, HudPalette.HUD_GAP / 2));
        if (opening.inputs().attached() && !opening.inputs().axesKnown()) {
            notes.add(note(getText("bindings.aliasDesigner.editor.axesUnknown"),
                    HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT));
        }
        if (opening.inputs().axesWithoutSlot() > 0) {
            notes.add(note(getText("bindings.aliasDesigner.editor.axesWithoutSlot",
                    opening.inputs().axesWithoutSlot()), HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT));
        }
        panel.add(notes, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buttonsTab() {
        List<String> buttons = opening.inputs().buttons();
        if (buttons.size() <= BUTTONS_PER_GROUP) return buttonGroup(buttons);
        JTabbedPane groups = AppTheme.makeCompactTabs();
        for (int from = 0; from < buttons.size(); from += BUTTONS_PER_GROUP) {
            List<String> group = buttons.subList(from, Math.min(from + BUTTONS_PER_GROUP, buttons.size()));
            // WHY: named by the game's numbers. An unattached controller's buttons come from its labels and may skip,
            // so the range is the first and last token in the group, not a count.
            groups.addTab(number(group.getFirst()) + "–" + number(group.getLast()), buttonGroup(group));
        }
        return groups;
    }

    private JComponent buttonGroup(List<String> buttons) {
        JPanel grid = transparentPanel(new GridBagLayout());
        for (int i = 0; i < buttons.size(); i++) {
            addBox(grid, buttons.get(i), (i / BUTTONS_PER_COLUMN) * 2, i % BUTTONS_PER_COLUMN);
        }
        return top(grid);
    }

    private JComponent hatsTab() {
        JPanel grid = transparentPanel(new GridBagLayout());
        for (int hat = 1; hat <= opening.inputs().hats(); hat++) {
            List<String> tokens = DeviceEditorInputs.hatTokens(hat);
            for (int direction = 0; direction < tokens.size(); direction++) {
                addBox(grid, tokens.get(direction), direction * 2, hat - 1);
            }
        }
        return top(grid);
    }

    private static String number(String buttonToken) {
        return buttonToken.substring(buttonToken.indexOf('_') + 1);
    }

    /** One input: its token, as the game names it, then its label box. */
    private void addBox(JPanel grid, String token, int column, int row) {
        grid.add(AppTheme.hudReadoutLabel(token), cell(column, row));
        HudTextField box = new HudTextField();
        sized(box, FIELD_COLUMNS);
        ((AbstractDocument) box.getDocument()).setDocumentFilter(new LengthLimit());
        box.setText(starting.get(token));
        box.setEditable(editable());
        refreshOnEdit(box);
        box.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // WHY: a box is never empty - clearing it puts the default back, and SAVE keeps what it shows.
                String kept = DeviceEditorInputs.clean(token, box.getText());
                if (!kept.equals(box.getText())) box.setText(kept);
            }
        });
        GridBagConstraints c = cell(column + 1, row);
        c.insets = new Insets(HudPalette.HUD_GAP / 2, HudPalette.HUD_GAP / 2, HudPalette.HUD_GAP / 2,
                HudPalette.HUD_GAP * 2);
        grid.add(box, c);
        boxes.put(token, box);
        restingColours.put(token, new Color[]{box.getBackground(), box.getForeground()});
    }

    /** Every edit to {@code field} - the name or a label - re-reads whether there is anything to SAVE. */
    private void refreshOnEdit(HudTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshDirty();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshDirty();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshDirty();
            }
        });
    }

    private Map<String, String> boxValues() {
        Map<String, String> values = new LinkedHashMap<>();
        boxes.forEach((token, box) -> values.put(token, box.getText()));
        return values;
    }

    private boolean dirty() {
        return editable() && (nameChanged() || DeviceEditorInputs.changed(starting, boxValues()));
    }

    private void refreshDirty() {
        // WHY: the boxes are filled in before SAVE exists, and each fill lands here.
        if (save == null) return;
        boolean dirty = dirty();
        String refused = nameRefusal();
        nameProblem.setText(refused == null ? " " : refused);
        nameProblem.setVisible(refused != null);
        unsavedHint.setVisible(dirty);
        save.setEnabled(dirty && refused == null && !busy);
    }

    // ------------------------------------------------------------------ leaving

    /** BACK, the close glyph and Esc: asks first when there are unsaved edits. */
    private void leave() {
        if (busy) return;
        if (!dirty()) {
            dispose();
            return;
        }
        HudConfirmDialog.Result answer = HudConfirmDialog.show(this,
                getText("bindings.aliasDesigner.editor.leave.title"),
                getText("bindings.aliasDesigner.editor.leave.message", titleName()),
                getText("button.save"),
                getText("bindings.aliasDesigner.editor.leave.discard"),
                getText("bindings.aliasDesigner.editor.leave.keepEditing"));
        switch (answer) {
            case PRIMARY -> save();
            case EXTRA -> dispose();
            // WHY: Keep editing is the dismiss answer, so Esc on the question returns to the dialog rather than
            // throwing edits away.
            case DISMISS -> { }
        }
    }

    /**
     * Puts the device's whole label set - and its new name, when it has one - into the edits, off the EDT, and closes
     * once they are there. A refused name is never saved: SAVE stays dimmed, and the leave question's Save says why
     * rather than closing.
     */
    private void save() {
        if (!dirty() || busy) return;
        String refused = nameRefusal();
        if (refused != null) {
            HudConfirmDialog.info(this, getText("bindings.aliasDesigner.editor.heading"),
                    refused, getText("button.close"));
            return;
        }
        Map<String, String> labels =
                DeviceEditorInputs.toSave(opening.saved(), boxValues(), opening.gameDrawsIt());
        String name = opening.deviceName();
        if (!nameChanged()) {
            runAndClose(current -> DeviceEdits.withLabels(current, name, labels),
                    "bindings.aliasDesigner.editor.saveFailed");
            return;
        }
        String newName = typedName();
        MyDevice device = opening.device();
        Covering covering = opening.covering();
        FrontierStockDevices stock = FrontierStockDevices.getInstance();
        runAndClose(current -> {
            if (covering != null && name.equals(covering.name())) {
                return DeviceEdits.shadow(current, name, newName, device.vid(), device.pid(), labels, covering.shared(),
                        stock, FrontierStockButtonMaps.getInstance()).files();
            }
            // WHY: the name it opened with must still be the user's - a DISCARD meanwhile could have taken it away, and
            // renaming what is no longer there would leave the bindings on a name nothing has.
            requireNamed(current, name, stock);
            if (covering != null && DeviceRename.backToFrontier(newName, covering.name())) {
                return DeviceEdits.backToFrontier(current, name, newName, labels).files();
            }
            return DeviceEdits.rename(current, name, newName, labels).files();
        }, "bindings.aliasDesigner.editor.saveFailed");
    }

    /** The edits still hold the user's element by this name, or the device changed since the dialog opened. */
    private static void requireNamed(ConfigurationFiles current, String name, FrontierStockDevices stock)
            throws IOException {
        if (!DeviceEdits.usersNames(current, stock).contains(name)) throw new Stale();
    }

    /** The device changed since the dialog opened, so nothing was done. */
    private static final class Stale extends IOException {
        Stale() {
            super("the device changed since the dialog opened");
        }
    }

    // ------------------------------------------------------------------ the middle button

    /** Why the middle button cannot be pressed, as its tooltip - or {@code null} when it can. */
    private String whyText(DeviceEditorMiddle rule) {
        return switch (rule.why()) {
            case NONE -> null;
            case NO_NAME -> getText("bindings.aliasDesigner.editor.why.noName");
            case NOT_ADDED -> getText("bindings.aliasDesigner.editor.why.notAdded");
            case NOTHING_TO_RESET -> getText("bindings.aliasDesigner.editor.why.nothingToReset");
            case NAMED_IN_BINDS -> getText("bindings.aliasDesigner.editor.namedInBinds", rule.bindings(),
                    opening.deviceName());
            case BINDS_UNREAD -> getText("bindings.aliasDesigner.editor.why.bindsUnread");
        };
    }

    /**
     * CLEAR and RESET LABELS ask first - naming no installations, which editors never show (principle 12; Alan,
     * 2026-10-27); the undo does not ask, since it puts back what the working copy holds. Each changes the edits alone,
     * then closes.
     */
    private void middleAction() {
        DeviceEditorMiddle rule = opening.middle();
        if (busy || !rule.enabled()) return;
        String name = opening.deviceName();
        FrontierStockDevices stock = FrontierStockDevices.getInstance();
        if (rule.undoing()) {
            runAndClose((current, applied) -> DeviceEdits.restore(current, applied, name, stock),
                    "bindings.aliasDesigner.editor.actionFailed");
            return;
        }
        boolean reset = rule.kind() == DeviceEditorMiddle.Kind.RESET_LABELS;
        if (!HudConfirmDialog.confirm(this, getText("bindings.aliasDesigner.editor.heading"),
                getText(reset
                        ? "bindings.aliasDesigner.editor.reset.confirm"
                        : "bindings.aliasDesigner.editor.clear.confirm", name),
                getText(reset
                        ? "bindings.aliasDesigner.editor.button.resetLabels"
                        : "bindings.aliasDesigner.editor.button.clear"),
                getText("button.back"))) {
            return;
        }
        if (reset) {
            runAndClose(current -> DeviceEdits.resetLabels(current, name, FrontierStockButtonMaps.getInstance()),
                    "bindings.aliasDesigner.editor.actionFailed");
            return;
        }
        runAndClose(current -> {
            requireNamed(current, name, stock);
            return DeviceEdits.clear(current, name);
        }, "bindings.aliasDesigner.editor.actionFailed");
    }

    /**
     * Changes the edits off the EDT - SAVE, CLEAR, RESET LABELS or an undo - then closes; the screen reads again when
     * the edits announce the change. A device that changed since the dialog opened is reported, not closed on in
     * silence (review, 2026-10-11).
     *
     * @param failedKey the message for a change that failed, taking the reason as its argument. A stale device has its
     *                  own message, shown alone - wrapped in this one it would say "could not be done" and "nothing
     *                  was done" in one breath (review, 2026-10-11)
     */
    private void runAndClose(Edits.Edit edit, String failedKey) {
        runAndClose((current, applied) -> edit.apply(current), failedKey);
    }

    /** As {@link #runAndClose(Edits.Edit, String)}, for a change that needs the working copy too - the undo. */
    private void runAndClose(Edits.EditFromApplied edit, String failedKey) {
        busy = true;
        middle.setEnabled(false);
        refreshDirty();
        // WHY: the user pressed the button and must be told, a file the change could not use and a bug alike.
        Function<String, Runnable> failed = reason -> () -> notChanged(getText(failedKey, reason));
        String what = "The Device Editor's change of " + opening.deviceName();
        FlowSteps.inBackground("BindForge-DeviceEditorAction", log, what, () -> {
            try {
                Edits.stored().change(edit);
            } catch (Stale e) {
                return () -> notChanged(getText("bindings.aliasDesigner.editor.stale", opening.deviceName()));
            } catch (Edits.WriteRunningException e) {
                // WHY: shown alone, like a stale device - wrapped in failedKey it would say "could not be done" twice.
                log.info("The Device Editor did not change {}: {}", opening.deviceName(), e.getMessage());
                return () -> notChanged(getText("bindings.writeRunning"));
            }
            return () -> {
                busy = false;
                dispose();
            };
        }, failed, failed, FlowSteps.NOTHING);
    }

    /** After {@link #runAndClose}: the dialog stays, with {@code message} shown. On the EDT. */
    private void notChanged(String message) {
        busy = false;
        middle.setEnabled(opening.middle().enabled());
        refreshDirty();
        HudConfirmDialog.info(this, getText("bindings.aliasDesigner.editor.heading"), message,
                getText("button.close"));
    }

    // ------------------------------------------------------------------ live highlighting

    @Subscribe
    public void onButton(DeviceButtonEvent event) {
        if (isThisDevice(event.deviceId())) onEdt(() -> lights.button(event.buttonIndex(), event.pressed()));
    }

    @Subscribe
    public void onAxis(DeviceAxisEvent event) {
        if (isThisDevice(event.deviceId())) onEdt(() -> lights.axis(event.axisIndex(), event.value()));
    }

    @Subscribe
    public void onHat(DeviceHatEvent event) {
        if (isThisDevice(event.deviceId())) onEdt(() -> lights.hat(event.hatIndex(), event.directions()));
    }

    @Subscribe
    public void onDisconnected(DeviceDisconnectedEvent event) {
        if (isThisDevice(event.deviceId())) {
            onEdt(() -> {
                lights.clear();
                return true;
            });
        }
    }

    private boolean isThisDevice(int deviceId) {
        return opening.liveId() != null && opening.liveId() == deviceId;
    }

    /** Runs a change to the lights on the EDT, and repaints the boxes when anything lit or went out. */
    private void onEdt(BooleanSupplier change) {
        SwingUtilities.invokeLater(() -> {
            if (change.getAsBoolean()) paintLights();
        });
    }

    /**
     * A lit box takes a solid orange fill with dark text - the canon allows a fill for activity - and goes back to
     * its own colours the moment its input is released.
     * <p>
     * WHY orange, not the selection blue (Alan, 2026-10-15): the half-transparent blue barely showed against the
     * box, so a button pressed on the stick could be missed on screen.
     */
    private void paintLights() {
        Set<String> lit = lights.lit();
        boxes.forEach((token, box) -> {
            Color[] resting = restingColours.get(token);
            boolean on = lit.contains(token);
            box.setBackground(on ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION : resting[0]);
            box.setForeground(on ? HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT : resting[1]);
        });
    }

    // ------------------------------------------------------------------ layout helpers

    private static GridBagConstraints cell(int column, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(HudPalette.HUD_GAP / 2, 0, HudPalette.HUD_GAP / 2, HudPalette.HUD_GAP);
        return c;
    }

    /**
     * A field wide enough for {@code columns} characters at the HUD's field height. {@code HudTextField} fixes its
     * preferred width at zero, so columns alone would not size it.
     */
    private static void sized(HudTextField field, int columns) {
        int width = field.getFontMetrics(field.getFont()).charWidth('M') * columns;
        field.setPreferredSize(new Dimension(width, HudPalette.HUD_FIELD_HEIGHT));
    }

    private static void filler(JPanel panel, int row) {
        GridBagConstraints c = cell(0, row);
        c.weighty = 1;
        c.weightx = 1;
        c.gridwidth = 3;
        panel.add(Box.createGlue(), c);
    }

    private static JComponent top(JComponent content) {
        JPanel panel = transparentPanel(new BorderLayout());
        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private static JLabel note(String text, Color colour) {
        return AppTheme.hudReadoutValue(text, colour);
    }

    /** Holds a box to the 18 characters the game can show, however the text arrives. */
    private static final class LengthLimit extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
                throws BadLocationException {
            replace(fb, offset, 0, text, attrs);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            String insert = text == null ? "" : text;
            int room = DeviceEditorInputs.MAX_LABEL_LENGTH - (fb.getDocument().getLength() - length);
            if (room <= 0) {
                if (length > 0) fb.remove(offset, length);
                return;
            }
            fb.replace(offset, length, insert.length() > room ? insert.substring(0, room) : insert, attrs);
        }
    }
}
