package elite.intel.starvizion.overlay;

import com.google.common.eventbus.Subscribe;
import elite.intel.starvizion.event.SvKeyPressedEvent;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Vizlet that shows the most recently pressed keyboard key (with modifiers), reverting to a
 * "waiting" message after a short hold period.
 */
public class KeyboardVizlet extends VizletWindow {

    private static final int DEFAULT_W = 160;
    private static final int DEFAULT_H = 160;
    private static final int MARGIN = 10;
    private static final int HOLD_MS = 1000;
    private static final int MAX_FONT_SIZE = 48;
    private static final int MIN_FONT_SIZE = 10;

    // Configuration
    private Color backgroundColor = BG_FILL;
    private Color textColor = Color.WHITE;

    // Live state — updated by event bus (non-EDT), read on EDT during paint
    private volatile String displayText = getText("starvizion.keyboard.waiting");

    private final Timer holdTimer;

    public KeyboardVizlet() {
        super(DEFAULT_W, DEFAULT_H);
        holdTimer = new Timer(HOLD_MS, e -> {
            displayText = getText("starvizion.keyboard.waiting");
            contentPanel.repaint();
        });
        holdTimer.setRepeats(false);
    }

    // -- EventBus subscribers -------------------------------------------------

    @Subscribe
    public void onKeyPressed(SvKeyPressedEvent event) {
        displayText = event.keyName();
        SwingUtilities.invokeLater(() -> {
            holdTimer.restart();
            contentPanel.repaint();
        });
    }

    // -- Lifecycle --------------------------------------------------------------

    @Override
    public void closeVizlet() {
        holdTimer.stop();
        super.closeVizlet();
    }

    // -- Configuration --------------------------------------------------------

    public void configure(Color background, Color text) {
        this.backgroundColor = background;
        this.textColor = text;
        SwingUtilities.invokeLater(contentPanel::repaint);
    }

    public Color getBackgroundColor() { return backgroundColor; }
    public Color getTextColor() { return textColor; }

    // -- Paint ----------------------------------------------------------------

    @Override
    protected void paintVizlet(Graphics2D g2, int w, int h) {
        // Custom background fill/border, drawn over the default VizletWindow chrome.
        g2.setColor(backgroundColor);
        g2.fillRoundRect(0, 0, w, h, ARC, ARC);
        g2.setColor(BORDER_COLOR);
        g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);

        String text = displayText;
        g2.setColor(textColor);
        g2.setFont(fitFont(g2, text, w - MARGIN * 2, h - MARGIN * 2));
        FontMetrics fm = g2.getFontMetrics();
        int x = (w - fm.stringWidth(text)) / 2;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);
    }

    private static Font fitFont(Graphics2D g2, String text, int maxWidth, int maxHeight) {
        int size = MAX_FONT_SIZE;
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, size);
        FontMetrics fm = g2.getFontMetrics(font);
        while (size > MIN_FONT_SIZE && (fm.stringWidth(text) > maxWidth || fm.getHeight() > maxHeight)) {
            size -= 2;
            font = new Font(Font.SANS_SERIF, Font.BOLD, size);
            fm = g2.getFontMetrics(font);
        }
        return font;
    }

    // -- Settings -------------------------------------------------------------

    @Override
    protected void openSettings() {
        SwingUtilities.invokeLater(() -> {
            KeyboardSettingsDialog dlg = new KeyboardSettingsDialog(this);
            dlg.setVisible(true);
        });
    }
}
