package elite.intel.ui.widget;

import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeCheckBox;
import static elite.intel.ui.theme.AppTheme.transparentPanel;
import static elite.intel.ui.theme.HudPalette.HUD_GAP;

/**
 * A page of {@link SettingToggle}s, each writing straight back to the setting it reads.
 * <p>
 * Standalone toggles share one grid of {@value #COLUMN_COUNT} columns, filled top to bottom so reading down
 * a column follows the order they were given. A toggle with dependents is a group below that grid: its own
 * checkbox on top and its dependents indented beneath it along a spine, laid out in the same columns - and
 * a dependent with dependents of its own repeats the shape, so the nesting goes as deep as the settings do.
 * <p>
 * The dependency is shown by enabling, never by value: while a parent is off every toggle under it is
 * greyed out with its tick still visible, and comes back as it was when the parent is turned on again.
 */
public class ToggleTreePanel extends JPanel {

    private static final int COLUMN_COUNT = 3;
    /**
     * How far a dependent block sits in from its parent's checkbox, spine included.
     */
    private static final int DEPENDENT_INDENT = 18;
    /**
     * Around every grid cell, and so around every checkbox on the page.
     */
    private static final Insets CELL_INSETS = new Insets(4, 6, 4, 6);

    private final List<SettingToggle> roots;
    private final Map<SettingToggle, JCheckBox> boxes = new LinkedHashMap<>();

    public ToggleTreePanel(List<SettingToggle> roots) {
        this.roots = List.copyOf(roots);
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(HUD_GAP, HUD_GAP, HUD_GAP, HUD_GAP));

        add(page(this.roots, true));
        refresh();
    }

    /**
     * The checkbox showing {@code toggle}, for a caller that must dress it beyond what the setting says.
     */
    public JCheckBox checkBox(SettingToggle toggle) {
        JCheckBox box = boxes.get(toggle);
        if (box == null) throw new IllegalArgumentException("Not on this panel: " + toggle.labelKey());
        return box;
    }

    /**
     * Re-reads every setting and re-applies the dependency greying. Called when the page is shown again:
     * a voice command may have flipped a setting while the page was hidden.
     */
    public void refresh() {
        boxes.forEach((toggle, box) -> box.setSelected(toggle.read().getAsBoolean()));
        roots.forEach(this::cascade);
    }

    /**
     * Greys out or restores everything under {@code node} from its own state. Only the descendants are
     * touched: a root is always live, and a dependent's liveness is its parent's business.
     */
    private void cascade(SettingToggle node) {
        JCheckBox box = boxes.get(node);
        boolean dependentsLive = box.isEnabled() && box.isSelected();
        for (SettingToggle dependent : node.dependents()) {
            boxes.get(dependent).setEnabled(dependentsLive);
            cascade(dependent);
        }
    }

    private JCheckBox checkBoxFor(SettingToggle toggle) {
        JCheckBox box = makeCheckBox(getText(toggle.labelKey()), false);
        box.addActionListener(e -> {
            toggle.write().accept(box.isSelected());
            cascade(toggle);
        });
        boxes.put(toggle, box);
        return box;
    }

    /**
     * The shape shared by the page and every nesting level: the standalone toggles in a column grid, then
     * each toggle with dependents as a group beneath it. Groups never sit inside the grid - a group is as
     * tall as its subtree, and one in a grid cell would push its whole row down, splitting the plain
     * checkboxes beside it apart.
     *
     * @param ruled whether a rule separates the grid from the groups. The top of the page gets one, so the
     *              switches that stand alone read as a different kind of thing from the trees below; a nested
     *              page does not, its spine already frames it
     */
    private JPanel page(List<SettingToggle> nodes, boolean ruled) {
        JPanel page = transparentPanel(null);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));

        List<JComponent> standalone = new ArrayList<>();
        for (SettingToggle node : nodes) {
            if (!node.hasDependents()) standalone.add(checkBoxFor(node));
        }
        if (!standalone.isEmpty()) page.add(columnGrid(standalone));

        for (SettingToggle node : nodes) {
            if (node.hasDependents()) {
                if (page.getComponentCount() > 0) page.add(ruled ? rule() : Box.createVerticalStrut(HUD_GAP));
                page.add(group(node));
            }
        }
        return page;
    }

    /**
     * A hairline across the page in the spine's colour, with a gap either side.
     */
    private static JComponent rule() {
        JPanel rule = transparentPanel(null);
        rule.setBorder(new CompoundBorder(
                new EmptyBorder(HUD_GAP, CELL_INSETS.left, HUD_GAP, CELL_INSETS.right),
                new MatteBorder(1, 0, 0, 0, HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION)));
        rule.setMaximumSize(new Dimension(Integer.MAX_VALUE, rule.getPreferredSize().height));
        return rule;
    }

    /**
     * A toggle over its dependents: the checkbox, then the dependents' own page indented along a spine.
     */
    private JComponent group(SettingToggle node) {
        JPanel block = transparentPanel(new BorderLayout());
        // The head is a one-cell grid so it is exactly as wide as the first column beneath it.
        block.add(columnGrid(List.of(checkBoxFor(node))), BorderLayout.NORTH);

        JPanel dependents = page(node.dependents(), false);
        dependents.setBorder(new CompoundBorder(
                new EmptyBorder(0, DEPENDENT_INDENT / 2, 0, 0),
                new CompoundBorder(
                        new MatteBorder(0, 1, 0, 0, HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION),
                        new EmptyBorder(0, DEPENDENT_INDENT / 2, 0, 0))));
        block.add(dependents, BorderLayout.CENTER);
        return block;
    }

    /**
     * Lays the cells out in {@value #COLUMN_COUNT} equal columns, filled top to bottom. The placement is
     * computed from the list so a toggle added or moved never has to be hand-numbered into a column.
     */
    private static JPanel columnGrid(List<JComponent> cells) {
        JPanel grid = transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.insets = CELL_INSETS;

        int rows = (cells.size() + COLUMN_COUNT - 1) / COLUMN_COUNT;
        for (int i = 0; i < cells.size(); i++) {
            gbc.gridx = i / rows;
            gbc.gridy = i % rows;
            grid.add(cells.get(i), gbc);
        }

        // Every column is claimed even when the cells do not reach it, plus one of slack on the right, so
        // a grid of two cells has the same column widths as one of six: the nested grids line up under the
        // page's, and none stretches its few toggles across the full width.
        gbc.gridy = rows;
        for (int column = 0; column <= COLUMN_COUNT; column++) {
            gbc.gridx = column;
            grid.add(Box.createHorizontalGlue(), gbc);
        }
        return grid;
    }
}
