package elite.intel.gameapi.bookmarks;

import elite.intel.gameapi.journal.events.dto.TargetLocation;
import elite.intel.session.PlayerSession;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A surface bookmark the commander is flying to, waiting for its planet.
 * <p>
 * WHY surface guidance is not switched on at once: {@link TargetLocation} holds only a latitude and a
 * longitude, with no body, so guidance armed in another system would steer by the first planet the ship
 * happened to reach. The target waits here instead and is handed to guidance when the ship approaches the
 * one body it belongs to (see {@code BookmarkSurfaceTargetSubscriber}).
 * <p>
 * In memory only. It is dropped when the commander cancels navigation or plots a route somewhere else.
 */
public final class PendingSurfaceTarget {

    private static final PendingSurfaceTarget INSTANCE = new PendingSurfaceTarget();

    private final AtomicReference<LocationBookmark> pending = new AtomicReference<>();

    private PendingSurfaceTarget() {
    }

    public static PendingSurfaceTarget getInstance() {
        return INSTANCE;
    }

    public void arm(LocationBookmark bookmark) {
        if (!bookmark.hasCoordinates()) throw new IllegalArgumentException("Not a surface bookmark: " + bookmark);
        pending.set(bookmark);
    }

    public void clear() {
        pending.set(null);
    }

    public Optional<LocationBookmark> peek() {
        return Optional.ofNullable(pending.get());
    }

    /**
     * Takes the waiting target if it is on this body, leaving nothing behind; empty when nothing waits or it
     * waits for another body.
     */
    public Optional<LocationBookmark> takeIfOn(String starSystem, String bodyName) {
        LocationBookmark waiting = pending.get();
        if (waiting == null || !same(waiting.starSystem(), starSystem) || !same(waiting.planetName(), bodyName)) {
            return Optional.empty();
        }
        return pending.compareAndSet(waiting, null) ? Optional.of(waiting) : Optional.empty();
    }

    /**
     * Drops the waiting target when a route is plotted to anywhere but its system.
     */
    public void routePlottedTo(String destination) {
        LocationBookmark waiting = pending.get();
        if (waiting != null && !same(waiting.starSystem(), destination)) pending.compareAndSet(waiting, null);
    }

    /**
     * Switches surface guidance on for a bookmarked spot, the same way "navigate to coordinates" does.
     */
    public static void startGuidance(LocationBookmark bookmark) {
        PlayerSession playerSession = PlayerSession.getInstance();
        TargetLocation tracking = playerSession.getTracking();
        if (tracking == null) tracking = new TargetLocation(true);
        tracking.setEnabled(true);
        tracking.setLatitude(bookmark.latitude());
        tracking.setLongitude(bookmark.longitude());
        tracking.setRequestedTime(System.currentTimeMillis());
        playerSession.setTracking(tracking);
    }

    private static boolean same(String a, String b) {
        return a != null && b != null && a.trim().toLowerCase(Locale.ROOT).equals(b.trim().toLowerCase(Locale.ROOT));
    }
}
