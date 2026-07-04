package elite.intel.util;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.win32.StdCallLibrary;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppPaths {

    public static final String CUSTOM_COMMANDS_FILE_NAME = "custom_commands.json";

    private static Path APP_DIR;

    private AppPaths() {
    }

    public static Path getAppDirectory() {
        return APP_DIR;
    }


    /// --- APP USER DATA LOCATION
    public static Path getDatabasePath() throws IOException {
        Path dbDir = getAppDataBase().resolve("elite-intel/db");
        Files.createDirectories(dbDir);
        return dbDir.resolve("database.db");
    }

    /** Returns the custom command JSON file path, creating the custom command data directory if needed. */
    public static Path getCustomCommandsFilePath() throws IOException {
        Path dir = getAppDataBase().resolve("elite-intel/custom-commands");
        Files.createDirectories(dir);
        return dir.resolve(CUSTOM_COMMANDS_FILE_NAME);
    }

    /**
     * Returns the directory for durable, timestamped custom-command backups written before a
     * destructive import replaces the current set, creating it if needed.
     */
    public static Path getCustomCommandsBackupDir() throws IOException {
        Path dir = getAppDataBase().resolve("elite-intel/custom-commands/backups");
        Files.createDirectories(dir);
        return dir;
    }

    /** Returns the directory for per-preset working copies, creating it if needed. */
    public static Path getBindingsWorkingDir() throws IOException {
        Path dir = getAppDataBase().resolve("elite-intel/bindings");
        Files.createDirectories(dir);
        return dir;
    }

    /** Returns the directory for timestamped game-file backups, creating it if needed. */
    public static Path getBindingsBackupDir() throws IOException {
        Path dir = getAppDataBase().resolve("elite-intel/bindings/backups");
        Files.createDirectories(dir);
        return dir;
    }

    /**
     * Returns the directory for user-facing, on-demand player backups (bindings/preset
     * snapshots, and later other profile data), creating it if needed. Deliberately separate
     * from {@link #getBindingsBackupDir()}, which is an internal safety net tied to the
     * apply-pipeline rather than a user-triggered feature.
     */
    public static Path getPlayerBackupsDir() throws IOException {
        Path dir = getAppDataBase().resolve("elite-intel/playerbackups");
        Files.createDirectories(dir);
        return dir;
    }

    private static Path getAppDataBase() throws IOException {
        if (OsDetector.getOs() == OsDetector.OS.LINUX || OsDetector.getOs() == OsDetector.OS.MAC) {
            String dataHome = System.getenv("XDG_DATA_HOME");
            return dataHome != null && !dataHome.isEmpty()
                    ? Path.of(dataHome)
                    : Path.of(System.getProperty("user.home"), ".local/share");
        } else if (OsDetector.getOs() == OsDetector.OS.WINDOWS) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData == null || localAppData.isEmpty()) {
                throw new IllegalStateException("LOCALAPPDATA not set");
            }
            return Path.of(localAppData);
        }
        throw new IllegalStateException("Unsupported OS");
    }

    public static Path getTtsModelDir() {
        return getDistributionFile("tts");
    }

    public static Path getNativeLibDir() {
        return getDistributionFile("native");
    }

    public static Path getParakeetModelDir() {
        return getDistributionFile("parakeet");
    }

    /**
     * Directory holding the in-process text-embedding model (multilingual-e5-small int8 ONNX +
     * tokenizer), shipped in distribution/embed/ exactly like the Parakeet and Kokoro models so the
     * installer/updater bundle it and users never hunt one down.
     */
    public static Path getEmbedModelDir() {
        return getDistributionFile("embed/multilingual-e5-small");
    }

    private static Path getDistributionFile(String subPath) {
        if (isRunningFromJar()) {
            return APP_DIR.resolve(subPath);
        }
        return APP_DIR.resolve("../distribution/" + subPath).normalize();
    }

    private static boolean isRunningFromJar() {
        try {
            return Path.of(AppPaths.class.getProtectionDomain()
                            .getCodeSource().getLocation().toURI())
                    .toString().endsWith(".jar");
        } catch (Exception e) {
            return false;
        }
    }


    public static String getSecretKeyFile() {
        if (OsDetector.getOs() == OsDetector.OS.LINUX || OsDetector.getOs() == OsDetector.OS.MAC) {
            return System.getProperty("user.home")
                    + File.separator
                    + ".local"
                    + File.separator
                    + "share"
                    + File.separator
                    + "elite-intel"
                    + File.separator
                    + "secret.key";
        } else {
            return System.getenv("LOCALAPPDATA")
                    + File.separator
                    + "elite-intel"
                    + File.separator
                    + "secret.key";
        }
    }

    static {
        Path dir = null;

        // start optimistic
        try {
            // CASE 1: Running from a JAR → use the JAR's folder
            Path codeSource = Path.of(
                    AppPaths.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            );

            // If it's a JAR file → use its parent directory
            if (codeSource.toString().endsWith(".jar")) {
                dir = codeSource.getParent();
            } else {
                // CASE 2: Running from IDE → walk up to find project root (build.gradle)
                Path current = codeSource;
                while (current != null) {
                    if (Files.exists(current.resolve("build.gradle")) ||
                            Files.exists(current.resolve("build.gradle.kts")) ||
                            Files.exists(current.resolve("settings.gradle"))) {
                        dir = current;
                        break;
                    }
                    current = current.getParent();
                }
            }
        } catch (Exception e) {
            // ignore - will fall back
        }

        // FINAL FALLBACK: current working directory (./)
        if (dir == null) {
            dir = Path.of(".").toAbsolutePath().normalize();
        }

        APP_DIR = dir;
    }

    // -- Native path helpers --------------------------------------------------

    private interface Kernel32 extends StdCallLibrary {
        int GetShortPathNameW(WString lpszLongPath, char[] lpszShortPath, int cchBuffer);
    }

    /**
     * On Windows, converts a path to its 8.3 short form so native libraries that
     * do not handle non-ASCII characters (e.g. sherpa-onnx on a system with a
     * non-Latin username) can open the file.  No-op on Linux/macOS.
     */
    public static String toNativePath(Path path) {
        String s = path.toAbsolutePath().toString();
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return s;
        try {
            Kernel32 k32 = Native.load("kernel32", Kernel32.class);
            char[] buf = new char[s.length() + 260];
            int len = k32.GetShortPathNameW(new WString(s), buf, buf.length);
            if (len > 0 && len < buf.length) return new String(buf, 0, len);
        } catch (Throwable ignored) {
            // fall through - return original path
        }
        return s;
    }
}
