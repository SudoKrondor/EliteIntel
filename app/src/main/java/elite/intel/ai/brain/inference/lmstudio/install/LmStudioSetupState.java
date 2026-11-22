package elite.intel.ai.brain.inference.lmstudio.install;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Whether the LM Studio the app talks to is still being set up: a setup is running now, or the app's own install
 * stopped part-way and its model never passed the setup's test.
 * <p>
 * WHY the AI connection check asks: a model that answers one tiny request in the middle of a setup is not a working
 * VEGA. A Windows tester's first load ran past the load step's limit, the background check's next request got a
 * two-word reply, and the app said "Connection successful" seconds after the setup window said it had failed - while
 * the next commander turns timed out (2026-11-10).
 */
public final class LmStudioSetupState {

    // WHY a count, not a flag: nothing stops a second setup from starting while one is still running, and the end of
    // either one alone must not report the other as finished.
    private static final AtomicInteger RUNNING = new AtomicInteger();

    private LmStudioSetupState() {
    }

    static void began() {
        RUNNING.incrementAndGet();
    }

    static void ended() {
        RUNNING.decrementAndGet();
    }

    /**
     * Whether a setup is running, or the LM Studio at {@code address} is the app's own install with its setup
     * unfinished. Reads the install record and its marker, so call it off the event thread.
     */
    public static boolean isUnfinished(String address) {
        if (RUNNING.get() > 0) return true;
        return LmStudioSetupOffer.isOnThisPc(address)
                && LmStudioSetupOffer.ownedInstall().map(owned -> !owned.setupComplete()).orElse(false);
    }
}
