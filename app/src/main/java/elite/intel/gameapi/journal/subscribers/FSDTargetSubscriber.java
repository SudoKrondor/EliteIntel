package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.data.FsdTarget;
import elite.intel.gameapi.journal.events.FSDTargetEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.search.edsm.EdsmApiClient;
import elite.intel.search.edsm.dto.DeathsDto;
import elite.intel.search.edsm.dto.StarSystemDto;
import elite.intel.search.edsm.dto.TrafficDto;
import elite.intel.session.PlayerSession;

import static elite.intel.util.StringUtls.localizedEvent;

@SuppressWarnings("unused")
public class FSDTargetSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Subscribe
    public void onFSDTargetEvent(FSDTargetEvent event) {
        Thread.ofVirtual().start(() -> {
            LocationDto locationDto = locationManager.findBySystemAddress(event.getSystemAddress());
            StarSystemDto systemDto = EdsmApiClient.searchStarSystem(event.getName(), 1);
            DeathsDto deathsDto = EdsmApiClient.searchDeaths(event.getName());
            TrafficDto trafficDto = EdsmApiClient.searchTraffic(event.getName());
            playerSession.setFsdTarget(new FsdTarget(event.getName(), event.getStarClass(), locationDto, systemDto, deathsDto, trafficDto, isFuelStarClause(event.getStarClass())));
        });
    }

    private String isFuelStarClause(String starClass) {
        if (starClass == null) return " unknown ";
        boolean isFuelStar = "KGBFOAM".toUpperCase().contains(starClass.toUpperCase());
        return isFuelStar ? localizedEvent("event.jump.refuelPossible", playerSession.getVariablePlayerName()) : localizedEvent("event.jump.noFuelAvailable", playerSession.getVariablePlayerName());
    }
}
