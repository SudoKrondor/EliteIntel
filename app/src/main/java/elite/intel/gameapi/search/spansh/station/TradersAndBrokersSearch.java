package elite.intel.gameapi.search.spansh.station;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.search.spansh.station.traderandbroker.*;
import elite.intel.session.PlayerSession;
import elite.intel.util.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static elite.intel.util.StringUtls.localizedEvent;

public class TradersAndBrokersSearch {

    /**
     * How many candidates to ask Spansh for. Deliberately larger than the one result we use: every
     * station in the commander's own system is dropped before we pick (see
     * {@link #excludeCurrentSystem}) and a busy home system can account for several of them, then a
     * pledged commander skips past stations held by a rival power. The list has to survive both.
     */
    private static final int SEARCH_PAGE_SIZE = 10;

    private static TradersAndBrokersSearch instance;
    private static PlayerSession playerSession;

    private TradersAndBrokersSearch() {
    }

    public static synchronized TradersAndBrokersSearch getInstance() {
        if (instance == null) {
            instance = new TradersAndBrokersSearch();
            playerSession = PlayerSession.getInstance();
        }
        return instance;
    }


    public String location(TraderType traderType, BrokerType brokerType, Number maxDistance) {
        int distanceInLightYears = maxDistance == null ? 250 : maxDistance.intValue();
        LocationManager locationManager = LocationManager.getInstance();
        LocationDao.Coordinates galacticCoordinates = locationManager.getGalacticCoordinates();

        TraderAndBrokerSearchCriteria criteria = new TraderAndBrokerSearchCriteria();
        criteria.setSize(SEARCH_PAGE_SIZE);
        criteria.setPage(0);

        TraderAndBrokerSearchCriteria.ReferenceCoords coordinates = new TraderAndBrokerSearchCriteria.ReferenceCoords();
        if (galacticCoordinates == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(localizedEvent("event.search.noCoords")));
            return null;
        }

        coordinates.setX(galacticCoordinates.x());
        coordinates.setY(galacticCoordinates.y());
        coordinates.setZ(galacticCoordinates.z());

        criteria.setReferenceCoords(coordinates);
        criteria.setSort(new ArrayList<>());

        TraderAndBrokerSearchCriteria.Filters filters = new TraderAndBrokerSearchCriteria.Filters();
        TraderAndBrokerSearchCriteria.Distance distance = new TraderAndBrokerSearchCriteria.Distance();
        distance.setMin(0);
        distance.setMax(distanceInLightYears);
        filters.setDistance(distance);

        if (traderType != null) {
            TraderAndBrokerSearchCriteria.MaterialTrader trader = new TraderAndBrokerSearchCriteria.MaterialTrader();
            trader.setValue(
                    Collections.singletonList(
                            traderType.getType()
                    )
            );
            filters.setMaterialTrader(trader);
        } else if (brokerType != null) {
            TraderAndBrokerSearchCriteria.TechnologyBroker broker = new TraderAndBrokerSearchCriteria.TechnologyBroker();
            broker.setValue(
                    Collections.singletonList(
                            brokerType.getType()
                    )
            );
            filters.setTechnologyBroker(broker);
        }

        criteria.setFilters(filters);
        List<TraderAndBrokerSearchDto.Result> results = SearchForMaterialBrokerOrTrader.findMaterialTrader(criteria);

        if (results == null || results.isEmpty()) {
            GameEventBus.publish(new AiVoxResponseEvent(localizedEvent("event.search.noMatch")));
            return null;
        }

        results = CurrentSystemFilter.exclude(results, playerSession.getPrimaryStarName())
                .stream()
                .sorted(Comparator.comparingDouble(TraderAndBrokerSearchDto.Result::getDistance))
                .toList();

        if (results.isEmpty()) {
            GameEventBus.publish(new AiVoxResponseEvent(localizedEvent("event.search.noMatch")));
            return null;
        }

        TraderAndBrokerSearchDto.Result result = null;
        String pledgedPower = playerSession.getRankAndProgressDto().getPledgedToPower();
        if (pledgedPower == null || pledgedPower.isEmpty()) {
            result = results.getFirst();
        } else {
            for (TraderAndBrokerSearchDto.Result r : results) {
                if (r.getSystemControllingPower() == null) {
                    result = r;
                    break;
                }
                if (r.getSystemControllingPower() != null && r.getSystemControllingPower().equals(pledgedPower)) {
                    result = r;
                    break;
                }
            }
        }

        if (result == null) {
            // WHY: every candidate sits in rival-power space. Saying so beats the silent null this
            // used to return, which left the commander with no route and no explanation.
            GameEventBus.publish(new AiVoxResponseEvent(localizedEvent("event.search.noMatch")));
            return null;
        }

        GameEventBus.publish(
                new AiVoxResponseEvent(localizedEvent("event.search.headTo",
                        result.getSystemName(),
                        result.getStationName(),
                        TimeUtils.transformToYMDHtimeAgo(result.getUpdatedAt(), TimeUtils.LOCAL_DATE_TIME)))
        );
        ReminderManager.getInstance().setReminder(
                localizedEvent("event.search.reminder", result.getStationName()), result.getSystemName()
        );
        return result.getSystemName();
    }
}
