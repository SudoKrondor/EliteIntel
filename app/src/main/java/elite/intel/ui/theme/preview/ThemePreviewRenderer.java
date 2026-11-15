package elite.intel.ui.theme.preview;

import elite.intel.ui.theme.ActiveTheme;
import elite.intel.ui.theme.ColorTheme;
import elite.intel.ui.theme.HudLookAndFeel;
import elite.intel.ui.theme.ThemeFile;
import elite.intel.ui.theme.ThemeFileException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

/**
 * The colour theme preview's own process: draws the sample panel ({@link ThemePreviewSample}) in a draft theme and
 * writes it to stdout as a PNG. {@link ThemePreviewProcess} starts it and reads the picture back.
 * <p>
 * A second process because the running app cannot draw in another theme: {@code HudPalette} and the look and feel's
 * defaults are fixed for the life of a process. Here they are installed fresh, from the draft, exactly as the app
 * installs them ({@link HudLookAndFeel}), so the preview is the app's own drawing code - not an imitation of it.
 * <p>
 * In: the request as arguments ({@link ThemePreviewRequest}), and for {@link ThemePreviewRequest.Mode#PREVIEW} the
 * draft theme's JSON on stdin. Out: the PNG on stdout, and exit code 0. On any failure: a message on stderr and exit
 * code 1.
 */
public final class ThemePreviewRenderer {

    private ThemePreviewRenderer() {
    }

    public static void main(String[] args) {
        // WHY: stdout is the picture. Anything else printed to it - a library's chatter - would corrupt the PNG.
        PrintStream picture = System.out;
        System.setOut(System.err);
        try {
            ThemePreviewRequest request = ThemePreviewRequest.fromArguments(args);
            ActiveTheme.install(themeFor(request));
            HudLookAndFeel.prepare();
            ImageIO.write(renderOnEventThread(request), "png", (OutputStream) picture);
            picture.flush();
            System.exit(0);
        } catch (IOException | ThemeFileException | RuntimeException e) {
            System.err.println("Theme preview failed: " + e);
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static ColorTheme themeFor(ThemePreviewRequest request) throws IOException, ThemeFileException {
        if (request.mode() == ThemePreviewRequest.Mode.ROLE_MAP) return RoleMap.codeTheme();
        return ThemeFile.parse(new String(System.in.readAllBytes(), StandardCharsets.UTF_8));
    }

    private static BufferedImage renderOnEventThread(ThemePreviewRequest request) {
        BufferedImage[] image = new BufferedImage[1];
        try {
            SwingUtilities.invokeAndWait(() -> image[0] = render(request));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while drawing the preview", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Could not draw the preview", e.getCause());
        }
        return image[0];
    }

    private static BufferedImage render(ThemePreviewRequest request) {
        HudLookAndFeel.install();
        // WHY OFF for the role map: anti-aliased text is a fringe of blended colours, and a blend is no role's code.
        ThemePreviewRequest.TextAntialiasing text = request.mode() == ThemePreviewRequest.Mode.ROLE_MAP
                ? ThemePreviewRequest.TextAntialiasing.OFF : request.textAntialiasing();
        HudLookAndFeel.useTextAntialiasing(text.hint());
        if (request.lcdContrast() > 0) UIManager.put(RenderingHints.KEY_TEXT_LCD_CONTRAST, request.lcdContrast());
        return paint(ThemePreviewSample.build(request.language()), request.scale());
    }

    /**
     * WHY an opaque image: LCD text anti-aliasing, which the app uses on a desktop that asks for it, cannot be drawn
     * onto an image with transparency, and would fall back to something the app never draws.
     */
    static BufferedImage paint(JComponent component, double scale) {
        Dimension size = component.getPreferredSize();
        component.setSize(size);
        layOut(component);
        BufferedImage image = new BufferedImage(
                (int) Math.ceil(size.width * scale), (int) Math.ceil(size.height * scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.scale(scale, scale);
            component.printAll(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** WHY by hand: a component never shown in a window is never laid out by Swing. */
    private static void layOut(Component component) {
        component.doLayout();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) layOut(child);
        }
    }
}
