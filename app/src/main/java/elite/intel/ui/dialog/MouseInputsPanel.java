package elite.intel.ui.dialog;

import elite.intel.bindforge.model.BindingInput;

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
 * The MOUSE page of the Assign Control dialog (E4): a pad that captures a button pressed over it, and the wheel's
 * two directions to click.
 * <p>
 * <strong>Only a press over the pad is captured</strong> (Krondor, 2026-11-10, reversing the spec's pick list): the
 * mouse is how the dialog is worked, so a click anywhere else does what it always does. Over the pad, every button
 * is captured, the left one too - which the hint says.
 * <p>
 * <strong>The wheel is never captured</strong>: a hand resting on the mouse scrolls by accident in a way it does not
 * click by accident, and a stray scroll must not bind anything (bind-editor.md, <em>Mouse Inputs Are Chosen, Not
 * Captured</em>). Its two directions are cells to click.
 * <p>
 * Colours as on a controller's page: blue for what the control holds now (orange outline for the slot being
 * filled), else green for free and red for held by another control.
 */
final class MouseInputsPanel extends JPanel {

    static final String WHEEL_UP = "Pos_Mouse_ZAxis";
    static final String WHEEL_DOWN = "Neg_Mouse_ZAxis";

    private static final int PAD_HEIGHT = 140;
    private static final int CELL_HEIGHT = 26;

    private final Predicate<String> inUse;
    private final JLabel pad = new JLabel("", SwingConstants.CENTER);
    private final Map<String, JLabel> wheelCells = new LinkedHashMap<>();
    private final Map<String, Boolean> used = new HashMap<>();
    private String activeToken;
    private Set<String> assigned = Set.of();
    private Set<String> own = Set.of();
    private boolean enabled = true;

    /**
     * @param inUse  whether another control already holds this token, for the slot being edited
     * @param onPick the token pressed over the pad or clicked
     */
    MouseInputsPanel(Predicate<String> inUse, Consumer<String> onPick) {
        super(new BorderLayout(0, 8));
        setOpaque(false);
        this.inUse = inUse;

        pad.setOpaque(true);
        pad.setPreferredSize(new Dimension(0, PAD_HEIGHT));
        pad.setFont(pad.getFont().deriveFont(Font.BOLD, 13f));
        pad.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        pad.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!enabled) return;
                tokenForAwtButton(e.getButton()).ifPresent(onPick);
                e.consume();
            }
        });
        add(pad, BorderLayout.CENTER);

        JPanel wheel = new JPanel(new GridLayout(1, 0, 4, 4));
        wheel.setOpaque(false);
        JLabel title = new JLabel(getText("bindings.assign.mouse.wheel"));
        title.setForeground(HUD_COLOR_ROLE_SECONDARY_TEXT);
        wheel.add(title);
        wheel.add(wheelCell(WHEEL_UP, getText("bindings.assign.mouse.wheelUp"), onPick));
        wheel.add(wheelCell(WHEEL_DOWN, getText("bindings.assign.mouse.wheelDown"), onPick));
        add(wheel, BorderLayout.SOUTH);
        refresh();
    }

    /**
     * The game's token for a button as AWT numbers it. AWT counts left, middle, right; the game counts left, right,
     * middle ({@code Mouse_3} is the middle button that stock presets give Headlook), so the two swap. The side
     * buttons and any past them come through as AWT reports them: back is {@code Mouse_4}, forward {@code Mouse_5}.
     * The count is whatever the system reports, never capped.
     *
     * @return empty for {@link MouseEvent#NOBUTTON}
     */
    static Optional<String> tokenForAwtButton(int awtButton) {
        return switch (awtButton) {
            case MouseEvent.NOBUTTON -> Optional.empty();
            case MouseEvent.BUTTON2 -> Optional.of("Mouse_3");
            case MouseEvent.BUTTON3 -> Optional.of("Mouse_2");
            default -> awtButton > 0 ? Optional.of("Mouse_" + awtButton) : Optional.empty();
        };
    }

    static boolean isWheel(String token) {
        return WHEEL_UP.equals(token) || WHEEL_DOWN.equals(token);
    }

    String deviceName() {
        return BindingInput.MOUSE;
    }

    /** Read only while an installation is missing: a press over the pad does nothing. */
    void setPickEnabled(boolean enabled) {
        this.enabled = enabled;
        pad.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    /**
     * @param activeToken what the slot being edited will hold on the mouse, or {@code null}
     * @param assigned    every mouse input either slot will hold, modifiers included
     * @param own         the mouse inputs the control held alone when the dialog opened
     */
    void show(String activeToken, Set<String> assigned, Set<String> own) {
        this.activeToken = activeToken;
        this.assigned = Set.copyOf(assigned);
        this.own = Set.copyOf(own);
        used.clear();
        refresh();
    }

    private JLabel wheelCell(String token, String label, Consumer<String> onPick) {
        JLabel cell = new JLabel(label, SwingConstants.CENTER);
        cell.setOpaque(true);
        cell.setFont(cell.getFont().deriveFont(Font.PLAIN, 11f));
        cell.setPreferredSize(new Dimension(0, CELL_HEIGHT));
        cell.setToolTipText(BindingInput.MOUSE + " " + token);
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cell.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (enabled && SwingUtilities.isLeftMouseButton(e)) onPick.accept(token);
            }
        });
        wheelCells.put(token, cell);
        return cell;
    }

    private void refresh() {
        boolean buttonActive = activeToken != null && !isWheel(activeToken);
        pad.setText(buttonActive ? activeToken : getText("bindings.assign.mouse.pad"));
        pad.setBackground(buttonActive ? HUD_COLOR_ROLE_ASSIGNED_INPUT_BACKGROUND : HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        pad.setForeground(buttonActive ? HUD_COLOR_ROLE_PRIMARY_TEXT : HUD_COLOR_ROLE_SECONDARY_TEXT);
        pad.setBorder(BorderFactory.createLineBorder(
                buttonActive ? HUD_COLOR_ROLE_PRIMARY_ACTION : HUD_COLOR_ROLE_SECONDARY_BORDER, buttonActive ? 2 : 1));

        for (Map.Entry<String, JLabel> entry : wheelCells.entrySet()) {
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
