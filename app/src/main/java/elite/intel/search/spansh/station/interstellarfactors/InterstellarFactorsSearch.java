package elite.intel.search.spansh.station.interstellarfactors;

import elite.intel.search.spansh.station.StationSearchClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class InterstellarFactorsSearch {

    public static final String INTERSTELLAR_FACTORS_CONTACT = "Interstellar Factors Contact";
    private static final Logger log = LogManager.getLogger(InterstellarFactorsSearch.class);

    public static List<InterstellarFactorsResultDto.Result> findNearestInterstellarFactors(
            double x, double y, double z, int maxDistanceLy, int maxDistanceToArrivalLs
    ) {

        try {
            InterstellarFactorsSearchCriteria criteria = buildCriteria(x, y, z, maxDistanceLy, maxDistanceToArrivalLs);
            StationSearchClient client = StationSearchClient.getInstance();
            InterstellarFactorsResultDto dto = client.searchInterstellarFactors(criteria);
            if (dto == null) return null;
            return dto.getResults();
        } catch (Exception e) {
            log.error("Failed to find Interstellar Factors Contact", e);
        }
        return null;
    }

    private static InterstellarFactorsSearchCriteria buildCriteria(
            double x, double y, double z, int maxDistanceLy, int maxDistanceToArrivalLs) {

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

        InterstellarFactorsSearchCriteria criteria = new InterstellarFactorsSearchCriteria();
        criteria.setFilters(filters);
        criteria.setReferenceCoords(new InterstellarFactorsSearchCriteria.ReferenceCoords(x, y, z));
        return criteria;
    }
}
