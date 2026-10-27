package elite.intel.bindforge.devicefiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The {@code .buttonMap} files Frontier ships - {@code VPCPanel} and {@code VPCThrottle}, the only two against 51
 * device entries ({@code reference-data/FrontierStock-README.md}).
 * <p>
 * First setup must not take one into the master: it is Frontier's file, not the user's, and a master copy of it
 * would make Apply rewrite a file BindForge never owned. The files travel in the jar, as the stock
 * {@code DeviceMappings.xml} does, so a game update cannot change what they are compared against.
 * <p>
 * They are also what RESET LABELS puts back on those two devices: Frontier's map is restored rather than removed,
 * because a {@code .buttonMap} Frontier shipped is never removed (Alan, 2026-10-11, A10).
 */
public final class FrontierStockButtonMaps {

    private static final List<String> SHIPPED = List.of("VPCPanel", "VPCThrottle");

    private static final class Holder {
        private static final FrontierStockButtonMaps INSTANCE = load();
    }

    private final Map<String, Map<String, String>> labelsByDevice;
    private final Map<String, byte[]> contentByDevice;

    /** Package-private as a test seam: labels only, with no file content to restore. */
    FrontierStockButtonMaps(Map<String, Map<String, String>> labelsByDevice) {
        this(labelsByDevice, Map.of());
    }

    FrontierStockButtonMaps(Map<String, Map<String, String>> labelsByDevice, Map<String, byte[]> contentByDevice) {
        this.labelsByDevice = Map.copyOf(labelsByDevice);
        this.contentByDevice = Map.copyOf(contentByDevice);
    }

    public static FrontierStockButtonMaps getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Whether Frontier ships a {@code .buttonMap} under this device name, whatever a file by that name holds now.
     * Compared ignoring case, as the game finds the file. A file by one of these names is never removed from an
     * installation (alias-designer.md, <em>What APPLY does to each installation's device files</em>).
     */
    public static boolean shipsMapFor(String deviceName) {
        return SHIPPED.stream().anyMatch(shipped -> shipped.equalsIgnoreCase(deviceName));
    }

    /**
     * Whether this is Frontier's own file for that device: the name is one Frontier ships a map for, and the
     * labels are Frontier's.
     * <p>
     * Compared by label rather than by byte, so a copy whose line endings or indentation changed on the way is
     * still recognised. A file under Frontier's name whose labels the user changed is the user's.
     */
    public boolean isShipped(String deviceName, Map<String, String> labels) {
        Map<String, String> shipped = labelsByDevice.get(deviceName);
        return shipped != null && shipped.equals(labels);
    }

    /**
     * Frontier's own file for that device, byte for byte as it ships, or empty when Frontier ships none for it.
     * Bytes rather than labels, so a restored file is Frontier's file and not BindForge's rewrite of it.
     */
    public Optional<byte[]> shippedContent(String deviceName) {
        byte[] content = contentByDevice.get(deviceName);
        return content == null ? Optional.empty() : Optional.of(content.clone());
    }

    // WHY: a missing resource throws, as FrontierStockDevices does. Without it, Frontier's two maps would read as
    // the user's, and first setup would master files BindForge must never rewrite.
    private static FrontierStockButtonMaps load() {
        Map<String, Map<String, String>> labels = new LinkedHashMap<>();
        Map<String, byte[]> content = new LinkedHashMap<>();
        for (String device : SHIPPED) {
            // WHY: ".buttonMap.xml", not ".buttonMap". app/build.gradle packages resources by an extension list,
            // and .buttonMap is not on it - the file would be missing from the jar. The content is plain XML.
            String resource = "/bindforge/FrontierStock-" + device + ".buttonMap.xml";
            try (InputStream in = FrontierStockButtonMaps.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IllegalStateException("Frontier stock button map is missing from the jar: " + resource);
                }
                byte[] bytes = in.readAllBytes();
                labels.put(device, ButtonMapReader.read(bytes));
                content.put(device, bytes);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + resource, e);
            }
        }
        return new FrontierStockButtonMaps(labels, content);
    }
}
