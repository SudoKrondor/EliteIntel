package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.FuelScoop;
import elite.intel.gameapi.data.FsdTarget;
import elite.intel.gameapi.journal.events.FSDTargetEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.search.edsm.EdsmApiClient;
import elite.intel.gameapi.search.edsm.dto.DeathsDto;
import elite.intel.gameapi.search.edsm.dto.StarSystemDto;
import elite.intel.gameapi.search.edsm.dto.TrafficDto;
import elite.intel.session.PlayerSession;

@SuppressWarnings("unused")
public class FSDTargetSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Subscribe
    public void onFSDTargetEvent(FSDTargetEvent event) {
        Thread.ofVirtual().start(() -> {
            LocationDto locationDto = locationManager.findPrimaryStar(event.getSystemAddress()); // transient
            locationDto.setStarName(event.getName());
            locationDto.setStarClass(event.getStarClass());
            StarSystemDto systemDto = EdsmApiClient.searchStarSystem(event.getName(), 1);
            DeathsDto deathsDto = EdsmApiClient.searchDeaths(event.getName());
            TrafficDto trafficDto = EdsmApiClient.searchTraffic(event.getName());
            playerSession.setFsdTarget(new FsdTarget(event.getName(), event.getStarClass(), locationDto, systemDto, deathsDto, trafficDto, FuelScoop.starStatus(event.getStarClass())));
        });
    }
}
