package elite.intel.starvizion.overlay;

import com.google.common.eventbus.Subscribe;
import elite.intel.starvizion.event.SvKeyPressedEvent;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Vizlet that displays a running total of keystrokes detected by SDL3 during the current
 * session, regardless of which window has input focus. Read-only — no settings.
 */
public class CounterVizlet extends VizletWindow {

    private static final int DEFAULT_W = 160;
    private static final int DEFAULT_H = 160;
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final Font COUNT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 32);

    // Live state — updated by event bus (non-EDT), read on EDT during paint
    private volatile int count = 0;

    public CounterVizlet() {
        super(DEFAULT_W, DEFAULT_H);
    }

    // -- EventBus subscribers -------------------------------------------------

    @Subscribe
    public void onKeyPressed(SvKeyPressedEvent event) {
        count++;
        SwingUtilities.invokeLater(contentPanel::repaint);
    }

    // -- Paint ----------------------------------------------------------------

    @Override
    protected void paintVizlet(Graphics2D g2, int w, int h) {
        String label = getText("starvizion.counter.label");
        String value = String.valueOf(count);

        g2.setColor(TEXT_COLOR);

        g2.setFont(LABEL_FONT);
        FontMetrics labelFm = g2.getFontMetrics();

        g2.setFont(COUNT_FONT);
        FontMetrics countFm = g2.getFontMetrics();

        int totalHeight = labelFm.getHeight() + countFm.getHeight();
        int labelY = (h - totalHeight) / 2 + labelFm.getAscent();
        int countY = labelY + labelFm.getDescent() + countFm.getAscent();

        g2.setFont(LABEL_FONT);
        g2.drawString(label, (w - labelFm.stringWidth(label)) / 2, labelY);

        g2.setFont(COUNT_FONT);
        g2.drawString(value, (w - countFm.stringWidth(value)) / 2, countY);
    }

    // -- Settings (none) -------------------------------------------------------

    @Override
    protected boolean hasSettings() {
        return false;
    }

    @Override
    protected void openSettings() {
        // No settings for this Vizlet.
    }
}
