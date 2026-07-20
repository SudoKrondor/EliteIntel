package elite.intel.gameapi.search.spansh.findcarrier;

import elite.intel.db.dao.LocationDao;
import elite.intel.util.TimeUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class FleetCarrierSearch {

    private static FleetCarrierSearch instance;

    private FleetCarrierSearch() {
        // Private constructor to enforce singleton pattern
    }

    public static synchronized FleetCarrierSearch getInstance() {
        if (instance == null) {
            instance = new FleetCarrierSearch();
        }
        return instance;
    }


    public FleetCarrierSearchResultsDto findFleetCarrier(int range, CarrierAccess carrierAccess, LocationDao.Coordinates coords) {
        FleetCarrierSearchCriteriaDto criteria = new FleetCarrierSearchCriteriaDto();
        FleetCarrierSearchCriteriaDto.Filters filters = new FleetCarrierSearchCriteriaDto.Filters();

        FleetCarrierSearchCriteriaDto.Distance distance = new FleetCarrierSearchCriteriaDto.Distance();
        distance.setMax(range);
        distance.setMin(1); // excludes current star system
        filters.setDistance(distance);

        FleetCarrierSearchCriteriaDto.CarrierDockingAccess access = new FleetCarrierSearchCriteriaDto.CarrierDockingAccess();
        access.setValue(Arrays.asList(carrierAccess.getType()));
        filters.setCarrierDockingAccess(access);

        FleetCarrierSearchCriteriaDto.UpdatedAt updatedAt = new FleetCarrierSearchCriteriaDto.UpdatedAt();
        updatedAt.setComparison("<=>");

        LocalDateTime today = LocalDateTime.now();
        LocalDateTime yesterday = today.minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TimeUtils.ISO_INSTANT);
        updatedAt.setValue(Arrays.asList(yesterday.format(formatter), today.format(formatter)));

        filters.setUpdatedAt(updatedAt);

        FleetCarrierSearchCriteriaDto.ReferenceCoords referenceCoords = new FleetCarrierSearchCriteriaDto.ReferenceCoords();
        referenceCoords.setX(coords.x());
        referenceCoords.setY(coords.y());
        referenceCoords.setZ(coords.z());
        criteria.setReferenceCoords(referenceCoords);

        criteria.setSize(5);
        criteria.setPage(0);
        criteria.setFilters(filters);

        return FindFleetCarrierClient.getInstance().search(criteria);
    }
}