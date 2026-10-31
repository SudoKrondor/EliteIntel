package elite.intel.appdata;

import elite.intel.io.AtomicFiles;
import org.jdbi.v3.core.Jdbi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Backup Settings and Data: the commander's settings, databases, custom commands and bindings work, in one zip they
 * can carry to another computer and restore there ({@link AppDataRestore}).
 * <p>
 * Runs while the app does. Touches the disk - not for the EDT.
 */
public final class AppDataBackup {

    private AppDataBackup() {
    }

    /**
     * Writes the backup to {@code target}, replacing a file already there only once the new one is complete.
     *
     * @param dataDir    the app data folder
     * @param keyFile    the app's {@code secret.key}
     * @param password   locks the key into the zip; null leaves the key out, and the API keys with it
     * @param appVersion the version of the app writing it, recorded for the commander's benefit
     */
    public static void write(Path dataDir, Path keyFile, Path target, char[] password, String appVersion)
            throws IOException {
        Path snapshots = Files.createTempDirectory("elite-intel-backup");
        try {
            List<Path> databases = snapshotDatabases(dataDir.resolve(AppDataArchive.DB_FOLDER), snapshots);
            byte[] sealedKey = password == null ? null : SealedKey.seal(Files.readAllBytes(keyFile), password);
            AtomicFiles.writeWith(target, tmp -> {
                try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(tmp, StandardOpenOption.CREATE_NEW))) {
                    putInfo(zip, appVersion);
                    for (Path database : databases) {
                        putFile(zip, AppDataArchive.DB_FOLDER + AppDataArchive.ENTRY_SEPARATOR + database.getFileName(), database);
                    }
                    for (String folder : AppDataArchive.FOLDERS) {
                        putFolder(zip, dataDir, dataDir.resolve(folder));
                    }
                    if (sealedKey != null) putBytes(zip, AppDataArchive.SEALED_KEY, sealedKey);
                }
            });
        } finally {
            deleteTree(snapshots);
        }
    }

    /**
     * A consistent copy of every database file, taken with {@code VACUUM INTO}.
     * <p>
     * WHY not copy the files: they run in WAL mode, so recent writes may still sit in the {@code -wal} file beside
     * them, and a copy taken while the app writes can catch a page half-way. {@code VACUUM INTO} reads one
     * transaction's view, WAL included, and writes a single self-contained file.
     */
    private static List<Path> snapshotDatabases(Path dbFolder, Path snapshots) throws IOException {
        List<Path> copies = new ArrayList<>();
        if (!Files.isDirectory(dbFolder)) return copies;
        try (Stream<Path> files = Files.list(dbFolder)) {
            for (Path database : files.filter(AppDataBackup::isDatabaseFile).sorted().toList()) {
                Path copy = snapshots.resolve(database.getFileName().toString());
                Jdbi.create("jdbc:sqlite:" + database + "?busy_timeout=5000").useHandle(h ->
                        h.execute("VACUUM INTO '" + copy.toString().replace("'", "''") + "'"));
                copies.add(copy);
            }
        }
        return copies;
    }

    private static boolean isDatabaseFile(Path file) {
        return Files.isRegularFile(file) && file.getFileName().toString().endsWith(AppDataArchive.DATABASE_SUFFIX);
    }

    private static void putInfo(ZipOutputStream zip, String appVersion) throws IOException {
        Properties info = new Properties();
        info.setProperty(AppDataArchive.INFO_FORMAT, AppDataArchive.FORMAT);
        info.setProperty(AppDataArchive.INFO_APP_VERSION, String.valueOf(appVersion));
        info.setProperty(AppDataArchive.INFO_CREATED, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        info.setProperty(AppDataArchive.INFO_OS, System.getProperty("os.name", ""));
        zip.putNextEntry(new ZipEntry(AppDataArchive.INFO_FILE));
        info.store(zip, "Elite Intel - Backup Settings and Data");
        zip.closeEntry();
    }

    private static void putFolder(ZipOutputStream zip, Path dataDir, Path folder) throws IOException {
        if (!Files.isDirectory(folder)) return;
        try (Stream<Path> walk = Files.walk(folder)) {
            for (Path file : walk.filter(Files::isRegularFile).sorted().toList()) {
                String name = dataDir.relativize(file).toString().replace(file.getFileSystem().getSeparator(),
                        String.valueOf(AppDataArchive.ENTRY_SEPARATOR));
                putFile(zip, name, file);
            }
        }
    }

    private static void putFile(ZipOutputStream zip, String name, Path file) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        Files.copy(file, zip);
        zip.closeEntry();
    }

    private static void putBytes(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
