package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.ai.brain.inference.lmstudio.LMStudioClient;
import elite.intel.ai.brain.inference.lmstudio.LMStudioClient.ModelListing;
import elite.intel.db.managers.LmStudioInstallManager;
import elite.intel.session.SystemSession;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * What the LMStudio Setup panel offers to do for this PC, if anything.
 * <p>
 * See {@code docs/LM_STUDIO_INSTALL_ASSISTANCE.md}, "What the button says".
 */
public enum LmStudioSetupOffer {

    /**
     * No button: the PC cannot run the game and the model together, LM Studio runs on another machine, or there is
     * nothing left to do.
     */
    NONE(null),

    /**
     * No LM Studio on this PC: install it, then download and load the model.
     */
    INSTALL("settings.ai.lmStudio.install"),

    /**
     * An LM Studio the app did not install: download and load the model and fill in the fields, nothing more.
     */
    SET_UP_EXISTING("settings.ai.lmStudio.setUp"),

    /**
     * The app's own install stopped part-way: resume it at the first unfinished step.
     */
    CONTINUE_SETUP("settings.ai.lmStudio.continue"),

    /**
     * The app's own install is finished: offer to remove it.
     */
    UNINSTALL("settings.ai.lmStudio.uninstall");

    private static final Set<String> THIS_PC_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");

    private final String captionKey;

    LmStudioSetupOffer(String captionKey) {
        this.captionKey = captionKey;
    }

    /**
     * The button's caption, as a {@code gui} bundle key; empty for {@link #NONE}.
     */
    public Optional<String> captionKey() {
        return Optional.ofNullable(captionKey);
    }

    /**
     * What to offer, from the saved settings and this PC. Slow - it reads the graphics cards and asks LM Studio for
     * its models - so call it off the event thread.
     */
    public static LmStudioSetupOffer forThisPc() {
        // WHY first, before the GPU and the Address: an install the app made can always be finished or removed, even
        // after the card was swapped or the commander pointed the Address somewhere else.
        Optional<LmStudioInstallManager.Install> owned = ownedInstall();
        if (owned.isPresent()) {
            return forOwnedInstall(owned.get());
        }
        SystemSession session = SystemSession.getInstance();
        String address = session.getLmStudioAddress();
        if (!GpuRequirement.isMetByThisPc() || !isOnThisPc(address)) {
            return NONE;
        }
        boolean installed = LmStudioHome.locate().installedLms().isPresent();
        return decide(installed, LMStudioClient.getInstance().listModels(), session.getLmStudioCommandModel());
    }

    /**
     * The install the app made on this PC, if its folder still holds that install's {@link InstallMarker}. A record
     * without a matching marker - restored from another PC, or left after the folder was deleted by hand - is
     * ignored here rather than forgotten: a drive that is only unplugged today must still be uninstallable tomorrow.
     */
    public static Optional<LmStudioInstallManager.Install> ownedInstall() {
        return LmStudioInstallManager.getInstance().find()
                .filter(install -> InstallMarker.matches(install.homeDir(), install.installId()));
    }

    static LmStudioSetupOffer forOwnedInstall(LmStudioInstallManager.Install owned) {
        return owned.setupComplete() ? UNINSTALL : CONTINUE_SETUP;
    }

    /**
     * What to offer on a PC that meets the GPU bar and keeps LM Studio on this machine.
     *
     * @param installed whether LM Studio is installed here ({@link LmStudioHome#installedLms()}). A home LM Studio
     *                  was removed from, with only its {@code lms} left, is not: the install script puts it back.
     */
    static LmStudioSetupOffer decide(boolean installed, ModelListing served, String configuredModel) {
        if (installed) {
            return isAlreadySetUp(served, configuredModel) ? NONE : SET_UP_EXISTING;
        }
        // WHY NONE when something answers without an lms here: that LM Studio is one the app cannot drive.
        return served instanceof ModelListing.NotAnswering ? INSTALL : NONE;
    }

    private static boolean isAlreadySetUp(ModelListing served, String configuredModel) {
        return configuredModel != null && !configuredModel.isBlank()
                && served instanceof ModelListing.Listed listed && listed.includes(configuredModel);
    }

    /**
     * Whether the Address names this PC. A blank one does: the panel fills in LM Studio's local default.
     */
    static boolean isOnThisPc(String address) {
        if (address == null || address.isBlank()) return true;
        try {
            String host = URI.create(address.strip()).getHost();
            return host != null && THIS_PC_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }
}
