package elite.intel.search.edsm.commodity;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.AudioBeepCueBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.search.edsm.EdsmApiClient;
import elite.intel.search.edsm.dto.MarketDto;
import elite.intel.search.edsm.dto.StationsDto;
import elite.intel.search.edsm.dto.data.Commodity;
import elite.intel.search.edsm.dto.data.Station;
import elite.intel.search.spansh.starsystems.StarSystemClient;
import elite.intel.search.spansh.starsystems.StationSearchResult;
import elite.intel.search.spansh.starsystems.SystemSearchCriteria;
import elite.intel.util.AudioPlayer;
import elite.intel.util.PlayBeepEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static elite.intel.util.StringUtls.localizedEventPlural;

public class EdsmCommoditySearch {

    private final static List<String> ALLOWED_STATION_TYPES = Arrays.asList(
            "Asteroid base",
            "Coriolis Starport",
            "Dodec Starport",
            "Mega ship",
            "Ocellus Starport",
            "Orbis Starport"
    );


    public static List<CommoditySearchResult> search(String commodityToFind, String refStarSystem, int maxDistance, int distanceFromArrival, int cargoHoldCapacity, boolean returnClosest) {
        StarSystemClient edsmClient = StarSystemClient.getInstance();
        SystemSearchCriteria spanshCriteria = new SystemSearchCriteria();
        SystemSearchCriteria.Filters filters = new SystemSearchCriteria.Filters();
        SystemSearchCriteria.Distance distance = new SystemSearchCriteria.Distance();
        distance.setMin(0);
        distance.setMax(maxDistance);

        ArrayList<SystemSearchCriteria.StationFilter> stationFilters = new ArrayList<>();
        SystemSearchCriteria.StationFilter stationFilter = new SystemSearchCriteria.StationFilter();
        SystemSearchCriteria.TypeFilter type = new SystemSearchCriteria.TypeFilter();
        type.setValue(ALLOWED_STATION_TYPES);
        stationFilter.setType(type);
        stationFilters.add(stationFilter);

        filters.setStations(stationFilters);
        filters.setDistance(distance);
        spanshCriteria.setFilters(filters);
        spanshCriteria.setReferenceSystem(refStarSystem);

        List<StationSearchResult.SystemResult> starSystems = edsmClient.searchStarSystems(spanshCriteria);
        final List<Station> stations = new ArrayList<>();

        for (StationSearchResult.SystemResult star : starSystems) {
            if (star.getStations() == null || star.getStations().isEmpty()) continue;
            StationsDto data = EdsmApiClient.searchStations(star.getName(), 1000);
            if (data.getData() != null) {
                List<Station> list = data.getData().getStations();
                if (list != null) {
                    for (Station station : list) {
                        if (station.getDistanceToArrivalInLightSeconds() > distanceFromArrival) continue;
                        if (!ALLOWED_STATION_TYPES.contains(station.getType())) continue;
                        station.setStarSystemName(star.getName());
                        station.setTransientDistance(star.getDistance());
                        stations.add(station);
                    }
                }
            }
        }

        GameEventBus.publish(new MissionCriticalAnnouncementEvent(localizedEventPlural(stations.size(), "event.search.commodity.marketsFound")));

        List<CommoditySearchResult> results = new ArrayList<>();
        for (Station station : stations) {
            if (station.getMarketId() == 0) continue;

            MarketDto market = EdsmApiClient.searchMarket(station.getMarketId(), station.getStarSystemName(), station.getName(), 1000);
            if (market.getData().getId() == 0) continue;

            List<Commodity> commodities = market.getData().getCommodities();
            if (commodities == null) continue;

            String stationName = station.getName();
            String starSystem = station.getStarSystemName();
            AudioBeepCueBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_2));

            for (Commodity entry : commodities) {
                if (commodityToFind.equalsIgnoreCase(entry.getName())) {
                    if (entry.getStock() >= cargoHoldCapacity && entry.getBuyPrice() > 0) {  // buyPrice is what player pays
                        CommoditySearchResult result = new CommoditySearchResult();
                        result.setCommodity(entry.getName());
                        result.setPrice(entry.getBuyPrice());
                        result.setStarSystem(starSystem);
                        result.setStationName(stationName);
                        result.setStationType(station.getType());
                        result.setDistanceFromPlayer(station.getTransientDistance());
                        results.add(result);
                        AudioBeepCueBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_3));
                    }
                }
            }
        }

        if (returnClosest) {
            results.sort(Comparator.comparing(CommoditySearchResult::getDistanceFromPlayer));
        } else {
            results.sort(Comparator.comparing(CommoditySearchResult::getPrice));
        }
        return results;
    }


}
