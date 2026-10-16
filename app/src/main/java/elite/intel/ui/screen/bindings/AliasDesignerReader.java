package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devicefiles.*;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devices.*;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.install.InstallationRegistry;
import elite.intel.db.dao.BindForgeDeviceDraftDao.DraftRow;
import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.managers.BindForgeActivationManager;
import elite.intel.db.managers.BindForgeDeviceDraftManager;
import elite.intel.db.managers.BindForgeDeviceMasterManager;
import elite.intel.db.managers.BindForgeIdentityAnswerManager;
import elite.intel.devices.DeviceService;
import elite.intel.devices.model.Device;
import elite.intel.devices.model.DeviceDescription;
import elite.intel.devices.model.DeviceIdentity;
import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.util.StringUtls.messageOf;

/**
 * Everything the Alias Designer screen reads: the refresh's whole picture, what onboarding asks, and what the Device
 * Editor opens with. Touches the disk and the database - never the EDT, and never Swing; {@link AliasDesignerController}
 * runs it on its own threads and hands the result to {@link AliasDesignerPanel}.
 */
final class AliasDesignerReader {

    private static final Logger log = LogManager.getLogger(AliasDesignerReader.class);

    private final InstallationRegistry registry;
    private final InstallationDeviceScanner deviceScanner = new InstallationDeviceScanner();

    AliasDesignerReader(InstallationRegistry registry) {
        this.registry = registry;
    }

    /**
     * One refresh's picture of the screen.
     *
     * @param labels    what each installation is called, keyed as findings key them
     * @param paths     each installation's folder, keyed the same - for the Device Editor's install strip
     * @param report    the check the findings came from, or {@code null} before activation, when there are no ways out
     * @param activated {@code null} when the refresh failed before reading it, so the screen keeps its last answer - an
     *                  activated user is not sent back to SET UP by one failed query (review, 2026-10-13)
     * @param running   whether the Device Service was running; {@code null} when the refresh failed before the
     *                  hardware, for the same reason
     * @param listed    whether the Device Service had finished its first look at the hardware
     * @param identity  the identity questions (A12) - none before activation or before the hardware is listed, when
     *                  every controller reads absent
     */
    record Snapshot(Map<String, String> labels, Map<String, String> paths, DeviceFilesCheck.Report report,
                    List<DeviceDivergence.Finding> findings, List<MyDevice> myDevices, List<BuiltInDevice> builtIn,
                    Markers markers, Boolean activated, List<String> changes, Boolean running, boolean listed,
                    boolean applyWork, List<Long> foundAgain, IdentityQuestions.Reading identity) {
    }

    /**
     * The Installations columns: one per installation, keyed as findings key them, and each device's markers in the
     * order My Devices lists the devices. Empty before first setup.
     */
    record Markers(List<String> installs, List<Map<String, InstallationMarkers.Marker>> perDevice) {
        static final Markers NONE = new Markers(List.of(), List.of());
    }

