package elite.intel.ui.screen.colorthemes;

import elite.intel.i18n.Language;
import elite.intel.ui.theme.ColorTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.theme.ThemeRole;
import elite.intel.ui.theme.preview.ColorVisionSimulation;
import elite.intel.ui.theme.preview.RoleMap;
import elite.intel.ui.theme.preview.ThemePreviewProcess;
import elite.intel.ui.theme.preview.ThemePreviewRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * The theme preview: the app's own widgets drawn in the theme being edited, by a second process
 * ({@link ThemePreviewProcess}). A click on any part reports the colour role that painted it; pointing at a part
 * dims everything not painted in the same role, and pointing at a swatch hides everything not painted in its colour.
 * <p>
 * A render takes about a second and runs off the event thread. Changes made while one runs are folded into one more
 * render of the latest draft, so a burst of edits never queues a render per edit.
 */
final class ThemePreviewPane extends JComponent {

    private static final Logger log = LogManager.getLogger(ThemePreviewPane.class);

    private static final Dimension PLACEHOLDER_SIZE = new Dimension(900, 660);
    /**
     * How far from the pointer, in screen pixels before scaling, a part still counts as pointed at
     * ({@link RoleMap#roleAt(int, int, int)}). WHY 5: wide enough to land on a glyph from the gap beside it, narrow
     * enough that the fill of a button stays reachable between its frame and its text.
     */
    private static final int POINTER_REACH = 5;

    private final Language language;
    private final Consumer<ThemeRole> onRolePicked;

    private ThemePreviewRequest request;
    private RoleMap roleMap;
    private BufferedImage image;
    /** {@link #image} as {@link #simulation} sees it, made when first needed after either changes. */
    private BufferedImage simulated;
    private ColorVisionSimulation simulation = ColorVisionSimulation.NORMAL;
    private BufferedImage highlight;
    /**
     * While a swatch whose roles the preview cannot show is pointed at, what the preview says instead - naming those
     * parts, since there is nothing to point at - or null.
     */
    private String notShown;
    private ColorTheme wanted;
    private ColorTheme rendering;
    private ColorTheme rendered;
    private String message;

