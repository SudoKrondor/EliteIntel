package elite.intel.bindforge.storage;

import elite.intel.bindforge.devicefiles.ButtonMapReader;
import elite.intel.bindforge.devicefiles.ButtonMapWriter;
import elite.intel.bindforge.devicefiles.DeviceEntry;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.DeviceMappingsMerge;
import elite.intel.bindforge.devicefiles.DeviceMappingsParser;
import elite.intel.bindforge.devicefiles.FrontierStockButtonMaps;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.io.DeviceReferenceRewrite;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

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
        return files.buttonMaps().keySet().stream().filter(wanted::equalsIgnoreCase).findFirst();
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
     * CLEAR (A10): the device's element and its {@code .buttonMap} taken out. The caller refuses it while bindings
     * name the device - moving them to its hardware ID is A19's.
     */
    public static ConfigurationFiles clear(ConfigurationFiles files, String name) throws IOException {
        byte[] deviceMappings = DeviceMappingsMerge.remove(files.deviceMappings(), Set.of(name)).content();
        return files.withDeviceMappings(deviceMappings).withButtonMaps(withoutMap(files.buttonMaps(), name));
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
     * into the edits.
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
        result.keySet().removeIf(wanted::equalsIgnoreCase);
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