    /**
     * Reads the installations and their device files, the master, the draft and the hardware. A failure part-way is
     * logged and leaves what was not read empty or {@code null}, so the screen keeps showing what it can.
     *
     * @param judgeMissing whether this refresh is the one that judges the missing-controller warning
     */
    Snapshot read(boolean judgeMissing) {
        Map<String, String> labels = Map.of();
        Map<String, String> paths = Map.of();
        DeviceFilesCheck.Report report = null;
        List<DeviceDivergence.Finding> findings = List.of();
        List<MyDevice> myDevices = List.of();
        List<BuiltInDevice> builtIn = List.of();
        Markers markers = Markers.NONE;
        Boolean activated = null;
        List<String> changes = List.of();
        Boolean running = null;
        boolean listed = false;
        boolean applyWork = false;
        List<Long> foundAgain = List.of();
        IdentityQuestions.Reading identity = IdentityQuestions.Reading.NONE;
        try {
            activated = BindForgeActivationManager.getInstance().isActivated();
            changes = DraftChanges.stored();
            List<InstallationRow> rows = registry.currentWithStartupScan();
            labels = labelsFor(rows);
            paths = rows.stream().collect(Collectors.toMap(AliasDesignerReader::keyOf, InstallationRow::rootPath,
                    (first, second) -> first, LinkedHashMap::new));
            recordWhatEachInstallationHolds(rows);
            Path bindings = PlayerSession.getInstance().getBindingsDir();
            // WHY: decided by the check's own reading of the master, not a separate one, so the rows and the master
            // they are judged against are always the same master. Before activation the installations are compared
            // with each other - which is what shows the user why SET UP is worth pressing (Alan, 2026-10-08).
            DeviceFilesCheck.Report check = new DeviceFilesCheck().check();
            if (check.activated()) {
                report = check;
                findings = DeviceDivergenceScanner.scanAgainstMaster(check, bindings);
                // WHY: after the detection above, which marks a folder that came back as present again.
                foundAgain = MissedWrites.storedFoundAgain();
            } else {
                findings = DeviceDivergenceScanner.scan(controlSchemesByInstall(rows), bindings);
            }
            BindForgeDeviceMasterManager master = BindForgeDeviceMasterManager.getInstance();
            boolean renameWaiting = master.findAll().stream()
                    .anyMatch(row -> row.previousName() != null && !row.previousName().equals(row.deviceName()));
            applyWork = ApplyWork.pending(changes, master.pendingRemovals(), renameWaiting, findings, report);
            Map<String, List<DeviceEntry>> entries = entriesByInstall(rows);
            Attached attached = readAttached();
            running = attached.running();
            listed = attached.listed();
            myDevices = readMyDevices(attached.devices(), entries);
            builtIn = BuiltInDeviceList.build(FrontierStockDevices.getInstance(), attached.devices());
            // WHY: only against a master. Before first setup M, not added and the divergence colour mean
            // nothing, and the column is left out rather than filled with an invented state.
            if (report != null) markers = markersFor(rows, entries, myDevices, findings);
            if (report != null && listed) identity = readIdentity(myDevices, bindings);
            // WHY: last, so nothing it does can cost the screen what this refresh has already read.
            if (judgeMissing) {
                MissingControllerWarning.getInstance().judgeOnce(attached.listed(), myDevices, bindings);
            }
        } catch (RuntimeException e) {
            // WHY: broad on purpose. This runs on a thread boundary, and an exception escaping it would kill
            // the thread silently and leave the table showing whatever it showed before, with no clue why.
            log.warn("Could not refresh the divergence list", e);
        }
        return new Snapshot(labels, paths, report, findings, myDevices, builtIn, markers, activated, changes, running,
                listed, applyWork, foundAgain, identity);
    }

    // ------------------------------------------------------------------ the identity question

    /**
     * Which missing controllers to ask about, and which candidates onboarding leaves alone meanwhile (Alan, 2026-10-15,
     * A12). The user's controllers are read as the next Apply will leave them - the draft's when one is started - so a
     * retarget the user already answered is not asked again. Answers whose controller is attached again, or no longer
     * the user's, are forgotten here: that is the hardware changing.
     * <p>
     * Only once the hardware is listed - before then every controller reads absent, and the forgetting would be wrong
     * too. A {@code .binds} that cannot be read asks nothing: without the counts every missing controller would look
     * unbound.
     */
    private static IdentityQuestions.Reading readIdentity(List<MyDevice> devices, Path bindings) {
        Map<String, Integer> counts;
        try {
            counts = BindsDeviceReferences.referenceCounts(bindings);
        } catch (IOException e) {
            log.warn("Could not read which devices the bindings name, so no identity question is asked: {}",
                    e.getMessage());
            return IdentityQuestions.Reading.NONE;
        }
        List<DeviceRow> masterRows = BindForgeDeviceMasterManager.getInstance().findAll();
        BindForgeDeviceDraftManager draft = BindForgeDeviceDraftManager.getInstance();
        List<DraftRow> draftRows = draft.findAll();
        List<IdentityQuestions.Known> known = new ArrayList<>();
        if (draft.isStarted()) {
            Map<Long, DeviceRow> masterById = masterRows.stream()
                    .collect(Collectors.toMap(DeviceRow::id, row -> row, (first, second) -> first));
            for (DraftRow row : draftRows) {
                if (row.vid() == null || row.pid() == null) continue;
                DeviceRow from = row.masterId() == null ? null : masterById.get(row.masterId());
                known.add(new IdentityQuestions.Known(row.deviceName(), Arrays.asList(row.deviceName(),
                        row.previousName(), from == null ? null : from.deviceName(),
                        from == null ? null : from.previousName()), new HardwareId(row.vid(), row.pid())));
            }
        } else {
            for (DeviceRow row : masterRows) {
                if (row.vid() == null || row.pid() == null) continue;
                known.add(new IdentityQuestions.Known(row.deviceName(),
                        Arrays.asList(row.deviceName(), row.previousName()), new HardwareId(row.vid(), row.pid())));
            }
        }
        List<HardwareId> heldByDraft = draftRows.stream().map(row -> new HardwareId(row.vid(), row.pid())).toList();
        BindForgeIdentityAnswerManager answers = BindForgeIdentityAnswerManager.getInstance();
        FrontierStockDevices stock = FrontierStockDevices.getInstance();
        IdentityQuestions.Reading reading =
                IdentityQuestions.read(known, devices, heldByDraft, counts, stock::ships, answers.findAll());
        answers.forget(reading.forget());
        return reading;
    }

