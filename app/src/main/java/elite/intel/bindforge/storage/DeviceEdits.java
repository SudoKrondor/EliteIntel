package elite.intel.bindforge.storage;

import elite.intel.bindforge.devicefiles.BindsDeviceReferences;
import elite.intel.bindforge.devicefiles.ButtonMapReader;
import elite.intel.bindforge.devicefiles.ButtonMapWriter;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.DeviceEntry;
import elite.intel.bindforge.devicefiles.DeviceMappingsMerge;
import elite.intel.bindforge.devicefiles.DeviceMappingsParser;
import elite.intel.bindforge.devicefiles.FrontierStockButtonMaps;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.io.DeviceReferenceRewrite;
import elite.intel.bindforge.io.FileNames;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * What each Alias Designer action does to a set of the game input configuration files - the working copy with the
 * edits on screen (storage.md rules 6-7; alias-designer.md, <em>Actions</em>, <em>Renaming a Device</em>,
 * <em>Onboarding</em>). Pure: each takes a set and returns the changed set; {@link Edits} holds it until APPLY.
 * <p>
 * <strong>The device, its {@code .buttonMap} and every binds file naming it change together</strong>, as one edit
 * (Alan, 2026-10-24): a rename renames the element in place and the {@code .buttonMap}, and rewrites every
 * {@code Device=} naming the old name. The user's elements lead {@code <Root>}, Frontier's beneath them, since the
 * first element claiming a VID/PID is the one the game uses.
 * <p>
 * Frontier's elements are never changed or removed: a built-in is shadowed by an element of the user's, and its
 * labels reset by removing a {@code .buttonMap} - or, for {@code VPCPanel} and {@code VPCThrottle}, by putting
 * Frontier's own back.
 */
public final class DeviceEdits {

    /** One half of a hardware ID: four hex digits, as {@code DeviceReferenceRewrite} writes them. */
    private static final Pattern HEX_HALF = Pattern.compile("[0-9A-F]{4}");

    private DeviceEdits() {
    }

    // ------------------------------------------------------------------ reading

    /**
     * Every device element in the set's {@code DeviceMappings.xml}, in file order - empty when it holds none.
     *
     * @throws IOException if the file cannot be read as a {@code DeviceMappings.xml}
     */
    public static List<DeviceEntry> elements(ConfigurationFiles files) throws IOException {
        if (!files.hasDeviceMappings()) return List.of();
        return DeviceMappingsParser.parse(new ByteArrayInputStream(files.deviceMappings()),
                GameInstallation.DEVICE_MAPPINGS);
    }

    /** The user's element names, Frontier's left out, in file order - the order they lead the file in. */
    public static List<String> usersNames(ConfigurationFiles files, FrontierStockDevices stock) throws IOException {
        return elements(files).stream().map(DeviceEntry::name).filter(name -> !stock.ships(name)).toList();
    }

    /** The file name the set holds the device's {@code .buttonMap} under, found ignoring case, as the game finds it. */
    public static Optional<String> buttonMapFile(ConfigurationFiles files, String device) {
        String wanted = device + GameInstallation.BUTTON_MAP_SUFFIX;
        return files.buttonMaps().keySet().stream().filter(name -> FileNames.sameFile(name, wanted)).findFirst();
    }

    /**
     * The device's labels, by input token - empty when the set holds no {@code .buttonMap} for it.
     *
     * @throws IOException if its {@code .buttonMap} is not XML
     */
    public static Map<String, String> labels(ConfigurationFiles files, String device) throws IOException {
        Optional<String> file = buttonMapFile(files, device);
        if (file.isEmpty()) return Map.of();
        return ButtonMapReader.read(files.buttonMaps().get(file.get()));
    }

    // ------------------------------------------------------------------ editing

    /**
     * The device's labels, its whole {@code .buttonMap} - SAVE with the name unchanged. With no label left the file
     * goes, so the device has none.
     */
    public static ConfigurationFiles withLabels(ConfigurationFiles files, String device, Map<String, String> labels)
            throws IOException {
        Map<String, byte[]> maps = withoutMap(files.buttonMaps(), device);
        if (!labels.isEmpty()) maps.put(device + GameInstallation.BUTTON_MAP_SUFFIX, ButtonMapWriter.write(labels));
        return files.withButtonMaps(maps);
    }

