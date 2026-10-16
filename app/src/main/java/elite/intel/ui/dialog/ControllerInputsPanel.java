package elite.intel.ui.dialog;

import elite.intel.bindforge.devicefiles.ButtonMapLabels;
import elite.intel.bindforge.devicefiles.SavedDeviceLabels;
import elite.intel.bindforge.devices.CaptureControllers;
import elite.intel.bindforge.devices.DeviceEditorInputs;
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
 * One controller's page in the Assign Control dialog: its BUTTONS and HATS, each input named the way the
 * Device Editor names it. Clicking an input picks it, for anyone working from the list rather than the hardware;
 * pressing it on the controller does the same through the dialog.
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
    ControllerInputsPanel(CaptureControllers.Controller controller, SavedDeviceLabels labels,
                          Predicate<String> inUse, Consumer<String> onPick) {
        super(new BorderLayout());
        setOpaque(false);
        this.controller = controller;
        this.inUse = inUse;

        List<String> buttons = new ArrayList<>();
        for (int b = 1; b <= controller.buttonCount(); b++) buttons.add(ButtonMapLabels.buttonToken(b));
        List<String> hats = new ArrayList<>();
        for (int h = 1; h <= controller.hatCount(); h++) hats.addAll(DeviceEditorInputs.hatTokens(h));

        pages = AppTheme.makeCompactTabs();
        pages.addTab(getText("bindings.assign.page.buttons"), page(buttons, labels, onPick));
        pages.addTab(getText("bindings.assign.page.hats"), page(hats, labels, onPick));
        if (hats.isEmpty()) {
            pages.setEnabledAt(1, false);
            pages.setToolTipTextAt(1, getText("bindings.assign.page.hats.none"));
        }
        add(pages, BorderLayout.CENTER);
        refresh();
    }

    String deviceName() {
        return controller.name();
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
        JLabel cell = activeToken == null ? null : cells.get(activeToken);
        if (bringForward && cell != null) {
            pages.setSelectedIndex(isHat(activeToken) ? 1 : 0);
            Runnable group = showGroup.get(activeToken);
            if (group != null) group.run();
            cell.scrollRectToVisible(new Rectangle(cell.getSize()));
        }
    }

    private JComponent page(List<String> tokens, SavedDeviceLabels labels, Consumer<String> onPick) {
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

    private JComponent group(List<String> tokens, SavedDeviceLabels labels, Consumer<String> onPick) {
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
            cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cell.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) onPick.accept(token);
                }
            });
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
        return token.startsWith("Joy_POV");
    }

    private void refresh() {
        for (Map.Entry<String, JLabel> entry : cells.entrySet()) {
            String token = entry.getKey();
            JLabel cell = entry.getValue();
            boolean held = token.equals(activeToken) || assigned.contains(token);
            cell.setBackground(held ? HUD_COLOR_ROLE_ASSIGNED_INPUT_BACKGROUND : HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
            if (held) {
                cell.setForeground(HUD_COLOR_ROLE_PRIMARY_TEXT);
            } else {
                boolean taken = !own.contains(token) && used.computeIfAbsent(token, inUse::test);
                cell.setForeground(taken ? HUD_COLOR_ROLE_DANGER : HUD_COLOR_ROLE_SUCCESS);
            }
            cell.setBorder(token.equals(activeToken)
                    ? BorderFactory.createLineBorder(HUD_COLOR_ROLE_PRIMARY_ACTION, 2)
                    : BorderFactory.createLineBorder(HUD_COLOR_ROLE_SECONDARY_BORDER));
        }
        repaint();
    }
}
