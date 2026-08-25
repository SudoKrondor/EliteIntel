package elite.intel.gameapi.colonisation;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * How old a construction site's stored manifest is.
 * <p>
 * Worth carrying separately from the manifest itself because a colonisation build is not a solo activity:
 * other commanders haul to the same depot, so the numbers we last read are a claim about the past. Stating
 * them as the present is how a commander sets off to buy six hundred tonnes of something that was finished
 * while they were away.
 */
public final class ManifestAge {

    private ManifestAge() {
    }

    /**
     * Whole hours since {@code visitedAt}, or {@code 0} when the timestamp is missing or unreadable.
     * <p>
     * Zero for an unreadable timestamp on purpose: it reads as "current", which is the answer that adds no
     * caveat to what is said. Inventing a large age from a parse failure would have the companion warn the
     * commander off perfectly fresh data.
     */
    public static long hoursSince(String visitedAt) {
        if (visitedAt == null || visitedAt.isBlank()) return 0;
        try {
            long hours = Duration.between(Instant.parse(visitedAt.trim()), Instant.now()).toHours();
            return Math.max(0, hours);
        } catch (DateTimeParseException e) {
            return 0;
        }
    }
}
