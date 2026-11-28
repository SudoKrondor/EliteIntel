package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.ai.brain.inference.lmstudio.LmStudioDefaults;
import elite.intel.db.managers.LmStudioInstallManager;
import elite.intel.db.managers.LmStudioInstallManager.Install;
import elite.intel.session.SystemSession;
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
     * @param fields   the LMStudio Setup fields, filled in when a finished install is recorded with a blank Model
     */
    static Optional<Install> reclaim(LmStudioInstallManager installs, Optional<Install> recorded, Path userHome,
                                     LmStudioFields fields) {
        LmStudioHome home = LmStudioHome.locate(userHome);
        Optional<UUID> marked = InstallMarker.read(home.dir());
        if (marked.isEmpty()) return Optional.empty();
        if (recorded.isPresent() && !recorded.get().homeDir().equals(home.dir())) return Optional.empty();

        // WHY only for another home: the app writes the pointer only to install somewhere else, and one naming the
        // usual home was written by LM Studio's own installer, which Uninstall must leave alone.
        boolean pointerWritten = !home.dir().equals(userHome.resolve(LmStudioHome.DEFAULT_DIR));
        // WHY by what is installed: the record of how far the setup got went with it. With LM Studio and the model
        // there the commander gets Uninstall; without either, Continue Setup, which installs and downloads what is
        // missing, skips what is not, and saves Address and Model at the end.
        boolean setupComplete = home.installedLms().isPresent() && home.hasDownloaded(LmStudioDefaults.MODEL);
        Install reclaimed = installs.recordReclaimed(marked.get(), home.dir(), pointerWritten, setupComplete,
                Instant.now());
        log.info("Recorded again the app's install in {}, whose record was {}", home.dir(),
                recorded.isPresent() ? "for an earlier install" : "missing");
        if (setupComplete) fillInBlankModel(fields);
        return Optional.of(reclaimed);
    }

    /**
     * The Address and Model the finished install saved, put back when the Model is blank.
     * <p>
     * WHY: the record and the fields live in the same database, so whatever lost one lost the other - an erased
     * database came back with the install recorded again and the Model blank, and VEGA, which takes no input without
     * a model, said none was set up beside an LM Studio ready to serve it. The install only ever sets up
     * {@link LmStudioDefaults#MODEL}, and its manifest says it is downloaded. A Model the commander typed is never
     * replaced, nor one for an Address on another machine, which is not this install's.
     */
    private static void fillInBlankModel(LmStudioFields fields) {
        String model = fields.model();
        String address = fields.address();
        if (model != null && !model.isBlank()) return;
        if (!LmStudioSetupOffer.isOnThisPc(address)) return;
        String filledAddress = address == null || address.isBlank() ? LmStudioDefaults.ADDRESS : address;
        fields.save(filledAddress, LmStudioDefaults.MODEL);
        log.info("Filled in Model {} for the app's install, whose Model was blank", LmStudioDefaults.MODEL);
    }

    /**
     * The LMStudio Setup fields as saved. WHY a seam: the tests check what is filled in without the session.
     */
    interface LmStudioFields {
        String address();

        String model();

        void save(String address, String model);

        static LmStudioFields saved() {
            SystemSession session = SystemSession.getInstance();
            return new LmStudioFields() {
                @Override
                public String address() {
                    return session.getLmStudioAddress();
                }

                @Override
                public String model() {
                    return session.getLmStudioCommandModel();
                }

                @Override
                public void save(String address, String model) {
                    session.setLmStudioSettings(address, model);
                }
            };
        }
    }
}
