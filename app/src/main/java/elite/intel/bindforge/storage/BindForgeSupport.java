package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.FileNames;
import elite.intel.bindforge.devicefiles.BindsDeviceReferences;
import elite.intel.bindforge.devicefiles.DeviceEntry;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.bindforge.devices.DeviceIdentities;
import elite.intel.bindforge.devices.MissingControllers;
import elite.intel.bindforge.devices.MyDevice;
import elite.intel.bindforge.devices.MyDeviceList;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.storage.BindingsComparison.Differences;
import elite.intel.bindforge.storage.BindingsComparison.FileDifference;
import elite.intel.bindforge.storage.BindingsComparison.LineItem;
import elite.intel.bindforge.storage.BindingsComparison.NewerVersion;
import elite.intel.db.dao.BindForgeIdentityAnswerDao.AnswerRow;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.managers.BindForgeActivationManager;
import elite.intel.db.managers.BindForgeIdentityAnswerManager;
import elite.intel.db.managers.BindForgeInstallationsManager;
import elite.intel.db.managers.BindForgeSettingsManager;
import elite.intel.devices.DeviceService;
import elite.intel.devices.model.Device;
import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * What BindForge adds to a support bundle: a report of how it stands, and the game input files a report needs read.
 * <p>
 * WHY: a binding report is usually about a state nothing in the logs states - which installations BindForge knows and
 * how each compares with the working copy, what the game changed behind its back, which binds file each of the preset
 * file's four lines loads, and whether a controller's VID/PID matched its entry, which fails with no error anywhere.
 * Every one of those took a round of questions the commander had no way to answer.
 * <p>
 * Worked out only when the commander saves a bundle; nothing is recorded or sent otherwise. <strong>One section that
 * fails costs that section</strong>: this runs when something is already wrong, so what stopped a section is written in
 * its place. Reads only, with one exception it inherits: reading the working copy finishes a write that stopped part
 * way, as every BindForge screen's read does. Touches the disk and the database - not for the EDT.
 */
public final class BindForgeSupport {

    private static final Logger log = LogManager.getLogger(BindForgeSupport.class);

    /**
     * The report's name in the bundle.
     */
    public static final String REPORT_ENTRY = "bindforge-report.txt";

    private static final String FILES_FOLDER = "bindforge/";
    private static final String LIVE_BINDINGS = FILES_FOLDER + "live/Bindings/";
    private static final String WORKING_COPY_DEVICES = FILES_FOLDER + "working-copy/";
    private static final String INSTALLATIONS = FILES_FOLDER + "installations/";

    /**
     * The raw files are in the bundle for the rest: these bound the report, not what can be diagnosed.
     */
    private static final int MAX_ROWS_PER_FILE = 40;
    private static final int MAX_BACKUPS_LISTED = 30;

    /**
     * What a bundle carries from BindForge.
     *
     * @param report the report's text
     * @param files  the raw files, by their full entry name in the bundle
     */
    public record Contents(String report, Map<String, byte[]> files) {
        public Contents {
            files = Map.copyOf(files);
        }
    }

    /**
     * The controllers as the Device Service has them.
     */
    record Attached(List<Device> devices, boolean running, boolean listed) {
        Attached {
            devices = List.copyOf(devices);
        }
    }

    /**
     * What the report reads, stored or a test's.
     */
    interface Readings {
        boolean firstSetupComplete();

        Edits.Snapshot set() throws IOException;

        Path bindingsFolder();

        List<InstallationRow> installations();

        List<InstallationRow> installationsIncludingRemoved();

        DeviceCheck.Found judge(ConfigurationFiles workingCopy) throws IOException;

        /**
         * An installation's device files, or empty when it cannot be reached.
         */
        Optional<DeviceFiles> deviceFiles(InstallationRow row) throws IOException;

        Attached attached();

        Path backupFolder() throws IOException;

        /**
         * The folder the commander chose for backups, or {@code null} for the default.
         */
        String chosenBackupFolder();

        int dailyBackupDays();

        int beforeChangesBackupsKept();

        List<AnswerRow> identityAnswers();
    }

    private final Readings readings;
    private final StringBuilder text = new StringBuilder("BindForge report\n");
    private final Map<String, byte[]> files = new TreeMap<>();

    private BindForgeSupport(Readings readings) {
        this.readings = readings;
    }

    /**
     * Collects from the stored settings, the game's folders and the Device Service.
     */
    public static Contents collect() {
        return collect(new StoredReadings());
    }

