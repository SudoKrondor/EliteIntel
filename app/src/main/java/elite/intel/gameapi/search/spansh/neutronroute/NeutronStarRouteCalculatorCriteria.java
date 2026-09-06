package elite.intel.gameapi.search.spansh.neutronroute;

/**
 * One neutron-route request, as Spansh's {@code /api/route} takes it.
 *
 * @param from        the system to start from - the commander's current system
 * @param to          the destination system, as Elite's galaxy map wrote it to the clipboard
 * @param efficiency  1-100; lower trades extra jumps for a shorter total distance
 * @param range       the ship's laden jump range in light years
 * @param supercharge whether to plot through supercharged jumps. A yes or no here, not a number:
 *                    Spansh's form offers the two states only, and sends them as the two numbers
 *                    {@link NeutronStarRouteClient} maps them to.
 */
public record NeutronStarRouteCalculatorCriteria(
        String from,
        String to,
        int efficiency,
        double range,
        boolean supercharge
) {
}
