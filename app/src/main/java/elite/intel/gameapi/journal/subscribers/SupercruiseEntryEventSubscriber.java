package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.SupercruiseEntryEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SupercruiseEntryEventSubscriber {

    private final Logger log = LoggerFactory.getLogger(SupercruiseEntryEventSubscriber.class);
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Subscribe
    public void onSuperCruiseEntryEvent(SupercruiseEntryEvent event) {
        Thread.ofVirtual().start(() -> {
            LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
            currentLocation.setStarName(locationManager.findBySystemAddress(playerSession.getLocationData().getSystemAddress()).getStarName());
            currentLocation.setStationFaction(null);
            currentLocation.setStationName(null);
            currentLocation.setStationServices(new ArrayList<>());
            currentLocation.setStationAllegiance(null);
            currentLocation.setStationEconomy(null);
            currentLocation.setStationGovernment(null);
            currentLocation.setStationType(null);
            locationManager.save(currentLocation);
        });
    }
}
