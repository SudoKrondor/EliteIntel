package elite.intel.ui.dialog;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.devices.CaptureControllers;
import elite.intel.bindforge.devices.ControllerCapture;
import elite.intel.bindforge.io.AxisAssignment;
import elite.intel.bindforge.io.BindsValues;
import elite.intel.bindforge.model.BindingDisplayNames;
import elite.intel.bindforge.model.BindingInput;
import elite.intel.bindforge.model.ThrottleRanges;
import elite.intel.bindforge.storage.EditedDeviceLabels;
import elite.intel.devices.events.DeviceAxisEvent;
import elite.intel.devices.events.DeviceConnectedEvent;
import elite.intel.devices.events.DeviceDisconnectedEvent;
import elite.intel.devices.model.GameAxis;
import elite.intel.eventbus.DeviceBus;
import elite.intel.ui.support.BindingSlotDisplayFormatter;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Function;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * The bind editor for an axis-only control: which axis, Normal or Inverted, the dead zone and, where the control has
 * one, its throttle range. It offers controller axes and nothing else, because an axis row is never bound to a keyboard
 * key and takes no modifiers (bind-editor.md, <em>Axis slots take an axis only</em>), and VEGA never presses one.
 * <p>
 * Its own dialog rather than a mode of {@link AssignKeyboardBindingDialog}: that one edits a row of two slots, and
 * every part of it - cards, modifiers, keyboard map, AI access - means nothing here.
 * <p>
 * <strong>An axis another control holds does not block SAVE</strong>, unlike a button. Sharing one is how a file is
 * normally set up: {@code YawAxisRaw}, {@code YawAxisAlternate} and {@code YawAxis_Landing} all sit on the same stick.
 * The dialog names the others and the commander decides.
 */
public class AssignAxisDialog extends JDialog {
    private static final int MIN_WIDTH = 760;
    private static final int SCREEN_MARGIN = 40;
    private static final int COLUMNS = 4;
    private static final int CELL_HEIGHT = 26;
    private static final int TOGGLE_MIN_WIDTH = 160;
    private static final int TOGGLE_TEXT_MARGIN = 40;
    /**
     * Thousandths, so a value like {@code 0.05725} lands on the slider's own scale and a coarse step cannot move it.
     */
    private static final int DEADZONE_SCALE = 1000;

    /**
     * What SAVE decided.
     *
     * @param rangeSetting the throttle range element to write, or {@code null} when the choice did not change
     * @param rangeValue   its new value; ignored without {@code rangeSetting}
     */
    public record Choice(AxisAssignment axis, String rangeSetting, String rangeValue) {
    }

    /**
     * A throttle axis's range, as the file holds it.
     */
    public record RangeChoice(ThrottleRanges.Range range, String value) {
    }

    private final BindsValues.Axis axis;
    private final RangeChoice rangeChoice;
    private final List<CaptureControllers.Controller> controllers;
    private final EditedDeviceLabels labels;
    private final BindingSlotDisplayFormatter formatter;
    /**
     * The other controls an input is on, for what the dialog says about it.
     */
    private final Function<BindingInput, List<String>> holders;

    private final ControllerCapture capture = new ControllerCapture();
    private final Map<Integer, CaptureControllers.Controller> live = new HashMap<>();
    private final Map<String, JLabel> cells = new HashMap<>();
    private final Map<String, JComponent> tabOfDevice = new HashMap<>();
    /** Shown when the dialog opened with no controller at all; hidden once one connects. */
    private JComponent noControllersBanner;
    private final JLabel current = new JLabel();
    private final JLabel willHold = new JLabel(" ");
    private final JPanel alsoOn = transparentPanel(new BorderLayout());
    private final JButton undoButton = makeButtonSubtle(getText("bindings.assign.undo"));
    private final JButton clearButton = makeButtonSubtle(getText("bindings.assign.capture.clear"));
    private final HudToggleButton invertButton = new HudToggleButton("");
    private final HudSlider deadzoneSlider;
    private final int deadzoneAtOpen;
    private final HudToggleButton rangeButton = new HudToggleButton("");
    private final JButton saveButton = makeButton(getText("button.save"));
    private JTabbedPane sourceTabs;
    /**
     * Read only while an installation is missing (Storage S14b): the dialog shows the axis, and nothing in it changes
     * anything.
     */
    private final boolean viewOnly;

