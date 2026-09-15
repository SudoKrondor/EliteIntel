package elite.intel.gameapi.signals;

import java.util.HashSet;
import java.util.Set;

/**
 * Counts the conflict zones reported by one FSS sweep.
 * <p>
 * The twin of {@link ResourceSiteSweep}, with one difference: the same zone can be listed twice in
 * a single sweep (the journal shows {@code #index=3} arriving twice at one timestamp), so zones are
 * counted by identity - intensity and index - rather than by event.
 * <p>
 * The tally restarts whenever a signal belongs to a different sweep than the last one, so a caller
 * feeds signals in and reads the running profile back without tracking boundaries itself.
 */
public final class ConflictZoneSweep {

    private String currentSweep;
    private final Set<String> seen = new HashSet<>();
    private int low;
    private int medium;
    private int high;
    private int powerplay;

    /**
     * Adds one zone to the tally and returns what the sweep has reported so far.
     * <p>
     * WHY synchronized: the live subscriber handles each journal event on its own virtual thread, so
     * the events of one sweep arrive concurrently and in no particular order. Order does not matter
     * to a count, but losing an increment does.
     *
     * @param sweepKey identifies the sweep - the system address and the event timestamp together
     */
    public synchronized ConflictZoneProfile add(String sweepKey, ConflictZoneSignal signal) {
        if (!sweepKey.equals(currentSweep)) {
            currentSweep = sweepKey;
            seen.clear();
            low = 0;
            medium = 0;
            high = 0;
            powerplay = 0;
        }
        if (seen.add(signal.key())) {
            switch (signal.intensity()) {
                case LOW -> low++;
                case MEDIUM -> medium++;
                case HIGH -> high++;
                case POWERPLAY -> powerplay++;
            }
        }
        return new ConflictZoneProfile(low, medium, high, powerplay);
    }
}
