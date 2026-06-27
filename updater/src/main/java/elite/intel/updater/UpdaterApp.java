package elite.intel.updater;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Standalone updater application for EliteIntel.
 * <p>
 * Packaged as a small fat-jar (no Whisper/Piper/Ollama native deps).
 * Launched by the main app before it exits; receives the install directory
 * as the first CLI argument.
 * <p>
 * Usage:
 * java -jar elite_intel_updater.jar <installDir>
 * <p>
 * Responsibilities:
 * 1. Wait briefly for the main app process to exit.
 * 2. Fetch the latest release ZIP URL from the GitHub API.
 * 3. Download the ZIP with live progress feedback.
 * 4. Unpack it over the install directory.
 * 5. Relaunch elite_intel.jar from that directory.
 * 6. Exit.
 */
public class UpdaterApp {

    private static final String ICON_AI = "/images/update.png";
    // -- Palette (mirrors AppView exactly) ------------------------------------
    private static final Color BG = new Color(0x151519);
    private static final Color BG_PANEL = new Color(0x1F2032);
    private static final Color FG = new Color(0xE6E6E6);
    private static final Color FG_MUTED = new Color(0xB0B0B0);
    private static final Color ACCENT = new Color(0xFF8C00);
    private static final Color BUTTON_FG = new Color(0xFFFFFF);
    private static final Color BUTTON_BG = new Color(0x03529F);
    private static final Color CONSOLE_FG = new Color(0xE0FFEF);

    private static final String GITHUB_API = "https://api.github.com/repos/SudoKrondor/EliteIntel/releases/latest";
    private static final String MAIN_JAR = "elite_intel.jar";

    // -- UI components ---------------------------------------------------------
    private JFrame frame;
    private JTextArea logArea;
    private JProgressBar progressBar;
    private JLabel statusLabel;

