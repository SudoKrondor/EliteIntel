package elite.intel.search.spansh.neutronroute;

public record NeutronStarRouteCalculatorCriteria(
        String from,
        String to,
        int efficiency,
        double range,
        int superchargeMultiplier
) {
}
