package elite.intel.bindforge.install;

/**
 * Where an Elite Dangerous installation came from.
 * <p>
 * A label, not a key: a machine can hold more than one installation from the same storefront - a second
 * Frontier-launcher copy on Windows, or native, Flatpak and Snap Steam side by side on Linux. Two
 * installations may therefore carry the same value here, and are told apart by their paths.
 */
public enum Storefront {
    STEAM,
    EPIC,
    FRONTIER,
    OCULUS,
    /** Added by hand, because nothing records where a Frontier-launcher install went. */
    MANUAL
}
