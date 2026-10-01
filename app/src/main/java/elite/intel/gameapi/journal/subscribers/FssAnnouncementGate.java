package elite.intel.gameapi.journal.subscribers;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Whether an FSS signal announcement is worth saying, or was just said.
 * <p>
 * WHY: the FSS logs one line per instance of a signal, and identical instances log identical lines in the
 * same second - a jump into Colonia logged three {@code $Fixed_Event_Life_Cloud;} signals at once, and every
 * one of them was announced. Timed by the journal's own timestamps, not the wall clock, so a burst is a burst
 * however late its lines are read.
 */
final class FssAnnouncementGate {

    /**
     * For a fixed feature of the system, such as a notable stellar phenomenon: long enough to cover the burst
     * and a stay in the system; a return later in the session is a fresh arrival and is told again.
     */
    static final Duration SYSTEM_FEATURE = Duration.ofMinutes(10);

    /**
     * For a short-lived signal, such as salvage or a nonhuman source, keyed by the sentence itself: a system
     * can hold several real ones, and the minutes left and threat level in the sentence tell them apart, so
     * only the same sentence again is held back.
     */
    static final Duration SIGNAL_REPEAT = Duration.ofMinutes(1);

    private final Map<String, Instant> lastAnnounced = new HashMap<>();

    /**
     * Records the announcement and answers whether to make it. Synchronized because each signal is handled
     * on its own thread, so a burst arrives here all at once.
     *
     * @param key   what makes two announcements the same one
     * @param quiet how long the same one stays unsaid after it was said
     */
    synchronized boolean shouldAnnounce(String key, Instant at, Duration quiet) {
        Instant previous = lastAnnounced.get(key);
        if (previous != null && Duration.between(previous, at).abs().compareTo(quiet) < 0) {
            return false;
        }
        lastAnnounced.put(key, at);
        return true;
    }
}
