package elite.intel.bindforge.storage;

import elite.intel.bindforge.devicefiles.BindsDeviceReferences;
import elite.intel.bindforge.devicefiles.ButtonMapWriter;
import elite.intel.bindforge.devicefiles.DeviceMappingsMerge;
import elite.intel.bindforge.devicefiles.FirstSetupPlan;
import elite.intel.bindforge.devicefiles.FirstSetupReader;
import elite.intel.bindforge.devicefiles.FrontierStockButtonMaps;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.io.BindingsWriter;
import elite.intel.bindforge.io.DeviceReferenceRewrite;
import elite.intel.bindforge.io.RewrittenXml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * The working copy first-time startup creates, from what it read and how the user reconciled it (storage.md rules
 * 2-3; alias-designer.md, <em>Reconciliation is per device element</em> and <em>How the reconciled files are
 * made</em>). Pure: the files come from a {@link FirstSetupReading}, and nothing is written here.
 * <p>
 * Every binds file and the preset file go into the working copy as they are, whatever the route - first setup never
 * asks which binds file to use (Alan, 2026-10-24). Only a merge's losing names change them.
 */
public final class FirstSetupFiles {

    private static final String BINDS_SUFFIX = ".binds";

    private FirstSetupFiles() {
    }

    /**
     * A device entry "use one installation for all" would remove that the bindings use - what its confirmation names
     * before anything is written.
     *
     * @param name      the entry's name, as an installation other than the chosen one holds it
     * @param holders   the storefronts of the installations holding it
     * @param bindings  how many bindings name it, by binds file - only files where some do
     */
    public record Loss(String name, List<String> holders, Map<String, Integer> bindings) {
        public Loss {
            holders = List.copyOf(holders);
            bindings = Collections.unmodifiableMap(new TreeMap<>(bindings));
        }

