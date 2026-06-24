package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.SupercruiseExitEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;

@SuppressWarnings("unused")
public class SupercruiseExitedSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Subscribe
    public void onSupercruiseExited(SupercruiseExitEvent event) {
        Thread.ofVirtual().start(() -> {
            if (event.getBodyId() == null) return;
                    LocationDto here = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBodyId());
                    playerSession.setCurrentLocationId(event.getBodyId(), event.getSystemAddress());

                    here.setBodyType(event.getBodyType());
                    here.setBodyId(event.getBodyId());
                    here.setSystemAddress(event.getSystemAddress());
                    here.setStarName(playerSession.getPrimaryStarName());

                    LocationDto.LocationType locationType = LocationDto.determineType(event.getBodyType(), false);
                    if (LocationDto.LocationType.STATION == locationType) {
                        here.setStationName(event.getBody());
                    } else if (LocationDto.LocationType.PLANET == locationType || LocationDto.LocationType.MOON == locationType) {
                        here.setPlanetName(event.getBody());
                    } else if (LocationDto.LocationType.PLANETARY_RING == locationType) {
                        here.setStationName("Planetary Ring of " + here.getPlanetShortName());
                    }

                    if (here.getLocationType() == null || LocationDto.LocationType.UNCLASSIFIED == here.getLocationType()) {
                        here.setLocationType(locationType);
                    }
                }
        );
    }
}
