package elite.intel.ui.inputmonitor.overlay;

import elite.intel.eventbus.DeviceBus;
import elite.intel.eventbus.GameEventBus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Base class for all readout overlay windows.
 * Transparent, borderless, always-on-top, non-focus-stealing, draggable JWindow.
 * Subclasses implement paintReadout() and openSettings().
 */
public abstract class ReadoutWindow extends JWindow {

    protected static final Color BG_FILL = new Color(0x1A, 0x1A, 0x1E, 0xE0);
    protected static final Color BORDER_COLOR = new Color(0x44, 0x44, 0x55);
    protected static final Color UNCONFIGURED_FG = new Color(0xAA, 0xAA, 0xAA);
    protected static final Font UNCONFIGURED_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    protected static final int ARC = 8;

    private boolean locked = false;
    private Point dragOrigin;

    protected final ReadoutPanel contentPanel = new ReadoutPanel();

    protected ReadoutWindow(int defaultW, int defaultH) {
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

    /**
     * Show the readout and register with the event bus.
     */
    public void showReadout() {
        GameEventBus.register(this);
        DeviceBus.register(this);
        setVisible(true);
    }

    /**
     * Hide the readout and unregister from the event bus.
     */
    public void closeReadout() {
        setVisible(false);
        GameEventBus.unregister(this);
        DeviceBus.unregister(this);
        dispose();
    }

    // -- API for subclasses ---------------------------------------------------

    /**
     * Paint the readout content. Called inside paintComponent after the background fill.
     * g2 has antialiasing enabled. Clip is the full component bounds.
     */
    protected abstract void paintReadout(Graphics2D g2, int w, int h);

    protected abstract void openSettings();

    /** Override to return false to hide the Settings item from the right-click menu. */
    protected boolean hasSettings() {
        return true;
    }

    /**
     * Paints the window's backdrop before {@link #paintReadout}. The default is
     * the standard readout chrome: a rounded translucent fill with a thin border.
     * <p>
     * Overlays that own their whole surface - the HUD overlay, which needs a
     * user-adjustable background alpha and the flat square styling of
     * ED_HUD_REFERENCE.md - override this instead of reimplementing the window.
     */
    protected void paintBackground(Graphics2D g2, int w, int h) {
        g2.setColor(BG_FILL);
        g2.fillRoundRect(0, 0, w, h, ARC, ARC);
        g2.setColor(BORDER_COLOR);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
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
            JMenuItem settingsItem = new JMenuItem(getText("readout.menu.settings"));
            settingsItem.addActionListener(e -> openSettings());
            menu.add(settingsItem);
        }

        JMenuItem lockItem = new JMenuItem(getText("readout.menu.lock"));
        lockItem.addActionListener(e -> {
            locked = !locked;
            lockItem.setText(locked ? getText("readout.menu.unlock") : getText("readout.menu.lock"));
        });

        JMenuItem closeItem = new JMenuItem(getText("readout.menu.close"));
        closeItem.addActionListener(e -> closeReadout());

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

    protected final class ReadoutPanel extends JPanel {

        ReadoutPanel() {
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

                paintBackground(g2, w, h);
                paintReadout(g2, w, h);
            } finally {
                g2.dispose();
            }
        }
    }
}
