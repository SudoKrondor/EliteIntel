package elite.intel.ui.widget;

import elite.intel.ai.hands.BindingConflictScanner;
import elite.intel.ai.hands.EliteKeyboardKeys;
import elite.intel.ai.hands.KeyBindingsParser;

import javax.swing.*;
import java.awt.*;
import java.util.*;

import static elite.intel.ui.theme.HudPalette.*;

/**
 * A live QWERTY keyboard that shows, for the binding being edited, which main keys are safe to
 * assign given the modifiers currently held in the capture field. Each main key's glyph is tinted
 * by {@link BindingConflictScanner#candidateConflict}: green = free, red = would conflict,
 * grey = not assignable. Modifier keys highlight while held.
 * <p>
 * Call {@link #setHeldModifiers} whenever the held-modifier set changes; the view recolors live,
 * so holding Ctrl+Shift+Alt instantly shows which keys would still be free for that chord.
 */
public class KeyboardAvailabilityView extends JPanel {

    private static final int UNIT = 30;
    private static final int CELL_GAP = 2;

    private final String bindingId;
    private final Map<String, KeyBindingsParser.KeyBinding> existingBindings;
    private final Map<String, JLabel> mainKeyCells = new HashMap<>();
    private final Map<String, JLabel> modifierCells = new HashMap<>();
    private Set<String> heldModifiers = Set.of();
    private String currentKey;

    private record Key(String token, String label, double width, boolean modifier) {
    }

    public KeyboardAvailabilityView(String bindingId, Map<String, KeyBindingsParser.KeyBinding> existingBindings) {
        this.bindingId = bindingId;
        this.existingBindings = existingBindings == null ? Map.of() : existingBindings;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buildRows();
        refresh();
    }