    static Contents collect(Readings readings) {
        return new BindForgeSupport(readings).run();
    }

    private Contents run() {
        section("State", this::writeState);
        section("Installations", this::writeInstallations);
        section("Bindings folder compared with the working copy", this::writeLiveComparison);
        section("Preset file", this::writePresetFile);
        section("Controllers", this::writeControllers);
        section("Bindings naming a controller", this::writeBindingCounts);
        section("Backups", this::writeBackups);
        section("Identity answers", this::writeIdentityAnswers);
        section("Files", this::collectBindingsFiles);
        section("Working copy's device files", this::collectWorkingCopyDevices);
        return new Contents(text.toString(), files);
    }

    @FunctionalInterface
    private interface Section {
        void write() throws IOException;
    }

    private void section(String title, Section section) {
        text.append('\n').append(title).append(":\n");
        try {
            section.write();
        } catch (IOException | RuntimeException | LinkageError e) {
            // WHY: broad on purpose. The Device Service can fail on its native library and the database on anything,
            // and neither may cost the rest of the report - or the bundle, whose logs are what would explain them.
            log.warn("BindForge report: could not write the section {}", title, e);
            line("could not be read: " + e);
        }
    }

    private void line(String content) {
        text.append("  ").append(content).append('\n');
    }

    /**
     * The edits and the working copy, or empty before first setup has made them.
     */
    private Optional<Edits.Snapshot> set() throws IOException {
        return readings.firstSetupComplete() ? Optional.of(readings.set()) : Optional.empty();
    }

    private Map<String, byte[]> live() throws IOException {
        Path folder = readings.bindingsFolder();
        if (folder == null) throw new NoSuchFileException("no bindings folder is configured");
        return BindingsCheck.readBindingsFolder(folder);
    }

    private void writeState() throws IOException {
        line("Bindings folder: " + readings.bindingsFolder());
        Optional<Edits.Snapshot> set = set();
        if (set.isEmpty()) {
            line("First-time startup: not complete, so no working copy is expected");
            return;
        }
        ConfigurationFiles applied = set.get().applied();
        ConfigurationFiles current = set.get().current();
        line("First-time startup: complete");
        line("Working copy: " + applied.bindings().size() + " binds and preset files, "
                + (applied.hasDeviceMappings() ? "DeviceMappings.xml" : "no DeviceMappings.xml") + ", "
                + applied.buttonMaps().size() + " button maps");
        if (current.sameAs(applied)) {
            line("Edits not applied: none");
            return;
        }
        List<LineItem> rows = BindingsComparison.edited(applied.bindings(), current.bindings());
        line("Edits not applied: " + rows.size() + " rows in the binds and preset files; device files "
                + (current.withBindings(applied.bindings()).sameAs(applied) ? "unchanged" : "changed"));
        rows.stream().limit(MAX_ROWS_PER_FILE).forEach(row -> line("  " + row.file() + ": " + row.key()));
        if (rows.size() > MAX_ROWS_PER_FILE) line("  ... and " + (rows.size() - MAX_ROWS_PER_FILE) + " more");
    }

    private void writeInstallations() throws IOException {
        Set<Long> listed = readings.installations().stream().map(InstallationRow::id).collect(Collectors.toSet());
        Optional<Edits.Snapshot> set = set();
        Map<Long, DeviceCheck.Judged> judged = new HashMap<>();
        if (set.isPresent()) {
            readings.judge(set.get().applied()).installations().forEach(one -> judged.put(one.row().id(), one));
        }
        List<InstallationRow> rows = readings.installationsIncludingRemoved();
        if (rows.isEmpty()) line("none known");
        for (InstallationRow row : rows) {
            String facts = row.storefront() + " " + row.rootPath()
                    + (row.addedByHand() ? ", added by hand" : "")
                    + (row.missing() ? ", marked missing" : "")
                    + (listed.contains(row.id()) ? "" : ", removed by the user");
            if (set.isEmpty()) {
                line(facts + ": not judged, no working copy yet");
                if (listed.contains(row.id())) collectInstallationBeforeSetup(row);
            } else if (judged.containsKey(row.id())) {
                DeviceCheck.Judged one = judged.get(row.id());
                line(facts + ": " + one.state() + (one.problem() == null ? "" : " (" + one.problem() + ")"));
                if (one.state() != DeviceCheck.State.MATCHES && one.files() != null) {
                    collectDevices(installationFolder(row), one.files().deviceMappings(), one.files().buttonMaps());
                }
            } else {
                line(facts + ": not judged, not reachable");
            }
        }
    }

