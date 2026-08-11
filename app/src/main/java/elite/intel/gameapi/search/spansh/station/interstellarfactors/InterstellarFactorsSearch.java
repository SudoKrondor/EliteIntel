package elite.intel.gameapi.search.spansh.station.interstellarfactors;

import elite.intel.gameapi.search.spansh.station.StationSearchClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class InterstellarFactorsSearch {

    public static final String INTERSTELLAR_FACTORS_CONTACT = "Interstellar Factors Contact";
    private static final Logger log = LogManager.getLogger(InterstellarFactorsSearch.class);

    /**
     * The nearest stations offering Interstellar Factors, or null when the search failed.
     *
     * @param requiresLargePad when true, only stations with a large pad are returned. The commander's ship
     *                         decides this, exactly as it does for a trade route: sending a Cutter to an
     *                         outpost it cannot dock at is a wasted trip, and the bounty stays unpaid.
     */
    public static List<InterstellarFactorsResultDto.Result> findNearestInterstellarFactors(
            double x, double y, double z, int maxDistanceLy, int maxDistanceToArrivalLs, boolean requiresLargePad
    ) {

        try {
            InterstellarFactorsSearchCriteria criteria =
                    buildCriteria(x, y, z, maxDistanceLy, maxDistanceToArrivalLs, requiresLargePad);
            StationSearchClient client = StationSearchClient.getInstance();
            InterstellarFactorsResultDto dto = client.searchInterstellarFactors(criteria);
            if (dto == null) return null;
            return dto.getResults();
        } catch (Exception e) {
            log.error("Failed to find Interstellar Factors Contact", e);
        }
        return null;
    }

    /**
     * Test seam: package-private so the request body can be asserted without a live Spansh call.
     */
    static InterstellarFactorsSearchCriteria buildCriteria(
            double x, double y, double z, int maxDistanceLy, int maxDistanceToArrivalLs, boolean requiresLargePad) {

        InterstellarFactorsSearchCriteria.Distance distance =
                new InterstellarFactorsSearchCriteria.Distance("0.00", String.format("%.2f", (double) maxDistanceLy));

        InterstellarFactorsSearchCriteria.DistanceToArrival distanceToArrival =
                new InterstellarFactorsSearchCriteria.DistanceToArrival("<=>", 0, maxDistanceToArrivalLs);

        InterstellarFactorsSearchCriteria.Service service =
                new InterstellarFactorsSearchCriteria.Service(List.of(INTERSTELLAR_FACTORS_CONTACT));

        InterstellarFactorsSearchCriteria.Filters filters = new InterstellarFactorsSearchCriteria.Filters();
        filters.setDistance(distance);
        filters.setDistanceToArrival(distanceToArrival);
        filters.setServices(List.of(service));
        if (requiresLargePad) {
            filters.setLargePads(InterstellarFactorsSearchCriteria.LargePads.atLeastOne());
        }

        InterstellarFactorsSearchCriteria criteria = new InterstellarFactorsSearchCriteria();
        criteria.setFilters(filters);
        criteria.setReferenceCoords(new InterstellarFactorsSearchCriteria.ReferenceCoords(x, y, z));
        return criteria;
    }
}
