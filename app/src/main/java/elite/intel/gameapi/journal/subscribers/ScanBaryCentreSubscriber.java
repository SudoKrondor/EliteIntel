package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.ScanBaryCentreEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;

public class ScanBaryCentreSubscriber {

    private final LocationManager locationManager = LocationManager.getInstance();

    @Subscribe
    public void onScanBaryCentreEvent(ScanBaryCentreEvent event) {
        Thread.ofVirtual().start(() -> {
            LocationDto location = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBodyID());

            /// set galactic coordinates of the primary star
            LocationDto primaryStarLocation = locationManager.findBySystemAddress(event.getSystemAddress());
            location.setStarName(primaryStarLocation.getStarName());
            location.setX(primaryStarLocation.getX());
            location.setY(primaryStarLocation.getY());
            location.setZ(primaryStarLocation.getZ());

            location.setBodyId(event.getBodyID());
            location.setSystemAddress(event.getSystemAddress());
            location.setOrbitalPeriod(event.getOrbitalPeriod());
            locationManager.save(location);
        });
    }
}