    /**
     * WHY: before first setup there is no working copy, and an installation's files are what first setup will read.
     */
    private void collectInstallationBeforeSetup(InstallationRow row) {
        try {
            readings.deviceFiles(row).ifPresent(found ->
                    collectDevices(installationFolder(row), found.deviceMappings(), found.buttonMaps()));
        } catch (IOException e) {
            line("  its device files could not be read: " + e.getMessage());
        }
    }

    private static String installationFolder(InstallationRow row) {
        return INSTALLATIONS + row.storefront() + "-" + row.id() + "/";
    }

    private void collectDevices(String folder, byte[] deviceMappings, Map<String, byte[]> buttonMaps) {
        if (deviceMappings != null) files.put(folder + GameInstallation.DEVICE_MAPPINGS, deviceMappings);
        buttonMaps.forEach((name, content) ->
                files.put(folder + GameInstallation.DEVICE_BUTTON_MAPS + "/" + name, content));
    }

    private void writeLiveComparison() throws IOException {
        Optional<Differences> differences = differences();
        if (differences.isEmpty()) {
            line("not compared, no working copy yet");
            return;
        }
        if (differences.get().none()) {
            line("no differences: the game's files are as BindForge last wrote them");
            return;
        }
        for (FileDifference file : differences.get().files()) {
            line(file.file() + ": " + file.presence() + (file.reset() ? ", looks reset by a game update" : "")
                    + ", " + file.items().size() + " rows");
            writeRows(file.items());
        }
        for (NewerVersion newer : differences.get().newer()) {
            line("newer version " + newer.newer() + " of " + newer.older()
                    + (newer.reset() ? ", looks reset by a game update" : "")
                    + ", " + newer.newSlots().size() + " new slots, " + newer.items().size() + " rows");
            writeRows(newer.items());
        }
        differences.get().leftOut().forEach(name -> line("left out, cannot be read line by line: " + name));
    }

    private Optional<Differences> differences() throws IOException {
        Optional<Edits.Snapshot> set = set();
        if (set.isEmpty()) return Optional.empty();
        // WHY: compared as the check when Elite-Intel starts compares, without Frontier's factory files. This answers
        // what changed, and telling a reset from a change is what reading the raw files is for.
        return Optional.of(BindingsComparison.compare(set.get().applied().bindings(), live(),
                BindingsComparison.Factory.NONE));
    }

    private void writeRows(List<LineItem> rows) {
        rows.stream().limit(MAX_ROWS_PER_FILE).forEach(row ->
                line("  " + row.key() + ": working copy " + row.workingCopy() + " | game " + row.live()));
        if (rows.size() > MAX_ROWS_PER_FILE) line("  ... and " + (rows.size() - MAX_ROWS_PER_FILE) + " more");
    }

    private void writePresetFile() throws IOException {
        Map<String, byte[]> live = live();
        ConfigurationFiles set = new ConfigurationFiles(FileNames.oneSpelling(live, List.of()).files(), null, Map.of());
        Optional<String> presetFile = PresetEdits.presetFile(set);
        if (presetFile.isEmpty()) {
            line("none in the bindings folder, so the game is on a factory preset");
            return;
        }
        StartPresetSections sections = PresetEdits.sections(set).orElseThrow();
        line(presetFile.get() + (sections.singleLine() ? " (the single-line form)" : ""));
        for (BindingSection section : PresetEdits.SECTIONS) {
            String bindName = sections.forSection(section);
            List<String> named = filesNaming(bindName, live.keySet());
            line(section + ": " + (bindName.isEmpty() ? "(empty)" : bindName) + " -> "
                    + (named.isEmpty() ? "no binds file by this name here (a factory preset, or missing)"
                    : String.join(", ", named)));
        }
    }

    private static List<String> filesNaming(String bindName, Set<String> fileNames) {
        return fileNames.stream()
                .filter(name -> FileNames.bindName(name).filter(bindName::equalsIgnoreCase).isPresent())
                .sorted().toList();
    }