    /**
     * One controller named - onboarding's FINISH (Alan, 2026-10-26, Storage S4a): its element added after the user's
     * others, its {@code .buttonMap} with the generated labels unless one is already under the name, and every binding
     * naming it by hardware id moved to the name - the old id a replaced controller had (A17) included.
     *
     * @param hardwareIds the values the binds files name it by, exactly as found
     * @return the set, and how many bindings moved
     * @throws IOException if the set holds no {@code DeviceMappings.xml} to add it to, or a file cannot be changed
     */
    public static Changed name(ConfigurationFiles files, String name, String vid, String pid,
                               Map<String, String> labels, Collection<String> hardwareIds,
                               FrontierStockDevices stock) throws IOException {
        if (!files.hasDeviceMappings()) {
            throw new IOException("your working copy holds no " + GameInstallation.DEVICE_MAPPINGS
                    + " to add " + name + " to");
        }
        List<String> order = new ArrayList<>(usersNames(files, stock));
        order.add(name);
        byte[] deviceMappings = DeviceMappingsMerge.insert(files.deviceMappings(),
                List.of(new DeviceMappingsMerge.UserEntry(name, vid, pid)), order).content();
        Map<String, byte[]> maps = new TreeMap<>(files.buttonMaps());
        // WHY: a .buttonMap already under the name is the user's, kept as it is (A6b).
        if (buttonMapFile(files, name).isEmpty() && !labels.isEmpty()) {
            maps.put(name + GameInstallation.BUTTON_MAP_SUFFIX, ButtonMapWriter.write(labels));
        }
        List<DeviceReferenceRewrite> moves = hardwareIds.stream()
                .map(id -> new DeviceReferenceRewrite(Set.of(id), name))
                .toList();
        BindsFiles.Rewritten rewritten = BindsFiles.rewrite(files.bindings(), moves);
        return new Changed(new ConfigurationFiles(rewritten.bindings(), deviceMappings, maps), rewritten.moved());
    }

    /**
     * The identity question's yes for a named controller (A12; Storage S4b, Alan, 2026-10-28): the user's element given
     * the picked controller's VID/PID. The name, the labels and the binds files stay as they are - the bindings name the
     * device, so they follow it at APPLY.
     *
     * @param was the hardware the question was asked about
     * @return the changed set, or empty when the element is gone or no longer has {@code was} - the device changed
     *         between the question and the answer, so nothing is changed
     * @throws IOException if the set's {@code DeviceMappings.xml} cannot be read or changed
     */
    public static Optional<ConfigurationFiles> retarget(ConfigurationFiles files, String name, HardwareId was,
                                                        String vid, String pid) throws IOException {
        Optional<DeviceEntry> element = elements(files).stream().filter(e -> e.name().equals(name)).findFirst();
        if (element.isEmpty() || !was.equals(element.get().primary())) return Optional.empty();
        return Optional.of(files.withDeviceMappings(
                DeviceMappingsMerge.retarget(files.deviceMappings(), name, vid, pid).content()));
    }

    /**
     * One identity answer to make: the user's device {@code name}, asked about with hardware {@code was}, to get the
     * picked controller's VID/PID.
     */
    public record Retarget(String name, HardwareId was, String vid, String pid) {
        public Retarget {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(was, "was");
            Objects.requireNonNull(vid, "vid");
            Objects.requireNonNull(pid, "pid");
        }
    }

    /**
     * @param files the set with every retarget made
     * @param made  the retargets made, in order
     * @param stale the retargets whose device changed since the question - nothing was changed for them
     */
    public record Retargeted(ConfigurationFiles files, List<Retarget> made, List<Retarget> stale) {
        public Retargeted {
            Objects.requireNonNull(files, "files");
            made = List.copyOf(made);
            stale = List.copyOf(stale);
        }
    }

    /**
     * Every identity answer in one edit (review, 2026-10-28): each {@link #retarget} in turn on the set the last one
     * left, a stale one skipped and listed.
     *
     * @throws IOException if the set's {@code DeviceMappings.xml} cannot be read or changed - nothing is made
     */
    public static Retargeted retargetAll(ConfigurationFiles files, List<Retarget> retargets) throws IOException {
        ConfigurationFiles result = files;
        List<Retarget> made = new ArrayList<>();
        List<Retarget> stale = new ArrayList<>();
        for (Retarget retarget : retargets) {
            Optional<ConfigurationFiles> changed = retarget(result, retarget.name(), retarget.was(), retarget.vid(),
                    retarget.pid());
            if (changed.isPresent()) {
                result = changed.get();
                made.add(retarget);
            } else {
                stale.add(retarget);
            }
        }
        return new Retargeted(result, made, stale);
    }

