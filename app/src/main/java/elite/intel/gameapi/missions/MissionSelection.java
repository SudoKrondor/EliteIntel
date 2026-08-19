package elite.intel.gameapi.missions;

import elite.intel.gameapi.journal.events.dto.MissionDto;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Which mission the app means when the commander has several.
 * <p>
 * A board is taken in stacks, so "the mission" is never obvious: the HUD has to name one on a card and
 * the navigate command has to plot a route to one, and the two disagreeing is a bug the commander sees
 * as the app arguing with itself. Both read the ordering from here.
 */
public final class MissionSelection {

    private MissionSelection() {
    }

    /**
     * The order the game's transactions panel appears to list missions in: newest accepted first.
     * <p>
     * The journal never states this order. {@code Missions} reports the active set at login and nothing
     * else reports it at all, so it is inferred from observation - with a stack of ten couriers accepted
     * over seven minutes, the game's top entry was the last one accepted. It is deliberately one
     * comparator so that a counter-observation is a one-line correction here rather than a rewrite.
     * <p>
     * Journal timestamps are fixed-width UTC ISO-8601, so comparing them as strings is comparing them as
     * instants. Mission IDs rise over time too and break ties - and carry missions with no accepted time,
     * which sort last.
     */
    public static final Comparator<MissionDto> GAME_LIST_ORDER =
            Comparator.comparing(MissionSelection::acceptedAt, Comparator.reverseOrder())
                    .thenComparing(MissionDto::getMissionId, Comparator.reverseOrder());

    /**
     * Whether a route can be plotted to this mission at all.
     * <p>
     * Plenty of missions have nowhere to fly to. A donation is completed at the board it was taken
     * from, so the journal gives it no {@code DestinationSystem} at all - and a stack of them is
     * normal, because they are how a commander buys reputation without leaving the station. Anything
     * that picks a mission to fly to has to step over them rather than hand a null to the galaxy map.
     */
    public static boolean hasDestination(MissionDto mission) {
        String system = mission.getDestinationSystem();
        return system != null && !system.isBlank();
    }

    /**
     * The mission to plot a route to, or empty when none of them has a destination to plot to.
     * <p>
     * Missions ending where the ship already is sort last rather than being dropped: the destination is
     * real and the commander may well have meant it, but choosing one over a mission that is actually
     * somewhere else would plot a route to the system the ship is sitting in - which the galaxy map
     * accepts and which does nothing at all.
     *
     * @param missions      the candidates, already free of nulls
     * @param currentSystem where the ship is now, or null when it is not known yet
     */
    public static Optional<MissionDto> toPlotFor(List<MissionDto> missions, String currentSystem) {
        return missions.stream()
                .filter(Objects::nonNull)
                .filter(MissionSelection::hasDestination)
                .min(Comparator.comparing((MissionDto mission) -> isIn(mission, currentSystem))
                        .thenComparing(GAME_LIST_ORDER));
    }

    private static boolean isIn(MissionDto mission, String system) {
        return system != null && system.equalsIgnoreCase(mission.getDestinationSystem());
    }

    /**
     * Empty rather than null so the comparator can sort on it; an unset accepted time sorts last under
     * {@link #GAME_LIST_ORDER}.
     */
    private static String acceptedAt(MissionDto mission) {
        return mission.getAcceptedAt() == null ? "" : mission.getAcceptedAt();
    }
}