    private void writeControllers() throws IOException {
        Attached attached = readings.attached();
        line("Device Service: " + (attached.running() ? "running" : "not running")
                + (attached.listed() ? ", controllers listed" : ", controllers not listed yet"));
        line("Attached (" + attached.devices().size() + "):");
        for (Device device : attached.devices()) {
            String hardware = DeviceIdentities.of(device).map(id -> id.vid() + ":" + id.pid())
                    .orElse("VID/PID could not be read");
            line("  " + device.name() + " [" + hardware + "] GUID " + device.guid() + ", " + device.axisCount()
                    + " axes, " + device.buttonCount() + " buttons" + (device.gamepad() ? ", gamepad" : "")
                    + ", path " + device.usbPath());
        }
        FrontierStockDevices stock = FrontierStockDevices.getInstance();
        Optional<Edits.Snapshot> set = set();
        List<DeviceEntry> edited = set.isPresent() ? DeviceEdits.elements(set.get().current()) : List.of();
        List<DeviceEntry> applied = set.isPresent() ? DeviceEdits.elements(set.get().applied()) : List.of();
        List<MyDevice> rows = MyDeviceList.build(attached.devices(), edited, applied, stock);
        line("My Devices (" + rows.size() + "):");
        for (MyDevice row : rows) {
            line("  " + row.label() + " [" + row.vid() + ":" + row.pid() + "] "
                    + (row.attached() ? "attached" : "not attached")
                    + ", working copy names it " + row.entryName()
                    + ", edits name it " + row.alias()
                    + (row.builtIn() ? ", built-in" : ""));
        }
        if (!attached.listed()) {
            line("Missing: not judged, the controllers are not listed yet");
            return;
        }
        List<MissingControllers.Missing> missing = MissingControllers.of(rows,
                BindsDeviceReferences.countsIn(live()).names(), stock.names());
        line("Missing, with bindings naming them: " + (missing.isEmpty() ? "none" : missing.stream()
                .map(controller -> controller.name() + " (" + controller.bindings() + ")")
                .collect(Collectors.joining(", "))));
    }

    private void writeBindingCounts() throws IOException {
        BindsDeviceReferences.Counts counts = BindsDeviceReferences.countsIn(live());
        if (counts.names().isEmpty() && counts.hardwareIds().isEmpty()) {
            line("none: nothing in the bindings folder names a controller");
            return;
        }
        Optional<Edits.Snapshot> set = set();
        Set<String> entries = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (set.isPresent()) {
            DeviceEdits.elements(set.get().applied()).forEach(entry -> entries.add(entry.name()));
        }
        FrontierStockDevices stock = FrontierStockDevices.getInstance();
        new TreeMap<>(counts.names()).forEach((name, count) -> line(name + ": " + count
                // WHY: the game rejects a whole preset over one name it cannot resolve (measured 2026-09-22).
                + (set.isPresent() && !entries.contains(name) && !stock.ships(name)
                ? " *** no entry for it in the working copy's DeviceMappings.xml or Frontier's list ***" : "")));
        new TreeMap<>(counts.hardwareIds()).forEach((id, count) -> line(id + " (a hardware id): " + count));
    }

    private void writeBackups() throws IOException {
        String chosen = readings.chosenBackupFolder();
        Path folder = readings.backupFolder();
        line("Folder: " + folder + (chosen == null || chosen.isBlank() ? " (the default)"
                : folder.equals(Path.of(chosen)) ? " (chosen)" : " (*** the chosen " + chosen + " is not usable ***)"));
        line("Kept: daily for " + readings.dailyBackupDays() + " days, the newest "
                + readings.beforeChangesBackupsKept() + " before-changes");
        List<Backup> backups = Backups.list(folder);
        Map<Backups.Kind, Long> kinds = new EnumMap<>(Backups.Kind.class);
        backups.forEach(backup -> kinds.merge(backup.kind(), 1L, Long::sum));
        line("Held: " + (kinds.isEmpty() ? "none" : kinds.toString()));
        backups.stream().limit(MAX_BACKUPS_LISTED).forEach(this::writeBackup);
        if (backups.size() > MAX_BACKUPS_LISTED) line("... and " + (backups.size() - MAX_BACKUPS_LISTED) + " older");
    }

    private void writeBackup(Backup backup) {
        line(Backups.TAKEN_FORMAT.format(backup.taken()) + " " + backup.kind()
                + (backup.before() == null ? "" : " before " + backup.before())
                + (backup.full() ? ", full" : ", PARTIAL"));
        backup.notRead().forEach(reason -> line("  not read: " + reason));
    }

