package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallation;
import elite.intel.io.AtomicFiles;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * BindForge's one backup system (storage.md rules 11-12; file-manager.md, <em>Player Backups</em>).
 * <p>
 * <strong>A backup is a zip</strong>, named with its date stamp, holding a copy of every game input configuration
 * file: every {@code .binds} and {@code StartPreset.N.start} in the bindings folder, and each installation's
 * {@code DeviceMappings.xml} and {@code .buttonMap}s - the full set from every installation, even when they match,
 * with no comparison first. Every backup a zip, so restore has one format to read (Alan, 2026-10-24).
 * <p>
 * <strong>Four kinds, each in its own folder and kept by its own rule</strong> (Alan, 2026-10-24): daily, kept by age;
 * before changes, kept by count; user, kept until the user deletes them; first setup, kept forever.
 * <p>
 * <strong>The backup folder is the record.</strong> The list is the kind folders' listing, sorted by the stamp in each
 * name, with no database table. Only zips in the kind folders are listed, so V1.1's folder backups, the old BindForge
 * ones and Storage S1's dated folders are never listed and never deleted (Alan, 2026-10-24).
 * <p>
 * <strong>Every write to a game file goes through {@link WritePath}, which takes a backup first</strong> (principle 3) -
 * {@link #backUpBeforeChange(Before, BackupSources)}, or first setup's {@link Kind#FIRST_SETUP}. First-time startup calls it
 * since Storage S2b and RESTORE since S3 ({@link Restore}, reading the zip through {@link BackupContents}); APPLY
 * arrives with S4, and until then the old writers take their own backups.
 * Touches the disk - not for the EDT.
 */
public final class Backups {

    private static final Logger log = LogManager.getLogger(Backups.class);

    /** The file inside every zip that says what it holds. */
    public static final String INFO_FILE = "backup-info.txt";
    static final String BINDINGS = "Bindings";
    static final String INSTALLATIONS = "Installations";
    static final String CONTROL_SCHEMES = "ControlSchemes";
    private static final String ZIP = ".zip";
    /** Zip entry names always use a forward slash, whatever the platform. */
    private static final char ENTRY_SEPARATOR = '/';

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    /** How a backup's time is shown - in {@code backup-info.txt} and on screen. One pattern, used everywhere. */
    public static final DateTimeFormatter TAKEN_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** A stamp, then the number added when two backups of one kind share a second. */
    private static final Pattern NAME = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2})(-\\d+)?\\.zip");
    /** The temporary file {@link AtomicFiles} writes a zip to before renaming it into place. */
    private static final Pattern HALF_WRITTEN = Pattern.compile("\\..+\\.zip\\.elite-intel-.+\\.tmp");

    /** {@code <name>.<major>.<minor>.binds} - a {@code .binds.backup} copy or an unversioned name is not one. */
    private static final Pattern BINDS_FILE = Pattern.compile(".+\\.\\d+\\.\\d+\\.binds");
    /** {@code StartPreset.4.start}; a leftover pre-Odyssey {@code StartPreset.start} is not one. */
    private static final Pattern START_PRESET_FILE = Pattern.compile("StartPreset\\.\\d+\\.start");

    private static final String KIND_LINE = "Kind: ";
    private static final String BEFORE_LINE = "Taken before: ";
    private static final String STATUS_LINE = "Status: ";
    private static final String FULL = "Full";
    private static final String PARTIAL = "Partial";
    private static final String NOT_READ_HEADING = "Could not be read:";
    private static final String NOT_READ_ITEM = "- ";

    /**
     * One backup, one pruning or one delete at a time, across every caller: the startup backup and Backup Now can
     * overlap, and pruning removes a half-written zip left by a crash, which must never be one being written now.
     */
    private static final Object WRITING = new Object();

    /** The four kinds of backup (file-manager.md, <em>Four kinds of backup</em>). */
    public enum Kind {
        /** The first Elite-Intel start each day; kept by age. */
        DAILY("Daily", "Daily"),
        /** Before every write - APPLY, RESTORE, the check; kept by count. */
        BEFORE_CHANGES("BeforeChanges", "Before changes"),
        /** Backup Now; kept until the user deletes it. */
        USER("User", "User"),
        /** First-time startup's, before the working copy is created; kept forever (storage.md rule 3). */
        FIRST_SETUP("FirstSetup", "First setup");

        private final String folder;
        private final String infoLabel;

        Kind(String folder, String infoLabel) {
            this.folder = folder;
            this.infoLabel = infoLabel;
        }

        /** The folder inside the backup folder that holds this kind. */
        public String folder() {
            return folder;
        }
    }

    /** What a before-changes backup was taken before, recorded in its {@code backup-info.txt}. */
    public enum Before {
        APPLY("APPLY"),
        RESTORE("RESTORE"),
        /** The check, of the working copy before a live copy replaces it. */
        CHECK("the check");

        private final String infoLabel;

        Before(String infoLabel) {
            this.infoLabel = infoLabel;
        }
    }

    /**
     * The two retention rules, read from the settings at Elite-Intel start.
     *
     * @param dailyDays         daily backups older than this many days are deleted
     * @param beforeChangesKept the newest this many before-changes backups are kept
     */
    public record Retention(int dailyDays, int beforeChangesKept) {
        public Retention {
            // WHY: zero would delete the backup just taken. The setters refuse it; this refuses it again here, at the
            // one place it would do harm.
            if (dailyDays < 1) throw new IllegalArgumentException("daily retention must be at least one day, was " + dailyDays);
            if (beforeChangesKept < 1) {
                throw new IllegalArgumentException("at least one before-changes backup must be kept, was " + beforeChangesKept);
            }
        }
    }

    /** Thrown when not one file could be read, so no backup was written. */
    public static final class NothingToBackUpException extends IOException {
        /**
         * The bundle key of what the user does about it - the new-player message the startup setup check speaks and
         * Backup Now shows, so the log says the same words (Alan, 2026-10-24).
         */
        public static final String ADVICE_KEY = "speech.setup.noBindingsYet";

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

    private final Sources sources;
    private final Root root;
    private final Clock clock;

    /** Detection and the stored installations, the chosen backup folder, and the system clock. */
    public static Backups stored() {
        return new Backups(BackupSources::rescan, BackupDestination.stored()::resolve, Clock.systemDefaultZone());
    }

    Backups(Sources sources, Root root, Clock clock) {
        this.sources = sources;
        this.root = root;
        this.clock = clock;
    }

    /**
     * Backs up everything before a write - the one call every write to a game file uses first, through
     * {@link WritePath}.
     *
     * @param before what is about to write, recorded in {@code backup-info.txt}
     * @param from   the bindings folder and installations the write is about to write - the write's own list, so
     *               nothing is written that was not backed up (principle 3; review, 2026-10-24)
     * @return the backup taken, Full or Partial
     * @throws NothingToBackUpException if not one file could be read; nothing is written
     * @throws IOException              if the backup folder cannot be written
     */
    public Backup backUpBeforeChange(Before before, BackupSources from) throws IOException {
        if (before == null) throw new IllegalArgumentException("a before-changes backup says what it is taken before");
        return take(Kind.BEFORE_CHANGES, before, from);
    }

    /**
     * Backs up everything as a daily, user or first setup backup. A before-changes backup is
     * {@link #backUpBeforeChange(Before, BackupSources)}, which says what it is taken before.
     *
     * @throws NothingToBackUpException if not one file could be read; nothing is written
     * @throws IOException              if the backup folder cannot be written
     */
    public Backup backUpEverything(Kind kind) throws IOException {
        return backUpEverything(kind, sources.get());
    }

    /**
     * As {@link #backUpEverything(Kind)}, of the bindings folder and installations a write is about to write - first
     * setup's backup, taken through {@link WritePath}.
     */
    public Backup backUpEverything(Kind kind, BackupSources from) throws IOException {
        if (kind == Kind.BEFORE_CHANGES) {
            throw new IllegalArgumentException("a before-changes backup is taken with backUpBeforeChange");
        }
        return take(kind, null, from);
    }

    /**
     * Whether a daily backup stamped today is already in the backup folder - so the daily backup is taken once a day,
     * read from the folder rather than a table (storage.md rule 12). A Partial one counts: it was taken.
     */
    public boolean dailyTakenToday() throws IOException {
        Path folder = root.get().resolve(Kind.DAILY.folder);
        if (!Files.isDirectory(folder)) return false;
        LocalDate today = LocalDate.now(clock);
        try (DirectoryStream<Path> zips = Files.newDirectoryStream(folder, "*" + ZIP)) {
            for (Path zip : zips) {
                LocalDateTime taken = stampOf(zip);
                if (taken != null && taken.toLocalDate().equals(today)) return true;
            }
        }
        return false;
    }

    /**
     * Every backup of every kind, newest first. A file that is not a zip named with a date stamp, in one of the four
     * kind folders, is not one of these backups and is left out.
     */
    public List<Backup> list() throws IOException {
        return list(root.get());
    }

    /** As {@link #list()}, in a backup folder already resolved - so one prune works in one folder throughout. */
    private static List<Backup> list(Path backupRoot) throws IOException {
        List<Backup> backups = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            Path folder = backupRoot.resolve(kind.folder);
            if (!Files.isDirectory(folder)) continue;
            try (DirectoryStream<Path> zips = Files.newDirectoryStream(folder, "*" + ZIP)) {
                for (Path zip : zips) {
                    Backup backup = read(zip, kind);
                    if (backup != null) backups.add(backup);
                }
            }
        }
        backups.sort(Comparator.comparing(Backup::taken).thenComparing(b -> b.zip().getFileName().toString())
                .reversed());
        return backups;
    }

    /**
     * Retention (storage.md rule 11): daily backups older than the setting are deleted, and before-changes backups
     * beyond the newest N, and any half-written zip a crash left. User and first setup backups are never deleted
     * here. A backup that cannot be deleted is logged and left; the next start tries again.
     *
     * @return the files deleted
     */
    public List<Path> prune(Retention retention) throws IOException {
        synchronized (WRITING) {
            Path backupRoot = root.get();
            LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(retention.dailyDays());
            List<Path> deleted = new ArrayList<>();
            int beforeChangesSeen = 0;
            for (Backup backup : list(backupRoot)) {
                boolean doomed = switch (backup.kind()) {
                    case DAILY -> backup.taken().isBefore(cutoff);
                    case BEFORE_CHANGES -> ++beforeChangesSeen > retention.beforeChangesKept();
                    case USER, FIRST_SETUP -> false;
                };
                if (doomed && deleteQuietly(backup.zip())) deleted.add(backup.zip());
            }
            for (Kind kind : Kind.values()) {
                Path folder = backupRoot.resolve(kind.folder);
                if (!Files.isDirectory(folder)) continue;
                try (DirectoryStream<Path> leftovers = Files.newDirectoryStream(folder,
                        p -> HALF_WRITTEN.matcher(p.getFileName().toString()).matches())) {
                    for (Path leftover : leftovers) {
                        if (deleteQuietly(leftover)) deleted.add(leftover);
                    }
                }
            }
            if (!deleted.isEmpty()) {
                log.info("Deleted {} backup(s) by retention (daily older than {} days, before-changes beyond the newest {}): {}",
                        deleted.size(), retention.dailyDays(), retention.beforeChangesKept(), deleted);
            }
            return deleted;
        }
    }

    /**
     * Deletes a user backup - the user made it, so they decide when it goes (file-manager.md, <em>Retention</em>;
     * Alan, 2026-10-24). The other kinds have no Delete.
     *
     * @throws IllegalArgumentException if it is not a user backup
     */
    public void delete(Backup backup) throws IOException {
        if (backup.kind() != Kind.USER) {
            throw new IllegalArgumentException("only a user backup can be deleted, was " + backup.kind());
        }
        synchronized (WRITING) {
            Files.delete(backup.zip());
        }
        log.info("Deleted user backup {}", backup.zip());
    }

    // ---- taking

    private Backup take(Kind kind, Before before, BackupSources from) throws IOException {
        synchronized (WRITING) {
            Path folder = root.get().resolve(kind.folder);
            LocalDateTime now = LocalDateTime.now(clock).withNano(0);

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8);
            Copy copy = new Copy(zip);
            try (zip) {
                copyBindings(copy, from.bindingsFolder(), BINDINGS, "the bindings folder");
                List<BackupSources.Installation> installations = from.installations();
                for (int i = 0; i < installations.size(); i++) {
                    copyInstallation(copy, i + 1, installations.get(i), from.bindingsFolder());
                }
                if (copy.copied == 0) {
                    throw new NothingToBackUpException(copy.notRead);
                }
                copy.entry(INFO_FILE, info(now, kind, before, from, copy.notRead).getBytes(StandardCharsets.UTF_8),
                        null);
            }
            Files.createDirectories(folder);
            Path target = folder.resolve(uniqueName(folder, now.format(STAMP)));
            // WHY: written to a temporary file and renamed into place once complete, so a half-written backup is
            // never in the list. Built in memory first - a full backup is a few megabytes at most (Alan, 2026-10-24).
            AtomicFiles.write(target, bytes.toByteArray());

            Backup backup = new Backup(target, now, kind, before, copy.notRead.isEmpty(), copy.notRead);
            log.info("Backed up {} game input configuration files to {} ({}){}", copy.copied, target,
                    backup.full() ? FULL : PARTIAL,
                    backup.full() ? "" : " - could not read: " + String.join("; ", copy.notRead));
            return backup;
        }
    }

    /** Counts what was copied and collects what could not be read, so one bad file never stops the rest. */
    private static final class Copy {
        final ZipOutputStream zip;
        int copied;
        final List<String> notRead = new ArrayList<>();

        Copy(ZipOutputStream zip) {
            this.zip = zip;
        }

        /** Copies one file in; a file that cannot be read is recorded, not thrown. */
        void file(Path source, String entryName) throws IOException {
            byte[] content;
            FileTime modified;
            try {
                content = Files.readAllBytes(source);
                modified = Files.getLastModifiedTime(source);
            } catch (IOException e) {
                notRead.add(source + ": " + reason(e));
                log.warn("Could not back up {}", source, e);
                return;
            }
            entry(entryName, content, modified);
            copied++;
        }

        void entry(String name, byte[] content, FileTime modified) throws IOException {
            ZipEntry entry = new ZipEntry(name);
            if (modified != null) entry.setLastModifiedTime(modified);
            zip.putNextEntry(entry);
            zip.write(content);
            zip.closeEntry();
        }
    }

    private static void copyBindings(Copy copy, Path bindingsFolder, String into, String label) throws IOException {
        if (!Files.isDirectory(bindingsFolder)) {
            copy.notRead.add(label + " (" + bindingsFolder + "): folder not found");
            return;
        }
        List<Path> found;
        try (Stream<Path> files = Files.list(bindingsFolder)) {
            found = files.filter(Files::isRegularFile).filter(Backups::isBindingsFile).sorted().toList();
        } catch (IOException e) {
            copy.notRead.add(label + " (" + bindingsFolder + "): " + reason(e));
            log.warn("Could not list {}", bindingsFolder, e);
            return;
        }
        for (Path file : found) {
            copy.file(file, into + ENTRY_SEPARATOR + file.getFileName());
        }
    }

    private static void copyInstallation(Copy copy, int number, BackupSources.Installation installation,
                                         Path mainBindingsFolder) throws IOException {
        String label = "installation " + number + " (" + installation.storefront() + ", " + installation.root() + ")";
        String into = INSTALLATIONS + ENTRY_SEPARATOR + number + "-" + safe(installation.storefront());

        Path own = installation.bindingsFolder();
        if (own != null && !sameFolder(own, mainBindingsFolder)) {
            copyBindings(copy, own, into + ENTRY_SEPARATOR + BINDINGS, label + "'s bindings folder");
        }

        Path controlSchemes = installation.controlSchemes();
        if (!installation.reachable()) {
            copy.notRead.add(label + ": folder not found");
            return;
        }
        String schemesEntry = into + ENTRY_SEPARATOR + CONTROL_SCHEMES + ENTRY_SEPARATOR;
        Path deviceMappings = controlSchemes.resolve(GameInstallation.DEVICE_MAPPINGS);
        if (Files.isRegularFile(deviceMappings)) {
            copy.file(deviceMappings, schemesEntry + GameInstallation.DEVICE_MAPPINGS);
        }
        Path buttonMaps = controlSchemes.resolve(GameInstallation.DEVICE_BUTTON_MAPS);
        if (!Files.isDirectory(buttonMaps)) return;
        List<Path> found;
        try (Stream<Path> files = Files.list(buttonMaps)) {
            found = files.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(GameInstallation.BUTTON_MAP_SUFFIX)).sorted().toList();
        } catch (IOException e) {
            copy.notRead.add(label + "'s " + GameInstallation.DEVICE_BUTTON_MAPS + ": " + reason(e));
            log.warn("Could not list {}", buttonMaps, e);
            return;
        }
        for (Path file : found) {
            copy.file(file, schemesEntry + GameInstallation.DEVICE_BUTTON_MAPS + ENTRY_SEPARATOR + file.getFileName());
        }
    }

    /** Whether {@code name} is a binds file's name - {@code <name>.<major>.<minor>.binds}. */
    static boolean isBindsFile(String name) {
        return BINDS_FILE.matcher(name).matches();
    }

    static boolean isBindingsFile(Path file) {
        String name = file.getFileName().toString();
        return BINDS_FILE.matcher(name).matches() || START_PRESET_FILE.matcher(name).matches();
    }

    /** {@code backup-info.txt}: for a person reading the backup, and what {@link #list()} reads back. */
    private static String info(LocalDateTime taken, Kind kind, Before before, BackupSources from,
                               List<String> notRead) {
        StringBuilder text = new StringBuilder();
        text.append("Elite-Intel backup of your game input configuration files.\n");
        text.append("Taken: ").append(taken.format(TAKEN_FORMAT)).append('\n');
        text.append(KIND_LINE).append(kind.infoLabel).append('\n');
        if (before != null) text.append(BEFORE_LINE).append(before.infoLabel).append('\n');
        text.append(STATUS_LINE).append(notRead.isEmpty() ? FULL : PARTIAL).append('\n');
        switch (kind) {
            case DAILY -> text.append("Kept by age, as the daily backup retention setting says.\n");
            case BEFORE_CHANGES -> text.append("Kept by count, as the before-changes backup retention setting says.\n");
            case USER -> text.append("Kept until you delete it.\n");
            case FIRST_SETUP -> text.append("Kept forever: first-time startup's backup, taken before BindForge wrote anything.\n");
        }
        text.append('\n').append(BINDINGS).append(": ").append(from.bindingsFolder()).append('\n');
        List<BackupSources.Installation> installations = from.installations();
        for (int i = 0; i < installations.size(); i++) {
            BackupSources.Installation installation = installations.get(i);
            text.append(INSTALLATIONS).append(File.separatorChar).append(i + 1).append('-').append(safe(installation.storefront()))
                    .append(": ").append(installation.storefront()).append(", ").append(installation.root())
                    .append('\n');
        }
        if (!notRead.isEmpty()) {
            text.append('\n').append(NOT_READ_HEADING).append('\n');
            notRead.forEach(line -> text.append(NOT_READ_ITEM).append(line).append('\n'));
        }
        return text.toString();
    }

    private static String uniqueName(Path folder, String stamp) {
        String candidate = stamp + ZIP;
        for (int n = 2; Files.exists(folder.resolve(candidate)); n++) {
            candidate = stamp + "-" + n + ZIP;
        }
        return candidate;
    }

    // ---- reading

    /** The stamp in a backup's name, or {@code null} when the name is not one. */
    private static LocalDateTime stampOf(Path zip) {
        Matcher name = NAME.matcher(zip.getFileName().toString());
        if (!name.matches()) return null;
        try {
            return LocalDateTime.parse(name.group(1), STAMP);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** The backup in {@code zip}, of the kind its folder says, or {@code null} when it is not one. */
    static Backup read(Path zip, Kind kind) {
        LocalDateTime taken = stampOf(zip);
        if (taken == null || !Files.isRegularFile(zip)) return null;
        boolean full = true;
        Before before = null;
        List<String> notRead = new ArrayList<>();
        try (ZipFile file = new ZipFile(zip.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry entry = file.getEntry(INFO_FILE);
            if (entry == null) throw new IOException(INFO_FILE + " is missing");
            String text;
            try (InputStream in = file.getInputStream(entry)) {
                text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            boolean inNotRead = false;
            for (String line : text.split("\n")) {
                if (line.startsWith(STATUS_LINE)) full = FULL.equals(line.substring(STATUS_LINE.length()).trim());
                else if (line.startsWith(BEFORE_LINE)) before = beforeOf(line.substring(BEFORE_LINE.length()).trim());
                else if (line.equals(NOT_READ_HEADING)) inNotRead = true;
                else if (inNotRead && line.startsWith(NOT_READ_ITEM)) notRead.add(line.substring(NOT_READ_ITEM.length()));
            }
        } catch (IOException e) {
            // WHY: still listed - the zip is there, and only its summary could not be read.
            log.warn("Could not read {} in {}", INFO_FILE, zip, e);
            full = false;
            notRead.add(INFO_FILE + ": " + reason(e));
        }
        return new Backup(zip, taken, kind, kind == Kind.BEFORE_CHANGES ? before : null, full, notRead);
    }

    private static Before beforeOf(String label) {
        for (Before before : Before.values()) {
            if (before.infoLabel.equals(label)) return before;
        }
        return null;
    }

    // ---- helpers

    private static boolean deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
            return true;
        } catch (IOException e) {
            log.warn("Could not delete backup {}; tried again at the next start", file, e);
            return false;
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
