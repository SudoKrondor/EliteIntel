package elite.intel.ui.screen.bindings;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Whether the check at the BindForge click has run since Elite-Intel started, or since the bindings folder last changed
 * (storage.md rule 5). Set when a check starts; cleared only by a change in the folder (Storage S17) - so Not now waits
 * for a further change, never asks again at every click (Alan, 2026-11-11, S17 Q3). Safe on any thread.
 */
final class SessionCheck {

    private final AtomicBoolean checked = new AtomicBoolean();

    /** Whether a check has run since the start or the last change. */
    boolean checked() {
        return checked.get();
    }

    /**
     * Marks a check as started.
     *
     * @return {@code false} if one already had - the caller does not run it again
     */
    boolean start() {
        return checked.compareAndSet(false, true);
    }

    /** The bindings folder changed: the next click runs the check again. */
    void again() {
        checked.set(false);
    }
}
