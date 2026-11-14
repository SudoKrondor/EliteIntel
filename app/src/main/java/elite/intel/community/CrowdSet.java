package elite.intel.community;

import java.util.List;

/**
 * One massacre set other commanders reported, as the nightly job checked it against the galaxy index: a provider
 * system (with the index's coordinates, never a reporter's), the provider factions the index still places there,
 * the stations reporters took contracts at, and the pirate faction in the target.
 *
 * @param stackDepth how many of the reported provider factions are still in the provider system
 * @param reporters  how many installs reported it, a measure of confidence
 * @param lastSeen   the latest day any reporter saw it offered, {@code YYYY-MM-DD}, or null
 */
public record CrowdSet(String providerSystem,
                       double x,
                       double y,
                       double z,
                       List<String> providerFactions,
                       List<String> stations,
                       String targetSystem,
                       String targetFaction,
                       int stackDepth,
                       int reporters,
                       String lastSeen) {
}