    private final String installDir;
    private final long mainPid;

    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        String dir = args.length > 0 ? args[0] : detectInstallDir();
        long pid = args.length > 1 ? parsePidArg(args[1]) : -1L;
        new UpdaterApp(dir, pid).launch();
    }

    private static long parsePidArg(String arg) {
        try {
            return Long.parseLong(arg);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    public UpdaterApp(String installDir, long mainPid) {
        this.installDir = installDir;
        this.mainPid = mainPid;
    }

    // -- Bootstrap -------------------------------------------------------------

    private void launch() {
        installDarkDefaults();
        buildUi();
        frame.setVisible(true);

        // Run the update pipeline on a virtual thread so the EDT stays alive
        Thread.ofVirtual().name("updater-pipeline").start(this::runPipeline);
    }

    // -- UI construction (no MVC, just like the main app) ----------------------
    private ImageIcon scaledIcon(String resource) {
        return new ImageIcon(
                new ImageIcon(
                        Objects.requireNonNull(getClass().getResource(resource))
                ).getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH)
        );
    }

    private void buildUi() {
        frame = new JFrame("Elite Intel – Updater");
        ImageIcon aiIcon = scaledIcon(ICON_AI);
        frame.setIconImage(aiIcon.getImage());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(620, 380);
        frame.setMinimumSize(new Dimension(480, 300));
        frame.setLocationRelativeTo(null);
        frame.setUndecorated(false);

        // Try to load the same app icon if it happens to be on the classpath
        try {
            frame.setIconImage(Toolkit.getDefaultToolkit()
                    .getImage(getClass().getResource("/images/elite-logo.png")));
        } catch (Exception ignored) {
        }

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        frame.setContentPane(root);

        // -- Title -------------------------------------------------------------
        JLabel title = new JLabel("ELITE INTEL  ·  UPDATE", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setForeground(ACCENT);
        root.add(title, BorderLayout.NORTH);

        // -- Centre: log area --------------------------------------------------
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBackground(BG);
        logArea.setForeground(CONSOLE_FG);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        logArea.setBorder(new EmptyBorder(6, 6, 6, 6));

        JScrollPane scroll = new JScrollPane(logArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(new LineBorder(BG_PANEL, 1));
        scroll.setBackground(BG);
        root.add(scroll, BorderLayout.CENTER);

        // -- South: status label + progress bar --------------------------------
        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setBackground(BG);

        statusLabel = new JLabel("Initialising…");
        statusLabel.setForeground(FG_MUTED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(12f));
        south.add(statusLabel, BorderLayout.NORTH);

        progressBar = new JProgressBar(0, 100) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // track
                g2.setColor(BG_PANEL);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 6, 6));
                // fill
                int filled = (int) ((getValue() / (double) getMaximum()) * getWidth());
                if (filled > 0) {
                    g2.setColor(isIndeterminate() ? ACCENT : BUTTON_BG);
                    g2.fill(new RoundRectangle2D.Float(0, 0, filled, getHeight(), 6, 6));
                }
                // text
                if (isStringPainted()) {
                    g2.setColor(BUTTON_FG);
                    FontMetrics fm = g2.getFontMetrics();
                    String s = getString();
                    int tx = (getWidth() - fm.stringWidth(s)) / 2;
                    int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(s, tx, ty);
                }
                g2.dispose();
            }
        };
        progressBar.setStringPainted(true);
        progressBar.setString("");
        progressBar.setPreferredSize(new Dimension(0, 18));
        progressBar.setIndeterminate(true);   // spin until we know the file size
        progressBar.setBorderPainted(false);
        south.add(progressBar, BorderLayout.CENTER);

        root.add(south, BorderLayout.SOUTH);
    }

    // -- Update pipeline -------------------------------------------------------

    private void runPipeline() {
        try {
            log("Install directory : " + installDir);
            checkInstallDirWritable();
            waitForMainAppToExit();

            String zipUrl = fetchLatestZipUrl();  // logs the resolved URL itself

            Path zipPath = downloadZip(zipUrl);
            log("Download complete.");

            log("Unpacking…");
            unzip(zipPath, Path.of(installDir));
            Files.deleteIfExists(zipPath);
            log("Extraction complete.");

            log("Relaunching EliteIntel…");
            relaunch();

        } catch (Exception ex) {
            logError("Update failed: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Update failed – see log above.");
                progressBar.setIndeterminate(false);
                progressBar.setValue(0);
                progressBar.setString("Failed");
            });
        }
    }

    // -- Pre-flight: verify we can write to the install directory --------------

    private void checkInstallDirWritable() {
        Path probe = Path.of(installDir, ".updater-write-test");
        try {
            Files.write(probe, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.deleteIfExists(probe);
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                progressBar.setIndeterminate(false);
                progressBar.setValue(0);
                progressBar.setString("Permission error");
                statusLabel.setText("Cannot write to install directory.");

                String msg = "<html><body style='width:380px;font-family:sans-serif'>"
                        + "<b>Elite Intel cannot update itself from this install location.</b>"
                        + "<br><br>"
                        + "The installation directory:<br>"
                        + "<tt>&nbsp;&nbsp;" + installDir + "</tt><br><br>"
                        + "is protected by Windows and requires Administrator rights to modify. "
                        + "The recommended fix is to reinstall to a user-local directory:"
                        + "<ol>"
                        + "<li>Uninstall Elite Intel from <i>Windows Settings → Apps</i>.</li>"
                        + "<li>Download the latest installer from GitHub.</li>"
                        + "<li>Run the installer - it will default to<br>"
                        + "<tt>%LOCALAPPDATA%\\Programs\\Elite Intel</tt><br>"
                        + "which does not require Administrator rights.</li>"
                        + "</ol>"
                        + "<b>Your user data and settings will not be deleted</b> by the uninstaller."
                        + "</body></html>";

                JEditorPane pane = new JEditorPane("text/html", msg);
                pane.setEditable(false);
                pane.setBackground(BG_PANEL);
                pane.setForeground(FG);

                JOptionPane.showMessageDialog(frame, pane,
                        "Update Failed – Permission Error",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            });

            // Block the pipeline thread until the dialog closes + exit
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException ignored) {
            }
            System.exit(1);
        }
    }

    // -- Step 0 – wait for main app to release file locks ----------------------

    private void waitForMainAppToExit() throws InterruptedException {
        if (mainPid <= 0) {
            log("No PID supplied – waiting 15 s for main app to exit…");
            Thread.sleep(15_000);
            return;
        }
        log("Waiting for main app (PID " + mainPid + ") to exit…");
        Optional<ProcessHandle> handle = ProcessHandle.of(mainPid);
        if (handle.isEmpty() || !handle.get().isAlive()) {
            log("Main app already gone.");
        } else {
            long deadline = System.currentTimeMillis() + 30_000;
            while (true) {
                Optional<ProcessHandle> ph = ProcessHandle.of(mainPid);
                if (ph.isEmpty() || !ph.get().isAlive()) break;
                if (System.currentTimeMillis() > deadline) {
                    log("WARNING: main app still running after 30 s – proceeding anyway.");
                    break;
                }
                Thread.sleep(500);
            }
            log("Main app exited.");
        }
        // Brief pause to let the OS flush and release all file handles
        Thread.sleep(1_500);
    }

    // -- Step 1 – resolve download URL -----------------------------------------

    private String fetchLatestZipUrl() throws Exception {
        status("Checking GitHub for latest release…");

        // Use ALWAYS on the API client too - GitHub may redirect http→https or
        // api.github.com itself can 301 on certain paths.
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API))
                .header("User-Agent", "curl/8.0")
                .header("Accept", "application/vnd.github+json")
                .GET().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        int apiStatus = resp.statusCode();
        if (apiStatus < 200 || apiStatus >= 300)
            throw new IOException("GitHub API returned HTTP " + apiStatus
                    + "\nBody: " + resp.body().substring(0, Math.min(200, resp.body().length())));

        // Match the Linux ZIP asset - exclude .exe and Windows-only variants.
        // Pattern mirrors what installer.sh does:
        //   grep "browser_download_url" | grep "\.zip" | grep -v "\.exe"
        Matcher m = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]*elite_intel[^\"]*\\.zip)\"").matcher(resp.body());

        String zipUrl = null;
        while (m.find()) {
            String candidate = m.group(1);
            if (!candidate.endsWith(".exe")) {   // belt-and-braces: skip any misclassified asset
                zipUrl = candidate;
                break;
            }
        }
        if (zipUrl == null)
            throw new IOException("No ZIP asset found in GitHub release response.");

        log("Resolved asset URL: " + zipUrl);
        return zipUrl;
    }

    // -- Step 2 – download with progress ---------------------------------------

    private Path downloadZip(String url) throws Exception {
        status("Downloading update…");
        log("Source: " + url);

        // ALWAYS follow redirects - GitHub release asset URLs issue a 302 to
        // objects.githubusercontent.com before serving the actual bytes.
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        // GitHub's asset CDN rejects requests with Java's default User-Agent.
        // Mimic curl's UA - the same one installer.sh uses implicitly.
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "curl/8.0")
                .header("Accept", "*/*")
                .GET().build();

        // Stream the body so we can track bytes
        HttpResponse<InputStream> resp = client.send(req,
                HttpResponse.BodyHandlers.ofInputStream());

        int httpStatus = resp.statusCode();
        if (httpStatus < 200 || httpStatus >= 300)
            throw new IOException("Download failed, HTTP " + httpStatus
                    + " - URL was: " + url);

        long total = resp.headers()
                .firstValueAsLong("content-length")
                .orElse(-1L);

        Path tmp = Files.createTempFile("elite-intel-update-", ".zip");

        try (InputStream in = new BufferedInputStream(resp.body());
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(tmp))) {

            byte[] buf = new byte[32_768];
            long read = 0;
            int n;

            if (total > 0) {
                SwingUtilities.invokeLater(() -> progressBar.setIndeterminate(false));
            }

            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                read += n;
                if (total > 0) {
                    final int pct = (int) (read * 100L / total);
                    final String mb = String.format("%.1f / %.1f MB",
                            read / 1_048_576.0, total / 1_048_576.0);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(pct);
                        progressBar.setString(pct + "%  (" + mb + ")");
                        statusLabel.setText("Downloading…  " + mb);
                    });
                }
            }
        }

        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(100);
            progressBar.setString("100%");
        });
        return tmp;
    }

    // -- Step 3 – unzip --------------------------------------------------------

    private void unzip(Path zipFile, Path targetDir) throws IOException {
        status("Extracting files…");
        SwingUtilities.invokeLater(() -> {
            progressBar.setIndeterminate(true);
            progressBar.setString("Extracting…");
        });

        int extracted = 0;

        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(Files.newInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path dest = targetDir.resolve(entry.getName()).normalize();
                if (!dest.startsWith(targetDir))
                    throw new IOException("Zip-slip attempt: " + entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    // Buffer the entry so writeWithRetry can retry on Windows file-lock races.
                    byte[] content = zis.readAllBytes();
                    writeWithRetry(dest, content);
                    log("  extracted: " + entry.getName());
                    extracted++;
                }
                zis.closeEntry();
            }
        }

        log(String.format("Extraction summary: %d file(s) extracted.", extracted));

        SwingUtilities.invokeLater(() -> {
            progressBar.setIndeterminate(false);
            progressBar.setValue(100);
            progressBar.setString("Done");
        });
    }

    /**
     * Writes {@code content} to {@code dest}, retrying up to 10 times on any
     * {@link IOException}.  On Windows the previous JVM may release its
     * file-system lock on elite_intel.jar (or ONNX model files held by the
     * native runtime) a beat after the process exits; a short retry loop
     * absorbs that window without failing the whole update.
     *
     * Note: catching the broader IOException (not just AccessDeniedException)
     * is intentional – Windows "file in use" errors surface as
     * FileSystemException, which is a sibling of AccessDeniedException, not a
     * subclass.
     */
    private static void writeWithRetry(Path dest, byte[] content) throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                Files.write(dest, content,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                return;
            } catch (IOException e) {
                last = e;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
        }
        throw last;
    }

    // -- Step 4 – relaunch main app --------------------------------------------

    private String resolveJavaCommand() {
        if (System.getProperty("os.name", "").toLowerCase().contains("linux")) {
            Path bundled = Path.of(installDir, "jdk", "bin", "java");
            if (Files.exists(bundled))
                return bundled.toAbsolutePath().toString();
        }
        return "java";
    }

    private void relaunch() throws IOException {
        Path jar = Path.of(installDir, MAIN_JAR);
        if (!Files.exists(jar))
            throw new IOException("Cannot find " + jar + " after extraction.");

        String javaCmd = resolveJavaCommand();
        ProcessBuilder pb = new ProcessBuilder(
                javaCmd,
                "-Djava.library.path=native/sherpa-onnx",
                "-jar", jar.toAbsolutePath().toString()
        );
        pb.directory(jar.getParent().toFile());
        pb.start();     // fire-and-forget

        log("EliteIntel launched. Updater exiting.");
        status("Done – restarting EliteIntel.");

        // Brief pause so the user can read the final message, then close
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
        System.exit(0);
    }

    // -- UI helpers ------------------------------------------------------------

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void logError(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[ERROR] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void status(String msg) {
        log(msg);
        SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
    }

    // -- Dark theme defaults (mirrors AppView.installDarkDefaults) -------------

    private static void installDarkDefaults() {
        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.background", BG);
        UIManager.put("Label.foreground", FG);
        UIManager.put("ScrollPane.background", BG);
        UIManager.put("Viewport.background", BG);
        UIManager.put("TextArea.background", BG);
        UIManager.put("TextArea.foreground", FG);
        UIManager.put("ProgressBar.background", BG_PANEL);
        UIManager.put("ProgressBar.foreground", BUTTON_BG);
    }

    // -- Fallback: detect install dir from the updater jar's own location ------

    private static String detectInstallDir() {
        try {
            URI uri = UpdaterApp.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI();
            return Path.of(uri).getParent().toString();
        } catch (Exception e) {
            return System.getProperty("user.dir");
        }
    }
}