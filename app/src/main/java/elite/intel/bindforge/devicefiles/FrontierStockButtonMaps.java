package elite.intel.bindforge.devicefiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code .buttonMap} files Frontier ships - {@code VPCPanel} and {@code VPCThrottle}, the only two against 51
 * device entries ({@code reference-data/FrontierStock-README.md}).
 * <p>
 * First setup must not take one into the master: it is Frontier's file, not the user's, and a master copy of it
 * would make Apply rewrite a file BindForge never owned. The files travel in the jar, as the stock
 * {@code DeviceMappings.xml} does, so a game update cannot change what they are compared against.
 */
public final class FrontierStockButtonMaps {

    private static final List<String> SHIPPED = List.of("VPCPanel", "VPCThrottle");

    private static final class Holder {
        private static final FrontierStockButtonMaps INSTANCE = load();
    }

    private final Map<String, Map<String, String>> labelsByDevice;

    /** Package-private as a test seam. */
    FrontierStockButtonMaps(Map<String, Map<String, String>> labelsByDevice) {
        this.labelsByDevice = Map.copyOf(labelsByDevice);
    }

    public static FrontierStockButtonMaps getInstance() {
        return Holder.INSTANCE;
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

    // WHY: a missing resource throws, as FrontierStockDevices does. Without it, Frontier's two maps would read as
    // the user's, and first setup would master files BindForge must never rewrite.
    private static FrontierStockButtonMaps load() {
        Map<String, Map<String, String>> labels = new LinkedHashMap<>();
        for (String device : SHIPPED) {
            // WHY: ".buttonMap.xml", not ".buttonMap". app/build.gradle packages resources by an extension list,
            // and .buttonMap is not on it - the file would be missing from the jar. The content is plain XML.
            String resource = "/bindforge/FrontierStock-" + device + ".buttonMap.xml";
            try (InputStream in = FrontierStockButtonMaps.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IllegalStateException("Frontier stock button map is missing from the jar: " + resource);
                }
                labels.put(device, ButtonMapReader.read(in.readAllBytes()));
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + resource, e);
            }
        }
        return new FrontierStockButtonMaps(labels);
    }
}
