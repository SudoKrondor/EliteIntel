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
 * checkbox on top and its dependents indented beneath it along a spine - and a dependent with dependents of
 * its own repeats the shape, so the nesting goes as deep as the settings do. Every level shares the page's
 * columns: only a grid's first column gives way to the indent, the others line up down the whole page.
 * <p>
 * The dependency is shown by enabling, never by value: while a parent is off every toggle under it is
 * greyed out with its tick still visible, and comes back as it was when the parent is turned on again.
 */
public class ToggleTreePanel extends JPanel {

    private static final int COLUMN_COUNT = 3;
    /**
     * How far a dependent block sits in from its parent's checkbox, spine included.
     */
    private static final int DEPENDENT_INDENT = 19;
    private static final int SPINE_WIDTH = 1;
    /**
     * Around every grid cell, and so around every checkbox on the page.
     */
    private static final Insets CELL_INSETS = new Insets(4, 6, 4, 6);

    private final List<SettingToggle> roots;
    private final Map<SettingToggle, JCheckBox> boxes = new LinkedHashMap<>();

    public ToggleTreePanel(List<SettingToggle> roots) {
        this.roots = List.copyOf(roots);
        setOpaque(false);
        // The tab hands this panel its full height, and a grid given more than it asks for centres its
        // rows in the surplus. Pinning the page to the top leaves the surplus below it instead.
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(HUD_GAP, HUD_GAP, HUD_GAP, HUD_GAP));

        add(page(this.roots, 0), BorderLayout.NORTH);
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
     * <p>
     * A rule separates the grid from the groups at the top of the page only, so the switches that stand alone
     * read as a different kind of thing from the trees below; a nested page's spine already frames it.
     *
     * @param indent how far right of the page's left edge this page starts
     */
    private JPanel page(List<SettingToggle> nodes, int indent) {
        JPanel page = transparentPanel(null);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));

        List<JComponent> standalone = new ArrayList<>();
        for (SettingToggle node : nodes) {
            if (!node.hasDependents()) standalone.add(checkBoxFor(node));
        }
        if (!standalone.isEmpty()) page.add(columnGrid(standalone, indent));

        for (SettingToggle node : nodes) {
            if (node.hasDependents()) {
                if (page.getComponentCount() > 0) page.add(indent == 0 ? rule() : Box.createVerticalStrut(HUD_GAP));
                page.add(group(node, indent));
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
    private JComponent group(SettingToggle node, int indent) {
        JPanel block = transparentPanel(new BorderLayout());
        // The head is a one-cell grid so it is exactly as wide as the first column beneath it.
        block.add(columnGrid(List.of(checkBoxFor(node)), indent), BorderLayout.NORTH);

        int gap = (DEPENDENT_INDENT - SPINE_WIDTH) / 2;
        JPanel dependents = page(node.dependents(), indent + DEPENDENT_INDENT);
        dependents.setBorder(new CompoundBorder(
                new EmptyBorder(0, gap, 0, 0),
                new CompoundBorder(
                        new MatteBorder(0, SPINE_WIDTH, 0, 0, HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION),
                        new EmptyBorder(0, gap, 0, 0))));
        block.add(dependents, BorderLayout.CENTER);
        return block;
    }

    private static JPanel columnGrid(List<JComponent> cells, int indent) {
        JPanel grid = transparentPanel(new PageColumnsLayout(indent));
        cells.forEach(grid::add);
        return grid;
    }

    /**
     * Lays cells out in {@value #COLUMN_COUNT} equal columns, filled top to bottom. The placement is computed
     * from the list so a toggle added or moved never has to be hand-numbered into a column.
     * <p>
     * WHY not a GridBagLayout: its columns are sized from their own cells, so grids on one page disagree about
     * where a column starts. Here the column width comes from the page, whatever the cells or the depth, which
     * leaves a grid of two cells on the same columns as one of six.
     */
    private static final class PageColumnsLayout implements LayoutManager {

        private final int indent;

        PageColumnsLayout(int indent) {
            this.indent = indent;
        }

        @Override
        public void layoutContainer(Container grid) {
            int count = grid.getComponentCount();
            int rows = rows(count);
            int columnWidth = Math.max((grid.getWidth() + indent) / COLUMN_COUNT, narrowestColumn(grid));
            int cellHeight = cellHeight(grid);
            int rowPitch = cellHeight + CELL_INSETS.top + CELL_INSETS.bottom;
            for (int i = 0; i < count; i++) {
                int column = i / rows;
                int columnX = column == 0 ? 0 : column * columnWidth - indent;
                int width = column == 0 ? columnWidth - indent : columnWidth;
                grid.getComponent(i).setBounds(
                        columnX + CELL_INSETS.left,
                        (i % rows) * rowPitch + CELL_INSETS.top,
                        width - CELL_INSETS.left - CELL_INSETS.right,
                        cellHeight);
            }
        }

        @Override
        public Dimension preferredLayoutSize(Container grid) {
            int rowPitch = cellHeight(grid) + CELL_INSETS.top + CELL_INSETS.bottom;
            // One more set of cell insets below the last row: the air that sets a grid apart from what follows it.
            int height = rows(grid.getComponentCount()) * rowPitch + CELL_INSETS.top + CELL_INSETS.bottom;
            return new Dimension(COLUMN_COUNT * narrowestColumn(grid) - indent, height);
        }

        @Override
        public Dimension minimumLayoutSize(Container grid) {
            return preferredLayoutSize(grid);
        }

        @Override
        public void addLayoutComponent(String name, Component component) {
        }

        @Override
        public void removeLayoutComponent(Component component) {
        }

        private static int rows(int count) {
            return (count + COLUMN_COUNT - 1) / COLUMN_COUNT;
        }

        /**
         * The page column width at which this grid's widest cell still fits, even in the indented first column.
         */
        private int narrowestColumn(Container grid) {
            int widest = 0;
            for (Component cell : grid.getComponents()) {
                widest = Math.max(widest, cell.getPreferredSize().width);
            }
            return widest + CELL_INSETS.left + CELL_INSETS.right + indent;
        }

        private static int cellHeight(Container grid) {
            int tallest = 0;
            for (Component cell : grid.getComponents()) {
                tallest = Math.max(tallest, cell.getPreferredSize().height);
            }
            return tallest;
        }
    }
}