    // ------------------------------------------------------------------ onboarding

    /**
     * @param problem what stopped onboarding, already worded, or {@code null}
     */
    record OnboardingReading(List<Onboarding.Page> pages, List<String> taken, String problem) {
    }

    /**
     * Who onboarding asks, what each is offered, and every name already taken - the master's, the draft's, every
     * installation's entries and Frontier's. A {@code .binds} that cannot be read stops it: without it, a controller
     * the bindings name by hardware id would be named straight away, leaving those bindings on an id that may no
     * longer resolve.
     *
     * @param only     the controllers to ask about - one from the gear's NAME, those plugged in later (A7) - or
     *                 {@code null} for every controller that needs a name
     * @param heldBack controllers left out - candidates of an identity question still open, which naming would end
     *                 unasked (Alan, 2026-10-15, A12). Empty from NAME: the user chose to name that one
     */
    OnboardingReading readOnboarding(Set<HardwareId> only, Set<HardwareId> heldBack) {
        if (!BindForgeActivationManager.getInstance().isActivated()) {
            return new OnboardingReading(List.of(), List.of(), getText("bindings.aliasDesigner.onboarding.notActivated"));
        }
        Map<String, Integer> hardwareIds;
        try {
            hardwareIds = BindsDeviceReferences.hardwareIdCounts(PlayerSession.getInstance().getBindingsDir());
        } catch (IOException e) {
            log.warn("Onboarding did not open: the bindings could not be read", e);
            return new OnboardingReading(List.of(), List.of(),
                    getText("bindings.aliasDesigner.onboarding.bindsUnread", messageOf(e)));
        }
        List<InstallationRow> rows = registry.currentWithStartupScan();
        Map<String, List<DeviceEntry>> entries = entriesByInstall(rows);
        List<MyDevice> devices = readMyDevices(readAttached().devices(), entries);

        Set<String> taken = new LinkedHashSet<>(FrontierStockDevices.getInstance().names());
        // WHY: old names too - one still waiting for its rename to reach every installation is taken (A11).
        BindForgeDeviceMasterManager.getInstance().findAll().forEach(row -> {
            taken.add(row.deviceName());
            if (row.previousName() != null) taken.add(row.previousName());
        });
        List<DraftRow> draft = BindForgeDeviceDraftManager.getInstance().findAll();
        draft.forEach(row -> {
            taken.add(row.deviceName());
            if (row.previousName() != null) taken.add(row.previousName());
        });
        entries.values().forEach(list -> list.forEach(entry -> taken.add(entry.name())));
        List<HardwareId> heldByDraft = draft.stream().map(row -> new HardwareId(row.vid(), row.pid())).toList();

        List<MyDevice> asked = devices.stream()
                .filter(device -> only == null || only.contains(device.hardware()))
                .filter(device -> !heldBack.contains(device.hardware()))
                .toList();
        List<Onboarding.Page> pages = Onboarding.pages(asked, heldByDraft, taken, hardwareIds,
                AliasDesignerReader::generatedLabels);
        return new OnboardingReading(pages, List.copyOf(taken), null);
    }

    /**
     * The labels a controller gets when named - what startup generation would give it, read from it now.
     */
    private static Map<String, String> generatedLabels(MyDevice device) {
        Device live = attachedController(device.hardware());
        if (live == null) return Map.of();
        DeviceDescription description =
                DeviceService.getInstance().description(live.id()).orElse(DeviceDescription.axesUnknown(0));
        return ButtonMapLabels.generate(live.buttonCount(), live.axisCount(), description);
    }

    // ------------------------------------------------------------------ the Device Editor

