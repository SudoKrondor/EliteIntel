package elite.intel.ui.overlay;

import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Borderless companion window rendered above the game. It is deliberately built from portable Swing APIs so the
 * same always-on-top, non-focus-stealing behaviour is available on Windows and Linux desktop environments.
 */
public final class CompanionOverlayWindow extends JWindow implements OverlayWindow {

    private final List<OverlayModule> modules;
    private final Runnable onHidden;
    private Point dragOrigin;
    private boolean initialLocationSet;

    /** Creates the production overlay with conversation and route modules. */
    public CompanionOverlayWindow(Runnable onHidden) {
        this(onHidden, new RouteOverlayModule(), new ConversationOverlayModule());
    }

    /** Package-visible composition seam for lifecycle tests and future module variants. */
    CompanionOverlayWindow(Runnable onHidden, OverlayModule... modules) {
        this.onHidden = Objects.requireNonNull(onHidden, "onHidden");
        this.modules = List.of(modules);

        boolean transparent = supportsPerPixelTransparency();
        setBackground(transparent
                ? HudPalette.HUD_COLOR_ROLE_OVERLAY_TRANSPARENT_BACKGROUND
                : HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        setAutoRequestFocus(false);
        setType(Window.Type.UTILITY);
        getAccessibleContext().setAccessibleName(getText("obs.title"));

        OverlayRootPanel root = new OverlayRootPanel(transparent);
        for (int index = 0; index < this.modules.size(); index++) {
            JComponent component = this.modules.get(index).component();
            component.setAlignmentX(Component.LEFT_ALIGNMENT);
            root.add(component);
            if (index < this.modules.size() - 1) {
                root.add(Box.createVerticalStrut(HudPalette.HUD_GAP));
            }
        }
        setContentPane(root);
        installDragSupport(root);
    }

    /** Makes the overlay visible and starts every live module without stealing focus from the game. */
    @Override
    public void showOverlay() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::showOverlay);
            return;
        }
        if (isVisible()) {
            return;
        }
        modules.forEach(OverlayModule::start);
        setAlwaysOnTop(true);
        pack();
        if (!initialLocationSet) {
            setLocation(HudPalette.HUD_GAP * 2, HudPalette.HUD_GAP * 2);
            initialLocationSet = true;
        }
        setVisible(true);
    }

    /** Hides the overlay, stops live subscriptions, and lets the controlling button refresh its label. */
    @Override
    public void hideOverlay() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::hideOverlay);
            return;
        }
        if (!isVisible()) {
            return;
        }
        modules.forEach(OverlayModule::stop);
        setVisible(false);
        onHidden.run();
    }

    @Override
    public void dispose() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::dispose);
            return;
        }
        modules.forEach(OverlayModule::stop);
        super.dispose();
    }

    private void installDragSupport(Component root) {
        MouseAdapter dragListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    dragOrigin = event.getLocationOnScreen();
                }
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event) || dragOrigin == null) {
                    return;
                }
                Point current = event.getLocationOnScreen();
                Point location = getLocation();
                setLocation(location.x + current.x - dragOrigin.x, location.y + current.y - dragOrigin.y);
                dragOrigin = current;
            }
        };
        installDragSupport(root, dragListener);
    }

    private static void installDragSupport(Component component, MouseAdapter listener) {
        component.addMouseListener(listener);
        component.addMouseMotionListener(listener);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                installDragSupport(child, listener);
            }
        }
    }

    private static boolean supportsPerPixelTransparency() {
        if (GraphicsEnvironment.isHeadless()) {
            return false;
        }
        try {
            GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            return device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT);
        } catch (UnsupportedOperationException exception) {
            return false;
        }
    }

    /** Root panel with a stable overlay width and a transparent fallback for compositors that support it. */
    private static final class OverlayRootPanel extends JPanel {

        private OverlayRootPanel(boolean transparent) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(!transparent);
            setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
            setBorder(new EmptyBorder(
                    HudPalette.HUD_GAP,
                    HudPalette.HUD_GAP,
                    HudPalette.HUD_GAP,
                    HudPalette.HUD_GAP));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferred = super.getPreferredSize();
            return new Dimension(HudPalette.HUD_OVERLAY_DEFAULT_WIDTH, preferred.height);
        }
    }
}
