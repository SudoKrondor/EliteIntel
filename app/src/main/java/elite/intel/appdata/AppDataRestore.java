package elite.intel.appdata;

import elite.intel.db.util.DatabaseMigrator;
import elite.intel.util.Cypher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.Jdbi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * The first half of a restore: unpack a Backup Settings and Data zip beside the live data, check it, unlock its key,
 * and leave it for the next start to swap in ({@link PendingRestore}).
 * <p>
 * WHY in two halves: the databases are open while the app runs, and on Windows an open file cannot be replaced. So
 * nothing live is touched here; the swap happens at startup, before the database opens.
 * <p>
 * Everything a restored database says that belongs to the old computer is put right while it is still staged:
 * folders that do not exist here are forgotten, and API keys this computer's key cannot read are cleared.
 * Touches the disk - not for the EDT.
 */
public final class AppDataRestore {

    private static final Logger log = LogManager.getLogger(AppDataRestore.class);

    /**
     * The settings that name a folder on the computer the backup came from.
     */
    private static final List<FolderSetting> FOLDER_SETTINGS = List.of(
            new FolderSetting("user_preferences", "journal_dir"),
            new FolderSetting("user_preferences", "bindings_dir"),
            new FolderSetting("jukebox_state", "musicFolder"));

    private record FolderSetting(String table, String column) {
    }

    /**
     * The API keys, each encrypted with {@code secret.key}.
     */
    private static final List<String> ENCRYPTED_KEY_COLUMNS = List.of("encryptedLLMKey", "encryptedTTSKey");

    private AppDataRestore() {
    }

    /**
     * Why a zip was not accepted as a backup.
     */
    public enum Refusal {
        /**
         * Not a zip this feature wrote, or one missing its main database.
         */
        NOT_A_BACKUP,
        /**
         * Written by a newer Elite Intel: its databases have changes this version does not know.
         */
        NEWER_VERSION
    }

    /**
     * A zip that could not be restored, and why.
     */
    public static final class RefusedException extends Exception {
        private final Refusal refusal;

        RefusedException(Refusal refusal, String detail) {
            super(refusal + ": " + detail);
            this.refusal = refusal;
        }

        public Refusal refusal() {
            return refusal;
        }
    }

    /**
     * Unpacks {@code zip} into the staging folder and checks it is a backup this version can restore. A staging
     * folder left by an earlier attempt is replaced.
     */
    public static Staged stage(Path zip, Path dataDir) throws IOException, RefusedException {
        Path staging = dataDir.resolve(AppDataArchive.STAGING_FOLDER);
        AppDataBackup.deleteTree(staging);
        Files.createDirectories(staging);
        try {
            unpack(zip, staging);
            checkIsRestorable(staging);
            return new Staged(dataDir, staging);
        } catch (IOException | RefusedException | RuntimeException e) {
            AppDataBackup.deleteTree(staging);
            throw e;
        }
    }

    private static void unpack(Path zip, Path staging) throws IOException, RefusedException {
        try (ZipFile file = new ZipFile(zip.toFile())) {
            for (ZipEntry entry : file.stream().toList()) {
                if (entry.isDirectory()) continue;
                if (!AppDataArchive.isBackupEntry(entry.getName())) {
                    log.warn("Restore: skipping {}, which is not part of a backup", entry.getName());
                    continue;
                }
                Path target = staging.resolve(entry.getName()).normalize();
                // WHY: an entry named ../something would otherwise be written outside the staging folder.
                if (!target.startsWith(staging)) {
                    throw new RefusedException(Refusal.NOT_A_BACKUP, "entry outside the backup: " + entry.getName());
                }
                Files.createDirectories(target.getParent());
                try (InputStream in = file.getInputStream(entry)) {
                    Files.copy(in, target);
                }
            }
        } catch (ZipException e) {
            throw new RefusedException(Refusal.NOT_A_BACKUP, "not a zip: " + e.getMessage());
        }
    }