    private BindingInput picked;
    private ThrottleRanges.Reading rangeReading;
    private Choice result;

    /**
     * @param rangeChoice the throttle range, or {@code null} for an axis with none, or a file that does not hold it
     * @param viewOnly    true while BindForge is read only: everything shows, nothing changes
     */
    public AssignAxisDialog(Component parent, BindsValues.Axis axis, RangeChoice rangeChoice,
                            List<CaptureControllers.Controller> controllers, EditedDeviceLabels labels,
                            Function<BindingInput, List<String>> holders, boolean viewOnly) {
        super(SwingUtilities.getWindowAncestor(parent), getText("bindings.assign.dialogTitle"), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.axis = axis;
        this.rangeChoice = rangeChoice;
        this.controllers = controllers;
        this.labels = labels;
        this.formatter = new BindingSlotDisplayFormatter(labels);
        this.holders = holders;
        this.viewOnly = viewOnly;
        this.picked = axis.input().orElse(null);
        this.rangeReading = rangeChoice == null ? null : rangeChoice.range().read(rangeChoice.value());
        this.deadzoneAtOpen = Math.round(axis.deadzone() * DEADZONE_SCALE);
        this.deadzoneSlider = new HudSlider(0, DEADZONE_SCALE, 1, deadzoneAtOpen);
        buildUi();
        refresh();
    }

    /**
     * @return what to write, or empty when the commander left with nothing changed
     */
    public Optional<Choice> showDialog() {
        // WHY with no controller live: one may connect while the dialog is open - the Device Service starting late, or
        // a stick plugged in (Alan, 2026-11-10).
        boolean listening = changeable();
        if (listening) DeviceBus.register(this);
        try {
            setVisible(true);
        } finally {
            if (listening) DeviceBus.unregister(this);
        }
        return Optional.ofNullable(result);
    }

    @Subscribe
    public void onAxis(DeviceAxisEvent event) {
        SwingUtilities.invokeLater(() -> {
            CaptureControllers.Controller controller = live.get(event.deviceId());
            if (controller == null || event.axisIndex() < 0 || event.axisIndex() >= controller.axes().size()) return;
            capture.wholeAxis(controller.name(), controller.axes().get(event.axisIndex()), event.value())
                    .ifPresent(captured -> pick(captured.input()));
        });
    }

    @Subscribe
    public void onDisconnected(DeviceDisconnectedEvent event) {
        SwingUtilities.invokeLater(() -> {
            CaptureControllers.Controller controller = live.get(event.deviceId());
            if (controller != null) capture.release(controller.name());
        });
    }

    /**
     * A controller connected while the dialog is open (Alan, 2026-11-10): opened while the services were starting, the
     * dialog listed every controller as unplugged and none ever came to life. Its tab becomes live - or is added.
     */
    @Subscribe
    public void onConnected(DeviceConnectedEvent event) {
        SwingUtilities.invokeLater(() -> connect(CaptureControllers.connected(event.device(), labels)));
    }

    private void connect(CaptureControllers.Controller controller) {
        if (!usable(controller)) {
            if (sourceTabs.indexOfTab(controller.tabTitle()) < 0) addGreyedTab(controller);
            return;
        }
        live.values().removeIf(c -> c.name() != null && c.name().equalsIgnoreCase(controller.name()));
        JComponent old = tabOfDevice.get(controller.name().toLowerCase(Locale.ROOT));
        JComponent tab = controllerTab(controller);
        int index = old == null ? -1 : sourceTabs.indexOfComponent(old);
        if (index < 0) {
            sourceTabs.addTab(controller.tabTitle(), tab);
        } else {
            sourceTabs.setComponentAt(index, tab);
        }
        if (noControllersBanner != null) noControllersBanner.setVisible(false);
        refresh();
    }

    // ------------------------------------------------------------------ layout

    private void buildUi() {
        JPanel content = transparentPanel(new BorderLayout(0, 10));
        BindingDisplayNames.ControlName control = BindingDisplayNames.lookup(axis.id());
        content.add(hudReadoutValue(controlPath(control) + "   ·   " + axis.id(), HUD_COLOR_ROLE_PRIMARY_TEXT),
                BorderLayout.NORTH);

        JPanel stack = transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 8, 0);
        stack.add(bindingCard(), gbc);
        if (viewOnly) {
            stack.add(HudBanner.multiline(getText("bindings.readOnly.viewOnly"), StatusBadge.State.STANDBY), gbc);
        } else if (!axis.editable()) {
            stack.add(HudBanner.multiline(getText("bindings.axis.notEditable"), StatusBadge.State.OFFLINE), gbc);
        }
        stack.add(sources(), gbc);
        if (controllers.isEmpty()) {
            noControllersBanner = HudBanner.multiline(getText("bindings.assign.noControllers"), StatusBadge.State.INFO);
            stack.add(noControllersBanner, gbc);
        }
        stack.add(options(), gbc);
        content.add(stack, BorderLayout.CENTER);

        HudSection section = HudSection.flat(getText("bindings.axis.section"), new BorderLayout());
        section.body().add(content, BorderLayout.CENTER);

        JButton backButton = makeButtonSubtle(getText("button.back"));
        backButton.addActionListener(e -> dispose());
        saveButton.addActionListener(e -> save());

        setContentPane(AppTheme.hudModalScaffold(HudModalSpec.builder()
                .title(control.name())
                .onClose(this::dispose)
                .body(section)
                .scrollBody(true)
                .primary(saveButton)
                .dismiss(backButton)
                .build()));
        if (changeable()) getRootPane().setDefaultButton(saveButton);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        fitToScreen();
        setLocationRelativeTo(getOwner());
        setResizable(false);
    }

