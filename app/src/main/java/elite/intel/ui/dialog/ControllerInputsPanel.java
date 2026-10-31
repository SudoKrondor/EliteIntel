package elite.intel.ui.dialog;

import elite.intel.bindforge.devices.CaptureControllers;
import elite.intel.bindforge.devices.ControllerCapture;
import elite.intel.bindforge.devices.DeviceEditorInputs;
import elite.intel.bindforge.storage.EditedDeviceLabels;
import elite.intel.devices.model.GameAxis;
import elite.intel.ui.theme.AppTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * One controller's page in the Assign Control dialog: its BUTTONS, HATS and AXES, each input named the way the
 * Device Editor names it. Clicking a button or hat picks it, for anyone working from the list rather than the
 * hardware; pressing it on the controller does the same through the dialog. The AXES page only shows: an axis
 * cell stands for both of its directions, so a click could not say which one to pick, and moving the axis does.
 * <p>
 * <strong>Colours:</strong> an input the control holds right now, in either slot, sits on blue - the one in the
 * slot being edited also outlined in orange. Otherwise green is free and red is held by another control. An input
 * the control held when the dialog opened is always green for it, whoever shares it: clearing a slot must not
 * paint its own input as taken, and going back to it adds no clash.
 * <p>
 * Built for a named controller only - an unnamed one's tab is greyed by the dialog.
 */
final class ControllerInputsPanel extends JPanel {

    private static final int COLUMNS = 4;
    private static final int CELL_HEIGHT = 26;
    private static final int VISIBLE_ROWS = 8;
    /**
     * One group fills the page exactly, so a 32-button stick shows whole and a larger device is split, never
     * scrolled - the Device Editor groups its buttons the same way.
     */
    private static final int INPUTS_PER_GROUP = COLUMNS * VISIBLE_ROWS;

    private final CaptureControllers.Controller controller;
    private final Predicate<String> inUse;
    private final Map<String, JLabel> cells = new LinkedHashMap<>();
    /**
     * Whether each token is held elsewhere, for the slot being edited - asked once per slot.
     */
    private final Map<String, Boolean> used = new HashMap<>();
    /**
     * Brings each grouped token's group forward; a page of one group has no entry.
     */
    private final Map<String, Runnable> showGroup = new HashMap<>();
    private final JTabbedPane pages;
    /**
     * What the slot being edited will hold, when it is on this controller.
     */
    private String activeToken;
    /**
     * Every input on this controller the control holds right now, modifiers included.
     */
    private Set<String> assigned = Set.of();
    /**
     * The inputs on this controller the control held, pressed alone, when the dialog opened.
     */
    private Set<String> own = Set.of();

    /**
     * @param inUse  whether another control already holds this token pressed on its own, for the slot being edited
     * @param onPick the token clicked
     */
    ControllerInputsPanel(CaptureControllers.Controller controller, EditedDeviceLabels labels,
                          Predicate<String> inUse, Consumer<String> onPick) {
        super(new BorderLayout());
        setOpaque(false);
        this.controller = controller;
        this.inUse = inUse;

        List<String> buttons = buttonTokens(controller);
        List<String> hats = hatTokens(controller);
        List<String> axes = controller.axes().stream().map(GameAxis::token).toList();

        pages = AppTheme.makeCompactTabs();
        pages.addTab(getText("bindings.assign.page.buttons"), page(buttons, labels, onPick));
        pages.addTab(getText("bindings.assign.page.hats"), page(hats, labels, onPick));
        pages.addTab(getText("bindings.assign.page.axes"), page(axes, labels, null));
        if (hats.isEmpty()) {
            pages.setEnabledAt(1, false);
            pages.setToolTipTextAt(1, getText("bindings.assign.page.hats.none"));
        }
        if (axes.isEmpty()) pages.setEnabledAt(2, false);
        add(pages, BorderLayout.CENTER);
        refresh();
    }