    /**
     * One of the user's devices renamed (A11): the element renamed in place, keeping every child; the
     * {@code .buttonMap} renamed, never copied, with {@code labels} - or its own content when there are none; and every
     * binding naming the old name moved to the new one, a case-only change included.
     */
    public static Changed rename(ConfigurationFiles files, String oldName, String newName,
                                 Map<String, String> labels) throws IOException {
        byte[] deviceMappings = DeviceMappingsMerge.rename(files.deviceMappings(), oldName, newName).content();
        Map<String, byte[]> maps = movedMap(files, oldName, newName, labels);
        BindsFiles.Rewritten rewritten =
                BindsFiles.rewrite(files.bindings(), List.of(DeviceReferenceRewrite.rename(oldName, newName)));
        return new Changed(new ConfigurationFiles(rewritten.bindings(), deviceMappings, maps), rewritten.moved());
    }

    /**
     * A built-in renamed, which shadows it (A15): an element of the user's, carrying only the controller's VID/PID,
     * added at the top of the user's, Frontier's left where it is; the labels under the new name; and every binding
     * naming Frontier's element moved to the new name. Frontier's {@code .buttonMap} is put back as Frontier ships it
     * when Frontier ships one, kept when Frontier's element covers other controllers too - they are still called by
     * its name - and otherwise removed with the name.
     *
     * @param shared whether Frontier's element covers other hardware too
     */
    public static Changed shadow(ConfigurationFiles files, String frontierName, String newName, String vid, String pid,
                                 Map<String, String> labels, boolean shared, FrontierStockDevices stock,
                                 FrontierStockButtonMaps shipped) throws IOException {
        List<String> order = new ArrayList<>();
        order.add(newName);
        order.addAll(usersNames(files, stock));
        byte[] deviceMappings = DeviceMappingsMerge.insert(files.deviceMappings(),
                List.of(new DeviceMappingsMerge.UserEntry(newName, vid, pid)), order).content();
        Map<String, byte[]> maps = shared && shipped.shippedContent(frontierName).isEmpty()
                ? new TreeMap<>(files.buttonMaps())
                : frontierMapAsShipped(files.buttonMaps(), frontierName, shipped);
        maps = withoutMap(maps, newName);
        if (!labels.isEmpty()) maps.put(newName + GameInstallation.BUTTON_MAP_SUFFIX, ButtonMapWriter.write(labels));
        BindsFiles.Rewritten rewritten =
                BindsFiles.rewrite(files.bindings(), List.of(DeviceReferenceRewrite.rename(frontierName, newName)));
        return new Changed(new ConfigurationFiles(rewritten.bindings(), deviceMappings, maps), rewritten.moved());
    }

    /**
     * A shadow given Frontier's name again (A15): the user's element removed, the bindings moved back to Frontier's
     * name, and the labels written under it.
     */
    public static Changed backToFrontier(ConfigurationFiles files, String usersName, String frontierName,
                                         Map<String, String> labels) throws IOException {
        byte[] deviceMappings = DeviceMappingsMerge.remove(files.deviceMappings(), Set.of(usersName)).content();
        Map<String, byte[]> maps = movedMap(files, usersName, frontierName, labels);
        BindsFiles.Rewritten rewritten =
                BindsFiles.rewrite(files.bindings(), List.of(DeviceReferenceRewrite.rename(usersName, frontierName)));
        return new Changed(new ConfigurationFiles(rewritten.bindings(), deviceMappings, maps), rewritten.moved());
    }

    /**
     * Where CLEAR moves the bindings naming a device (A19): what the game itself would call the controller once the
     * element is gone.
     *
     * @param device   the {@code Device=} value the bindings move to
     * @param hardware the element's own pair
     * @param element  whether {@code device} is another element claiming the pair - a shadowed built-in - rather than
     *                 the hardware ID
     */
    public record ClearTarget(String device, HardwareId hardware, boolean element) {
        public ClearTarget {
            Objects.requireNonNull(device, "device");
            Objects.requireNonNull(hardware, "hardware");
        }

