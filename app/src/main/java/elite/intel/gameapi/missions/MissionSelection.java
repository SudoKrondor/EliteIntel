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
     * Soonest expiry first: the order a stack has to be worked in, whatever order it was accepted in.
     * A mission running out in two hours is the one that matters, not the one taken last.
     * <p>
     * Journal expiry timestamps are fixed-width UTC ISO-8601, so comparing them as strings is comparing
     * them as instants. Missions that never expire carry no {@code Expiry} at all and sort LAST rather
     * than first, which is what an empty string would otherwise do. Anything still tied - two missions
     * off the same board share an expiry to the second, and a stack stored before expiry was recorded
     * has none at all - falls through to {@link #GAME_LIST_ORDER}, so the card keeps agreeing with the
     * game's own list instead of resolving on a mission ID.
     */
    public static final Comparator<MissionDto> EXPIRY_ORDER =
            Comparator.comparing((MissionDto mission) -> expiry(mission).isEmpty())
                    .thenComparing(MissionSelection::expiry)
                    .thenComparing(GAME_LIST_ORDER);

    /**
     * The mission the app means by "the mission" when it has to name one: the HUD's card, and the companion's
     * mission fact both read it from here, so the card and the spoken answer cannot name different missions.
     * <p>
     * Two rules, in order:
     * <ol>
     *   <li><b>The plotted route's destination.</b> A plotted route is the commander stating where they are
     *       going, and it beats every guess this method could make. A mission whose destination system is the
     *       end of the route is the one being flown.</li>
     *   <li><b>{@link #EXPIRY_ORDER}</b> when no route is plotted, or when the route ends somewhere no mission
     *       does: with nothing stating where the ship is going, the mission that matters is the one running out
     *       first.</li>
     * </ol>
     * Ties inside either rule fall through to {@link #GAME_LIST_ORDER}, so the answer never depends on map
     * iteration order.
     *
     * @param missions         the active stack, already free of nulls
     * @param routeDestination the end of the plotted route, or null when none is plotted
     */
    public static Optional<MissionDto> featured(List<MissionDto> missions, String routeDestination) {
        List<MissionDto> ordered = missions.stream().sorted(EXPIRY_ORDER).toList();
        if (ordered.isEmpty()) {
            return Optional.empty();
        }
        return firstBoundFor(ordered, routeDestination).or(() -> Optional.of(ordered.getFirst()));
    }

    /**
     * The soonest-expiring mission heading for {@code system}, so that a system holding several of them always
     * resolves to the same one.
     */
    private static Optional<MissionDto> firstBoundFor(List<MissionDto> ordered, String system) {
        if (system == null || system.isBlank()) {
            return Optional.empty();
        }
        return ordered.stream()
                .filter(mission -> system.equalsIgnoreCase(mission.getDestinationSystem()))
                .findFirst();
    }

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

    /**
     * Empty rather than null so the comparator can sort on it; an unset expiry sorts last under
     * {@link #EXPIRY_ORDER}.
     */
    private static String expiry(MissionDto mission) {
        return mission.getExpiry() == null ? "" : mission.getExpiry().trim();
    }
}
