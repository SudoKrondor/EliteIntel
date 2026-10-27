package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devicefiles.*;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devices.*;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.storage.ConfigurationFiles;
import elite.intel.bindforge.storage.DeviceEdits;
import elite.intel.bindforge.storage.Edits;
import elite.intel.db.dao.BindForgeDeviceDraftDao.DraftRow;
import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import elite.intel.db.managers.BindForgeActivationManager;
import elite.intel.db.managers.BindForgeDeviceDraftManager;
import elite.intel.db.managers.BindForgeDeviceMasterManager;
import elite.intel.db.managers.BindForgeHardwareIdMoveManager;
import elite.intel.db.managers.BindForgeIdentityAnswerManager;
import elite.intel.db.managers.BindForgeInstallationsManager;
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
 * Everything the Alias Designer screen reads: the refresh's picture, what onboarding asks, and what the Device Editor
 * opens with. <strong>It reads the working copy with the edits on screen</strong> ({@link Edits}; Storage S4a) - never an
 * installation: the editors care what is being edited, and only File Manager cares where the files are (principle 12).
 * Touches the disk - never the EDT, and never Swing; {@link AliasDesignerController} runs it on its own threads and
 * hands the result to {@link AliasDesignerPanel}.
 */
final class AliasDesignerReader {

    private static final Logger log = LogManager.getLogger(AliasDesignerReader.class);

    /**
     * One refresh's picture of the screen.
     *
     * @param activated {@code null} when the refresh failed before reading it, so the screen keeps its last answer
     * @param running   whether the Device Service was running; {@code null} when the refresh failed before the
     *                  hardware, for the same reason
     * @param listed    whether the Device Service had finished its first look at the hardware
     * @param identity  the identity questions (A12) - none before first-time startup or before the hardware is listed,
     *                  when every controller reads absent
     */
    record Snapshot(List<MyDevice> myDevices, List<BuiltInDevice> builtIn, Boolean activated, Boolean running,
                    boolean listed, IdentityQuestions.Reading identity) {
    }

    /**
     * The working copy, and it with the edits on top - both empty before first-time startup, or when it cannot be read.
     */
    private record Sets(ConfigurationFiles edited, ConfigurationFiles applied) {
        static final Sets NONE = new Sets(null, null);

        List<DeviceEntry> editedElements() throws IOException {
            return edited == null ? List.of() : DeviceEdits.elements(edited);
        }

        List<DeviceEntry> appliedElements() throws IOException {
            return applied == null ? List.of() : DeviceEdits.elements(applied);
        }
    }

    /**
     * Reads the edits, the working copy and the hardware, and works out the default labels APPLY will write. A failure
     * part-way is logged and leaves what was not read empty or {@code null}, so the screen keeps showing what it can.
     *
     * @param judgeMissing whether this refresh is the one that judges the missing-controller warning
     */
    Snapshot read(boolean judgeMissing) {
        List<MyDevice> myDevices = List.of();
        List<BuiltInDevice> builtIn = List.of();
        Boolean activated = null;
        Boolean running = null;
        boolean listed = false;
        IdentityQuestions.Reading identity = IdentityQuestions.Reading.NONE;
        try {
            boolean complete = BindForgeActivationManager.getInstance().isFirstTimeStartupComplete();
            activated = complete;
            Sets sets = complete ? readSets() : Sets.NONE;
            Path bindings = PlayerSession.getInstance().getBindingsDir();
            Attached attached = readAttached();
            running = attached.running();
            listed = attached.listed();
            myDevices = MyDeviceList.build(attached.devices(), sets.editedElements(), sets.appliedElements(),
                    FrontierStockDevices.getInstance());
            builtIn = BuiltInDeviceList.build(FrontierStockDevices.getInstance(), attached.devices());
            if (sets.edited() != null) Edits.stored().defaultLabels(defaultLabels(sets.edited(), myDevices));
            if (complete && listed) identity = readIdentity(myDevices, bindings);
            // WHY: last, so nothing it does can cost the screen what this refresh has already read.
            if (judgeMissing) {
                MissingControllerWarning.getInstance().judgeOnce(attached.listed(), myDevices, bindings);
            }
        } catch (IOException | RuntimeException e) {
            // WHY: broad on purpose. This runs on a thread boundary, and an exception escaping it would kill
            // the thread silently and leave the table showing whatever it showed before, with no clue why.
            log.warn("Could not refresh the Alias Designer", e);
        }
        return new Snapshot(myDevices, builtIn, activated, running, listed, identity);
    }