        public int total() {
            return bindings.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    /**
     * A merge's working copy.
     *
     * @param files         the working copy
     * @param moves         each losing name, by the name its bindings moved to
     * @param bindingsMoved how many bindings moved, across every binds file
     * @param devices       how many of the user's devices it holds, built-ins carrying labels included
     */
    public record Merged(ConfigurationFiles files, Map<String, String> moves, int bindingsMoved, int devices) {
        public Merged {
            Objects.requireNonNull(files, "files");
            moves = Collections.unmodifiableMap(new TreeMap<>(moves));
        }
    }

    /**
     * The working copy when the installations agree: the first installation's device files, since they are all the
     * same - or, with no installation, the binds and preset files alone.
     *
     * @throws IllegalStateException if the installations are not {@linkplain FirstSetupReading#identical() identical}
     */
    public static ConfigurationFiles identical(FirstSetupReading reading) {
        if (!reading.identical()) throw new IllegalStateException("the installations' device files differ");
        if (reading.installations().isEmpty()) return new ConfigurationFiles(reading.bindings(), null, Map.of());
        return useOne(reading, reading.installations().getFirst().installId());
    }

    /**
     * "Use one installation for all" (Alan, 2026-10-24): that installation's {@code DeviceMappings.xml} and every
     * {@code .buttonMap} it holds, whole.
     *
     * @throws IllegalArgumentException if it holds no {@code DeviceMappings.xml} - the route is not offered for one
     *                                  (Alan, 2026-10-25), since the working copy would then hold none
     */
    public static ConfigurationFiles useOne(FirstSetupReading reading, long installId) {
        InstallationFiles chosen = reading.installation(installId);
        if (!chosen.hasDeviceMappings() && reading.installations().size() > 1) {
            throw new IllegalArgumentException(chosen.storefront() + " holds no DeviceMappings.xml");
        }
        return new ConfigurationFiles(reading.bindings(), chosen.deviceMappings(), chosen.buttonMaps());
    }

    /**
     * The entries "use one installation for all" would remove that the bindings use, with the binds files and counts
     * its confirmation names - <em>"Epic has an entry Steam does not - LVWAP. 131 bindings in
     * DualVirpilDawnTreader.4.2.binds use it."</em> An entry no binding uses is simply overwritten, and is not here.
     * Counted across every binds file, since any can be made active later and the game checks them all.
     */
    public static List<Loss> lossesUsingOne(FirstSetupReading reading, long installId) {
        FirstSetupPlan.Installation chosen = planOf(reading, installId);
        Set<String> kept = folded(chosen.elements().keySet());
        Map<String, List<String>> holders = new TreeMap<>();
        for (FirstSetupPlan.Installation other : reading.plan().installations()) {
            if (other.installId() == installId) continue;
            for (String name : other.elements().keySet()) {
                if (kept.contains(name.toLowerCase(Locale.ROOT))) continue;
                holders.computeIfAbsent(name, n -> new ArrayList<>()).add(other.storefront());
            }
        }
        List<Loss> losses = new ArrayList<>();
        holders.forEach((name, storefronts) -> {
            Map<String, Integer> bindings = new TreeMap<>();
            reading.references().forEach((file, counts) -> {
                int count = BindsDeviceReferences.bindingsNaming(List.of(name), counts);
                if (count > 0) bindings.put(file, count);
            });
            if (!bindings.isEmpty()) losses.add(new Loss(name, storefronts, bindings));
        });
        return List.copyOf(losses);
    }

    /**
     * Merge: every installation's device files reconciled by the answers. One {@code DeviceMappings.xml} from the
     * first installation holding one, with the user's elements taken out and the kept ones put at the top, each copied
     * whole from its own installation; one {@code .buttonMap} per device with labels, kept byte for byte when its
     * labels came through unchanged; Frontier's own {@code VPCPanel} and {@code VPCThrottle} as found; and every
     * losing name's bindings moved to the winner in every binds file - a case-only difference included.
     *
     * @throws IllegalStateException if a question is unanswered
     * @throws IOException           if a device file or a binds file cannot be made - the message names it
     */
    public static Merged merge(FirstSetupReading reading, Map<String, String> answers) throws IOException {
        FirstSetupPlan.Resolution resolution = reading.plan().resolve(answers);
        byte[] deviceMappings = deviceMappings(reading, resolution);
        Map<String, byte[]> buttonMaps = buttonMaps(reading, resolution);

        Map<String, byte[]> bindings = new TreeMap<>(reading.bindings());
        int moved = moveBindings(bindings, resolution.moves());
        return new Merged(new ConfigurationFiles(bindings, deviceMappings, buttonMaps), resolution.moves(), moved,
                resolution.devices().size());
    }

    private static byte[] deviceMappings(FirstSetupReading reading, FirstSetupPlan.Resolution resolution)
            throws IOException {
        InstallationFiles base = reading.installations().stream().filter(InstallationFiles::hasDeviceMappings)
                .findFirst().orElse(null);
        // WHY: elements come only from a DeviceMappings.xml, so with none anywhere there is nothing of the user's to
        // put in one either - and the write leaves every installation's alone.
        if (base == null) return null;

        Set<String> usersNames = new LinkedHashSet<>();
        reading.plan().installations().forEach(install -> usersNames.addAll(install.elements().keySet()));
        List<DeviceMappingsMerge.Taken> taken = new ArrayList<>();
        for (FirstSetupPlan.Kept device : resolution.devices()) {
            if (device.builtIn()) continue;
            InstallationFiles source = reading.installation(device.source());
            taken.add(new DeviceMappingsMerge.Taken(source.deviceMappings(), device.name(), device.hardware().vid(),
                    device.hardware().pid()));
        }
        try {
            return DeviceMappingsMerge.assemble(base.deviceMappings(), usersNames, taken);
        } catch (IOException e) {
            throw new IOException(GameInstallation.DEVICE_MAPPINGS + " could not be made: " + e.getMessage(), e);
        }
    }

    private static Map<String, byte[]> buttonMaps(FirstSetupReading reading, FirstSetupPlan.Resolution resolution)
            throws IOException {
        Map<String, byte[]> maps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (FirstSetupPlan.Kept device : resolution.devices()) {
            if (device.labels().isEmpty()) continue;
            String file = device.name() + GameInstallation.BUTTON_MAP_SUFFIX;
            maps.put(file, unchangedOrWritten(reading, device));
        }
        // WHY: Frontier's own maps are left out of the plan as shipped, so they would be missing from the working
        // copy - kept as found, so the working copy is what the installations hold (Alan, 2026-10-25).
        for (InstallationFiles installation : reading.installations()) {
            installation.buttonMaps().forEach((file, content) -> {
                String device = file.substring(0, file.length() - GameInstallation.BUTTON_MAP_SUFFIX.length());
                if (FrontierStockButtonMaps.shipsMapFor(device)) maps.putIfAbsent(file, content);
            });
        }
        return new TreeMap<>(maps);
    }

    /** The bytes of an installation's file whose labels are exactly the ones kept, or the labels written afresh. */
    private static byte[] unchangedOrWritten(FirstSetupReading reading, FirstSetupPlan.Kept device)
            throws IOException {
        for (FirstSetupPlan.Installation install : reading.plan().installations()) {
            if (!device.labels().equals(install.labels().get(device.name()))) continue;
            InstallationFiles files = reading.installation(install.installId());
            String file = FirstSetupReader.buttonMapFileOf(files.buttonMaps(), device.name());
            if (file != null) return files.buttonMaps().get(file);
        }
        return ButtonMapWriter.write(device.labels());
    }

    /**
     * Moves every losing name's bindings to its winner in each binds file, rewriting only the files a move changes.
     *
     * @return how many bindings moved
     */
    private static int moveBindings(Map<String, byte[]> bindings, Map<String, String> moves) throws IOException {
        if (moves.isEmpty()) return 0;
        Map<String, Set<String>> losersByWinner = new LinkedHashMap<>();
        moves.forEach((loser, winner) -> losersByWinner.computeIfAbsent(winner, w -> new LinkedHashSet<>()).add(loser));

        BindingsWriter writer = new BindingsWriter();
        int moved = 0;
        for (Map.Entry<String, byte[]> file : bindings.entrySet()) {
            if (!file.getKey().endsWith(BINDS_SUFFIX)) continue;
            Encoded text = Encoded.of(file.getValue());
            String xml = text.xml();
            int inFile = 0;
            try {
                for (Map.Entry<String, Set<String>> winner : losersByWinner.entrySet()) {
                    RewrittenXml rewritten = writer.rewriteDeviceReferences(xml,
                            new DeviceReferenceRewrite(winner.getValue(), winner.getKey()));
                    xml = rewritten.xml();
                    inFile += rewritten.references();
                }
            } catch (IllegalArgumentException e) {
                throw new IOException(file.getKey() + " could not be rewritten: " + e.getMessage(), e);
            }
            if (inFile == 0) continue;
            file.setValue(text.with(xml));
            moved += inFile;
        }
        return moved;
    }

    private static FirstSetupPlan.Installation planOf(FirstSetupReading reading, long installId) {
        return reading.plan().installations().stream().filter(install -> install.installId() == installId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no installation " + installId + " was read"));
    }

    private static Set<String> folded(Set<String> names) {
        Set<String> folded = new LinkedHashSet<>();
        names.forEach(name -> folded.add(name.toLowerCase(Locale.ROOT)));
        return folded;
    }

    /** A binds file's text, and whether it began with a UTF-8 byte-order mark, which is written back. */
    private record Encoded(String xml, boolean bom) {

        private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

        static Encoded of(byte[] content) {
            boolean bom = content.length >= 3 && content[0] == BOM[0] && content[1] == BOM[1] && content[2] == BOM[2];
            int start = bom ? 3 : 0;
            return new Encoded(new String(content, start, content.length - start, StandardCharsets.UTF_8), bom);
        }

        byte[] with(String xml) {
            byte[] text = xml.getBytes(StandardCharsets.UTF_8);
            if (!bom) return text;
            byte[] content = new byte[BOM.length + text.length];
            System.arraycopy(BOM, 0, content, 0, BOM.length);
            System.arraycopy(text, 0, content, BOM.length, text.length);
            return content;
        }
    }
}