    static List<String> buttonTokens(CaptureControllers.Controller controller) {
        List<String> buttons = new ArrayList<>();
        for (int sdlIndex = 0; sdlIndex < controller.buttonCount(); sdlIndex++) {
            ControllerCapture.buttonToken(controller.gamepad(), sdlIndex).ifPresent(buttons::add);
        }
        return List.copyOf(buttons);
    }

    static List<String> hatTokens(CaptureControllers.Controller controller) {
        List<String> hats = new ArrayList<>();
        for (int h = 1; h <= controller.hatCount(); h++) {
            // WHY: SDL reports a gamepad's D-pad as its first hat; the game names it with its own GamePad tokens.
            if (controller.gamepad() && h == 1) hats.addAll(ControllerCapture.gamepadDPadTokens());
            else hats.addAll(DeviceEditorInputs.hatTokens(h));
        }
        return List.copyOf(hats);
    }

    /**
     * @param sdlIndex SDL's button index, from 0
     * @return the token pressing it captures, as the page lists it; empty for a button no binding can use
     */
    Optional<String> buttonToken(int sdlIndex) {
        return ControllerCapture.buttonToken(controller.gamepad(), sdlIndex);
    }

    Optional<GameAxis> axis(int sdlIndex) {
        return sdlIndex >= 0 && sdlIndex < controller.axes().size()
                ? Optional.of(controller.axes().get(sdlIndex)) : Optional.empty();
    }

    String deviceName() {
        return controller.name();
    }

    boolean gamepad() {
        return controller.gamepad();
    }

    /**
     * What the control holds on this controller. Called when the slot being edited changes, or a choice does: free
     * and used are asked again, since a slot's Hold is part of the answer.
     *
     * @param activeToken  what the slot being edited will hold here, or {@code null}
     * @param assigned     every input here either slot will hold, modifiers included
     * @param own          the inputs here the control held alone when the dialog opened
     * @param bringForward whether to show the active token's page and scroll to it
     */
    void show(String activeToken, Set<String> assigned, Set<String> own, boolean bringForward) {
        this.activeToken = activeToken;
        this.assigned = Set.copyOf(assigned);
        this.own = Set.copyOf(own);
        used.clear();
        refresh();
        String displayToken = displayToken(activeToken);
        JLabel cell = displayToken == null ? null : cells.get(displayToken);
        if (bringForward && cell != null) {
            pages.setSelectedIndex(isHat(displayToken) ? 1 : isAxis(displayToken) ? 2 : 0);
            Runnable group = showGroup.get(displayToken);
            if (group != null) group.run();
            cell.scrollRectToVisible(new Rectangle(cell.getSize()));
        }
    }

    private JComponent page(List<String> tokens, EditedDeviceLabels labels, Consumer<String> onPick) {
        if (tokens.size() <= INPUTS_PER_GROUP) return group(tokens, labels, onPick);
        JTabbedPane groups = AppTheme.makeCompactTabs();
        for (int from = 0; from < tokens.size(); from += INPUTS_PER_GROUP) {
            int to = Math.min(from + INPUTS_PER_GROUP, tokens.size());
            List<String> group = tokens.subList(from, to);
            int index = groups.getTabCount();
            // WHY positions, not token numbers: buttons run from 1 without gaps, so for BUTTONS the two agree,
            // and a hat page has no single number per token.
            groups.addTab((from + 1) + "–" + to, group(group, labels, onPick));
            for (String token : group) showGroup.put(token, () -> groups.setSelectedIndex(index));
        }
        return groups;
    }