        /** As the user is shown it: the element's name, or the hardware ID as {@code VID:PID}. */
        public String shown() {
            return element ? device : hardware.vid() + ":" + hardware.pid();
        }
    }

    /**
     * Where CLEAR would move the bindings naming the user's device {@code name} (A19; Krondor, 2026-11-10). The game names
     * a device by the first element claiming its VID/PID and by VID then PID in hex when none does (format section
     * 4.0), so: the next element in the file claiming the element's own pair - Frontier's, for a shadow - or else the
     * hardware ID, upper case.
     *
     * @return empty when the set holds no element by that name, or the element has no well-formed pair of its own - an
     *         element with only {@code <Alternative>} pairs, made by hand, names no one controller to move them to
     * @throws IOException if the set's {@code DeviceMappings.xml} cannot be read
     */
    public static Optional<ClearTarget> clearTarget(ConfigurationFiles files, String name) throws IOException {
        List<DeviceEntry> all = elements(files);
        HardwareId pair = all.stream().filter(e -> e.name().equals(name)).findFirst()
                .map(DeviceEntry::primary).orElse(null);
        if (pair == null || !HEX_HALF.matcher(pair.vid()).matches() || !HEX_HALF.matcher(pair.pid()).matches()) {
            return Optional.empty();
        }
        // WHY exact: DeviceMappingsMerge.remove takes out every element spelled exactly so, and only those.
        Optional<String> next = all.stream()
                .filter(e -> !e.name().equals(name) && e.hardware().contains(pair))
                .map(DeviceEntry::name)
                .findFirst();
        return Optional.of(next.map(element -> new ClearTarget(element, pair, true))
                .orElseGet(() -> new ClearTarget(pair.vid() + pair.pid(), pair, false)));
    }

    /**
     * CLEAR (A10, A19): the device's element and its {@code .buttonMap} taken out, and every binding naming it moved to
     * {@link #clearTarget} - one edit, so no binds file is left naming a device the game no longer knows (Krondor,
     * 2026-10-24). A hardware-ID reference already in a binds file in another case is written the same way, since
     * the game rejects a file that spells one device two ways (format section 4.3; Krondor, 2026-11-10); one written PID
     * first is left alone - a rewrite never matches defensively.
     *
     * @throws IOException if bindings name the device and there is nowhere to move them ({@link #clearTarget} is
     *                     empty) - the screen refuses CLEAR first - or a file cannot be changed
     */
    public static Changed clear(ConfigurationFiles files, String name) throws IOException {
        Optional<ClearTarget> target = clearTarget(files, name);
        List<DeviceReferenceRewrite> moves = target.map(t -> clearMoves(name, t)).orElse(List.of());
        if (target.isEmpty() && BindsDeviceReferences.bindingsNaming(List.of(name),
                BindsDeviceReferences.countsIn(files.bindings()).names()) > 0) {
            throw new IOException(name + " has no hardware ID of its own to move its bindings to");
        }
        byte[] deviceMappings = DeviceMappingsMerge.remove(files.deviceMappings(), Set.of(name)).content();
        BindsFiles.Rewritten rewritten = BindsFiles.rewrite(files.bindings(), moves);
        return new Changed(new ConfigurationFiles(rewritten.bindings(), deviceMappings,
                withoutMap(files.buttonMaps(), name)), rewritten.moved());
    }

    private static List<DeviceReferenceRewrite> clearMoves(String name, ClearTarget target) {
        if (target.element()) return List.of(DeviceReferenceRewrite.rename(name, target.device()));
        List<DeviceReferenceRewrite> moves = new ArrayList<>();
        moves.add(DeviceReferenceRewrite.nameToHardwareId(name, target.hardware().vid(), target.hardware().pid()));
        // WHY: matched ignoring case, and the exact spelling left alone, so this catches every other case of the id.
        String lower = target.device().toLowerCase(Locale.ROOT);
        if (!lower.equals(target.device())) moves.add(new DeviceReferenceRewrite(Set.of(lower), target.device()));
        return moves;
    }

