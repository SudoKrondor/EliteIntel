package elite.intel.ui.overlay;

import java.util.Optional;

/**
 * Projects one domain's state into a {@link HudObjective} for the HUD overlay.
 * <p>
 * This is the extension seam for the overlay's top section. Adding a new kind
 * of objective - exobiology sampling, a trade-route hop, a mining target, a
 * timer - means adding an implementation here and registering it with
 * {@code HudOverlayWindow}; no renderer change is needed, because the renderer
 * only understands {@link HudObjective} and {@link HudRow}.
 * <p>
 * Implementations are polled on a timer from the EDT, so they must be cheap and
 * must not block. Read already-loaded manager/session state; do not perform
 * network calls or heavy DB scans.
 * <p>
 * <b>Derive, never remember.</b> An objective must be recomputed from persisted
 * state on every call. The app is restarted mid-voyage all the time, and a card
 * that lives in a field vanishes when it is - the commander closes the app with
 * work on screen and reopens it to an empty overlay. Anything worth showing is
 * already in the database (missions, routes, scanned bodies, samples); read it
 * back rather than caching it here or subscribing to the event that produced it.
 */
public interface HudObjectiveSource {

    /**
     * The current objective for this domain, or empty when this source has
     * nothing to contribute (no active mission, not sampling, no route, ...).
     * <p>
     * Returning empty is the normal quiet state, not an error.
     */
    Optional<HudObjective> currentObjective();
}
