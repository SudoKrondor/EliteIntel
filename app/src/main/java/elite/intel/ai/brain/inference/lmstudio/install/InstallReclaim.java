package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.db.managers.LmStudioInstallManager;
import elite.intel.db.managers.LmStudioInstallManager.Install;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Takes back an install the app made when its record was lost but its {@link InstallMarker} stayed.
 * <p>
 * WHY: the record lives in the shared database and the marker in LM Studio's home, so a database swapped in from
 * outside the app - a copied data folder, a file restored by hand from an older snapshot - drops or outdates the
 * record while the install it described is still there. The app then took its own install for one the commander set
 * up, and no longer stopped it with the services or offered to uninstall it (a support bundle, 2026-11-12). The marker
 * is only ever written by the app, into a folder it installed into, so it is proof enough to record the install again.
 */
final class InstallReclaim {

    private static final Logger log = LogManager.getLogger(InstallReclaim.class);

    private InstallReclaim() {
    }

    /**
     * The install LM Studio's home holds the marker of, recorded again, when the record is missing or names that same
     * home with another id. A record naming another folder is left alone: its drive may only be unplugged, and there
     * is one record at most.
     *
     * @param recorded what is recorded now, which does not match a marker in its own folder
     * @param userHome the folder LM Studio's home pointer lives in
     */
    static Optional<Install> reclaim(LmStudioInstallManager installs, Optional<Install> recorded, Path userHome) {
        LmStudioHome home = LmStudioHome.locate(userHome);
        Optional<UUID> marked = InstallMarker.read(home.dir());
        if (marked.isEmpty()) return Optional.empty();
        if (recorded.isPresent() && !recorded.get().homeDir().equals(home.dir())) return Optional.empty();

        // WHY only for another home: the app writes the pointer only to install somewhere else, and one naming the
        // usual home was written by LM Studio's own installer, which Uninstall must leave alone.
        boolean pointerWritten = !home.dir().equals(userHome.resolve(LmStudioHome.DEFAULT_DIR));
        // WHY by what is installed: the record of how far the setup got went with it. With LM Studio there the
        // commander gets Uninstall; without it Continue Setup, which installs LM Studio again.
        boolean setupComplete = home.installedLms().isPresent();
        Install reclaimed = installs.recordReclaimed(marked.get(), home.dir(), pointerWritten, setupComplete,
                Instant.now());
        log.info("Recorded again the app's install in {}, whose record was {}", home.dir(),
                recorded.isPresent() ? "for an earlier install" : "missing");
        return Optional.of(reclaimed);
    }
}
