package elite.intel.ui.theme.preview;

import elite.intel.i18n.Language;
import elite.intel.ui.theme.ColorTheme;
import elite.intel.ui.theme.ThemeFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Draws the colour theme preview by running {@link ThemePreviewRenderer} as a second process, and reads the picture
 * back. Each call blocks for about a second, so call it off the event thread.
 * <p>
 * The process runs on this app's own Java and classpath, so the preview is drawn by exactly the code the app runs.
 * It is headless and is pointed at a throwaway in-memory database, so a widget that reached for the database by
 * mistake could never touch the commander's data.
 */
public final class ThemePreviewProcess {

    private static final Logger log = LogManager.getLogger(ThemePreviewProcess.class);

    private static final long TIMEOUT_SECONDS = 20;
    private static final String MAX_HEAP = "-Xmx256m";
    private static final String LOG_CONFIG = "log4j2-theme-preview.xml";
    private static final String THROWAWAY_DATABASE = "jdbc:sqlite:file:theme-preview?mode=memory&cache=shared";

    private ThemePreviewProcess() {
    }

    /**
     * How this app draws on this screen, for a preview that matches it: the app's language, its text anti-aliasing
     * and the screen's scale factor.
     *
     * @param screen the screen the preview will be shown on, from the component that shows it
     */
    public static ThemePreviewRequest requestFor(Language language, GraphicsConfiguration screen) {
        Object contrast = UIManager.get(RenderingHints.KEY_TEXT_LCD_CONTRAST);
        return new ThemePreviewRequest(
                ThemePreviewRequest.Mode.PREVIEW,
                language,
                ThemePreviewRequest.TextAntialiasing.of(UIManager.get(RenderingHints.KEY_TEXT_ANTIALIASING)),
                contrast instanceof Integer value ? value : 0,
                screen.getDefaultTransform().getScaleX());
    }

    /** The sample panel drawn in {@code theme}. */
    public static BufferedImage preview(ThemePreviewRequest request, ColorTheme theme) throws IOException {
        return run(request.withMode(ThemePreviewRequest.Mode.PREVIEW), ThemeFile.format(theme));
    }

    /** The role map behind the preview. It is the same for every theme, so draw it once per screen and language. */
    public static RoleMap roleMap(ThemePreviewRequest request) throws IOException {
        return new RoleMap(run(request.withMode(ThemePreviewRequest.Mode.ROLE_MAP), ""));
    }

    private static BufferedImage run(ThemePreviewRequest request, String stdin) throws IOException {
        Process process = new ProcessBuilder(command(request)).start();
        CompletableFuture<byte[]> picture = readAll(process.getInputStream());
        CompletableFuture<byte[]> errors = readAll(process.getErrorStream());
        writeQuietly(process.getOutputStream(), stdin);
        try {
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("The theme preview took longer than " + TIMEOUT_SECONDS + " seconds");
            }
            if (process.exitValue() != 0) {
                throw new IOException("The theme preview failed: " + text(errors.get()));
            }
            return decode(picture.get());
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while drawing the theme preview", e);
        } catch (ExecutionException e) {
            throw new IOException("Could not read the theme preview", e.getCause());
        }
    }

    private static List<String> command(ThemePreviewRequest request) {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable().toString());
        command.add("-Djava.awt.headless=true");
        command.add(MAX_HEAP);
        command.add("-Dlog4j2.configurationFile=" + LOG_CONFIG);
        command.add("-Delite.intel.db.url=" + THROWAWAY_DATABASE);
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(ThemePreviewRenderer.class.getName());
        command.addAll(request.toArguments());
        return command;
    }

    /**
     * WHY java.home and not this process's own command: the installed app is started by a native launcher, so its
     * command is the launcher, not a Java executable that would accept these arguments.
     */
    private static Path javaExecutable() {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
        return Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java");
    }

    /**
     * WHY quietly: a process that failed at startup has closed its stdin, and the write fails. The exit code and
     * stderr then say why, which a broken pipe would not.
     */
    private static void writeQuietly(OutputStream stream, String text) {
        try (stream) {
            stream.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Could not send the draft theme to the theme preview process", e);
        }
    }

    /** WHY on their own threads: a process blocks when a pipe nobody reads fills up. */
    private static CompletableFuture<byte[]> readAll(InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try (stream) {
                return stream.readAllBytes();
            } catch (IOException e) {
                log.error("Could not read from the theme preview process", e);
                return new byte[0];
            }
        }, runnable -> Thread.ofVirtual().name("theme-preview-pipe").start(runnable));
    }

    private static BufferedImage decode(byte[] png) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        if (image == null) throw new IOException("The theme preview sent no picture (" + png.length + " bytes)");
        return image;
    }

    private static String text(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8).strip();
    }
}