    /** The edits and the working copy, or none when the working copy cannot be read - the tab click offers RESTORE. */
    private static Sets readSets() {
        try {
            Edits edits = Edits.stored();
            return new Sets(edits.current(), edits.applied());
        } catch (IOException e) {
            log.warn("Could not read the working copy for the Alias Designer: {}", e.getMessage());
            return Sets.NONE;
        }
    }

    /**
     * Default labels - {@code Button 1}, {@code X Axis}, {@code Hat 1 Up} - for every connected controller with a name
     * and no {@code .buttonMap} (alias-designer.md, <em>Default labels</em>; Alan, 2026-10-24). Written at the next
     * APPLY, never lighting the bar. Skipped for a device the game draws itself, where a {@code Button 3} would replace
     * an icon. Only the axes the controller has, four labels per hat.
     */
    private static Map<String, byte[]> defaultLabels(ConfigurationFiles edited, List<MyDevice> devices) {
        FrontierStockDevices stock = FrontierStockDevices.getInstance();
        Map<String, byte[]> maps = new TreeMap<>();
        for (MyDevice device : devices) {
            if (!device.attached()) continue;
            String name = device.alias() != null ? device.alias()
                    : device.builtIn() ? stock.covering(device.vid(), device.pid()).map(DeviceEntry::name).orElse(null)
                    : null;
            if (name == null || stock.gameDrawsIt(name) || DeviceEdits.buttonMapFile(edited, name).isPresent()) {
                continue;
            }
            Map<String, String> labels = generatedLabels(device);
            if (labels.isEmpty()) continue;
            try {
                maps.put(name + GameInstallation.BUTTON_MAP_SUFFIX, ButtonMapWriter.write(labels));
            } catch (IOException e) {
                log.warn("Could not make default labels for {}: {}", name, e.getMessage());
            }
        }
        return maps;
    }

    // ------------------------------------------------------------------ the identity question

