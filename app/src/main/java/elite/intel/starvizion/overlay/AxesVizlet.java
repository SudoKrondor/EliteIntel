package elite.intel.starvizion.overlay;

import com.google.common.eventbus.Subscribe;
import elite.intel.devices.events.DeviceAxisEvent;
import elite.intel.devices.events.DeviceDisconnectedEvent;
import elite.intel.devices.model.Device;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Vizlet that renders a two-axis position indicator with a moving dot and dashed crosshair.
 */
public class AxesVizlet extends VizletWindow {

    public enum DotShape { CIRCLE, SQUARE, CROSSHAIR, TRIANGLE }

    private static final int DEFAULT_W = 200;
    private static final int DEFAULT_H = 200;
    private static final int MARGIN = 12;
    private static final float[] DASH = {4f, 4f};
    private static final Stroke DASHED = new BasicStroke(1f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10f, DASH, 0f);
    private static final Color CROSSHAIR_COLOR = new Color(0x55, 0x55, 0x66);

    // Configuration — written on EDT, read on EDT via repaint
    private Integer assignedDeviceId;
    private int xAxisIndex = 0;
    private int yAxisIndex = 1;
    private Color dotColor = new Color(0xFF8C00); // orange
    private DotShape dotShape = DotShape.CIRCLE;

    // Live values — updated by event bus (possibly non-EDT), read on EDT during paint
    private volatile float xValue = 0f;
    private volatile float yValue = 0f;

    public AxesVizlet() {
        super(DEFAULT_W, DEFAULT_H);
    }

    // -- EventBus subscribers -------------------------------------------------

    @Subscribe
    public void onAxisState(DeviceAxisEvent event) {
        if (assignedDeviceId == null || event.deviceId() != assignedDeviceId) return;
        if (event.axisIndex() == xAxisIndex) xValue = event.value();
        else if (event.axisIndex() == yAxisIndex) yValue = event.value();
        SwingUtilities.invokeLater(contentPanel::repaint);
    }

    @Subscribe
    public void onDeviceDisconnected(DeviceDisconnectedEvent event) {
        if (assignedDeviceId != null && event.deviceId() == assignedDeviceId) {
            assignedDeviceId = null;
            xValue = 0f;
            yValue = 0f;
            SwingUtilities.invokeLater(contentPanel::repaint);
        }
    }

    // -- Configuration --------------------------------------------------------

    public void configure(Device device, int xAxis, int yAxis, Color color, DotShape shape) {
        this.assignedDeviceId = device.id();
        this.xAxisIndex = xAxis;
        this.yAxisIndex = yAxis;
        this.dotColor = color;
        this.dotShape = shape;
        xValue = 0f;
        yValue = 0f;
        SwingUtilities.invokeLater(contentPanel::repaint);
    }

    public Integer getAssignedDeviceId() { return assignedDeviceId; }
    public int getXAxisIndex() { return xAxisIndex; }
    public int getYAxisIndex() { return yAxisIndex; }
    public Color getDotColor() { return dotColor; }
    public DotShape getDotShape() { return dotShape; }

    // -- Paint ----------------------------------------------------------------

    @Override
    protected void paintVizlet(Graphics2D g2, int w, int h) {
        if (assignedDeviceId == null) {
            drawUnconfiguredHint(g2, w, h);
            return;
        }

        int left = MARGIN, top = MARGIN;
        int right = w - MARGIN, bottom = h - MARGIN;
        int areaW = right - left, areaH = bottom - top;

        // Map [-1, 1] to pixel coordinates
        int dotX = left + (int) ((xValue + 1f) / 2f * areaW);
        int dotY = top  + (int) ((yValue + 1f) / 2f * areaH);
        dotX = Math.max(left, Math.min(right, dotX));
        dotY = Math.max(top,  Math.min(bottom, dotY));

        // Dashed crosshair extending from dot to all four edges
        g2.setStroke(DASHED);
        g2.setColor(CROSSHAIR_COLOR);
        g2.drawLine(dotX, top, dotX, bottom);    // vertical
        g2.drawLine(left, dotY, right, dotY);    // horizontal

        // Dot
        g2.setStroke(new BasicStroke(1.5f));
        drawDot(g2, dotX, dotY, 9);
    }

    private void drawDot(Graphics2D g2, int cx, int cy, int r) {
        g2.setColor(dotColor);
        switch (dotShape) {
            case CIRCLE -> {
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                g2.setColor(dotColor.darker());
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);
            }
            case SQUARE -> {
                g2.fillRect(cx - r, cy - r, r * 2, r * 2);
                g2.setColor(dotColor.darker());
                g2.drawRect(cx - r, cy - r, r * 2, r * 2);
            }
            case CROSSHAIR -> {
                int arm = r + 3;
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawLine(cx - arm, cy, cx + arm, cy);
                g2.drawLine(cx, cy - arm, cx, cy + arm);
            }
            case TRIANGLE -> {
                Path2D tri = new Path2D.Float();
                tri.moveTo(cx, cy - r);
                tri.lineTo(cx + r, cy + r);
                tri.lineTo(cx - r, cy + r);
                tri.closePath();
                g2.fill(tri);
                g2.setColor(dotColor.darker());
                g2.draw(tri);
            }
        }
    }

    private void drawUnconfiguredHint(Graphics2D g2, int w, int h) {
        g2.setColor(UNCONFIGURED_FG);
        g2.setFont(UNCONFIGURED_FONT);
        String msg = getText("vizlet.unconfigured");
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2 + fm.getAscent() / 2);
    }

    // -- Settings -------------------------------------------------------------

    @Override
    protected void openSettings() {
        SwingUtilities.invokeLater(() -> {
            AxesSettingsDialog dlg = new AxesSettingsDialog(this);
            dlg.setVisible(true);
        });
    }
}
