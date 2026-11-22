package elite.intel.bindforge.install;

import elite.intel.util.OsDetector;

import java.nio.file.Path;
import java.util.List;

/**
 * Finds the Elite Dangerous installations on this machine.
 * <p>
 * One implementation per operating system, so that Windows-versus-Linux lives behind this seam instead of
 * spreading through BindForge as conditionals. Krondor, 2026-09-26: <em>"if and when we change this for Linux
 * the blast radius will be only the LinuxGameInstallationProviderImpl class and not all over the code in a
 * bunch of IF/ELSE mess."</em>
 * <p>
 * <strong>Detection is a convenience, never a guarantee.</strong> Only Steam and Epic record where an
 * installation actually went, so only those survive the user moving one. A Frontier-launcher install moved off
 * its documented defaults is undetectable by construction, which is why a manual add has to exist alongside
 * this rather than as a fallback of last resort.
 */
public interface GameInstallationProvider {

    /**
     * Every installation found, in no guaranteed order, or empty when none was found.
     * <p>
     * Empty is an ordinary answer, not a failure: the game may not be installed, or may be somewhere nothing
     * records. Implementations report what they can read and leave the rest to the manual add.
     */
    List<GameInstallation> findInstallations();

    /**
     * Whether an installation added by hand is asked for its Wine prefix, the folder holding {@code drive_c} - where its
     * own bindings and journal folders are (linux-several-installations.md, rule 6). False where every installation
     * shares the Windows user's folders.
     */
    default boolean asksForWinePrefix() {
        return false;
    }

    /**
     * The provider for the operating system Elite-Intel is running on - the one place that asks.
     *
     * @param windowsBindingsFolder the one {@code Options\Bindings} folder every Windows installation shares; Linux
     *                              finds each installation's own inside its Proton prefix
     */
    static GameInstallationProvider forThisPlatform(Path windowsBindingsFolder) {
        return switch (OsDetector.getOs()) {
            case WINDOWS -> new WindowsGameInstallationProvider(windowsBindingsFolder);
            case LINUX -> new LinuxGameInstallationProvider();
            // WHY: the game does not run on macOS. The manual add is still there for anyone who tries.
            case MAC -> List::of;
        };
    }
}