    /**
     * What the Device Editor opens with. The name is the draft's for a master device it holds - renamed there, it
     * opens under the new name - else the master's, or Frontier's for a built-in the master does not hold; with none,
     * the device has no name yet. The labels are the draft's when it holds the device - SAVE writes there - and the
     * master's otherwise. The inputs are the controller's when it is attached, read by which axis they are, and the
     * ones its labels name when it is not.
     */
    DeviceEditorDialog.Opening openingFor(MyDevice device, List<DeviceEditorDialog.StripLine> strip) {
        FrontierStockDevices stock = FrontierStockDevices.getInstance();
        BindForgeDeviceMasterManager master = BindForgeDeviceMasterManager.getInstance();
        DeviceRow masterRow = device.alias() == null ? null : master.findByName(device.alias());
        DraftRow draftRow = draftRowFor(device, masterRow);
        String name = draftRow != null ? draftRow.deviceName()
                : device.alias() != null ? device.alias()
                : device.builtIn() ? stock.covering(device.vid(), device.pid()).map(DeviceEntry::name).orElse(null)
                : null;
        Map<String, String> saved = name == null ? Map.of() : SavedDeviceLabels.of(name);

        Device live = device.attached() ? attachedController(device.hardware()) : null;
        DeviceDescription description = live == null
                ? null
                : DeviceService.getInstance().description(live.id()).orElse(DeviceDescription.axesUnknown(0));
        DeviceEditorInputs inputs = live == null
                ? DeviceEditorInputs.ofSavedLabels(saved)
                : DeviceEditorInputs.ofController(live.buttonCount(), live.axisCount(), description);
        DeviceEditorMiddle middle = middleFor(device, name, masterRow, stock);
        DeviceRename.Allowed rename = DeviceRename.allowed(name, name != null && stock.ships(name), middle.undoing(),
                masterRow == null ? null : masterRow.deviceName(), masterRow == null ? null : masterRow.previousName());
        Set<String> taken = rename == DeviceRename.Allowed.YES ? takenNames(name, masterRow, draftRow, stock) : Set.of();
        return new DeviceEditorDialog.Opening(device, name, live == null ? null : live.id(), description, inputs,
                saved, name != null && stock.gameDrawsIt(name), strip, middle, rename, taken);
    }

    /**
     * The draft row for this device: the one copied from its master row, or - for a controller named in onboarding
     * and waiting for APPLY - the one with its hardware. {@code null} when there is no draft, or it holds none.
     */
    private static DraftRow draftRowFor(MyDevice device, DeviceRow masterRow) {
        BindForgeDeviceDraftManager draft = BindForgeDeviceDraftManager.getInstance();
        if (!draft.isStarted()) return null;
        List<DraftRow> rows = draft.findAll();
        if (masterRow != null) {
            return rows.stream().filter(row -> Objects.equals(row.masterId(), masterRow.id())).findFirst().orElse(null);
        }
        if (device.alias() != null) return null;
        return rows.stream()
                .filter(row -> new HardwareId(row.vid(), row.pid()).equals(device.hardware()))
                .findFirst().orElse(null);
    }

    /**
     * The names a rename must not clash with ({@link DeviceRename#taken}): Frontier's, every installation's entries,
     * and every name and old name in the master and the draft - less this device's own.
     */
    private Set<String> takenNames(String name, DeviceRow masterRow, DraftRow draftRow, FrontierStockDevices stock) {
        List<String> entries = new ArrayList<>();
        entriesByInstall(registry.currentWithStartupScan()).values()
                .forEach(list -> list.forEach(entry -> entries.add(entry.name())));
        List<DeviceRename.Named> master = BindForgeDeviceMasterManager.getInstance().findAll().stream()
                .map(row -> new DeviceRename.Named(row.deviceName(), row.previousName())).toList();
        List<DeviceRename.Named> draft = BindForgeDeviceDraftManager.getInstance().findAll().stream()
                .map(row -> new DeviceRename.Named(row.deviceName(), row.previousName())).toList();
        List<String> own = new ArrayList<>();
        own.add(name);
        if (masterRow != null) {
            own.add(masterRow.deviceName());
            own.add(masterRow.previousName());
        }
        if (draftRow != null) own.add(draftRow.previousName());
        return DeviceRename.taken(stock.names(), entries, master, draft, own);
    }