    /**
     * @param language     the app's language, for the sample's text
     * @param onRolePicked called on the event thread with the role a click landed on
     */
    ThemePreviewPane(Language language, Consumer<ThemeRole> onRolePicked) {
        this.language = language;
        this.onRolePicked = onRolePicked;
        this.message = getText("themes.preview.loading");
        setOpaque(true);
        installMouse();
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) renderIfDue();
        });
    }

    /** Draws the preview in {@code theme}, now or as soon as the render in progress finishes. */
    void show(ColorTheme theme) {
        wanted = theme;
        renderIfDue();
    }

    /**
     * Dims everything but the parts painted in one of {@code roles}, or clears that for an empty collection.
     * Surfaces - backgrounds - are left out ({@link RoleMap#isSurface}): spotlighting one would dim every widget on
     * it. Pointing at a background therefore dims nothing, and the hand cursor alone says it can be changed.
     */
    void highlight(Collection<ThemeRole> roles) {
        List<ThemeRole> parts = roleMap == null ? List.of()
                : roles.stream().filter(role -> !roleMap.isSurface(role)).toList();
        highlight = parts.isEmpty() ? null : roleMap.spotlight(parts, HudPalette.HUD_COLOR_ROLE_MODAL_SCRIM);
        notShown = null;
        repaint();
    }

    /**
     * Hides everything but the parts painted in one of {@code roles}, backgrounds included, for a swatch: the
     * commander pointing at a colour asks what it paints, and a dimmed rest left thin parts hard to find. When the
     * preview shows none of them, it names them instead ({@code themes.part.<wireName>}): the one place a part gets
     * a name, because it is the one place the commander has no picture to find it by.
     *
     * @param color the colour those roles share, so the cover stands off it
     */
    void isolate(Collection<ThemeRole> roles, Color color) {
        if (roleMap == null) return;
        boolean shown = roles.stream().anyMatch(roleMap::paints);
        notShown = shown ? null : getText("themes.preview.notShown", roles.stream()
                .map(role -> getText("themes.part." + role.wireName()))
                .collect(Collectors.joining(", ")));
        highlight = shown ? roleMap.isolate(roles, color) : null;
        repaint();
    }

    /** Shows the preview as a kind of colour vision sees it. The role map and clicks are unaffected. */
    void simulate(ColorVisionSimulation vision) {
        simulation = vision;
        simulated = null;
        repaint();
    }

    /**
     * WHY only while showing: the Commander screen builds every tab at startup, and a pane that rendered then would
     * start two processes on each app start for a tab the commander may never open. The screen's scale is known only
     * once the pane is on one, too.
     */
    private void renderIfDue() {
        if (rendering == null && isShowing() && wanted != null && wanted != rendered) renderNext();
    }

    @Override
    public Dimension getPreferredSize() {
        if (image == null) return PLACEHOLDER_SIZE;
        return new Dimension((int) Math.round(image.getWidth() / scale()), (int) Math.round(image.getHeight() / scale()));
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (image == null) {
            paintMessage(g);
            return;
        }
        Dimension size = getPreferredSize();
        if (notShown != null) {
            paintNotShown(g, size);
            return;
        }
        g.drawImage(shownImage(), 0, 0, size.width, size.height, null);
        if (highlight != null) g.drawImage(highlight, 0, 0, size.width, size.height, null);
    }

    private void paintNotShown(Graphics g, Dimension size) {
        smoothText(g);
        g.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(notShown, Math.max(0, (size.width - metrics.stringWidth(notShown)) / 2), size.height / 2);
    }

    private BufferedImage shownImage() {
        if (simulated == null) simulated = simulation.apply(image);
        return simulated;
    }

    private void renderNext() {
        if (request == null) request = ThemePreviewProcess.requestFor(language, getGraphicsConfiguration());
        rendering = wanted;
        ColorTheme theme = rendering;
        ThemePreviewRequest forScreen = request;
        boolean needsRoleMap = roleMap == null;
        new SwingWorker<Rendered, Void>() {
            @Override
            protected Rendered doInBackground() throws Exception {
                RoleMap map = needsRoleMap ? ThemePreviewProcess.roleMap(forScreen) : null;
                return new Rendered(ThemePreviewProcess.preview(forScreen, theme), map);
            }

            @Override
            protected void done() {
                finished(this);
            }
        }.execute();
    }

    private void finished(SwingWorker<Rendered, Void> worker) {
        try {
            Rendered result = worker.get();
            image = result.preview();
            simulated = null;
            if (result.roleMap() != null) roleMap = result.roleMap();
            message = null;
        } catch (ExecutionException e) {
            log.error("Could not draw the colour theme preview", e.getCause());
            image = null;
            simulated = null;
            message = getText("themes.preview.failed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        rendered = rendering;
        rendering = null;
        highlight = null;
        notShown = null;
        revalidate();
        repaint();
        renderIfDue();
    }

    private void installMouse() {
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) roleAt(e).ifPresent(onRolePicked);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Optional<ThemeRole> role = roleAt(e);
                setCursor(Cursor.getPredefinedCursor(role.isPresent() ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                highlight(role.map(List::of).orElse(List.of()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                highlight(List.of());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private Optional<ThemeRole> roleAt(MouseEvent e) {
        if (roleMap == null || image == null) return Optional.empty();
        return roleMap.roleAt((int) (e.getX() * scale()), (int) (e.getY() * scale()),
                (int) Math.round(POINTER_REACH * scale()));
    }

    private double scale() {
        return request == null ? 1.0 : request.scale();
    }

    private void paintMessage(Graphics g) {
        if (message == null) return;
        smoothText(g);
        g.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(message, HudPalette.HUD_GAP * 2, HudPalette.HUD_GAP * 2 + metrics.getAscent());
    }

    /**
     * Text drawn straight onto the pane, as the app's widgets draw theirs: with the anti-aliasing
     * {@code HudLookAndFeel} chose for this desktop. A bare {@code drawString} gets none and looks jagged.
     */
    private static void smoothText(Graphics g) {
        Object hint = UIManager.get(RenderingHints.KEY_TEXT_ANTIALIASING);
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                hint != null ? hint : RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private record Rendered(BufferedImage preview, RoleMap roleMap) {
    }
}
