package elite.intel.ui.widget;

import elite.intel.ai.hands.BindingConflictScanner;
import elite.intel.ai.hands.EliteKeyboardKeys;
import elite.intel.ai.hands.KeyBindingsParser;
import elite.intel.ai.hands.ReservedKeyChords;

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
    /**
     * Width units every key row must sum to. A shared total means one unit is the same pixel width in
     * every row, so same-size keys render identically and the key columns line up; {@link #row} fails
     * fast if a row's definition drifts from it.
     */
    private static final double UNITS_PER_ROW = 15;

    private final String bindingId;
    private final Map<String, KeyBindingsParser.KeyBinding> existingBindings;
    /**
     * The keys the commander has on the game menu, which no other control may use - read once, because
     * the binding map behind this view does not change while the dialog is open. Empty while the game
     * menu itself is being edited: that control is the one place its own key belongs.
     */
    private final Set<String> gameMenuKeys;
    private final Map<String, JLabel> mainKeyCells = new HashMap<>();
    private final Map<String, JLabel> modifierCells = new HashMap<>();
    private Set<String> heldModifiers = Set.of();
    private String currentKey;

    private record Key(String token, String label, double width, boolean modifier) {
    }

    public KeyboardAvailabilityView(String bindingId, Map<String, KeyBindingsParser.KeyBinding> existingBindings) {
        this.bindingId = bindingId;
        this.existingBindings = existingBindings == null ? Map.of() : existingBindings;
        this.gameMenuKeys = ReservedKeyChords.GAME_MENU_ACTION.equals(bindingId)
                ? Set.of()
                : ReservedKeyChords.gameMenuKeys(this.existingBindings);
        setOpaque(false);
        setLayout(new GridBagLayout());
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
        // Each row's key widths sum to UNITS_PER_ROW (enforced in row()).
        int y = 0;
        addRow(y++, 0, row(
                k("Key_Escape", "Esc"), gap(1),
                k("Key_F1", "F1"), k("Key_F2", "F2"), k("Key_F3", "F3"), k("Key_F4", "F4"), gap(0.5),
                k("Key_F5", "F5"), k("Key_F6", "F6"), k("Key_F7", "F7"), k("Key_F8", "F8"), gap(0.5),
                k("Key_F9", "F9"), k("Key_F10", "F10"), k("Key_F11", "F11"), k("Key_F12", "F12")));
        addRow(y++, CELL_GAP, row(
                k("Key_Grave", "`"), k("Key_1", "1"), k("Key_2", "2"), k("Key_3", "3"), k("Key_4", "4"),
                k("Key_5", "5"), k("Key_6", "6"), k("Key_7", "7"), k("Key_8", "8"), k("Key_9", "9"),
                k("Key_0", "0"), k("Key_Minus", "-"), k("Key_Equals", "="), w("Key_Backspace", "Bksp", 2)));
        addRow(y++, CELL_GAP, row(
                w("Key_Tab", "Tab", 1.5),
                k("Key_Q", "Q"), k("Key_W", "W"), k("Key_E", "E"), k("Key_R", "R"), k("Key_T", "T"),
                k("Key_Y", "Y"), k("Key_U", "U"), k("Key_I", "I"), k("Key_O", "O"), k("Key_P", "P"),
                k("Key_LeftBracket", "["), k("Key_RightBracket", "]"), w("Key_BackSlash", "\\", 1.5)));
        addRow(y++, CELL_GAP, row(
                w("Key_CapsLock", "Caps", 1.75),
                k("Key_A", "A"), k("Key_S", "S"), k("Key_D", "D"), k("Key_F", "F"), k("Key_G", "G"),
                k("Key_H", "H"), k("Key_J", "J"), k("Key_K", "K"), k("Key_L", "L"),
                k("Key_SemiColon", ";"), k("Key_Apostrophe", "'"), w("Key_Return", "Enter", 2.25)));
        addRow(y++, CELL_GAP, row(
                mod("Key_LeftShift", "Shift", 2.25),
                k("Key_Z", "Z"), k("Key_X", "X"), k("Key_C", "C"), k("Key_V", "V"), k("Key_B", "B"),
                k("Key_N", "N"), k("Key_M", "M"), k("Key_Comma", ","), k("Key_Period", "."), k("Key_Slash", "/"),
                mod("Key_RightShift", "Shift", 2.75)));
        addRow(y++, CELL_GAP, row(
                mod("Key_LeftControl", "Ctrl", 1.5), mod("Key_LeftAlt", "Alt", 1.5),
                w("Key_Space", "Space", 9),
                mod("Key_RightAlt", "Alt", 1.5), mod("Key_RightControl", "Ctrl", 1.5)));
        addRow(y, 6, row(
                k("Key_LeftArrow", "←"), k("Key_UpArrow", "↑"),
                k("Key_DownArrow", "↓"), k("Key_RightArrow", "→"), gap(11)));
    }

    /**
     * Adds one key row to the vertical stack, pinned to {@code gridx=0} and stretched to the full
     * width ({@code fill=HORIZONTAL}, {@code weightx=1}) so every row spans the same width; the keys
     * within it are then flushed to that row's edges by {@link ProportionalRowLayout}. {@code topGap}
     * is the vertical spacing above the row (matching the horizontal {@link #CELL_GAP} between keys;
     * larger before the arrow cluster).
     */
    private void addRow(int gridy, int topGap, JPanel rowPanel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = gridy;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(topGap, 0, 0, 0);
        add(rowPanel, gbc);
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
        double units = 0;
        for (Key key : keys) {
            units += key.width();
        }
        // Fail fast: a row whose units drift from UNITS_PER_ROW would silently distort key widths
        // (a unit would be a different pixel size in this row than in the others).
        if (Math.abs(units - UNITS_PER_ROW) > 1e-9) {
            throw new IllegalStateException(
                    "Keyboard row must sum to " + UNITS_PER_ROW + " units but was " + units);
        }
        // Custom ProportionalRowLayout instead of GridBagLayout: GridBag rounds fractional column
        // weights to whole pixels and centres the leftover, so each row drifts a few px and the edges
        // never line up. ProportionalRowLayout distributes pixels by cumulative rounding, so the
        // widths sum exactly to the row width - first key at x=0, last key flush to the right edge.
        JPanel rowPanel = new JPanel(new ProportionalRowLayout(CELL_GAP));
        rowPanel.setOpaque(false);
        for (Key key : keys) {
            JComponent cell = cell(key);
            cell.putClientProperty(WEIGHT_KEY, key.width());
            rowPanel.add(cell);
        }
        return rowPanel;
    }

    /** Client-property key carrying a cell's width in units, read by {@link ProportionalRowLayout}. */
    private static final String WEIGHT_KEY = "eliteIntel.kbd.weight";

    /**
     * Distributes {@code available} pixels across cells proportionally to {@code weights}, using
     * cumulative rounding so the returned widths sum EXACTLY to {@code available} (no per-cell drift,
     * no leftover gap that would leave the last cell short of the edge). Package-private and pure so
     * the exact-sum guarantee is unit-testable without AWT.
     *
     * @param weights   per-cell width units, at least one and all positive
     * @param available total pixels to share out (already net of inter-cell gaps)
     * @return one width per weight, summing to {@code available}
     */
    static int[] distribute(double[] weights, int available) {
        double totalWeight = 0;
        for (double weight : weights) {
            totalWeight += weight;
        }
        int[] widths = new int[weights.length];
        double acc = 0;
        int placed = 0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i] / totalWeight * available;
            int end = (int) Math.round(acc);
            widths[i] = end - placed;
            placed = end;
        }
        return widths;
    }

    /**
     * Lays a row of keys left-to-right, each sized proportionally to its unit weight with a fixed
     * {@code gap} between keys. Pixel widths are assigned by cumulative rounding so they sum exactly
     * to the available width: the first key sits at x=0 and the last key ends flush against the right
     * edge, in every row - which GridBagLayout cannot guarantee with fractional weights.
     */
    private static final class ProportionalRowLayout implements LayoutManager {
        private final int gap;

        ProportionalRowLayout(int gap) {
            this.gap = gap;
        }

        @Override public void addLayoutComponent(String name, Component comp) { }

        @Override public void removeLayoutComponent(Component comp) { }

        @Override public Dimension preferredLayoutSize(Container parent) {
            int h = 0;
            for (Component c : parent.getComponents()) {
                h = Math.max(h, c.getPreferredSize().height);
            }
            Insets in = parent.getInsets();
            return new Dimension(0, h + in.top + in.bottom);
        }

        @Override public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override public void layoutContainer(Container parent) {
            Component[] comps = parent.getComponents();
            int n = comps.length;
            if (n == 0) {
                return;
            }
            Insets in = parent.getInsets();
            int available = parent.getWidth() - in.left - in.right - gap * (n - 1);
            double[] weights = new double[n];
            for (int i = 0; i < n; i++) {
                weights[i] = weightOf(comps[i]);
            }
            int[] widths = distribute(weights, available);
            int h = parent.getHeight() - in.top - in.bottom;
            int x = in.left;
            for (int i = 0; i < n; i++) {
                comps[i].setBounds(x, in.top, widths[i], h);
                x += widths[i] + gap;
            }
        }

        private static double weightOf(Component c) {
            Object w = (c instanceof JComponent jc) ? jc.getClientProperty(WEIGHT_KEY) : null;
            return (w instanceof Number num) ? num.doubleValue() : 1.0;
        }
    }

    private JComponent cell(Key key) {
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
        // Reserved keys and chords (the game-menu key, Alt+F4, Linux Ctrl+Alt+F*) can never be assigned:
        // flag amber so the user sees why a key is unavailable - the game-menu key on its own, the OS
        // chords as soon as their modifiers are held.
        if (ReservedKeyChords.isReserved(token, heldModifiers, gameMenuKeys)) {
            return HUD_COLOR_ROLE_WARNING;
        }
        boolean conflicts = BindingConflictScanner.candidateConflict(
                bindingId, token, heldModifiers, existingBindings) != null;
        return conflicts ? HUD_COLOR_ROLE_DANGER : HUD_COLOR_ROLE_SUCCESS;
    }
}