    // Lock the height to the natural (preferred) height so the keyboard only ever resizes
    // horizontally. Without this, GridBag's shrink pass can squash the rows when a sibling row
    // (e.g. a conflict message) appears, even when there is spare vertical space elsewhere.
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, getPreferredSize().height);
    }

    /**
     * Updates the held-modifier set (Elite key tokens) and recolors every key.
     */
    public void setHeldModifiers(Collection<String> modifiers) {
        this.heldModifiers = modifiers == null ? Set.of() : new HashSet<>(modifiers);
        refresh();
    }

    /**
     * Marks the main key currently assigned to the binding being edited, so it reads as the
     * existing choice (highlighted) rather than as a free key. {@code null} clears the marker.
     */
    public void setCurrentKey(String key) {
        this.currentKey = key;
        refresh();
    }

    private void buildRows() {
        add(row(
                k("Key_Escape", "Esc"), gap(0.5),
                k("Key_F1", "F1"), k("Key_F2", "F2"), k("Key_F3", "F3"), k("Key_F4", "F4"),
                k("Key_F5", "F5"), k("Key_F6", "F6"), k("Key_F7", "F7"), k("Key_F8", "F8"),
                k("Key_F9", "F9"), k("Key_F10", "F10"), k("Key_F11", "F11"), k("Key_F12", "F12")));
        add(row(
                k("Key_Grave", "`"), k("Key_1", "1"), k("Key_2", "2"), k("Key_3", "3"), k("Key_4", "4"),
                k("Key_5", "5"), k("Key_6", "6"), k("Key_7", "7"), k("Key_8", "8"), k("Key_9", "9"),
                k("Key_0", "0"), k("Key_Minus", "-"), k("Key_Equals", "="), w("Key_Backspace", "Bksp", 2)));
        add(row(
                w("Key_Tab", "Tab", 1.5),
                k("Key_Q", "Q"), k("Key_W", "W"), k("Key_E", "E"), k("Key_R", "R"), k("Key_T", "T"),
                k("Key_Y", "Y"), k("Key_U", "U"), k("Key_I", "I"), k("Key_O", "O"), k("Key_P", "P"),
                k("Key_LeftBracket", "["), k("Key_RightBracket", "]"), w("Key_BackSlash", "\\", 1.5)));
        add(row(
                w("Key_CapsLock", "Caps", 1.75),
                k("Key_A", "A"), k("Key_S", "S"), k("Key_D", "D"), k("Key_F", "F"), k("Key_G", "G"),
                k("Key_H", "H"), k("Key_J", "J"), k("Key_K", "K"), k("Key_L", "L"),
                k("Key_SemiColon", ";"), k("Key_Apostrophe", "'"), w("Key_Return", "Enter", 2.25)));
        add(row(
                mod("Key_LeftShift", "Shift", 2.25),
                k("Key_Z", "Z"), k("Key_X", "X"), k("Key_C", "C"), k("Key_V", "V"), k("Key_B", "B"),
                k("Key_N", "N"), k("Key_M", "M"), k("Key_Comma", ","), k("Key_Period", "."), k("Key_Slash", "/"),
                mod("Key_RightShift", "Shift", 2.75)));
        add(row(
                mod("Key_LeftControl", "Ctrl", 1.5), mod("Key_LeftAlt", "Alt", 1.5),
                w("Key_Space", "Space", 6),
                mod("Key_RightAlt", "Alt", 1.5), mod("Key_RightControl", "Ctrl", 1.5)));
        add(Box.createVerticalStrut(6));
        add(row(
                k("Key_LeftArrow", "←"), k("Key_UpArrow", "↑"),
                k("Key_DownArrow", "↓"), k("Key_RightArrow", "→"), gap(8)));
    }

    private static Key k(String token, String label) {
        return new Key(token, label, 1, false);
    }

    private static Key w(String token, String label, double width) {
        return new Key(token, label, width, false);
    }

    private static Key mod(String token, String label, double width) {
        return new Key(token, label, width, true);
    }

    private static Key gap(double width) {
        return new Key(null, "", width, false);
    }

    private JPanel row(Key... keys) {
        // GridBagLayout with per-cell weightx = the key's width units, so the row stretches to
        // fill the available width and cells stay proportional (rectangular keys are fine).
        JPanel rowPanel = new JPanel(new GridBagLayout());
        rowPanel.setOpaque(false);
        rowPanel.setAlignmentX(LEFT_ALIGNMENT);
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, UNIT));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, CELL_GAP);
        int x = 0;
        for (Key key : keys) {
            gbc.gridx = x++;
            gbc.weightx = key.width();
            rowPanel.add(cell(key), gbc);
        }
        return rowPanel;
    }

    private Component cell(Key key) {
        if (key.token() == null) {
            JPanel spacer = new JPanel();
            spacer.setOpaque(false);
            spacer.setPreferredSize(new Dimension(0, UNIT));
            return spacer;
        }
        JLabel label = new JLabel(key.label(), SwingConstants.CENTER);
        label.setOpaque(true);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f)); // fits multi-char labels (F10-F12)
        label.setBackground(HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        label.setBorder(BorderFactory.createLineBorder(HUD_COLOR_ROLE_SECONDARY_BORDER));
        label.setPreferredSize(new Dimension(0, UNIT)); // width comes from the GridBag weight
        (key.modifier() ? modifierCells : mainKeyCells).put(key.token(), label);
        return label;
    }

    private void refresh() {
        for (Map.Entry<String, JLabel> entry : mainKeyCells.entrySet()) {
            String token = entry.getKey();
            JLabel cell = entry.getValue();
            if (token.equals(currentKey)) {
                // The binding's existing key: highlight it as the current choice, not as "free".
                cell.setForeground(HUD_COLOR_ROLE_PRIMARY_ACTION);
                cell.setBorder(BorderFactory.createLineBorder(HUD_COLOR_ROLE_PRIMARY_ACTION, 2));
            } else {
                cell.setForeground(statusColor(token));
                cell.setBorder(BorderFactory.createLineBorder(HUD_COLOR_ROLE_SECONDARY_BORDER));
            }
        }
        for (Map.Entry<String, JLabel> entry : modifierCells.entrySet()) {
            boolean held = heldModifiers.contains(entry.getKey());
            entry.getValue().setBackground(held ? HUD_COLOR_ROLE_PRIMARY_ACTION : HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
            entry.getValue().setForeground(held ? HUD_COLOR_ROLE_APPLICATION_BACKGROUND : HUD_COLOR_ROLE_SECONDARY_TEXT);
        }
        repaint();
    }

    private Color statusColor(String token) {
        if (!EliteKeyboardKeys.isAssignable(token)) {
            return HUD_COLOR_ROLE_DISABLED;
        }
        boolean conflicts = BindingConflictScanner.candidateConflict(
                bindingId, token, heldModifiers, existingBindings) != null;
        return conflicts ? HUD_COLOR_ROLE_DANGER : HUD_COLOR_ROLE_SUCCESS;
    }
}
