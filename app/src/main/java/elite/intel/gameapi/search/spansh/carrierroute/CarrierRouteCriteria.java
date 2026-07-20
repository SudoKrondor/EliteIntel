package elite.intel.gameapi.search.spansh.carrierroute;

/**
 * Record for carrier route search criteria.
 */
public record CarrierRouteCriteria(
        String sourceSystem,
        String destinationSystem,
        int capacity,
        int capacityUsed,
        int startingFuel
) {}