    private static void checkIsRestorable(Path staging) throws IOException, RefusedException {
        Path info = staging.resolve(AppDataArchive.INFO_FILE);
        if (!Files.isRegularFile(info))
            throw new RefusedException(Refusal.NOT_A_BACKUP, "no " + AppDataArchive.INFO_FILE);
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(info)) {
            properties.load(in);
        }
        String format = properties.getProperty(AppDataArchive.INFO_FORMAT);
        if (!AppDataArchive.FORMAT.equals(format)) {
            throw new RefusedException(Refusal.NEWER_VERSION, "backup format " + format);
        }
        Path dbFolder = staging.resolve(AppDataArchive.DB_FOLDER);
        if (!Files.isRegularFile(dbFolder.resolve(AppDataArchive.SHARED_DATABASE))) {
            throw new RefusedException(Refusal.NOT_A_BACKUP, "no " + AppDataArchive.SHARED_DATABASE);
        }
        try (Stream<Path> databases = Files.list(dbFolder)) {
            for (Path database : databases.toList()) {
                checkNotNewer(database);
            }
        }
    }

    /**
     * Refuses a database migrated past the newest migration this version ships: a backup from a newer Elite Intel.
     * Its tables would not be what this version's code expects, and nothing here can undo a migration. An older
     * backup is fine - the next start migrates it forward like any other database.
     * <p>
     * WHY the newest one rather than every applied migration being known: a long-lived database records migrations
     * that were later removed or renamed in the tree (the Ollama removal, early schema files), so an applied name
     * this version does not ship is not by itself a sign of a newer version. Migrations apply in filename order
     * ({@link DatabaseMigrator}), so one that sorts after everything shipped is.
     */
    private static void checkNotNewer(Path database) throws RefusedException {
        String name = database.getFileName().toString();
        DatabaseMigrator.Tree tree = name.startsWith(AppDataArchive.COMMANDER_DATABASE_PREFIX)
                ? DatabaseMigrator.Tree.COMMANDER
                : DatabaseMigrator.Tree.SHARED;
        Optional<String> newestApplied = withDatabase(database, h -> h.createQuery("SELECT version FROM schema_migration")
                .mapTo(String.class)
                .stream()
                .max(Comparator.naturalOrder()));
        String newestShipped;
        try {
            newestShipped = Collections.max(DatabaseMigrator.findMigrationFiles(tree));
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Cannot list this version's own migrations", e);
        }
        if (newestApplied.isPresent() && newestApplied.get().compareTo(newestShipped) > 0) {
            throw new RefusedException(Refusal.NEWER_VERSION,
                    name + " has " + newestApplied.get() + ", this version ends at " + newestShipped);
        }
    }

    private static <R> R withDatabase(Path database, HandleCallback<R, RuntimeException> work) {
        return Jdbi.create("jdbc:sqlite:" + database).withHandle(work);
    }

    /**
     * A backup unpacked and checked, waiting for its key to be unlocked (when it has one) and for the commander to
     * go ahead.
     */
    public static final class Staged {

        private final Path dataDir;
        private final Path staging;

        private Staged(Path dataDir, Path staging) {
            this.dataDir = dataDir;
            this.staging = staging;
        }

        /**
         * Whether the backup carries a password-locked key, which {@link #unlock} opens.
         */
        public boolean hasLockedKey() {
            return Files.isRegularFile(staging.resolve(AppDataArchive.SEALED_KEY));
        }

        /**
         * Opens the locked key with {@code password}.
         *
         * @return false when the password is wrong; nothing changes and the commander may try again
         */
        public boolean unlock(char[] password) throws IOException {
            try {
                byte[] key = SealedKey.unseal(Files.readAllBytes(staging.resolve(AppDataArchive.SEALED_KEY)), password);
                Files.write(staging.resolve(AppDataArchive.KEY_FILE), key);
                return true;
            } catch (SealedKey.WrongPasswordException e) {
                return false;
            }
        }

        /**
         * Puts the old computer's settings right and leaves the backup for the next start.
         * <p>
         * The key the restored API keys are tested against is the unlocked one, else this computer's
         * {@code currentKeyFile}: a backup restored onto the computer that made it keeps its API keys even when its
         * key was left out.
         */
        public void commit(Path currentKeyFile) throws IOException {
            Path sharedDatabase = staging.resolve(AppDataArchive.DB_FOLDER).resolve(AppDataArchive.SHARED_DATABASE);
            Path unlockedKey = staging.resolve(AppDataArchive.KEY_FILE);
            Path key = Files.isRegularFile(unlockedKey) ? unlockedKey : currentKeyFile;
            byte[] keyBytes = Files.isRegularFile(key) ? Files.readAllBytes(key) : null;
            withDatabase(sharedDatabase, h -> {
                forgetMissingFolders(h);
                clearUnreadableApiKeys(h, keyBytes);
                return null;
            });
            Files.deleteIfExists(staging.resolve(AppDataArchive.SEALED_KEY));

            Path pending = dataDir.resolve(AppDataArchive.PENDING_FOLDER);
            AppDataBackup.deleteTree(pending);
            Files.move(staging, pending);
        }

        /**
         * Drops the staged backup; nothing live was touched.
         */
        public void discard() throws IOException {
            AppDataBackup.deleteTree(staging);
        }
    }

    /**
     * Forgets each folder setting that names a folder this computer does not have - the old computer's journal and
     * bindings folders, or its music. The app then falls back to this platform's usual folders, and the setup check
     * says what still needs choosing.
     */
    private static void forgetMissingFolders(Handle h) {
        for (FolderSetting setting : FOLDER_SETTINGS) {
            String table = setting.table();
            String column = setting.column();
            if (!hasColumn(h, table, column)) continue;
            List<String> stored = h.createQuery("SELECT " + column + " FROM " + table + " WHERE " + column + " IS NOT NULL")
                    .mapTo(String.class)
                    .list();
            for (String folder : stored) {
                if (isFolderHere(folder)) continue;
                h.execute("UPDATE " + table + " SET " + column + " = NULL WHERE " + column + " = ?", folder);
            }
        }
    }

    private static boolean isFolderHere(String folder) {
        try {
            return !folder.isBlank() && Files.isDirectory(Path.of(folder.trim()));
        } catch (InvalidPathException e) {
            return false;
        }
    }

    /**
     * Clears each stored API key that {@code keyBytes} cannot decrypt, so the app asks for it again rather than
     * failing to read it. No key at all clears every one.
     */
    private static void clearUnreadableApiKeys(Handle h, byte[] keyBytes) {
        for (String column : ENCRYPTED_KEY_COLUMNS) {
            if (!hasColumn(h, "game_session", column)) continue;
            List<String> stored = h.createQuery("SELECT " + column + " FROM game_session WHERE " + column + " IS NOT NULL AND " + column + " <> ''")
                    .mapTo(String.class)
                    .list();
            for (String encrypted : stored) {
                if (keyBytes != null && Cypher.decryptsWith(keyBytes, encrypted)) continue;
                h.execute("UPDATE game_session SET " + column + " = NULL WHERE " + column + " = ?", encrypted);
            }
        }
    }

    private static boolean hasColumn(Handle h, String table, String column) {
        return h.createQuery("SELECT COUNT(*) FROM pragma_table_info(:table) WHERE name = :column")
                .bind("table", table)
                .bind("column", column)
                .mapTo(Integer.class)
                .one() > 0;
    }
}
