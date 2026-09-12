package elite.intel.gameapi.missions;

import java.util.List;

/**
 * A mission provider system paired with the hunting ground its contracts send the commander to.
 * <p>
 * A pair is knowledge earned twice over: the commander took a pirate massacre contract in
 * {@code providerSystem} against {@code targetFaction} in {@code targetSystem}, and separately
 * saw resource extraction sites in that target system. Either half alone is not a pair - a
 * contract against a system with no sites is a contract with nowhere to fight it.
 *
 * @param providerFactions  every faction seen issuing against this target from this system. Its size
 *                          is the stack depth, and it is the number that matters: a second contract
 *                          from a faction already in the pile queues behind its first and adds its
 *                          full kill count, while one from a fresh faction runs alongside and may
 *                          cost nothing extra at all
 * @param stations          where those contracts were taken, so the commander knows which pads to
 *                          walk. A faction may issue from more than one
 * @param missionsCompleted contracts from this pairing the commander has actually finished, which is
 *                          what separates a pairing that works from one that was only ever tried
 * @param distanceLy        to the provider system, which is where the commander flies first
 */
public record MassacrePair(String providerSystem,
                           List<String> providerFactions,
                           List<String> stations,
                           String targetSystem,
                           String targetFaction,
                           ResourceSiteProfile sites,
                           int missionsCompleted,
                           double distanceLy) {

    public int stackDepth() {
        return providerFactions.size();
    }
}