    /**
     * What the editor's middle button is: read from the master, the draft and {@code .binds} as it opens. A
     * {@code .binds} that cannot be read - a folder that is not there included - refuses CLEAR rather than offering it
     * on a device it might name, since the game would reject the whole preset. Labels can still be edited.
     * <p>
     * Every name the device has is counted - its draft name, its master name, and the old name of a rename still
     * under way - since {@code .binds} may name it by any of them until Apply moves the bindings. <strong>A rename
     * still under way refuses CLEAR</strong> (review, 2026-10-15, A11): clearing would drop the old name the next
     * Apply needs to finish it.
     */
    private static DeviceEditorMiddle middleFor(MyDevice device, String name, DeviceRow masterRow,
                                                FrontierStockDevices stock) {
        boolean builtIn = name == null ? device.builtIn() : stock.ships(name);
        if (name == null) return DeviceEditorMiddle.of(null, builtIn, false, false, false, 0);
        BindForgeDeviceDraftManager draft = BindForgeDeviceDraftManager.getInstance();
        boolean started = draft.isStarted();
        boolean inMaster = masterRow != null || BindForgeDeviceMasterManager.getInstance().findByName(name) != null;
        OptionalInt bindings;
        try {
            Map<String, Integer> counts =
                    BindsDeviceReferences.referenceCounts(PlayerSession.getInstance().getBindingsDir());
            List<String> names = masterRow == null
                    ? List.of(name)
                    : Arrays.asList(name, masterRow.deviceName(), masterRow.previousName());
            bindings = OptionalInt.of(BindsDeviceReferences.bindingsNaming(names, counts));
        } catch (IOException e) {
            log.warn("Could not read which devices the bindings name, so Clear is refused for {}: {}", name,
                    e.getMessage());
            bindings = OptionalInt.empty();
        }
        DeviceEditorMiddle middle = DeviceEditorMiddle.of(name, builtIn, inMaster, started,
                started && draft.findByName(name) != null, bindings);
        boolean renamePending = masterRow != null && masterRow.previousName() != null
                && !masterRow.previousName().equals(masterRow.deviceName());
        return renamePending ? middle.whileRenamePending() : middle;
    }

