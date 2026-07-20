package elite.intel.gameapi.search.spansh.neutronroute;

public record NeutronStarRouteCalculatorCriteria(
        String from,
        String to,
        int efficiency,
        double range,
        int superchargeMultiplier
) {
}