    private void writeIdentityAnswers() {
        List<AnswerRow> answers = readings.identityAnswers();
        if (answers.isEmpty()) line("none");
        answers.forEach(answer -> line(answer.deviceName() + " (" + answer.candidates() + "): " + answer.answer()));
    }

    /**
     * The preset files, the binds files their lines name and the ones that differ from the working copy.
     * <p>
     * WHY not only the file the bundle already carries at its root: that is the preset file's first non-empty line,
     * and a split preset loads Ship, SRV and On Foot from others. WHY not the whole folder: the ones nothing names are
     * only counted against, and the report has their counts.
     */
    private void collectBindingsFiles() throws IOException {
        Map<String, byte[]> live = live();
        Set<String> taken = new TreeSet<>();
        live.keySet().stream().filter(FileNames::isPresetFile).forEach(taken::add);
        PresetEdits.sections(new ConfigurationFiles(FileNames.oneSpelling(live, List.of()).files(), null, Map.of())).ifPresent(sections -> {
            for (BindingSection section : PresetEdits.SECTIONS) {
                taken.addAll(filesNaming(sections.forSection(section), live.keySet()));
            }
        });
        Optional<Differences> differences = differences();
        if (differences.isPresent()) {
            differences.get().files().forEach(file -> taken.add(file.file()));
            differences.get().newer().forEach(newer -> taken.add(newer.newer()));
        }
        List<String> carried = new ArrayList<>();
        for (String name : taken) {
            byte[] content = live.get(name);
            if (content == null) continue;
            files.put(LIVE_BINDINGS + name, content);
            carried.add(name);
        }
        line("From the bindings folder: " + (carried.isEmpty() ? "none" : String.join(", ", carried)));
        line("Everything in it: " + String.join(", ", new TreeSet<>(live.keySet())));
    }

    private void collectWorkingCopyDevices() throws IOException {
        Optional<Edits.Snapshot> set = set();
        if (set.isEmpty()) {
            line("none, no working copy yet");
            return;
        }
        ConfigurationFiles applied = set.get().applied();
        collectDevices(WORKING_COPY_DEVICES, applied.hasDeviceMappings() ? applied.deviceMappings() : null,
                applied.buttonMaps());
        line((applied.hasDeviceMappings() ? "" : "no ") + GameInstallation.DEVICE_MAPPINGS + ", "
                + applied.buttonMaps().size() + " button maps");
    }

    private static final class StoredReadings implements Readings {

        @Override
        public boolean firstSetupComplete() {
            return BindForgeActivationManager.getInstance().isFirstTimeStartupComplete();
        }

        @Override
        public Edits.Snapshot set() throws IOException {
            return Edits.stored().snapshot();
        }

        @Override
        public Path bindingsFolder() {
            return PlayerSession.getInstance().getBindingsDir();
        }

        @Override
        public List<InstallationRow> installations() {
            return BindForgeInstallationsManager.getInstance().findAll();
        }

        @Override
        public List<InstallationRow> installationsIncludingRemoved() {
            return BindForgeInstallationsManager.getInstance().findAllIncludingRemoved();
        }

        @Override
        public DeviceCheck.Found judge(ConfigurationFiles workingCopy) throws IOException {
            return DeviceCheck.stored().find(workingCopy);
        }

        @Override
        public Optional<DeviceFiles> deviceFiles(InstallationRow row) throws IOException {
            return BackupSources.reachable(row) ? Optional.of(DeviceFiles.read(Path.of(row.rootPath())))
                    : Optional.empty();
        }

        @Override
        public Attached attached() {
            DeviceService service = DeviceService.getInstance();
            // WHY: read before the list, so "listed" vouches for a list taken after the first enumeration finished.
            boolean listed = service.hasEnumerated();
            boolean running = service.isAvailable();
            return new Attached(running ? service.getConnectedDevices() : List.of(), running, running && listed);
        }

        @Override
        public Path backupFolder() throws IOException {
            return BackupDestination.stored().resolve();
        }

        @Override
        public String chosenBackupFolder() {
            return BindForgeSettingsManager.getInstance().getBackupDestination();
        }

        @Override
        public int dailyBackupDays() {
            return BindForgeSettingsManager.getInstance().getDailyBackupRetentionDays();
        }

        @Override
        public int beforeChangesBackupsKept() {
            return BindForgeSettingsManager.getInstance().getBeforeChangesBackupsKept();
        }

        @Override
        public List<AnswerRow> identityAnswers() {
            return BindForgeIdentityAnswerManager.getInstance().findAll();
        }
    }
}
