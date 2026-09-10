package elite.intel.gameapi.signals;

import elite.intel.gameapi.missions.ResourceSiteProfile;

/**
 * Counts the resource extraction sites reported by one FSS sweep.
 * <p>
 * The game announces a system's sites as a burst of {@code FSSSignalDiscovered} events that all
 * carry the same system address and the same timestamp - one event per site, so three hazardous
 * sites arrive as three events. That burst is a sweep, and its counts are the system's inventory.
 * Counting across sweeps instead would report Sol as having forty low sites, because every arrival
 * re-announces the same five.
 * <p>
 * The tally restarts whenever a signal belongs to a different sweep than the last one, so a caller
 * feeds signals in and reads the running profile back without tracking boundaries itself.
 */
public final class ResourceSiteSweep {

    private String currentSweep;
    private int standard;
    private int low;
    private int high;
    private int hazardous;

    /**
     * Adds one site to the tally and returns what the sweep has reported so far.
     * <p>
     * WHY synchronized: the live subscriber handles each journal event on its own virtual thread, so
     * the events of one sweep arrive concurrently and in no particular order. Order does not matter
     * to a count, but losing an increment does.
     *
     * @param sweepKey identifies the sweep - the system address and the event timestamp together
     */
    public synchronized ResourceSiteProfile add(String sweepKey, ResourceSiteGrade grade) {
        if (!sweepKey.equals(currentSweep)) {
            currentSweep = sweepKey;
            standard = 0;
            low = 0;
            high = 0;
            hazardous = 0;
        }
        switch (grade) {
            case STANDARD -> standard++;
            case LOW -> low++;
            case HIGH -> high++;
            case HAZARDOUS -> hazardous++;
        }
        return new ResourceSiteProfile(standard, low, high, hazardous);
    }
}
