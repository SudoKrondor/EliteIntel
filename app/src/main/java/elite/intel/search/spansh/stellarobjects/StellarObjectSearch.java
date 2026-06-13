package elite.intel.search.spansh.stellarobjects;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.dao.LocationDao;
import elite.intel.gameapi.EventBusManager;

import java.util.Arrays;

import static elite.intel.util.StringUtls.localizedEvent;

public class StellarObjectSearch {

    private static StellarObjectSearch instance;

    private StellarObjectSearch() {
    }

    public static StellarObjectSearch getInstance() {
        if (instance == null) {
            instance = new StellarObjectSearch();
        }
        return instance;
    }


    public StellarObjectSearchResultDto findRings(String material, ReserveLevel level, LocationDao.Coordinates coords, int range) {

            StellarObjectSearchClient client = StellarObjectSearchClient.getInstance();
            StellarObjectSearchRequestDto criteria = new StellarObjectSearchRequestDto();

            StellarObjectSearchRequestDto.ReserveLevel reserveLevel = new StellarObjectSearchRequestDto.ReserveLevel();
            reserveLevel.setValue(Arrays.asList(level.getType()));

            StellarObjectSearchRequestDto.Filters filters = new StellarObjectSearchRequestDto.Filters();
            filters.setReserveLevel(reserveLevel);
            StellarObjectSearchRequestDto.Distance distance = new StellarObjectSearchRequestDto.Distance();
            distance.setMin(0);
            distance.setMax(range);
            filters.setDistance(distance);

            StellarObjectSearchRequestDto.RingSignal ringSignal = new StellarObjectSearchRequestDto.RingSignal();
            ringSignal.setCount(Arrays.asList(1, 17));
            ringSignal.setComparison("<=>");
            ringSignal.setName(material);

            filters.setRingSignals(Arrays.asList(ringSignal));


            criteria.setSize(10);
        criteria.setPage(0);
            criteria.setFilters(filters);

            StellarObjectSearchRequestDto.ReferenceCoords referenceCoords = new StellarObjectSearchRequestDto.ReferenceCoords();
            if(coords == null) {
                EventBusManager.publish(new AiVoxResponseEvent(localizedEvent("event.search.stellar.noCoords")));
                return null;
            }
            referenceCoords.setX(coords.x());
            referenceCoords.setY(coords.y());
            referenceCoords.setZ(coords.z());
            criteria.setReferenceCoords(referenceCoords);

            return client.search(criteria);
    }
}