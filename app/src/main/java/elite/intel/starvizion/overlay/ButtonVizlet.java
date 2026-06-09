package elite.intel.starvizion.overlay;

import com.google.common.eventbus.Subscribe;
import elite.intel.starvizion.event.SvButtonStateEvent;
import elite.intel.starvizion.event.SvDeviceDisconnectedEvent;
import elite.intel.starvizion.model.SvDevice;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Vizlet that highlights a shape when the assigned button is pressed.
 */
public class ButtonVizlet extends VizletWindow {

    public enum ButtonShape { SQUARE, RECTANGLE, CIRCLE, HEXAGON, TRIANGLE }

    private static final int DEFAULT_W = 120;
    private static final int DEFAULT_H = 120;
    private static final int MARGIN = 14;
    private static final Color IDLE_COLOR = new Color(0x33, 0x33, 0x3D);
    private static final Color PRESSED_TEXT_COLOR = Color.WHITE;
    private static final Font PRESSED_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);

    // Configuration
    private Integer assignedDeviceId;
    private int buttonIndex = 0;
    private Color pressedColor = new Color(0xFF8C00);
    private ButtonShape buttonShape = ButtonShape.CIRCLE;

    // Live state
    private volatile boolean pressed = false;

    public ButtonVizlet() {
        super(DEFAULT_W, DEFAULT_H);
    }

    // -- EventBus subscribers -------------------------------------------------

    @Subscribe
    public void onButtonState(SvButtonStateEvent event) {
        if (assignedDeviceId == null || event.deviceId() != assignedDeviceId) return;
        if (event.buttonIndex() != buttonIndex) return;
        pressed = event.pressed();
        SwingUtilities.invokeLater(contentPanel::repaint);
    }

    @Subscribe
    public void onDeviceDisconnected(SvDeviceDisconnectedEvent event) {
        if (assignedDeviceId != null && event.deviceId() == assignedDeviceId) {
            assignedDeviceId = null;
            pressed = false;
            SwingUtilities.invokeLater(contentPanel::repaint);
        }
    }

    // -- Configuration --------------------------------------------------------

    public void configure(SvDevice device, int button, Color color, ButtonShape shape) {
        this.assignedDeviceId = device.id();
        this.buttonIndex = button;
        this.pressedColor = color;
        this.buttonShape = shape;
        pressed = false;
        SwingUtilities.invokeLater(contentPanel::repaint);
    }

    public Integer getAssignedDeviceId() { return assignedDeviceId; }
    public int getButtonIndex() { return buttonIndex; }
    public Color getPressedColor() { return pressedColor; }
    public ButtonShape getButtonShape() { return buttonShape; }

    // -- Paint ----------------------------------------------------------------

    @Override
    protected void paintVizlet(Graphics2D g2, int w, int h) {
        if (assignedDeviceId == null) {
            drawUnconfiguredHint(g2, w, h);
            return;
        }

        int left = MARGIN, top = MARGIN;
        int areaW = w - MARGIN * 2, areaH = h - MARGIN * 2;
        Color fill = pressed ? pressedColor : IDLE_COLOR;

        g2.setColor(fill);
        drawShape(g2, left, top, areaW, areaH, fill);

        if (pressed) {
            g2.setColor(PRESSED_TEXT_COLOR);
            g2.setFont(PRESSED_FONT);
            String label = "Pressed";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, left + (areaW - fm.stringWidth(label)) / 2,
                    top + areaH / 2 + fm.getAscent() / 2 - 2);
        }
    }

    private void drawShape(Graphics2D g2, int x, int y, int w, int h, Color fill) {
        switch (buttonShape) {
            case SQUARE -> {
                g2.fillRoundRect(x, y, w, w, 6, 6); // force square using w
                g2.setColor(fill.darker());
                g2.drawRoundRect(x, y, w, w, 6, 6);
            }
            case RECTANGLE -> {
                g2.fillRoundRect(x, y, w, h, 6, 6);
                g2.setColor(fill.darker());
                g2.drawRoundRect(x, y, w, h, 6, 6);
            }
            case CIRCLE -> {
                int r = Math.min(w, h);
                int cx = x + (w - r) / 2, cy = y + (h - r) / 2;
                g2.fillOval(cx, cy, r, r);
                g2.setColor(fill.darker());
                g2.drawOval(cx, cy, r, r);
            }
            case HEXAGON -> {
                g2.fill(hexagon(x + w / 2, y + h / 2, Math.min(w, h) / 2 - 1));
                g2.setColor(fill.darker());
                g2.draw(hexagon(x + w / 2, y + h / 2, Math.min(w, h) / 2 - 1));
            }
            case TRIANGLE -> {
                Path2D tri = triangle(x, y, w, h);
                g2.fill(tri);
                g2.setColor(fill.darker());
                g2.draw(tri);
            }
        }
    }

    private static Path2D hexagon(int cx, int cy, int r) {
        Path2D hex = new Path2D.Float();
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 6 + i * Math.PI / 3;
            double px = cx + r * Math.cos(angle);
            double py = cy + r * Math.sin(angle);
            if (i == 0) hex.moveTo(px, py);
            else hex.lineTo(px, py);
        }
        hex.closePath();
        return hex;
    }

    private static Path2D triangle(int x, int y, int w, int h) {
        Path2D tri = new Path2D.Float();
        tri.moveTo(x + w / 2.0, y);
        tri.lineTo(x + w, y + h);
        tri.lineTo(x, y + h);
        tri.closePath();
        return tri;
    }

    private void drawUnconfiguredHint(Graphics2D g2, int w, int h) {
        g2.setColor(UNCONFIGURED_FG);
        g2.setFont(UNCONFIGURED_FONT);
        String msg = getText("vizlet.unconfigured");
        FontMetrics fm = g2.getFontMetrics();
        // Two-line wrap at the word "to"
        String line1 = "Right click to";
        String line2 = "configure";
        int lineH = fm.getHeight();
        int y0 = h / 2 - lineH + fm.getAscent();
        g2.drawString(line1, (w - fm.stringWidth(line1)) / 2, y0);
        g2.drawString(line2, (w - fm.stringWidth(line2)) / 2, y0 + lineH);
    }

    // -- Settings -------------------------------------------------------------

    @Override
    protected void openSettings() {
        SwingUtilities.invokeLater(() -> {
            ButtonSettingsDialog dlg = new ButtonSettingsDialog(this);
            dlg.setVisible(true);
        });
    }
}