    /**
     * RESET LABELS on a built-in (A10): its {@code .buttonMap} removed - or put back byte for byte as Frontier ships it,
     * for {@code VPCPanel} and {@code VPCThrottle}. Frontier's element is never touched. Default labels come back at the
     * next APPLY for a controller that is plugged in.
     */
    public static ConfigurationFiles resetLabels(ConfigurationFiles files, String frontierName,
                                                 FrontierStockButtonMaps shipped) {
        return files.withButtonMaps(frontierMapAsShipped(files.buttonMaps(), frontierName, shipped));
    }

    /**
     * UNDO CLEAR and UNDO RESET: the device's element and {@code .buttonMap} as the working copy holds them, put back
     * into the edits. For the user's element, the bindings CLEAR moved come back to its name (A19; Krondor, 2026-11-10):
     * every reference to its hardware ID, in any case - what the game writes once an element claims the pair - or, for
     * a shadow, every reference to the element CLEAR moved them to, as shadowing moves them. The binds files are never
     * put back whole, since that would throw away bindings edited since the CLEAR.
     *
     * @param applied the working copy - what the last write left
     */
    public static ConfigurationFiles restore(ConfigurationFiles files, ConfigurationFiles applied, String name,
                                             FrontierStockDevices stock) throws IOException {
        ConfigurationFiles result = files;
        if (applied.hasDeviceMappings() && files.hasDeviceMappings() && !stock.ships(name)
                && usersNames(applied, stock).contains(name)) {
            List<String> order = new ArrayList<>(usersNames(files, stock));
            List<String> appliedOrder = usersNames(applied, stock);
            int place = appliedOrder.indexOf(name);
            // WHY: back where it was among the user's elements - after the last one that led it in the working copy.
            int at = 0;
            for (int i = 0; i < order.size(); i++) {
                int was = appliedOrder.indexOf(order.get(i));
                if (was >= 0 && was < place) at = i + 1;
            }
            order.add(at, name);
            result = result.withDeviceMappings(DeviceMappingsMerge.copyIn(files.deviceMappings(),
                    applied.deviceMappings(), name, order).content());
            Optional<ClearTarget> target = clearTarget(result, name);
            if (target.isPresent()) {
                result = result.withBindings(BindsFiles.rewrite(result.bindings(),
                        List.of(new DeviceReferenceRewrite(Set.of(target.get().device()), name))).bindings());
            }
        }
        Map<String, byte[]> maps = withoutMap(result.buttonMaps(), name);
        buttonMapFile(applied, name).ifPresent(file -> maps.put(file, applied.buttonMaps().get(file)));
        return result.withButtonMaps(maps);
    }

    /**
     * A set after an edit, and how many bindings the edit moved.
     */
    public record Changed(ConfigurationFiles files, int bindingsMoved) {
        public Changed {
            Objects.requireNonNull(files, "files");
        }
    }

    // ------------------------------------------------------------------ the button maps

    /** The maps without any spelling of the device's. */
    private static Map<String, byte[]> withoutMap(Map<String, byte[]> maps, String device) {
        Map<String, byte[]> result = new TreeMap<>(maps);
        String wanted = device + GameInstallation.BUTTON_MAP_SUFFIX;
        result.keySet().removeIf(name -> FileNames.sameFile(name, wanted));
        return result;
    }

    /** The old name's map moved to the new name: with {@code labels}, or its own bytes when there are none. */
    private static Map<String, byte[]> movedMap(ConfigurationFiles files, String oldName, String newName,
                                                Map<String, String> labels) throws IOException {
        byte[] old = buttonMapFile(files, oldName).map(file -> files.buttonMaps().get(file)).orElse(null);
        Map<String, byte[]> maps = withoutMap(withoutMap(files.buttonMaps(), oldName), newName);
        byte[] content = !labels.isEmpty() ? ButtonMapWriter.write(labels) : old;
        if (content != null) maps.put(newName + GameInstallation.BUTTON_MAP_SUFFIX, content);
        return maps;
    }

    /** Frontier's map for the device as Frontier ships it, or no map when Frontier ships none. */
    private static Map<String, byte[]> frontierMapAsShipped(Map<String, byte[]> maps, String frontierName,
                                                            FrontierStockButtonMaps shipped) {
        Map<String, byte[]> result = withoutMap(maps, frontierName);
        shipped.shippedContent(frontierName)
                .ifPresent(content -> result.put(frontierName + GameInstallation.BUTTON_MAP_SUFFIX, content));
        return result;
    }
}
