package elite.intel.gameapi.journal.subscribers;

import elite.intel.gameapi.journal.events.CommitCrimeEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Which crimes are worth interrupting the commander for.
 *
 * <p>WHY: the game logs a crime per victim, and a single on-foot assassination contract is not one crime.
 * The reported run took a settlement apart and produced eighteen murders in twelve minutes, each of which
 * stopped whatever the companion was saying to report the same faction, the same offence and the same
 * thousand-credit bounty. The commander does not learn anything from the seventeenth; he learns it from
 * the first, and everything after that is noise on a channel reserved for things that matter.
 *
 * <p>So the first crime of its kind speaks and the rest of the spree stays quiet. The window slides: every
 * further offence pushes the silence out again, and the alert re-arms only once the commander has gone
 * {@link #QUIET_PERIOD} without committing that crime again - which is what makes a second settlement a
 * second alert rather than a continuation of the first.
 *
 * <p>Kept apart from {@link CrimeReportPhrase} on purpose: that class decides how a crime is worded, this
 * one decides whether it is spoken at all, and the two questions have no bearing on each other.
 */
final class CrimeAlertGate {

    /**
     * How long the commander must go without repeating a crime before it is announced again.
     *
     * <p>Three minutes because that is what the reported spree needed: its longest gap between two murders
     * was one minute fifty-two, twice, while the commander crossed the settlement looking for the next
     * target. A shorter window would have split one spree into three alerts and reported nothing new twice.
     */
    static final Duration QUIET_PERIOD = Duration.ofMinutes(3);

    /**
     * When each kind of crime was last committed. Keyed by faction as well as crime type: two factions
     * putting a price on the commander's head are two pieces of news, however alike the offences are.
     */
    private final Map<String, Instant> lastCommitted = new HashMap<>();

    /**
     * Whether this crime should be announced. Called on the bus thread, before the narration is handed to a
     * thread of its own, so that two crimes a second apart cannot both pass the gate.
     */
    synchronized boolean admit(CommitCrimeEvent event) {
        // The journal's own clock, not ours: it is the one that measures the spree, and it stays right when
        // the app is busy. Parsing is safe here because the parser already read this timestamp upstream to
        // decide the event was not a replay.
        Instant committed = Instant.parse(event.getTimestamp());
        lastCommitted.values().removeIf(previous -> hasLapsed(previous, committed));
        return lastCommitted.put(spree(event), committed) == null;
    }

    private static boolean hasLapsed(Instant previous, Instant now) {
        return Duration.between(previous, now).compareTo(QUIET_PERIOD) > 0;
    }

    private static String spree(CommitCrimeEvent event) {
        return event.getFaction() + " " + event.getCrimeType();
    }
}