    /**
     * @param onPick the token clicked, or {@code null} for a page that only shows
     */
    private JComponent group(List<String> tokens, EditedDeviceLabels labels, Consumer<String> onPick) {
        JPanel grid = new JPanel(new GridLayout(0, COLUMNS, 4, 4));
        grid.setOpaque(false);
        for (String token : tokens) {
            String label = labels.label(controller.name(), token).orElse(null);
            JLabel cell = new JLabel(label == null ? token : label, SwingConstants.CENTER);
            cell.setOpaque(true);
            cell.setFont(cell.getFont().deriveFont(Font.PLAIN, 11f));
            cell.setBackground(HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
            cell.setPreferredSize(new Dimension(0, CELL_HEIGHT));
            cell.setToolTipText(controller.name() + " " + token);
            if (onPick != null) {
                cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                cell.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e)) onPick.accept(token);
                    }
                });
            }
            cells.put(token, cell);
            grid.add(cell);
        }
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(grid, BorderLayout.NORTH);
        JScrollPane scroll = AppTheme.hudApplicationScrollPane(top);
        scroll.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(CELL_HEIGHT);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // WHY a fixed height: the page keeps its size whatever the controller has, so the dialog does not jump
        // between tabs. A group fills it, so it scrolls only when the screen caps the dialog (bind-editor.md,
        // Capture Dialog).
        scroll.setPreferredSize(new Dimension(0, VISIBLE_ROWS * (CELL_HEIGHT + 4) + 6));
        return scroll;
    }

    private static boolean isHat(String token) {
        return token.startsWith("Joy_POV") || token.startsWith("GamePad_DPad");
    }

    private static boolean isAxis(String token) {
        return token.startsWith("Joy_") && token.endsWith("Axis");
    }

    /**
     * Maps a binding token back to the cell this panel shows for it: an axis cell stands for both its directions
     * and a GamePad trigger token for the axis SDL reports it as.
     */
    static String displayToken(String token) {
        if (token == null) return null;
        if (token.startsWith("Pos_Joy_") || token.startsWith("Neg_Joy_")) {
            return token.substring(4);
        }
        return ControllerCapture.gamepadTriggerAxis(token).map(GameAxis::token).orElse(token);
    }

    /**
     * The binding tokens a cell stands for, which is what the control holds and what other controls clash with: an
     * axis cell stands for both of its directions, and a gamepad's trigger cell for its trigger token.
     */
    static List<String> bindingTokens(String cellToken, boolean gamepad) {
        if (!isAxis(cellToken)) return List.of(cellToken);
        if (gamepad) {
            Optional<String> trigger = Arrays.stream(GameAxis.values())
                    .filter(axis -> axis.token().equals(cellToken))
                    .findFirst()
                    .flatMap(ControllerCapture::gamepadTriggerToken);
            if (trigger.isPresent()) return List.of(trigger.get());
        }
        return List.of("Neg_" + cellToken, "Pos_" + cellToken);
    }

    private void refresh() {
        String activeCell = displayToken(activeToken);
        Set<String> assignedCells = new HashSet<>();
        assigned.forEach(assignedToken -> assignedCells.add(displayToken(assignedToken)));
        for (Map.Entry<String, JLabel> entry : cells.entrySet()) {
            String token = entry.getKey();
            JLabel cell = entry.getValue();
            boolean held = token.equals(activeCell) || assignedCells.contains(token);
            cell.setBackground(held ? HUD_COLOR_ROLE_ASSIGNED_INPUT_BACKGROUND : HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
            if (held) {
                cell.setForeground(HUD_COLOR_ROLE_PRIMARY_TEXT);
            } else {
                boolean taken = bindingTokens(token, controller.gamepad()).stream()
                        .anyMatch(binding -> !own.contains(binding) && used.computeIfAbsent(binding, inUse::test));
                cell.setForeground(taken ? HUD_COLOR_ROLE_DANGER : HUD_COLOR_ROLE_SUCCESS);
            }
            cell.setBorder(token.equals(activeCell)
                    ? BorderFactory.createLineBorder(HUD_COLOR_ROLE_PRIMARY_ACTION, 2)
                    : BorderFactory.createLineBorder(HUD_COLOR_ROLE_SECONDARY_BORDER));
        }
        repaint();
    }
}
