package elite.intel.appdata;

import elite.intel.db.util.DatabaseMigrator;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A backup made on one computer ({@code oldDir}, its key {@code oldKey}) restored onto another ({@code newDir},
 * {@code newKey}), through both halves: staging and the swap at the next start.
 */
class AppDataBackupRestoreTest {

    private static final byte[] OLD_KEY = "old-computer-key-0123456789abcde".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] NEW_KEY = "new-computer-key-0123456789abcde".getBytes(StandardCharsets.US_ASCII);
    private static final char[] PASSWORD = "o7 commander".toCharArray();

    @TempDir
    Path root;

    private Path oldDir;
    private Path oldKey;
    private Path newDir;
    private Path newKey;
    private Path zip;

    @BeforeEach
    void setUp() throws Exception {
        oldDir = Files.createDirectories(root.resolve("old/elite-intel"));
        oldKey = Files.write(oldDir.resolve("secret.key"), OLD_KEY);
        newDir = Files.createDirectories(root.resolve("new/elite-intel"));
        newKey = Files.write(newDir.resolve("secret.key"), NEW_KEY);
        zip = root.resolve("backup.zip");

        Path sharedDatabase = Files.createDirectories(oldDir.resolve("db")).resolve("database.db");
        Path commanderDatabase = oldDir.resolve("db/cmdr_F1234.db");
        migrate(sharedDatabase, DatabaseMigrator.Tree.SHARED);
        migrate(commanderDatabase, DatabaseMigrator.Tree.COMMANDER);
        Files.createDirectories(oldDir.resolve("custom-commands"));
        Files.writeString(oldDir.resolve("custom-commands/custom_commands.json"), "[\"launch\"]");
    }

    @Test
    void aPasswordCarriesTheKeySoTheApiKeysStillRead() throws Exception {
        String llmKey = encrypt(OLD_KEY, "sk-test");
        // WHY the open handle: the write then sits in the WAL file, which a plain copy of the .db would miss.
        try (Handle live = open(oldDir.resolve("db/database.db"))) {
            setApiKey(live, llmKey);
            AppDataBackup.write(oldDir, oldKey, zip, PASSWORD, "1.2.0-test");
        }

        AppDataRestore.Staged staged = AppDataRestore.stage(zip, newDir);
        assertTrue(staged.hasLockedKey());
        assertFalse(staged.unlock("wrong".toCharArray()));
        assertTrue(staged.unlock(PASSWORD));
        staged.commit(newKey);

        assertEquals(PendingRestore.Outcome.RESTORED, PendingRestore.applyIfPresent(newDir, newKey));
        assertArrayEquals(OLD_KEY, Files.readAllBytes(newKey));
        assertEquals(Optional.of(llmKey), apiKey(newDir));
        assertEquals("[\"launch\"]", Files.readString(newDir.resolve("custom-commands/custom_commands.json")));
        assertTrue(Files.exists(newDir.resolve("db/cmdr_F1234.db")));
        assertFalse(Files.exists(newDir.resolve(AppDataArchive.PENDING_FOLDER)));
        assertArrayEquals(NEW_KEY, Files.readAllBytes(safetyFolder(newDir).resolve("secret.key")));
    }

    @Test
    void withoutAPasswordAnotherComputerLosesTheApiKeys() throws Exception {
        try (Handle live = open(oldDir.resolve("db/database.db"))) {
            setApiKey(live, encrypt(OLD_KEY, "sk-test"));
        }
        AppDataBackup.write(oldDir, oldKey, zip, null, "1.2.0-test");

        AppDataRestore.Staged staged = AppDataRestore.stage(zip, newDir);
        assertFalse(staged.hasLockedKey());
        staged.commit(newKey);
        PendingRestore.applyIfPresent(newDir, newKey);

        assertEquals(Optional.empty(), apiKey(newDir));
        assertArrayEquals(NEW_KEY, Files.readAllBytes(newKey));
    }

    @Test
    void withoutAPasswordTheSameComputerKeepsTheApiKeys() throws Exception {
        String llmKey = encrypt(OLD_KEY, "sk-test");
        try (Handle live = open(oldDir.resolve("db/database.db"))) {
            setApiKey(live, llmKey);
        }
        AppDataBackup.write(oldDir, oldKey, zip, null, "1.2.0-test");

        AppDataRestore.stage(zip, oldDir).commit(oldKey);
        PendingRestore.applyIfPresent(oldDir, oldKey);

        assertEquals(Optional.of(llmKey), apiKey(oldDir));
    }

    @Test
    void foldersThatDoNotExistHereAreForgotten() throws Exception {
        Path bindingsHere = Files.createDirectories(root.resolve("bindings-here"));
        try (Handle live = open(oldDir.resolve("db/database.db"))) {
            live.execute("UPDATE user_preferences SET journal_dir = ?, bindings_dir = ? WHERE id = 1",
                    "C:\\Users\\cmdr\\Saved Games\\Frontier Developments\\Elite Dangerous", bindingsHere.toString());
        }
        AppDataBackup.write(oldDir, oldKey, zip, null, "1.2.0-test");

        AppDataRestore.stage(zip, newDir).commit(newKey);
        PendingRestore.applyIfPresent(newDir, newKey);

        try (Handle restored = open(newDir.resolve("db/database.db"))) {
            assertNull(restored.createQuery("SELECT journal_dir FROM user_preferences WHERE id = 1").mapTo(String.class).one());
            assertEquals(bindingsHere.toString(),
                    restored.createQuery("SELECT bindings_dir FROM user_preferences WHERE id = 1").mapTo(String.class).one());
        }
    }

    @Test
    void aBackupFromANewerVersionIsRefused() throws Exception {
        try (Handle live = open(oldDir.resolve("db/database.db"))) {
            live.execute("INSERT INTO schema_migration (version) VALUES ('99999__from_the_future.sql')");
        }
        AppDataBackup.write(oldDir, oldKey, zip, null, "9.9.0-test");

        AppDataRestore.RefusedException refused = assertThrows(AppDataRestore.RefusedException.class,
                () -> AppDataRestore.stage(zip, newDir));

        assertEquals(AppDataRestore.Refusal.NEWER_VERSION, refused.refusal());
        assertFalse(Files.exists(newDir.resolve(AppDataArchive.STAGING_FOLDER)));
    }

    @Test
    void aMigrationRemovedFromTheTreeSinceIsNotANewerVersion() throws Exception {
        // WHY: long-lived databases record migrations later dropped from the tree, such as 01028__remove_ollama.sql.
        try (Handle live = open(oldDir.resolve("db/database.db"))) {
            live.execute("INSERT INTO schema_migration (version) VALUES ('00054__schema.sql')");
        }
        AppDataBackup.write(oldDir, oldKey, zip, null, "1.2.0-test");

        AppDataRestore.stage(zip, newDir).commit(newKey);

        assertEquals(PendingRestore.Outcome.RESTORED, PendingRestore.applyIfPresent(newDir, newKey));
    }

    @Test
    void aZipThatIsNotABackupIsRefused() throws Exception {
        writeZip(zip, "readme.txt");

        AppDataRestore.RefusedException refused = assertThrows(AppDataRestore.RefusedException.class,
                () -> AppDataRestore.stage(zip, newDir));

        assertEquals(AppDataRestore.Refusal.NOT_A_BACKUP, refused.refusal());
    }

    @Test
    void anEntryReachingOutsideTheDataFolderIsRefused() throws Exception {
        writeZip(zip, "db/../../escaped.txt");

        AppDataRestore.RefusedException refused = assertThrows(AppDataRestore.RefusedException.class,
                () -> AppDataRestore.stage(zip, newDir));

        assertEquals(AppDataRestore.Refusal.NOT_A_BACKUP, refused.refusal());
        assertFalse(Files.exists(newDir.resolve("escaped.txt")));
    }

    @Test
    void aStartWithNothingWaitingChangesNothing() throws IOException {
        assertEquals(PendingRestore.Outcome.NONE, PendingRestore.applyIfPresent(newDir, newKey));
        assertArrayEquals(NEW_KEY, Files.readAllBytes(newKey));
    }

    private static void migrate(Path database, DatabaseMigrator.Tree tree) throws Exception {
        try (Handle h = open(database)) {
            DatabaseMigrator.migrate(h, tree);
        }
    }

    private static Handle open(Path database) {
        return Jdbi.create("jdbc:sqlite:" + database).open();
    }

    private static void setApiKey(Handle h, String encrypted) {
        h.execute("INSERT OR IGNORE INTO game_session (id) VALUES (1)");
        h.execute("UPDATE game_session SET encryptedLLMKey = ? WHERE id = 1", encrypted);
    }

    private static Optional<String> apiKey(Path dataDir) {
        try (Handle h = open(dataDir.resolve("db/database.db"))) {
            return Optional.ofNullable(h.createQuery("SELECT encryptedLLMKey FROM game_session WHERE id = 1")
                    .mapTo(String.class)
                    .one());
        }
    }

    /**
     * As {@code Cypher.encrypt}, under a key of the test's choosing.
     */
    private static String encrypt(byte[] key, String text) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        return Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static Path safetyFolder(Path dataDir) throws IOException {
        try (Stream<Path> entries = Files.list(dataDir)) {
            return entries.filter(p -> p.getFileName().toString().startsWith(AppDataArchive.SAFETY_FOLDER_PREFIX))
                    .findFirst()
                    .orElseThrow();
        }
    }

    private static void writeZip(Path zip, String entryName) throws IOException {
        try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream zipOut = new ZipOutputStream(out)) {
            zipOut.putNextEntry(new ZipEntry(entryName));
            zipOut.write("x".getBytes(StandardCharsets.US_ASCII));
            zipOut.closeEntry();
        }
    }
}
