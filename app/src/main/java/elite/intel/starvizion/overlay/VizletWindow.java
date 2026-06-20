package elite.intel.starvizion.overlay;

import elite.intel.gameapi.DeviceBus;
import elite.intel.gameapi.EventBusManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Base class for all Vizlet overlay windows.
 * Transparent, borderless, always-on-top, non-focus-stealing, draggable JWindow.
 * Subclasses implement paintVizlet() and openSettings().
 */
public abstract class VizletWindow extends JWindow {

    protected static final Color BG_FILL = new Color(0x1A, 0x1A, 0x1E, 0xE0);
    protected static final Color BORDER_COLOR = new Color(0x44, 0x44, 0x55);
    protected static final Color UNCONFIGURED_FG = new Color(0xAA, 0xAA, 0xAA);
    protected static final Font UNCONFIGURED_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    protected static final int ARC = 8;

    private boolean locked = false;
    private Point dragOrigin;

    protected final VizletPanel contentPanel = new VizletPanel();

    protected VizletWindow(int defaultW, int defaultH) {
        setSize(defaultW, defaultH);
        setBackground(new Color(0, 0, 0, 0));
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        setAutoRequestFocus(false);
        setType(Window.Type.UTILITY);

        setContentPane(contentPanel);
        contentPanel.setOpaque(false);

        installDragSupport();
        installContextMenu();
    }

    // -- Lifecycle ------------------------------------------------------------

    /** Show the vizlet and register with the event bus. */
    public void showVizlet() {
        EventBusManager.register(this);
        DeviceBus.register(this);
        setVisible(true);
    }

    /** Hide the vizlet and unregister from the event bus. */
    public void closeVizlet() {
        setVisible(false);
        EventBusManager.unregister(this);
        DeviceBus.unregister(this);
        dispose();
    }

    // -- API for subclasses ---------------------------------------------------

    /**
     * Paint the vizlet content. Called inside paintComponent after the background fill.
     * g2 has antialiasing enabled. Clip is the full component bounds.
     */
    protected abstract void paintVizlet(Graphics2D g2, int w, int h);

    protected abstract void openSettings();

    /** Override to return false to hide the Settings item from the right-click menu. */
    protected boolean hasSettings() {
        return true;
    }

    // -- Drag -----------------------------------------------------------------

    private void installDragSupport() {
        MouseAdapter drag = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && !locked) {
                    dragOrigin = e.getLocationOnScreen();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || locked || dragOrigin == null) return;
                Point now = e.getLocationOnScreen();
                Point loc = getLocation();
                setLocation(loc.x + (now.x - dragOrigin.x), loc.y + (now.y - dragOrigin.y));
                dragOrigin = now;
            }
        };
        contentPanel.addMouseListener(drag);
        contentPanel.addMouseMotionListener(drag);
    }

    // -- Context menu ---------------------------------------------------------

    private void installContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        if (hasSettings()) {
            JMenuItem settingsItem = new JMenuItem(getText("vizlet.menu.settings"));
            settingsItem.addActionListener(e -> openSettings());
            menu.add(settingsItem);
        }

        JMenuItem lockItem = new JMenuItem(getText("vizlet.menu.lock"));
        lockItem.addActionListener(e -> {
            locked = !locked;
            lockItem.setText(locked ? getText("vizlet.menu.unlock") : getText("vizlet.menu.lock"));
        });

        JMenuItem closeItem = new JMenuItem(getText("vizlet.menu.close"));
        closeItem.addActionListener(e -> closeVizlet());

        menu.add(lockItem);
        menu.addSeparator();
        menu.add(closeItem);

        MouseAdapter popup = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) menu.show(e.getComponent(), e.getX(), e.getY());
            }
        };
        contentPanel.addMouseListener(popup);
    }

    // -- Inner panel ----------------------------------------------------------

    protected final class VizletPanel extends JPanel {

        VizletPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // Background fill with alpha
                g2.setColor(BG_FILL);
                g2.fillRoundRect(0, 0, w, h, ARC, ARC);

                // Border
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);

                paintVizlet(g2, w, h);
            } finally {
                g2.dispose();
            }
        }
    }
}
