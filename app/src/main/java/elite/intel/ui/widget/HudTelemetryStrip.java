package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal strip of {@link HudTelemetryBlock} items separated by dim vertical rules.
 * All blocks occupy equal-width cells; each block is horizontally and vertically centered
 * within its cell via {@link GridBagLayout}. Separators are painted between cells.
 * Add blocks via {@link #addBlock(HudTelemetryBlock)}.
 */
public class HudTelemetryStrip extends JPanel {

    private final List<HudTelemetryBlock> blocks = new ArrayList<>();

    public HudTelemetryStrip() {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
    }

    /**
     * Appends a block. Rebuilds the layout so all blocks share equal cell widths
     * and remain centered within their respective cells.
     */
    public void addBlock(HudTelemetryBlock block) {
        blocks.add(block);
        removeAll();
        setLayout(new GridLayout(1, blocks.size(), 0, 0));
        for (HudTelemetryBlock b : blocks) {
            JPanel cell = new JPanel(new GridBagLayout());
            cell.setOpaque(false);
            cell.add(b);
            add(cell);
        }
        revalidate();
        repaint();
    }

    /** Paints dim vertical rules at cell boundaries, matching HUD border style. */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (blocks.size() < 2) return;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(AppTheme.HUD_BORDER_DIM);
            // draw a separator at the left edge of every cell except the first
            for (int i = 1; i < getComponentCount(); i++) {
                int x = getComponent(i).getX();
                g2.drawLine(x, 4, x, getHeight() - 5);
            }
        } finally {
            g2.dispose();
        }
    }
}