    /**
     * Which missing controllers to ask about, and which candidates onboarding leaves alone meanwhile (Alan, 2026-10-15,
     * A12). <em>Still read from the master and the draft until Storage S4b moves it onto the edits</em> - between the
     * two slices a named controller's retarget does not reach the game (Alan, 2026-10-26).
     * <p>
     * Only once the hardware is listed - before then every controller reads absent, and the forgetting would be wrong
     * too. A {@code .binds} that cannot be read asks nothing: without the counts every missing controller would look
     * unbound.
     */
    private static IdentityQuestions.Reading readIdentity(List<MyDevice> devices, Path bindings) {
        BindsDeviceReferences.Counts counts;
        try {
            counts = BindsDeviceReferences.counts(bindings);
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
        BindForgeHardwareIdMoveManager moves = BindForgeHardwareIdMoveManager.getInstance();
        FrontierStockDevices stock = FrontierStockDevices.getInstance();
        List<HardwareId> heldByMaster = masterRows.stream().map(row -> new HardwareId(row.vid(), row.pid())).toList();
        List<String> namesInUse = new ArrayList<>();
        masterRows.forEach(row -> namesInUse.addAll(Arrays.asList(row.deviceName(), row.previousName())));
        draftRows.forEach(row -> namesInUse.addAll(Arrays.asList(row.deviceName(), row.previousName())));
        // WHY: a device of the user's in the edits is not a missing id, whatever the master says (Storage S4a).
        devices.forEach(device -> namesInUse.add(device.alias()));
        IdentityQuestions.ById byId = new IdentityQuestions.ById(counts.hardwareIds(), heldByMaster,
                hardware -> stock.covering(hardware.vid(), hardware.pid()).isPresent(), moves.findAll(), namesInUse);
        IdentityQuestions.Reading reading = IdentityQuestions.read(known, devices, heldByDraft, counts.names(),
                stock::ships, answers.findAll(), byId);
        answers.forget(reading.forget());
        moves.forget(reading.forgetMoves());
        return reading;
    }

    // ------------------------------------------------------------------ onboarding

    /**
     * @param problem what stopped onboarding, already worded, or {@code null}
     */
    record OnboardingReading(List<Onboarding.Page> pages, List<String> taken, String problem) {
    }

    /**
     * Who onboarding asks, what each is offered, and every name already taken - the edits', the working copy's and
     * Frontier's. Binds files that cannot be read stop it: without them, a controller the bindings name by hardware id
     * would be named while its bindings stayed on the id.
     *
     * @param only      the controllers to ask about - one from the gear's NAME, those plugged in later (A7) - or
     *                  {@code null} for every controller that needs a name
     * @param heldBack  controllers left out - candidates of an identity question still open, which naming would end
     *                  unasked (Alan, 2026-10-15, A12). Empty from NAME: the user chose to name that one
     * @param replacing the old hardware id each controller is, by the controller's hardware - the identity question's
     *                  yes for a controller the bindings know only by its id (A17); its page promises the bindings on
     *                  the old id. Empty otherwise
     */
    OnboardingReading readOnboarding(Set<HardwareId> only, Set<HardwareId> heldBack,
                                     Map<HardwareId, HardwareId> replacing) {
        if (!BindForgeActivationManager.getInstance().isFirstTimeStartupComplete()) {
            return new OnboardingReading(List.of(), List.of(), getText("bindings.aliasDesigner.onboarding.notActivated"));
        }
        Sets sets;
        Map<String, Integer> hardwareIds;
        try {
            Edits edits = Edits.stored();
            sets = new Sets(edits.current(), edits.applied());
            hardwareIds = BindsDeviceReferences.countsIn(sets.edited().bindings()).hardwareIds();
        } catch (IOException e) {
            log.warn("Onboarding did not open: the bindings could not be read", e);
            return new OnboardingReading(List.of(), List.of(),
                    getText("bindings.aliasDesigner.onboarding.bindsUnread", messageOf(e)));
        }
        List<MyDevice> devices;
        Set<String> taken = new LinkedHashSet<>(FrontierStockDevices.getInstance().names());
        try {
            List<DeviceEntry> edited = sets.editedElements();
            List<DeviceEntry> applied = sets.appliedElements();
            devices = MyDeviceList.build(readAttached().devices(), edited, applied, FrontierStockDevices.getInstance());
            // WHY: the working copy's names too - one a device gave up in the edits is taken until APPLY (no chains).
            edited.forEach(entry -> taken.add(entry.name()));
            applied.forEach(entry -> taken.add(entry.name()));
        } catch (IOException e) {
            log.warn("Onboarding did not open: the working copy's device file could not be read", e);
            return new OnboardingReading(List.of(), List.of(),
                    getText("bindings.aliasDesigner.editor.loadFailed", messageOf(e)));
        }
        List<MyDevice> asked = devices.stream()
                .filter(device -> only == null || only.contains(device.hardware()))
                .filter(device -> !heldBack.contains(device.hardware()))
                .toList();
        List<Onboarding.Page> pages = Onboarding.pages(asked, taken, hardwareIds,
                AliasDesignerReader::generatedLabels, replacing);
        return new OnboardingReading(pages, List.copyOf(taken), null);
    }

    /**
     * The labels a controller gets when named, or as its defaults - worked out from it now.
     */
    private static Map<String, String> generatedLabels(MyDevice device) {
        Device live = attachedController(device.hardware());
        if (live == null) return Map.of();
        DeviceDescription description =
                DeviceService.getInstance().description(live.id()).orElse(DeviceDescription.axesUnknown(0));
        if (GamepadSupport.isGamepad(live)) {
            description = DeviceDescription.gamepad(live.axisCount(), description.hatCount());
        }
        return ButtonMapLabels.generate(live.buttonCount(), live.axisCount(), description);
    }

    // ------------------------------------------------------------------ the Device Editor

    /**
     * What the Device Editor opens with, read from the edits and the working copy.
     * <p>
     * The name is the device's in the edits; for a device cleared in the edits, the working copy's, so it opens with
     * its undo; for a built-in, Frontier's; with none, the device has no name yet. The labels are the edits' - or, for a
     * device cleared there, the working copy's, to show what the undo puts back. The inputs are the controller's when it
     * is attached, read by which axis they are, and the ones its labels name when it is not.
     *
     * @throws IOException if the working copy cannot be read
     */
    DeviceEditorDialog.Opening openingFor(MyDevice device) throws IOException {
        FrontierStockDevices stock = FrontierStockDevices.getInstance();
        FrontierStockButtonMaps shipped = FrontierStockButtonMaps.getInstance();
        Edits edits = Edits.stored();
        ConfigurationFiles edited = edits.current();
        ConfigurationFiles applied = edits.applied();
        DeviceEntry covering = stock.covering(device.vid(), device.pid()).orElse(null);
        String name = device.alias() != null ? device.alias()
                : device.entryName() != null ? device.entryName()
                : covering != null ? covering.name()
                : null;
        boolean frontiers = name == null ? device.builtIn() : stock.ships(name);

        Map<String, String> saved = name == null ? Map.of() : DeviceEdits.labels(edited, name);
        if (saved.isEmpty() && name != null) saved = DeviceEdits.labels(applied, name);

        Device live = device.attached() ? attachedController(device.hardware()) : null;
        boolean gamepad = live != null && GamepadSupport.isGamepad(live);
        DeviceDescription description = live == null
                ? null
                : DeviceService.getInstance().description(live.id()).orElse(DeviceDescription.axesUnknown(0));
        if (gamepad) {
            description = DeviceDescription.gamepad(live.axisCount(), description.hatCount());
        }
        DeviceEditorInputs inputs = live == null
                ? DeviceEditorInputs.ofSavedLabels(saved)
                : DeviceEditorInputs.ofController(live.buttonCount(), live.axisCount(), description);

        DeviceEditorMiddle middle = DeviceEditorMiddle.of(name, frontiers, holds(applied, name, frontiers, stock, shipped),
                holds(edited, name, frontiers, stock, shipped), bindingsNaming(edited, name));
        DeviceRename.Owner owner = frontiers ? DeviceRename.Owner.FRONTIER : DeviceRename.Owner.USER;
        DeviceRename.Presence presence = device.attached() ? DeviceRename.Presence.ATTACHED
                : DeviceRename.Presence.UNPLUGGED;
        DeviceRename.Allowed rename = DeviceRename.allowed(name, owner, presence, middle.undoing());
        Set<String> taken = Set.of();
        if (rename == DeviceRename.Allowed.YES) {
            List<String> inUse = new ArrayList<>();
            DeviceEdits.elements(edited).forEach(entry -> inUse.add(entry.name()));
            DeviceEdits.elements(applied).forEach(entry -> inUse.add(entry.name()));
            taken = DeviceRename.taken(stock.names(), inUse, Arrays.asList(name, device.entryName()));
        }
        DeviceEditorDialog.Covering frontier = covering == null ? null
                : new DeviceEditorDialog.Covering(covering.name(), covering.hardware().size() > 1,
                frontierNameHeld(edited, covering, shipped));
        int installations = BindForgeInstallationsManager.getInstance().findAll().size();
        return new DeviceEditorDialog.Opening(device, name, live == null ? null : live.id(), description, gamepad, inputs,
                saved, name != null && stock.gameDrawsIt(name), middle, rename, taken, frontier, installations);
    }

    /**
     * Whether a set holds the device: the user's element by that name, or - for a built-in - labels of the user's
     * under Frontier's name, a {@code .buttonMap} that is not Frontier's own as shipped.
     */
    private static boolean holds(ConfigurationFiles files, String name, boolean frontiers, FrontierStockDevices stock,
                                 FrontierStockButtonMaps shipped) throws IOException {
        if (name == null) return false;
        if (!frontiers) return DeviceEdits.usersNames(files, stock).contains(name);
        Map<String, String> labels = DeviceEdits.labels(files, name);
        return !labels.isEmpty() && !shipped.isShipped(name, labels);
    }

    /**
     * Whether going back to Frontier's name is closed (A15): Frontier's element covers other controllers too, and
     * labels of the user's are under its name - another controller's, since this one is under a name of its own. Going
     * back would put this controller's labels over them. <em>Since Storage S4a this is read from the files: there is no
     * record of which controller a shared element's labels were made for (Alan, 2026-10-26).</em>
     */
    private static boolean frontierNameHeld(ConfigurationFiles edited, DeviceEntry covering,
                                            FrontierStockButtonMaps shipped) throws IOException {
        if (covering.hardware().size() <= 1) return false;
        Map<String, String> labels = DeviceEdits.labels(edited, covering.name());
        return !labels.isEmpty() && !shipped.isShipped(covering.name(), labels);
    }

    /**
     * How many bindings in the edits' binds files name the device. Empty when they cannot be read - CLEAR is then
     * refused rather than offered on a device they might name, since the game would reject the whole binds file.
     */
    private static OptionalInt bindingsNaming(ConfigurationFiles edited, String name) {
        if (name == null) return OptionalInt.of(0);
        try {
            Map<String, Integer> names = BindsDeviceReferences.countsIn(edited.bindings()).names();
            return OptionalInt.of(BindsDeviceReferences.bindingsNaming(List.of(name), names));
        } catch (IOException e) {
            log.warn("Could not read which devices the bindings name, so Clear is refused for {}: {}", name,
                    e.getMessage());
            return OptionalInt.empty();
        }
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

    // ------------------------------------------------------------------ the hardware

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
                    + "what the working copy names");
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
}
