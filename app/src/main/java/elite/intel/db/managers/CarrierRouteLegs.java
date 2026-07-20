package elite.intel.db.managers;

import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;

import java.util.ArrayList;
import java.util.List;

/**
 * The one rule every carrier route obeys: a route is the legs the carrier has <em>still to fly</em>.
 *
 * <p>"Enter next carrier destination" types leg 1 into the game's navigation field, so leg 1 must be
 * somewhere the carrier is not. A plotted route runs from the carrier's position to the destination,
 * which means the current system, and everything the carrier already flew past, is not part of it:
 *
 * <pre>
 *   plotted A B C D E, carrier at B  ->  C D E
 *   plotted A B C D E, carrier at A  ->  B C D E
 * </pre>
 *
 * <p>Truncating at the <em>last</em> occurrence rather than dropping matches by name is what makes
 * the guarantee hold: the passed prefix goes with it, and no surviving leg can name the current
 * system even if the route doubles back through it.
 */
public final class CarrierRouteLegs {

    private CarrierRouteLegs() {
        // static holder for a single rule.
    }

    /**
     * The legs still to fly, renumbered from 1 so that leg 1 is always the next jump.
     *
     * <p>A carrier sitting somewhere the route never mentions has flown off-route: nothing is
     * dropped, and the caller re-plots from where it now is.
     *
     * @param plottedLegs   the full plotted route, in leg order.
     * @param currentSystem the system the carrier is in; null or blank truncates nothing.
     */
    public static List<CarrierJump> stillToFly(List<CarrierJump> plottedLegs, String currentSystem) {
        int lastVisited = -1;
        for (int i = 0; i < plottedLegs.size(); i++) {
            if (isSameSystem(plottedLegs.get(i).getSystemName(), currentSystem)) {
                lastVisited = i;
            }
        }

        List<CarrierJump> remaining = new ArrayList<>(plottedLegs.size() - lastVisited - 1);
        int legNumber = 1;
        for (int i = lastVisited + 1; i < plottedLegs.size(); i++) {
            CarrierJump leg = plottedLegs.get(i);
            leg.setLeg(legNumber++);
            remaining.add(leg);
        }
        return remaining;
    }

    /**
     * WHY case- and whitespace-insensitive: Spansh and the journal are the two sources of a system
     * name and they are not guaranteed to agree on spacing or case. An exact match that misses reads
     * an arrival as off-route, leaves the arrival leg in the table, and re-plots for nothing.
     */
    public static boolean isSameSystem(String one, String other) {
        if (one == null || other == null) return false;
        return one.trim().equalsIgnoreCase(other.trim());
    }

    /**
     * The system name as the route table should match it: trimmed, never blank.
     */
    public static String normalise(String systemName) {
        if (systemName == null) return null;
        String trimmed = systemName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
