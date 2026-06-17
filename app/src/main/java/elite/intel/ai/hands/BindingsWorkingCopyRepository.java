package elite.intel.ai.hands;

import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;

/**
 * Manages per-preset working copies of Elite Dangerous {@code .binds} files.
 * <p>
 * Working copies are stored in {@code elite-intel/bindings/} so the editor never
 * touches the game directory until the user explicitly applies via
 * {@link BindingsApplyService}. Each preset gets its own file keyed by the
 * original {@code .binds} filename.
 * <p>
 * The initial import is a byte-for-byte copy of the game file, preserving the
 * UTF-8 BOM if present. Subsequent edits are handled by {@link BindingsWriter}
 * which writes directly to the working copy path.
 */
public class BindingsWorkingCopyRepository {

    private static final Logger log = LogManager.getLogger(BindingsWorkingCopyRepository.class);
    private static final String BASELINE_HASH_SUFFIX = ".elite-intel.base.sha256";
    private static final String TMP_SUFFIX = ".tmp";
    private final Path workingDirectory;

    public BindingsWorkingCopyRepository() {
        this(null);
    }

    BindingsWorkingCopyRepository(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * Returns the working copy path for the given preset file name.
     * The file may not exist yet.
     *
     * @throws IllegalStateException if the working directory cannot be resolved
     */
    public Path getWorkingCopyPath(String presetFileName) {
        try {
            Path directory = workingDirectory != null ? workingDirectory : AppPaths.getBindingsWorkingDir();
            return directory.resolve(presetFileName);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot resolve bindings working directory", e);
        }
    }

    /**
     * Ensures a working copy exists for the given preset, importing from the game
     * file on first use. Returns the working copy {@link Path} for the caller to
     * use for parsing and editing.
     *
     * @param presetFileName filename of the active {@code .binds} file (e.g. {@code Custom.3.0.binds})
     * @param gameFile       path to the active game binds file; used only for the initial import
     */
    public Path loadOrImportFromGame(String presetFileName, Path gameFile) throws IOException {
        Path workingCopy = getWorkingCopyPath(presetFileName);
        if (Files.exists(workingCopy)) {
            log.debug("Using existing working copy for '{}' at {}", presetFileName, workingCopy);
            refreshFromGameIfClean(presetFileName, gameFile);
            return workingCopy;
        }
        log.info("No working copy for '{}', importing from game file {}", presetFileName, gameFile);
        Files.createDirectories(workingCopy.getParent());
        // Byte-perfect copy preserves BOM and any encoding details of the original.
        Files.copy(gameFile, workingCopy);
        writeBaselineHash(presetFileName, sha256(gameFile));
        log.info("Working copy created at {}", workingCopy);
        return workingCopy;
    }

    /**
     * Atomically writes {@code xmlContent} as the working copy for the given preset.
     * A {@code .bak} sibling is created before overwriting to guard against corruption.
     * <p>
     * Note: {@link BindingsWriter} writes to the working copy directly via its own
     * atomic write; this method is used for bulk saves (e.g. reimport on revert).
     */
    public void save(String presetFileName, String xmlContent) throws IOException {
        Path workingCopy = getWorkingCopyPath(presetFileName);
        Files.createDirectories(workingCopy.getParent());

        if (Files.exists(workingCopy)) {
            Path bak = workingCopy.resolveSibling(workingCopy.getFileName() + ".bak");
            try {
                Files.copy(workingCopy, bak, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.warn("Could not create .bak for '{}'  proceeding: {}", workingCopy.getFileName(), e.getMessage());
            }
        }

        Path tmp = workingCopy.resolveSibling(workingCopy.getFileName() + ".tmp");
        Files.writeString(tmp, xmlContent, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            Files.move(tmp, workingCopy, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            log.debug("Atomic move not supported for working copy — falling back");
            Files.move(tmp, workingCopy, StandardCopyOption.REPLACE_EXISTING);
        }
        log.debug("Saved working copy for '{}' to {}", presetFileName, workingCopy);
    }

    /** Returns {@code true} if a working copy exists for the given preset. */
    public boolean exists(String presetFileName) {
        try {
            return Files.exists(getWorkingCopyPath(presetFileName));
        } catch (Exception e) {
            return false;
        }
    }

    /** Deletes the working copy and its {@code .bak} sibling for the given preset. */
    public void delete(String presetFileName) {
        try {
            Path workingCopy = getWorkingCopyPath(presetFileName);
            Files.deleteIfExists(workingCopy);
            Files.deleteIfExists(workingCopy.resolveSibling(workingCopy.getFileName() + ".bak"));
            Files.deleteIfExists(baselineHashPath(presetFileName));
            log.info("Deleted working copy for '{}'", presetFileName);
        } catch (IOException e) {
            log.warn("Could not fully delete working copy for '{}': {}", presetFileName, e.getMessage());
        }
    }

    /**
     * Returns {@code true} if the working copy is byte-for-byte identical to the
     * game file. Returns {@code false} on any I/O error or if either file is absent.
     */
    public boolean isSyncedWithGame(String presetFileName, Path gameFile) {
        try {
            Path workingCopy = getWorkingCopyPath(presetFileName);
            if (!Files.exists(workingCopy) || !Files.exists(gameFile)) {
                return false;
            }
            return Files.mismatch(workingCopy, gameFile) == -1L;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns {@code true} only when the EI working copy differs from its imported baseline.
     * If EI has not changed the working copy and the game file has changed, the working copy is refreshed first.
     */
    public boolean hasUnappliedDraft(String presetFileName, Path gameFile) throws IOException {
        refreshFromGameIfClean(presetFileName, gameFile);
        Path workingCopy = getWorkingCopyPath(presetFileName);
        if (!Files.exists(workingCopy)) {
            return false;
        }
        String baselineHash = ensureBaselineHash(presetFileName, gameFile);
        return !sha256(workingCopy).equals(baselineHash);
    }

    /**
     * Returns {@code true} when the current game file still matches the baseline used by the EI draft.
     */
    public boolean gameFileMatchesBaseline(String presetFileName, Path gameFile) throws IOException {
        Path workingCopy = getWorkingCopyPath(presetFileName);
        if (!Files.exists(workingCopy)) {
            return false;
        }
        if (!Files.exists(gameFile)) {
            return true;
        }
        String baselineHash = ensureBaselineHash(presetFileName, gameFile);
        return sha256(gameFile).equals(baselineHash);
    }

    /**
     * Marks the current working copy as applied so later game-only changes are detected against the new baseline.
     */
    public void markApplied(String presetFileName) throws IOException {
        Path workingCopy = getWorkingCopyPath(presetFileName);
        if (!Files.exists(workingCopy)) {
            throw new IOException("Working copy does not exist: " + workingCopy);
        }
        writeBaselineHash(presetFileName, sha256(workingCopy));
    }

    private void refreshFromGameIfClean(String presetFileName, Path gameFile) throws IOException {
        Path workingCopy = getWorkingCopyPath(presetFileName);
        if (!Files.exists(workingCopy) || !Files.exists(gameFile)) {
            return;
        }

        String baselineHash = ensureBaselineHash(presetFileName, gameFile);
        String workingHash = sha256(workingCopy);
        String gameHash = sha256(gameFile);

        if (workingHash.equals(baselineHash) && !gameHash.equals(baselineHash)) {
            copyReplacing(gameFile, workingCopy);
            writeBaselineHash(presetFileName, gameHash);
            log.info("Refreshed clean working copy for '{}' from changed game file", presetFileName);
        }
    }

    private String ensureBaselineHash(String presetFileName, Path gameFile) throws IOException {
        Path baselineHashPath = baselineHashPath(presetFileName);
        if (Files.exists(baselineHashPath)) {
            return Files.readString(baselineHashPath, StandardCharsets.UTF_8).trim();
        }
        return migrateLegacyWorkingCopy(presetFileName, gameFile);
    }

    private String migrateLegacyWorkingCopy(String presetFileName, Path gameFile) throws IOException {
        Path workingCopy = getWorkingCopyPath(presetFileName);
        String workingHash = sha256(workingCopy);
        if (!Files.exists(gameFile)) {
            writeBaselineHash(presetFileName, workingHash);
            return workingHash;
        }

        String gameHash = sha256(gameFile);
        if (workingHash.equals(gameHash)) {
            writeBaselineHash(presetFileName, gameHash);
            return gameHash;
        }

        FileTime workingModified = Files.getLastModifiedTime(workingCopy);
        FileTime gameModified = Files.getLastModifiedTime(gameFile);
        if (workingModified.compareTo(gameModified) <= 0) {
            copyReplacing(gameFile, workingCopy);
            writeBaselineHash(presetFileName, gameHash);
            log.info("Migrated stale clean working copy for '{}' from changed game file", presetFileName);
            return gameHash;
        }

        writeBaselineHash(presetFileName, gameHash);
        log.warn("Migrated existing working copy for '{}' as an EI draft because it is newer than the game file", presetFileName);
        return gameHash;
    }

    private Path baselineHashPath(String presetFileName) {
        Path workingCopy = getWorkingCopyPath(presetFileName);
        return workingCopy.resolveSibling(workingCopy.getFileName() + BASELINE_HASH_SUFFIX);
    }

    private void writeBaselineHash(String presetFileName, String hash) throws IOException {
        Path baselineHashPath = baselineHashPath(presetFileName);
        Files.createDirectories(baselineHashPath.getParent());
        Path tmp = baselineHashPath.resolveSibling(baselineHashPath.getFileName() + TMP_SUFFIX);
        Files.writeString(tmp, hash + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try {
            Files.move(tmp, baselineHashPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, baselineHashPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copyReplacing(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + TMP_SUFFIX);
        Files.copy(source, tmp, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
