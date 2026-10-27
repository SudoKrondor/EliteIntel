package elite.intel.bindforge.storage;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.bindforge.devicefiles.FrontierStockButtonMaps;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.install.GameInstallationProvider;
import elite.intel.bindforge.install.InstallationRegistry;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.managers.BindForgeActivationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * First-time startup (storage.md rules 1-4): on a click on the BindForge tab while the "first-time startup complete"
 * flag is not set, the user confirms the installations, reconciles their device files if they differ, and the working
 * copy is created - backed up first, with the first setup backup kept forever - and written to every installation.
 * <p>
 * <strong>Nothing is written until {@link #write}.</strong> Reading can be repeated and abandoned; the next click on
 * the tab offers it again (Alan, 2026-10-25).
 * <p>
 * <strong>A new player with no bindings folder yet is not taken through it</strong> (Alan, 2026-10-25): the game makes
 * the folder only after the training, so nothing of theirs exists yet - {@link #read} refuses with
 * {@link NoBindingsYetException}, and the flag stays unset. Nor is first-time startup run when every installation is
 * unreachable ({@link NoInstallationReachableException}). Touches the disk and the database - not for the EDT.
 */
public final class FirstTimeStartup {

    private static final Logger log = LogManager.getLogger(FirstTimeStartup.class);

    /** The stored flag - {@link BindForgeActivationManager}. */
    interface Flag {
        boolean complete();

        void markComplete();

        void clear();
    }

    /** Thrown by {@link #read} when the bindings folder is not there: a new player, before the training. */
    public static final class NoBindingsYetException extends IOException {
        /** The words to say - the setup check's (storage.md rule 12). */
        public static final String ADVICE_KEY = Backups.NothingToBackUpException.ADVICE_KEY;

        NoBindingsYetException(Path folder) {
            super("the bindings folder is not there yet: " + folder);
        }
    }

    /**
     * Thrown by {@link #read} when Elite-Intel knows installations but none of their folders is there - a drive
     * unplugged. First-time startup does not run on none of them (Alan, 2026-10-25); the flag stays unset. With no
     * installation known at all it runs, with the binds and preset files alone.
     */
    public static final class NoInstallationReachableException extends IOException {
        private final transient List<InstallationRow> unreachable;

        NoInstallationReachableException(List<InstallationRow> unreachable) {
            super("no installation could be reached: " + unreachable.stream()
                    .map(row -> row.storefront() + " (" + row.rootPath() + ")").toList());
            this.unreachable = List.copyOf(unreachable);
        }

        /** The installations that could not be reached, for the user to plug in or Remove. */
        public List<InstallationRow> unreachable() {
            return unreachable;
        }
    }

    /**
     * Writes the set to the places given - {@link WritePath#write(ConfigurationFiles, BackupSources,
     * WritePath.BackUpFirst)}; RESTORE uses it too.
     */
    @FunctionalInterface
    interface Writer {
        WriteReport write(ConfigurationFiles files, BackupSources places, WritePath.BackUpFirst backUp)
                throws IOException;
    }

    private final Flag flag;
    private final Supplier<Path> bindingsFolder;
    private final Supplier<List<InstallationRow>> installations;
    private final Supplier<List<GameInstallation>> detection;
    private final Writer writer;
    private final WritePath.BackUpFirst firstSetupBackup;
    private final Consumer<Backup> announce;

    /**
     * The places {@link #read()} read - the bindings folder, the installations it read, and the ones it could not, marked
     * missing - and the only ones {@link #write} backs up and writes. Set on one background thread, read on a later
     * one; the EDT hand-over between them orders the two.
     */
    private volatile BackupSources read;

    /** The stored flag, installations, bindings folder and write path. */
    public static FirstTimeStartup stored() {
        Supplier<Path> folder = () -> PlayerSession.getInstance().getBindingsDir();
        return new FirstTimeStartup(storedFlag(), folder,
                () -> new InstallationRegistry(GameInstallationProvider.forThisPlatform(folder.get()))
                        .currentWithStartupScan(),
                () -> GameInstallationProvider.forThisPlatform(folder.get()).findInstallations(),
                storedWriter(),
                sources -> Backups.stored().backUpEverything(Backups.Kind.FIRST_SETUP, sources),
                FirstTimeStartup::announce);
    }

    /**
     * The write path, run as a write of the shared {@link Edits} - RESTORE uses it too ({@link Restore}). While it runs
     * the APPLY / DISCARD bar is dimmed and no APPLY can start: one started meanwhile would write the edits it read
     * before this write, over the set this write leaves (review, 2026-10-27).
     *
     * @throws Edits.WriteRunningException if an APPLY or another write is already running - nothing is written
     */
    static Writer storedWriter() {
        return (files, places, backUp) -> Edits.stored()
                .whileWriting(() -> WritePath.stored().write(files, places, backUp));
    }

    /** The stored flag - RESTORE sets it too ({@link Restore}). */
    static Flag storedFlag() {
        BindForgeActivationManager manager = BindForgeActivationManager.getInstance();
        return new Flag() {
            @Override
            public boolean complete() {
                return manager.isFirstTimeStartupComplete();
            }

            @Override
            public void markComplete() {
                manager.markFirstTimeStartupComplete();
            }

            @Override
            public void clear() {
                manager.clearFirstTimeStartup();
            }
        };
    }

    /**
     * @param detection what installation detection finds now - used only for each installation's own bindings folder
     *                  on Linux, never to add an installation to the write
     */
    FirstTimeStartup(Flag flag, Supplier<Path> bindingsFolder, Supplier<List<InstallationRow>> installations,
                     Supplier<List<GameInstallation>> detection, Writer writer, WritePath.BackUpFirst firstSetupBackup,
                     Consumer<Backup> announce) {
        this.flag = flag;
        this.bindingsFolder = bindingsFolder;
        this.installations = installations;
        this.detection = detection;
        this.writer = writer;
        this.firstSetupBackup = firstSetupBackup;
        this.announce = announce;
    }

    /** Whether first-time startup is still to run - the flag is not set. */
    public boolean needed() {
        return !flag.complete();
    }

    /** The installations the list shows: every one the stored list holds, removed ones left out. */
    public List<InstallationRow> installations() {
        return installations.get();
    }

    /**
     * Reads the bindings folder and every installation in the list whose folder is there. Writes nothing.
     *
     * @throws NoBindingsYetException            if the bindings folder is not there
     * @throws NoInstallationReachableException if installations are known and none of their folders is there
     * @throws IOException            if a file that is there cannot be read - the message names the installation and
     *                                the file. First setup does not run on a partial picture
     */
    public FirstSetupReading read() throws IOException {
        Path folder = bindingsFolder.get();
        if (!Files.isDirectory(folder)) throw new NoBindingsYetException(folder);
        Map<String, byte[]> bindings = readBindings(folder);

        List<InstallationFiles> reachable = new ArrayList<>();
        List<InstallationRow> reachableRows = new ArrayList<>();
        List<InstallationRow> unreachable = new ArrayList<>();
        for (InstallationRow row : installations.get()) {
            if (!BackupSources.reachable(row)) {
                unreachable.add(row);
                continue;
            }
            try {
                reachable.add(InstallationFiles.read(row));
                reachableRows.add(row);
            } catch (IOException e) {
                throw new IOException(row.storefront() + " (" + row.rootPath() + "): " + e.getMessage(), e);
            }
        }
        // WHY: installations are known but none can be read - a drive unplugged. Going on would create a working copy
        // with no device files, which the next write would put over theirs when they come back (review, 2026-10-25).
        if (reachable.isEmpty() && !unreachable.isEmpty()) throw new NoInstallationReachableException(unreachable);
        FirstSetupReading reading = FirstSetupReading.of(folder, bindings, reachable, unreachable,
                FrontierStockDevices.getInstance(), FrontierStockButtonMaps.getInstance());
        // WHY: the write covers these and no other. Found again at write time, an installation plugged in while the
        // dialogs were open would get the working copy over files nobody reconciled (storage.md rule 1; Krondor's
        // review, 2026-10-27).
        read = BackupSources.of(folder, reachableRows, unreachable, detection.get());
        return reading;
    }

    /**
     * Creates the working copy: the first setup backup, kept forever, then the working copy, then every live copy -
     * the one write path (storage.md, <em>How the files are written</em>) - and marks first-time startup complete. Told
     * once, spoken and logged, that the backup was taken (alias-designer.md, <em>Activation</em>).
     * <p>
     * <strong>Writes only the places {@link #read} read</strong>: an installation that was unreachable then, or that
     * turned up since, is not written (storage.md rule 1).
     * <p>
     * The flag is set once the working copy is saved, whatever the live copies did: first setup is then over, and an
     * installation that missed the write is reported, for the check (storage.md rule 8). <strong>A flag that cannot be
     * set is reported, not thrown</strong> (Krondor's review, 2026-10-27): the files are written by then, so the result
     * says so, with {@link Written#setUpAskedAgain()} - as RESTORE does.
     *
     * @throws IllegalStateException       if {@link #read()} has not read the files first
     * @throws IOException                 if the backup cannot be taken - {@link Backups.NothingToBackUpException} when
     *                                     there is nothing at all - or the working copy cannot be saved. Nothing was
     *                                     written, and the flag stays unset
     * @throws Edits.WriteRunningException if an APPLY or another write is running - nothing was written
     */
    public Written write(ConfigurationFiles files) throws IOException {
        BackupSources places = read;
        if (places == null) throw new IllegalStateException("first-time startup writes only what it has read");
        AtomicReference<Backup> taken = new AtomicReference<>();
        WriteReport report = writer.write(files, places, sources -> {
            Backup backup = firstSetupBackup.take(sources);
            taken.set(backup);
            return backup;
        });
        boolean askedAgain = !markComplete(flag);
        announce.accept(taken.get());
        log.info("First-time startup {}: the working copy holds {} binds and preset files, {}, and {} button maps; "
                        + "installations updated {} of {}", askedAgain ? "written, not marked complete" : "complete",
                files.bindings().size(), files.hasDeviceMappings() ? "a DeviceMappings.xml" : "no DeviceMappings.xml",
                files.buttonMaps().size(), report.installationsUpdated(), report.installations().size());
        return new Written(report, taken.get(), askedAgain);
    }

    /**
     * Marks first-time startup complete if it is not - after first-time startup's write and RESTORE's, which both leave
     * the working copy and the live copies matching.
     *
     * @return whether it is complete afterwards. When it is not, the files are written all the same and the user is
     * told the next click on the BindForge tab offers first-time startup again - which then runs over files BindForge
     * already wrote, with a backup first: told, not hidden (reviews, 2026-10-25 and 2026-10-27)
     */
    static boolean markComplete(Flag flag) {
        try {
            if (!flag.complete()) flag.markComplete();
            return true;
        } catch (RuntimeException e) {
            log.error("The files are written, but first-time startup could not be marked complete", e);
            return false;
        }
    }

    /**
     * Run first-time startup again (file-manager.md, <em>Game Install Locations</em>): clears the flag and nothing
     * else - no backup is deleted, no game file is touched, and the working copy stays until first-time startup
     * replaces it.
     */
    public void runAgain() {
        flag.clear();
        log.info("First-time startup will run again at the next click on the BindForge tab");
    }

    private static Map<String, byte[]> readBindings(Path folder) throws IOException {
        List<Path> found;
        try (Stream<Path> files = Files.list(folder)) {
            found = files.filter(Files::isRegularFile).filter(Backups::isBindingsFile).sorted().toList();
        } catch (NoSuchFileException e) {
            throw new NoBindingsYetException(folder);
        }
        Map<String, byte[]> bindings = new TreeMap<>();
        for (Path file : found) {
            try {
                bindings.put(file.getFileName().toString(), Files.readAllBytes(file));
            } catch (IOException e) {
                throw new IOException(file.getFileName() + " could not be read: " + e.getMessage(), e);
            }
        }
        return bindings;
    }

    /** Told once, on the first setup backup being taken: one spoken line, and a log line naming where it is. */
    private static void announce(Backup backup) {
        GameEventBus.publish(new VocalisationRequestEvent(StringUtls.localizedSpeech("speech.bindforgeBackedUp"),
                MissionCriticalAnnouncementEvent.class, false));
        UiBus.publish(new AppLogEvent("BindForge backed up your input configuration files before changing anything: "
                + backup.zip()));
    }
}
