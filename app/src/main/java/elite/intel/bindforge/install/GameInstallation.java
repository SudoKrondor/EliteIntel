package elite.intel.bindforge.install;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * One installation of Elite Dangerous, and where its input files live.
 * <p>
 * <strong>The bindings folder hangs off the installation, not off the provider.</strong> On Windows every
 * installation reports the same shared {@code Options\Bindings} folder, which is true; on Linux each
 * installation carries its own inside its Proton prefix, which is also true. Callers loop over installations
 * either way and never ask which operating system they are on.
 * <p>
 * There is deliberately no {@code primary} flag. No installation is authoritative - any file inside a game
 * folder can be wiped by a patch, which is why the master lives in Elite-Intel's own folder.
 *
 * @param storefront       where it came from; a label, never a key
 * @param root             the installation root, the folder holding {@code Products}
 * @param controlSchemes   the Live product's {@code ControlSchemes} folder
 * @param bindingsFolder   the folder holding this installation's {@code .binds} and {@code StartPreset}
 */
public record GameInstallation(Storefront storefront, Path root, Path controlSchemes, Path bindingsFolder) {

    /**
     * The only product BindForge touches. An installation can hold several - a pre-merge copy may still have
     * Horizons' {@code elite-dangerous-64} beside it - and each has its own {@code ControlSchemes}, so
     * anything scanning for these files must take this one rather than the first it finds.
     */
    public static final String LIVE_PRODUCT = "elite-dangerous-odyssey-64";

    public static final String DEVICE_MAPPINGS = "DeviceMappings.xml";
    public static final String DEVICE_BUTTON_MAPS = "DeviceButtonMaps";
    public static final String BUTTON_MAP_SUFFIX = ".buttonMap";

    public GameInstallation {
        Objects.requireNonNull(storefront, "storefront");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(controlSchemes, "controlSchemes");
        Objects.requireNonNull(bindingsFolder, "bindingsFolder");
    }

    /**
     * The Live product's {@code ControlSchemes} folder inside {@code root}, whether or not it exists.
     */
    public static Path controlSchemesUnder(Path root) {
        return root.resolve("Products").resolve(LIVE_PRODUCT).resolve("ControlSchemes");
    }

    /**
     * Whether {@code root} looks like an Elite Dangerous installation BindForge can work with.
     * <p>
     * Judged by structure rather than by name: the Live product's {@code ControlSchemes} folder is either
     * there or it is not. That is what separates a real installation from something merely named like one -
     * an Epic manifest for the soundtrack carries a similar display name and no such folder.
     */
    public static boolean looksLikeAnInstall(Path root) {
        return Files.isDirectory(controlSchemesUnder(root));
    }

    public Path deviceMappings() {
        return deviceMappingsIn(controlSchemes);
    }

    /**
     * The folder holding this installation's {@code .buttonMap} files. <strong>A missing one is normal</strong>,
     * not a fault - Frontier ships button maps for only a couple of devices, and Horizons ships none.
     */
    public Path deviceButtonMaps() {
        return deviceButtonMapsIn(controlSchemes);
    }

    // WHY: the device-file paths are spelled once, here. Apply, the startup check and the startup .buttonMap each
    // decide which file is a device's, and a typo in one copy would make them disagree.

    /** The {@code DeviceMappings.xml} in a {@code ControlSchemes} folder, whether or not it exists. */
    public static Path deviceMappingsIn(Path controlSchemes) {
        return controlSchemes.resolve(DEVICE_MAPPINGS);
    }

    /** The {@code DeviceButtonMaps} folder in a {@code ControlSchemes} folder, whether or not it exists. */
    public static Path deviceButtonMapsIn(Path controlSchemes) {
        return controlSchemes.resolve(DEVICE_BUTTON_MAPS);
    }

    /**
     * A device's {@code .buttonMap} in a {@code ControlSchemes} folder: the filename stem is the device's element
     * name in {@code DeviceMappings.xml}.
     * <p>
     * <strong>The file must land directly in {@code DeviceButtonMaps}, or this refuses.</strong> An XML tag may
     * hold a colon, and on Windows {@code C:x.buttonMap} is not refused as a filename - it is a path on drive C,
     * relative to that drive's working folder, and {@code resolve} follows it there, or drops the {@code C:} when
     * the folder is on drive C. A tag in a hand-edited {@code DeviceMappings.xml} must never steer a write out of
     * the game folder, or under another name.
     *
     * @throws InvalidPathException when the name cannot be a filename, or would not be one inside
     *                              {@code DeviceButtonMaps}
     */
    public static Path buttonMapIn(Path controlSchemes, String deviceName) {
        Path folder = deviceButtonMapsIn(controlSchemes);
        String filename = deviceName + BUTTON_MAP_SUFFIX;
        // WHY: checked before resolving as well as after. "C:x" on another drive leaves the folder, but on the
        // folder's own drive resolve quietly drops the "C:" and lands inside it under the wrong name - so the name
        // itself must have no root, and the result must carry exactly that name.
        Path name = folder.getFileSystem().getPath(filename);
        Path file = folder.resolve(name);
        if (name.getRoot() != null || name.getNameCount() != 1
                || !folder.equals(file.getParent()) || !filename.equals(file.getFileName().toString())) {
            throw new InvalidPathException(deviceName, "a device name must be a filename inside " + folder);
        }
        return file;
    }
}
