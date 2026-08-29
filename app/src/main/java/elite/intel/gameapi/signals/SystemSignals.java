package elite.intel.gameapi.signals;

import elite.intel.gameapi.journal.events.dto.FssSignalDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;

import java.util.*;

/**
 * Every signal the FSS has found in one system, each counted once.
 *
 * <p><b>Why this is not simply the union of the rows.</b> A system is stored as many {@link LocationDto} rows -
 * one per body, station and depot - and an {@code FSSSignalDiscovered} is filed against whichever of them was
 * current when the FSS reported it. Honk a system on arrival, fly to a body, honk again, and the same signal is
 * now on two rows. Each row's own {@code detectedSignals} is a {@link Set}, so it never holds a signal twice -
 * but nothing was collapsing them ACROSS rows, and every reader was walking the rows and counting what it found.
 *
 * <p>That is invisible for a signal reported by name, because a name said twice still reads as one place. It is
 * not invisible for the one thing that is reported as a number: measured live in Hyades Sector MH-V c2-8, a
 * commander with a single carrier parked in an otherwise empty system was told there were "two listed Fleet
 * Carrier stations named GHY-L8X". A callsign is unique galaxy-wide, so two of one carrier is not a thing that
 * can exist, and the commander was right to disbelieve it.
 *
 * <p>The identity used is {@link FssSignalDto}'s own equality, which is what each row already dedupes on. This
 * only widens that existing rule from one row to the system, so nothing a single row would have merged is kept
 * apart here, and nothing it would have kept apart is merged.
 */
public final class SystemSignals {

    /**
     * The name {@code FSSBodySignalsSubscriber} files its per-body counts under. It is the event's name rather
     * than a signal name, because a body-signal record has none of its own - which is exactly what marks it as
     * belonging to its body. Two bodies each reporting biological signals produce records that are equal in
     * every field, and they are two findings, not one seen twice. Only the system-wide {@code
     * FSSSignalDiscovered} signals are the ones scattered across rows, so only those are collapsed.
     */
    private static final String BODY_SIGNAL_MARKER = "FSSBodySignals";

    private SystemSignals() {
    }

    /**
     * One signal, and the body whose record it was filed against.
     *
     * @param recordedAgainst the body name on that row - which is where the app was, NOT where the signal is.
     *                        An {@code FSSSignalDiscovered} carries no body of its own, so a reader that shows
     *                        this is showing provenance rather than a location
     */
    public record Sighting(FssSignalDto signal, String recordedAgainst) {
    }

    /**
     * The system's signals with the cross-row duplicates collapsed, in the order first met. Where the same signal
     * sits on several rows the first row's provenance is kept, the rest being the same signal met again.
     */
    public static List<Sighting> distinct(Collection<LocationDto> locations) {
        List<Sighting> sightings = new ArrayList<>();
        if (locations == null) {
            return sightings;
        }
        Set<Key> seen = new LinkedHashSet<>();
        for (LocationDto location : locations) {
            if (location == null || location.getDetectedSignals() == null) {
                continue;
            }
            String body = location.getPlanetName();
            for (FssSignalDto signal : location.getDetectedSignals()) {
                if (seen.add(new Key(signal, isBodyScoped(signal) ? body : null))) {
                    sightings.add(new Sighting(signal, body));
                }
            }
        }
        return sightings;
    }

    /**
     * True for a record that belongs to the body it was filed against rather than to the system.
     */
    private static boolean isBodyScoped(FssSignalDto signal) {
        return BODY_SIGNAL_MARKER.equals(signal.getSignalName());
    }

    /**
     * What counts as the same finding: the signal, and for a body-scoped record the body it was found on.
     */
    private record Key(FssSignalDto signal, String body) {
    }
}
