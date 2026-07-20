package elite.intel.gameapi.search.spansh.market;

public record MarketSearchCriteria(
        String referenceSystem,
        int minDistance,
        int maxDistance,
        String commodityName,
        boolean requireLargePad,
        Boolean requirePlanetary,
        boolean requireSupply,
        int minSupply,
        boolean wantToBuy,
        boolean orderByDistance
) {
}