    /**
     * The attached controller with this VID/PID - the first, since two identical units are out of scope.
     */
    private static Device attachedController(HardwareId hardware) {
        for (Device device : DeviceService.getInstance().getConnectedDevices()) {
            Optional<DeviceIdentity> identity = DeviceIdentities.of(device);
            if (identity.isPresent() && identity.get().vid().equals(hardware.vid())
                    && identity.get().pid().equals(hardware.pid())) {
                return device;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ the parts of a refresh

    /**
     * Every installation gets a column, its folder there or not - one that is gone says <em>not found</em> rather
     * than vanishing, since the user still has it (Alan, 2026-10-08).
     */
    private static Markers markersFor(List<InstallationRow> rows, Map<String, List<DeviceEntry>> entries,
                                      List<MyDevice> devices, List<DeviceDivergence.Finding> findings) {
        List<String> installs = rows.stream().map(AliasDesignerReader::keyOf).toList();
        Set<String> notFound = rows.stream().filter(InstallationRow::missing)
                .map(AliasDesignerReader::keyOf)
                .collect(Collectors.toSet());
        List<Map<String, InstallationMarkers.Marker>> perDevice = devices.stream()
                .map(device -> InstallationMarkers.of(device, installs, notFound, entries, findings))
                .toList();
        return new Markers(installs, perDevice);
    }

    /**
     * Records what each installation's device files hold right now.
     * <p>
     * A write on a refresh, which is deliberate: these rows are BindForge's only record of what is actually
     * on disk, and they are worth nothing if they are older than the disk. The installation list is already
     * refreshed the same way, on the same trigger.
     * <p>
     * The Installations markers do not read these rows: they need labels and a severity. Their colour and the
     * differences come from the check against the master; which names an installation gives the hardware comes from
     * this refresh's own read of its file (Alan, 2026-10-08). The rows stay the stored record of reality, for
     * whatever must notice a change between refreshes.
     */
    private void recordWhatEachInstallationHolds(List<InstallationRow> rows) {
        for (InstallationRow row : rows) {
            // WHY: a missing installation is skipped rather than scanned and found empty. Its folder is not
            // there, so reading it would record no devices - which is indistinguishable from a game patch
            // having wiped the file, and would raise exactly the alarm these rows exist to raise.
            if (row.missing()) continue;
            deviceScanner.scan(row.id(), GameInstallation.controlSchemesUnder(Path.of(row.rootPath())));
        }
    }

    /**
     * The controllers attached right now, read once per refresh for both device views.
     * <p>
     * {@code DeviceService} runs as one of the application's services, so with those stopped it reports no
     * controllers at all. The lists still build - the file half needs no hardware - but My Devices is then only
     * half a list and no built-in can read attached, and both views say so on screen rather than leaving an
     * absence to be mistaken for "nothing is plugged in".
     */
    private static Attached readAttached() {
        DeviceService service = DeviceService.getInstance();
        // WHY: read before the list, so "listed" vouches for a list taken after the first enumeration finished.
        boolean listed = service.hasEnumerated();
        boolean running = service.isAvailable();
        if (!running) {
            log.info("Device service is not running, so no controllers can be read - the list will show only "
                    + "what the installations' files name");
        }
        return new Attached(running ? service.getConnectedDevices() : List.of(), running, running && listed);
    }

    /**
     * The controllers one refresh read, and what that read can vouch for.
     *
     * @param running whether the Device Service was running, which the device views show
     * @param listed  whether it had finished its first look at the hardware, so an absent controller is really absent
     *                - it is running a moment before that (review, 2026-10-11)
     */
    private record Attached(List<Device> devices, boolean running, boolean listed) {
    }

    /**
     * Gathers the four things My Devices is built from: the attached controllers, every installation's device
     * entries, the master's records, and Frontier's shipped list.
     */
    private static List<MyDevice> readMyDevices(List<Device> attached,
                                                Map<String, List<DeviceEntry>> entriesByInstall) {
        return MyDeviceList.build(
                attached,
                MyDeviceList.entriesAcross(entriesByInstall.values()),
                BindForgeDeviceMasterManager.getInstance().findAll(),
                FrontierStockDevices.getInstance());
    }

    /**
     * Each present installation's device entries.
     * <p>
     * An installation whose file cannot be read is left out rather than counted as holding nothing. The device
     * list is a union, so a missing contribution only makes it shorter - but silently treating an unreadable
     * file as an empty one would hide a real problem behind a shorter list.
     */
    private static Map<String, List<DeviceEntry>> entriesByInstall(List<InstallationRow> rows) {
        Map<String, List<DeviceEntry>> byInstall = new LinkedHashMap<>();
        controlSchemesByInstall(rows).forEach((install, controlSchemes) -> {
            try {
                byInstall.put(install,
                        DeviceMappingsParser.parseIfPresent(GameInstallation.deviceMappingsIn(controlSchemes)));
            } catch (IOException e) {
                log.warn("Could not read device entries for {}: {}", install, e.getMessage());
            }
        });
        return byInstall;
    }

    /**
     * Only installations whose folder is there: a missing one cannot be read, and reporting its entries as
     * absent would fill the list with red caused by an unplugged drive.
     * <p>
     * Keyed by row id. <strong>Not by storefront</strong>, which is a label rather than a key - a machine can
     * hold two Frontier copies, several hand-added installations, or three Steams on Linux, and keying by
     * storefront would drop all but the last of them from the comparison without saying so.
     */
    private static Map<String, Path> controlSchemesByInstall(List<InstallationRow> rows) {
        Map<String, Path> byInstall = new LinkedHashMap<>();
        for (InstallationRow row : rows) {
            if (row.missing()) continue;
            byInstall.put(keyOf(row), GameInstallation.controlSchemesUnder(Path.of(row.rootPath())));
        }
        return byInstall;
    }

    /**
     * The identity a finding carries. Unique by construction, and never shown to the user. The same key findings
     * against the master carry, so they find their names too.
     */
    static String keyOf(InstallationRow row) {
        return DeviceDivergence.installKey(row.id());
    }

    /**
     * What each installation is called on screen.
     * <p>
     * The storefront alone where it identifies one installation, and the storefront with its folder where it
     * does not - so two Frontier copies read as two different things rather than twice as "FRONTIER". Verbose
     * only when being brief would be ambiguous.
     */
    static Map<String, String> labelsFor(List<InstallationRow> rows) {
        Map<String, Long> countByStorefront = rows.stream()
                .collect(Collectors.groupingBy(InstallationRow::storefront, Collectors.counting()));

        Map<String, String> labels = new LinkedHashMap<>();
        for (InstallationRow row : rows) {
            boolean storefrontIsEnough = countByStorefront.getOrDefault(row.storefront(), 0L) == 1;
            labels.put(keyOf(row), storefrontIsEnough
                    ? row.storefront()
                    : row.storefront() + " (" + row.rootPath() + ")");
        }
        return labels;
    }
}
