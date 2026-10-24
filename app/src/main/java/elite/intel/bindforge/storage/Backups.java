package elite.intel.bindforge.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * BindForge's one backup system (storage.md rules 11-12; file-manager.md, <em>Player Backups</em>).
 * <p>
 * <strong>A backup is a folder</strong>, named with its date stamp, holding a copy of every game input configuration
 * file: every {@code .binds} and {@code StartPreset.N.start} in the bindings folder, and each installation's
 * {@code DeviceMappings.xml} and {@code .buttonMap}s - the full set from every installation, even when they match,
 * with no comparison first. A folder, not a zip, so the user can look through it (Alan, 2026-10-24).
 * <p>
 * <strong>The backup folder is the record.</strong> The list is its listing, sorted by the stamp in each name, with
 * no database table. A folder is a backup only when it holds {@code backup-info.txt}, so V1.1's folder backups and the
 * old BindForge ones are never listed and never deleted (Alan, 2026-10-24).
 * <p>
 * <strong>Every write to a game file calls {@link #backUpEverything()} first</strong> (principle 3). Touches the
 * disk - not for the EDT.
 */
public final class Backups {

    private static final Logger log = LogManager.getLogger(Backups.class);

    /** The file that makes a folder a backup, and says what it holds. */
    public static final String INFO_FILE = "backup-info.txt";
    static final String BINDINGS = "Bindings";
    static final String INSTALLATIONS = "Installations";
    static final String CONTROL_SCHEMES = "ControlSchemes";
    static final String DEVICE_MAPPINGS = "DeviceMappings.xml";
    static final String DEVICE_BUTTON_MAPS = "DeviceButtonMaps";
    static final String FIRST_SETUP_SUFFIX = "_first-setup";
    private static final String TAKING_PREFIX = ".taking-";

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter READABLE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** A stamp, then first setup's mark, then the number added when two backups share a second. */
    private static final Pattern NAME =
            Pattern.compile("(\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2})(" + FIRST_SETUP_SUFFIX + ")?(-\\d+)?");

    /** {@code <name>.<major>.<minor>.binds} - a {@code .binds.backup} copy or an unversioned name is not one. */
    private static final Pattern BINDS_FILE = Pattern.compile(".+\\.\\d+\\.\\d+\\.binds");
    /** {@code StartPreset.4.start}; a leftover pre-Odyssey {@code StartPreset.start} is not one. */
    private static final Pattern START_PRESET_FILE = Pattern.compile("StartPreset\\.\\d+\\.start");
    private static final String BUTTON_MAP_SUFFIX = ".buttonMap";

    private static final String STATUS_LINE = "Status: ";
    private static final String FULL = "Full";
    private static final String PARTIAL = "Partial";
    private static final String NOT_READ_HEADING = "Could not be read:";
    private static final String NOT_READ_ITEM = "- ";

    /**
     * One backup or one pruning at a time, across every caller: the startup backup and Backup Now can overlap, and
     * pruning removes a half-taken backup left by a crash, which must never be one being taken now.
     */
    private static final Object WRITING = new Object();

    /** What a backup takes. */
    public enum Kind {
        /** Every other backup: at Elite-Intel start, Backup Now, before a write. */
        REGULAR,
        /** First-time startup's, before the working copy is created - kept forever (storage.md rule 3). */
        FIRST_SETUP
    }

    /** Thrown when not one file could be read, so no backup was written. */
    public static final class NothingToBackUpException extends IOException {
        /**
         * What the user does about it, in the log (Alan, 2026-10-24). The screen says the same from its bundles.
         */
        public static final String ADVICE = "The game creates its input configuration files once the player starts "
                + "Elite Dangerous and plays past the tutorials.";

        private final transient List<String> notRead;

        NothingToBackUpException(List<String> notRead) {
            super("No game input configuration file could be read"
                    + (notRead.isEmpty() ? "" : ": " + String.join("; ", notRead)));
            this.notRead = List.copyOf(notRead);
        }

        /** What could not be read, one line each. */
        public List<String> notRead() {
            return notRead;
        }
    }

    /** Gathers where the files are; may run detection. */
    @FunctionalInterface
    interface Sources {
        BackupSources get();
    }

    /** Resolves the backup folder. */
    @FunctionalInterface
    interface Root {
        Path get() throws IOException;
    }

    /** Deletes a folder and everything in it. */
    @FunctionalInterface
    interface TreeDeleter {
        void delete(Path folder) throws IOException;
    }

    private final Sources sources;
    private final Root root;
    private final Clock clock;
    private final TreeDeleter treeDeleter;

    /** Detection and the stored installations, the chosen backup folder, and the system clock. */
    public static Backups stored() {
        return new Backups(BackupSources::gather, BackupDestination.stored()::resolve, Clock.systemDefaultZone());
    }

    Backups(Sources sources, Root root, Clock clock) {
        this(sources, root, clock, Backups::walkAndDelete);
    }

    /** Test seam: {@code treeDeleter} can fail part way, as a locked file would. */
    Backups(Sources sources, Root root, Clock clock, TreeDeleter treeDeleter) {
        this.sources = sources;
        this.root = root;
        this.clock = clock;
        this.treeDeleter = treeDeleter;
    }

    /**
     * Backs up everything - the one call every write uses first.
     *
     * @return the backup taken, Full or Partial
     * @throws NothingToBackUpException if not one file could be read; nothing is written
     * @throws IOException              if the backup folder cannot be written
     */
    public Backup backUpEverything() throws IOException {
        return backUpEverything(Kind.REGULAR);
    }

    /** As {@link #backUpEverything()}, marked as {@code kind}. */
    public Backup backUpEverything(Kind kind) throws IOException {
        BackupSources from = sources.get();
        synchronized (WRITING) {
            Path backupRoot = root.get();
            LocalDateTime now = LocalDateTime.now(clock).withNano(0);
            String name = uniqueName(backupRoot, now.format(STAMP) + (kind == Kind.FIRST_SETUP ? FIRST_SETUP_SUFFIX : ""));
            Path taking = backupRoot.resolve(TAKING_PREFIX + name);
            Path folder = backupRoot.resolve(name);

            Copy copy = new Copy(taking);
            try {
                Files.createDirectories(taking);
                copyBindings(copy, from.bindingsFolder(), taking.resolve(BINDINGS), "the bindings folder");
                List<BackupSources.Installation> installations = from.installations();
                for (int i = 0; i < installations.size(); i++) {
                    copyInstallation(copy, i + 1, installations.get(i), from.bindingsFolder());
                }
                if (copy.copied == 0) {
                    throw new NothingToBackUpException(copy.notRead);
                }
                Files.writeString(taking.resolve(INFO_FILE), info(now, kind, from, copy.notRead),
                        StandardCharsets.UTF_8);
                // WHY: renamed into place only once complete, so a half-taken backup is never in the list.
                Files.move(taking, folder, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException | RuntimeException e) {
                deleteQuietly(taking, e);
                throw e;
            }
            Backup backup = new Backup(folder, now, kind == Kind.FIRST_SETUP, copy.notRead.isEmpty(), copy.notRead);
            log.info("Backed up {} game input configuration files to {} ({}){}", copy.copied, folder,
                    backup.full() ? FULL : PARTIAL,
                    backup.full() ? "" : " - could not read: " + String.join("; ", copy.notRead));
            return backup;
        }
    }

    /**
     * Every backup, newest first. A folder without {@code backup-info.txt}, or whose name is not a date stamp, is not
     * one of these backups and is left out.
     */
    public List<Backup> list() throws IOException {
        return list(root.get());
    }

    /** As {@link #list()}, in a backup folder already resolved - so one prune works in one folder throughout. */
    private static List<Backup> list(Path backupRoot) throws IOException {
        List<Backup> backups = new ArrayList<>();
        try (DirectoryStream<Path> folders = Files.newDirectoryStream(backupRoot, Files::isDirectory)) {
            for (Path folder : folders) {
                Backup backup = read(folder);
                if (backup != null) backups.add(backup);
            }
        }
        backups.sort(Comparator.comparing(Backup::taken).thenComparing(b -> b.folder().getFileName().toString())
                .reversed());
        return backups;
    }

    /**
     * Deletes every backup older than {@code retentionDays}, except first setup's, and any half-taken backup a crash
     * left (storage.md rule 11). A backup that cannot be deleted is logged and left; the next start tries again.
     *
     * @return the folders deleted
     */
    public List<Path> prune(int retentionDays) throws IOException {
        if (retentionDays < 1) {
            // WHY: zero would delete the backup just taken. The setter refuses it; this refuses it again here, at the
            // one place it would do harm.
            throw new IllegalArgumentException("retention must be at least one day, was " + retentionDays);
        }
        synchronized (WRITING) {
            Path backupRoot = root.get();
            LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(retentionDays);
            List<Path> deleted = new ArrayList<>();
            for (Backup backup : list(backupRoot)) {
                if (backup.firstSetup() || !backup.taken().isBefore(cutoff)) continue;
                if (deleteBackup(backupRoot, backup.folder())) deleted.add(backup.folder());
            }
            try (DirectoryStream<Path> leftovers = Files.newDirectoryStream(backupRoot, TAKING_PREFIX + "*")) {
                for (Path leftover : leftovers) {
                    if (Files.isDirectory(leftover) && deleteTree(leftover)) deleted.add(leftover);
                }
            }
            if (!deleted.isEmpty()) {
                log.info("Deleted {} backup folder(s) older than {} days: {}", deleted.size(), retentionDays, deleted);
            }
            return deleted;
        }
    }

    // ---- taking

    /** Counts what was copied and collects what could not be read, so one bad file never stops the rest. */
    private static final class Copy {
        final Path into;
        int copied;
        final List<String> notRead = new ArrayList<>();

        Copy(Path into) {
            this.into = into;
        }

        void file(Path source, Path target) {
            try {
                Files.createDirectories(target.getParent());
                Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                copied++;
            } catch (IOException e) {
                notRead.add(source + ": " + reason(e));
                log.warn("Could not back up {}", source, e);
            }
        }
    }

    private static void copyBindings(Copy copy, Path bindingsFolder, Path into, String label) {
        if (!Files.isDirectory(bindingsFolder)) {
            copy.notRead.add(label + " (" + bindingsFolder + "): folder not found");
            return;
        }
        try (Stream<Path> files = Files.list(bindingsFolder)) {
            for (Path file : files.filter(Files::isRegularFile).filter(Backups::isBindingsFile).sorted().toList()) {
                copy.file(file, into.resolve(file.getFileName().toString()));
            }
        } catch (IOException e) {
            copy.notRead.add(label + " (" + bindingsFolder + "): " + reason(e));
            log.warn("Could not list {}", bindingsFolder, e);
        }
    }

    private static void copyInstallation(Copy copy, int number, BackupSources.Installation installation,
                                         Path mainBindingsFolder) {
        String label = "installation " + number + " (" + installation.storefront() + ", " + installation.root() + ")";
        Path into = copy.into.resolve(INSTALLATIONS).resolve(number + "-" + safe(installation.storefront()));

        Path own = installation.bindingsFolder();
        if (own != null && !sameFolder(own, mainBindingsFolder)) {
            copyBindings(copy, own, into.resolve(BINDINGS), label + "'s bindings folder");
        }

        Path controlSchemes = installation.controlSchemes();
        if (installation.missing() || !Files.isDirectory(controlSchemes)) {
            copy.notRead.add(label + ": folder not found");
            return;
        }
        Path deviceMappings = controlSchemes.resolve(DEVICE_MAPPINGS);
        if (Files.isRegularFile(deviceMappings)) {
            copy.file(deviceMappings, into.resolve(CONTROL_SCHEMES).resolve(DEVICE_MAPPINGS));
        }
        Path buttonMaps = controlSchemes.resolve(DEVICE_BUTTON_MAPS);
        if (!Files.isDirectory(buttonMaps)) return;
        try (Stream<Path> files = Files.list(buttonMaps)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(BUTTON_MAP_SUFFIX)).sorted().toList()) {
                copy.file(file, into.resolve(CONTROL_SCHEMES).resolve(DEVICE_BUTTON_MAPS)
                        .resolve(file.getFileName().toString()));
            }
        } catch (IOException e) {
            copy.notRead.add(label + "'s " + DEVICE_BUTTON_MAPS + ": " + reason(e));
            log.warn("Could not list {}", buttonMaps, e);
        }
    }

    static boolean isBindingsFile(Path file) {
        String name = file.getFileName().toString();
        return BINDS_FILE.matcher(name).matches() || START_PRESET_FILE.matcher(name).matches();
    }

    /** {@code backup-info.txt}: for a person reading the backup, and what {@link #list()} reads back. */
    private static String info(LocalDateTime taken, Kind kind, BackupSources from, List<String> notRead) {
        StringBuilder text = new StringBuilder();
        text.append("Elite-Intel backup of your game input configuration files.\n");
        text.append("Taken: ").append(taken.format(READABLE)).append('\n');
        text.append(STATUS_LINE).append(notRead.isEmpty() ? FULL : PARTIAL).append('\n');
        if (kind == Kind.FIRST_SETUP) {
            text.append("Kept forever: first-time startup's backup, taken before BindForge wrote anything.\n");
        }
        text.append('\n').append(BINDINGS).append(": ").append(from.bindingsFolder()).append('\n');
        List<BackupSources.Installation> installations = from.installations();
        for (int i = 0; i < installations.size(); i++) {
            BackupSources.Installation installation = installations.get(i);
            text.append(INSTALLATIONS).append('\\').append(i + 1).append('-').append(safe(installation.storefront()))
                    .append(": ").append(installation.storefront()).append(", ").append(installation.root())
                    .append('\n');
        }
        if (!notRead.isEmpty()) {
            text.append('\n').append(NOT_READ_HEADING).append('\n');
            notRead.forEach(line -> text.append(NOT_READ_ITEM).append(line).append('\n'));
        }
        return text.toString();
    }

    private static String uniqueName(Path backupRoot, String name) {
        String candidate = name;
        for (int n = 2; Files.exists(backupRoot.resolve(candidate))
                || Files.exists(backupRoot.resolve(TAKING_PREFIX + candidate)); n++) {
            candidate = name + "-" + n;
        }
        return candidate;
    }

    // ---- reading

    /** The backup in {@code folder}, or {@code null} when it is not one. */
    static Backup read(Path folder) {
        Matcher name = NAME.matcher(folder.getFileName().toString());
        Path info = folder.resolve(INFO_FILE);
        if (!name.matches() || !Files.isRegularFile(info)) return null;
        LocalDateTime taken;
        try {
            taken = LocalDateTime.parse(name.group(1), STAMP);
        } catch (DateTimeParseException e) {
            return null;
        }
        boolean full = true;
        List<String> notRead = new ArrayList<>();
        try {
            boolean inNotRead = false;
            for (String line : Files.readAllLines(info, StandardCharsets.UTF_8)) {
                if (line.startsWith(STATUS_LINE)) full = FULL.equals(line.substring(STATUS_LINE.length()).trim());
                else if (line.equals(NOT_READ_HEADING)) inNotRead = true;
                else if (inNotRead && line.startsWith(NOT_READ_ITEM)) notRead.add(line.substring(NOT_READ_ITEM.length()));
            }
        } catch (IOException e) {
            // WHY: still listed - the files are there, and only the summary could not be read.
            log.warn("Could not read {}", info, e);
            full = false;
            notRead.add(INFO_FILE + ": " + reason(e));
        }
        return new Backup(folder, taken, name.group(2) != null, full, notRead);
    }

    // ---- helpers

    /**
     * Deletes a backup by first renaming it to a {@code .taking-} folder, then deleting that.
     * <p>
     * <strong>Why the rename</strong> (review, 2026-10-24): deleting in place can remove {@code backup-info.txt} before
     * a file that then refuses to go - on Linux it sorts after the capitalised folders, so it goes first. What is left
     * is no longer a backup, so it would never be listed or pruned again. Renamed first, a delete that stops part way
     * leaves a leftover the next prune's sweep removes.
     */
    private boolean deleteBackup(Path backupRoot, Path folder) {
        Path doomed = backupRoot.resolve(uniqueName(backupRoot, TAKING_PREFIX + folder.getFileName()));
        try {
            Files.move(folder, doomed, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.warn("Could not delete backup folder {}; tried again at the next start", folder, e);
            return false;
        }
        return deleteTree(doomed);
    }

    private boolean deleteTree(Path folder) {
        try {
            treeDeleter.delete(folder);
            return true;
        } catch (IOException e) {
            log.warn("Could not delete backup folder {}; tried again at the next start", folder, e);
            return false;
        }
    }

    /** Deletes a folder and everything in it, deepest first. */
    private static void walkAndDelete(Path folder) throws IOException {
        try (Stream<Path> paths = Files.walk(folder)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    /** Removes a backup that failed part way. What cannot be removed is attached to the failure, not thrown over it. */
    private static void deleteQuietly(Path folder, Exception failure) {
        if (!Files.exists(folder)) return;
        try (Stream<Path> paths = Files.walk(folder)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            failure.addSuppressed(e);
        }
    }

    private static boolean sameFolder(Path a, Path b) {
        return a.toAbsolutePath().normalize().equals(b.toAbsolutePath().normalize());
    }

    /** A storefront is a label, not a filename; anything that could not be one is replaced. */
    private static String safe(String storefront) {
        return storefront.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private static String reason(IOException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getClass().getSimpleName() + " " + e.getMessage();
    }
}