    private void fitToScreen() {
        GraphicsConfiguration screen = getOwner() != null ? getOwner().getGraphicsConfiguration() : getGraphicsConfiguration();
        Rectangle bounds = screen.getBounds();
        Insets taskbar = Toolkit.getDefaultToolkit().getScreenInsets(screen);
        Dimension preferred = getPreferredSize();
        int width = Math.min(Math.max(preferred.width, MIN_WIDTH), bounds.width - taskbar.left - taskbar.right - SCREEN_MARGIN);
        int height = Math.min(preferred.height, bounds.height - taskbar.top - taskbar.bottom - SCREEN_MARGIN);
        setSize(width, height);
    }

    private String controlPath(BindingDisplayNames.ControlName control) {
        String section = getText(control.section().getLabelKey());
        return control.group() == null ? section : section + " › " + control.group();
    }

    /**
     * What the control holds now and what it will hold, as a slot's card does in the button dialog.
     */
    private JComponent bindingCard() {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setOpaque(true);
        card.setBackground(HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HUD_COLOR_ROLE_PRIMARY_ACTION, 2),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        undoButton.addActionListener(e -> pick(axis.input().orElse(null)));
        clearButton.addActionListener(e -> pick(null));
        JPanel buttons = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(undoButton);
        buttons.add(clearButton);
        JPanel top = transparentPanel(new BorderLayout());
        top.add(hudGroupLabel(getText("bindings.axis.binding").toUpperCase()), BorderLayout.WEST);
        top.add(buttons, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        current.setText(textOf(axis.input().orElse(null)));
        current.setForeground(HUD_COLOR_ROLE_PRIMARY_TEXT);
        willHold.setForeground(HUD_COLOR_ROLE_PRIMARY_ACTION);
        JPanel values = transparentPanel(new GridLayout(0, 1, 0, 2));
        values.add(current);
        values.add(willHold);
        JPanel body = transparentPanel(new BorderLayout(0, 4));
        body.add(values, BorderLayout.NORTH);
        body.add(alsoOn, BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /**
     * A tab per controller, showing only its axes. Greyed for one with no name, which cannot be written, and for a
     * gamepad, whose sticks are not {@code Joy_} axes: what the game writes for them is still being measured (E7).
     */
    private JComponent sources() {
        sourceTabs = AppTheme.makeSectionTabs();
        for (CaptureControllers.Controller controller : controllers) {
            if (usable(controller)) {
                sourceTabs.addTab(controller.tabTitle(), controllerTab(controller));
            } else {
                addGreyedTab(controller);
            }
        }
        axis.input().ifPresent(this::bringForward);
        return sourceTabs;
    }

    /** Whether a controller's axes can be picked here: named, not a gamepad, with axes. */
    private static boolean usable(CaptureControllers.Controller controller) {
        return controller.named() && !controller.gamepad() && !controller.axes().isEmpty();
    }

    private void addGreyedTab(CaptureControllers.Controller controller) {
        sourceTabs.addTab(controller.tabTitle(), transparentPanel(new BorderLayout()));
        int index = sourceTabs.getTabCount() - 1;
        sourceTabs.setEnabledAt(index, false);
        sourceTabs.setToolTipTextAt(index, getText(!controller.named()
                ? "bindings.assign.controller.unnamed" : "bindings.axis.controller.gamepad"));
    }

    /** One usable controller's tab - live, with where its axes rest, when it is connected. */
    private JComponent controllerTab(CaptureControllers.Controller controller) {
        if (controller.connected()) {
            live.put(controller.liveId(), controller);
            CaptureControllers.restAxes(capture, controller);
        }
        JPanel tab = transparentPanel(new BorderLayout(0, 6));
        tab.setBorder(BorderFactory.createEmptyBorder(6, 6, 3, 6));
        tab.add(HudBanner.multiline(getText(controller.connected()
                        ? "bindings.axis.controller.hint" : "bindings.axis.controller.notConnected"),
                StatusBadge.State.INFO), BorderLayout.NORTH);
        tab.add(axisGrid(controller), BorderLayout.CENTER);
        tabOfDevice.put(controller.name().toLowerCase(Locale.ROOT), tab);
        return tab;
    }

    private JComponent axisGrid(CaptureControllers.Controller controller) {
        JPanel grid = new JPanel(new GridLayout(0, COLUMNS, 4, 4));
        grid.setOpaque(false);
        for (GameAxis gameAxis : controller.axes()) {
            String token = gameAxis.token();
            BindingInput input = new BindingInput(controller.name(), token);
            JLabel cell = new JLabel(labels.label(controller.name(), token).orElseGet(() -> formatter.formatBindingToken(token)),
                    SwingConstants.CENTER);
            cell.setOpaque(true);
            cell.setFont(cell.getFont().deriveFont(Font.PLAIN, 11f));
            cell.setPreferredSize(new Dimension(0, CELL_HEIGHT));
            cell.setToolTipText(controller.name() + " " + token);
            if (changeable()) {
                cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                cell.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e)) pick(input);
                    }
                });
            }
            cells.put(cellKey(input), cell);
            grid.add(cell);
        }
        return grid;
    }

    private JComponent options() {
        JPanel options = transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 0, 4, 10);

        invertButton.setSelected(axis.inverted());
        invertButton.addActionListener(e -> refresh());
        gbc.gridy = 0;
        addOption(options, gbc, getText("bindings.axis.direction"), invertButton, false);

        deadzoneSlider.setValueFormatter(AssignAxisDialog::deadzoneText);
        deadzoneSlider.addChangeListener(e -> refresh());
        gbc.gridy = 1;
        addOption(options, gbc, getText("bindings.axis.deadzone"), deadzoneSlider, true);

        if (rangeChoice != null) {
            rangeButton.setSelected(rangeReading == ThrottleRanges.Reading.FORWARD_ONLY);
            rangeButton.addActionListener(e -> {
                rangeReading = rangeReading == ThrottleRanges.Reading.FORWARD_ONLY
                        ? ThrottleRanges.Reading.FULL_RANGE : ThrottleRanges.Reading.FORWARD_ONLY;
                refresh();
            });
            gbc.gridy = 2;
            addOption(options, gbc, getText("bindings.axis.throttleRange"), rangeButton, false);
        }
        return options;
    }

    private static void addOption(JPanel panel, GridBagConstraints gbc, String label, JComponent control, boolean wide) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(hudReadoutLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = wide ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        panel.add(control, gbc);
    }

    public static String deadzoneText(int thousandths) {
        return String.format("%.1f%%", thousandths / 10.0);
    }

    // ------------------------------------------------------------------ choosing

    private void pick(BindingInput input) {
        if (!changeable()) return;
        picked = input;
        if (input != null) bringForward(input);
        refresh();
    }

    /**
     * Whether anything in the dialog may change: an axis the writer knows, while BindForge is not read only.
     */
    private boolean changeable() {
        return axis.editable() && !viewOnly;
    }

    private void bringForward(BindingInput input) {
        JComponent tab = tabOfDevice.get(input.device().toLowerCase(Locale.ROOT));
        if (tab != null) sourceTabs.setSelectedComponent(tab);
    }

    /**
     * The value at open when the slider was left alone: the slider's scale must not rewrite a value it merely shows.
     */
    private float deadzone() {
        return deadzoneSlider.getValue() == deadzoneAtOpen
                ? axis.deadzone() : deadzoneSlider.getValue() / (float) DEADZONE_SCALE;
    }

    private boolean rangeChanged() {
        return rangeChoice != null && rangeReading != rangeChoice.range().read(rangeChoice.value());
    }

    private boolean bindingChanged() {
        return !Objects.equals(picked, axis.input().orElse(null));
    }

    private boolean changed() {
        return bindingChanged() || invertButton.isSelected() != axis.inverted() || deadzone() != axis.deadzone()
                || rangeChanged();
    }

    private void save() {
        if (!changeable() || !changed()) return;
        result = new Choice(new AxisAssignment(axis.id(), picked, invertButton.isSelected(), deadzone()),
                rangeChanged() ? rangeChoice.range().setting() : null,
                rangeChanged() ? rangeChoice.range().valueFor(rangeReading) : null);
        dispose();
    }

    // ------------------------------------------------------------------ showing

    private void refresh() {
        willHold.setText(bindingChanged() ? "→ " + textOf(picked) : " ");
        undoButton.setVisible(bindingChanged());
        clearButton.setEnabled(changeable() && picked != null);
        showToggle(invertButton, getText(invertButton.isSelected() ? "bindings.axis.inverted" : "bindings.axis.normal"));
        invertButton.setEnabled(changeable());
        deadzoneSlider.setEnabled(changeable());
        if (rangeChoice != null) {
            showToggle(rangeButton, rangeText());
            rangeButton.setEnabled(changeable());
        }
        showAlsoOn();
        for (Map.Entry<String, JLabel> entry : cells.entrySet()) {
            boolean active = picked != null && entry.getKey().equals(cellKey(picked));
            boolean own = axis.input().map(input -> entry.getKey().equals(cellKey(input))).orElse(false);
            JLabel cell = entry.getValue();
            cell.setBackground(active ? HUD_COLOR_ROLE_ASSIGNED_INPUT_BACKGROUND : HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
            cell.setForeground(active ? HUD_COLOR_ROLE_PRIMARY_TEXT
                    : own ? HUD_COLOR_ROLE_SUCCESS : HUD_COLOR_ROLE_SECONDARY_TEXT);
            cell.setBorder(active
                    ? BorderFactory.createLineBorder(HUD_COLOR_ROLE_PRIMARY_ACTION, 2)
                    : BorderFactory.createLineBorder(HUD_COLOR_ROLE_SECONDARY_BORDER));
        }
        saveButton.setEnabled(changeable() && changed());
        if (viewOnly) {
            for (JComponent off : List.of(clearButton, invertButton, deadzoneSlider, rangeButton, saveButton)) {
                off.setToolTipText(getText("bindings.readOnly.tooltip"));
            }
        }
        if (isDisplayable() && getPreferredSize().height > getHeight()) {
            Point at = getLocation();
            pack();
            fitToScreen();
            setLocation(at);
        }
    }

    /**
     * WHY the width: a HUD toggle fixes its size when it is built, and these two change their words as they are pressed.
     */
    private static void showToggle(HudToggleButton button, String text) {
        button.setText(text.toUpperCase());
        int width = button.getFontMetrics(button.getFont()).stringWidth(button.getText()) + TOGGLE_TEXT_MARGIN;
        button.setPreferredSize(new Dimension(Math.max(width, TOGGLE_MIN_WIDTH), HUD_BUTTON_HEIGHT));
        button.revalidate();
    }

    private String rangeText() {
        return switch (rangeReading) {
            case FULL_RANGE -> getText("bindings.axis.throttleRange.full");
            case FORWARD_ONLY -> getText("bindings.axis.throttleRange.forward");
            case UNRECOGNISED -> getText("bindings.axis.throttleRange.unknown", rangeChoice.value());
        };
    }

    private void showAlsoOn() {
        alsoOn.removeAll();
        List<String> others = picked == null ? List.of() : holders.apply(picked);
        if (!others.isEmpty()) {
            alsoOn.add(HudBanner.multiline(getText("bindings.axis.alsoOn", String.join(", ", others)),
                    StatusBadge.State.INFO), BorderLayout.CENTER);
        }
        alsoOn.revalidate();
        alsoOn.repaint();
    }

    private String textOf(BindingInput input) {
        return input == null ? getText("bindings.status.notDefined")
                : formatter.formatInputs(input.device(), List.of(), input.key());
    }

    private static String cellKey(BindingInput input) {
        return (input.device() + "|" + input.key()).toLowerCase(Locale.ROOT);
    }
}
