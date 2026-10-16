package elite.intel.bindforge.devicefiles;

import elite.intel.db.managers.BindForgeInstallationsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Which installations missed a write the others took - what catch-up asks about when one is found again (Alan,
 * 2026-10-15, A14).
 * <p>
 * <strong>Stored, not derived.</strong> The check alone cannot tell an installation that missed an Apply from one
 * edited in the game: both differ from the master, and they need different questions.
 * <p>
 * <strong>Any write that misses an installation marks it.</strong> <strong>Only a push carrying everything Apply
 * carries clears it</strong> - Apply, first setup and catch-up. Revert pushes the master without the pending
 * removals, and onboarding writes one new element, so either can land on an installation that still lacks a CLEAR.
 */
public final class MissedWrites {

    private static final Logger log = LogManager.getLogger(MissedWrites.class);

    /** Where the flag is kept. */
    @FunctionalInterface
    interface Store {
        void set(long installId, boolean missed);
    }

    /** Records nothing - for the tests of the writes themselves. */
    static final MissedWrites NONE = new MissedWrites((installId, missed) -> {
    });

    private final Store store;

    MissedWrites(Store store) {
        this.store = store;
    }

    /** Kept on the installation's stored row. */
    public static MissedWrites stored() {
        return new MissedWrites(BindForgeInstallationsManager.getInstance()::setMissedWrite);
    }

    /**
     * The installations catch-up asks about: each missed a write and can be reached now - found again.
     */
    public static List<DeviceFilesPush.Target> foundAgain(List<DeviceFilesPush.Target> targets) {
        return targets.stream().filter(DeviceFilesPush.Target::missedWrite).filter(DeviceFilesPush.Target::isReachable)
                .toList();
    }

    /** The stored installations catch-up asks about, by id. */
    public static List<Long> storedFoundAgain() {
        return foundAgain(DeviceFilesPush.storedInstallations()).stream().map(DeviceFilesPush.Target::installId)
                .toList();
    }

    /**
     * A push's outcome, installation by installation.
     *
     * @param carriesEverything whether the push carried everything Apply does - the master, the pending removals and
     *                          the renames - so landing means the installation has caught up. {@code false} for
     *                          revert, which removes nothing
     */
    void afterPush(DeviceFilesPush.Report report, boolean carriesEverything) {
        for (DeviceFilesPush.InstallationResult result : report.installations()) {
            if (!result.outcome().landed()) {
                mark(result.target(), true);
            } else if (carriesEverything) {
                mark(result.target(), false);
            }
        }
    }

    /**
     * Onboarding's outcome. A miss marks the installation; landing clears nothing, as onboarding writes only the new
     * element and the installation may lack more.
     */
    void afterOnboarding(List<DeviceOnboarding.InstallationResult> results) {
        for (DeviceOnboarding.InstallationResult result : results) {
            if (!result.outcome().landed()) {
                mark(result.target(), true);
            }
        }
    }

    private void mark(DeviceFilesPush.Target target, boolean missed) {
        if (target.missedWrite() == missed) return;
        try {
            store.set(target.installId(), missed);
            log.info("{} ({}) {}", target.storefront(), target.controlSchemes(),
                    missed ? "missed a write - catch-up asks about it when it is found again" : "has caught up");
        } catch (RuntimeException e) {
            // WHY: broad on purpose. The write it reports on has already happened, and its report must still reach
            // the user; a lost flag costs one catch-up question, and APPLY stays lit for the installation anyway.
            log.warn("Could not record whether {} missed a write", target.storefront(), e);
        }
    }
}
