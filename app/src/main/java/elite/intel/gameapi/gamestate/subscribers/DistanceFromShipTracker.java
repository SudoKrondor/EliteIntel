package elite.intel.gameapi.gamestate.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.gamestate.status_events.PlayerMovedEvent;
import elite.intel.gameapi.journal.events.DockSRVEvent;
import elite.intel.gameapi.journal.events.LaunchSRVEvent;
import elite.intel.gameapi.journal.events.TouchdownEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.intel.util.NavigationUtils.calculateSurfaceDistance;
import static elite.intel.util.StringUtls.localizedEvent;


public class DistanceFromShipTracker {

    private static final Logger log = LogManager.getLogger(DistanceFromShipTracker.class);
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private double previousDistance = -1;
    private boolean announcedForCurrentEntry = false;

    @Subscribe
    public void onPlayerMovedEvent(PlayerMovedEvent event) {

        Status status = Status.getInstance();
        if (status.getStatus() == null) {
            return;
        }

        if (playerSession.isShipAutoDeparted()) return;

        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        if (currentLocation == null) {
            log.debug("Current location is null, skipping distance check.");
            return;
        }

        double[] landingCoordinates = currentLocation.getLandingCoordinates();
        if (landingCoordinates == null || landingCoordinates.length != 2) {
            //log.debug("Landing coordinates invalid, skipping distance check.");
            return;
        }

        double latitude = event.getLatitude();
        double longitude = event.getLongitude();
        double planetRadius = event.getPlanetRadius();
        double lzLat = landingCoordinates[0];
        double lzLon = landingCoordinates[1];

        double distance = calculateSurfaceDistance(latitude, longitude, lzLat, lzLon, planetRadius, 0);

        double innerDonut = 1800.0;
        double outerDonut = 2000.0;

        boolean movingAway = previousDistance >= 0 && distance > previousDistance;
        boolean isInDonut = distance >= innerDonut && distance <= outerDonut;

        if (isInDonut && movingAway && !announcedForCurrentEntry && !status.isInMainShip()) {
            GameEventBus.publish(
                    new MissionCriticalAnnouncementEvent(
                            localizedEvent("event.distance.shipProximity", Math.round(distance))
                    )
            );
            log.info("Alert triggered: Player moving away from ship at {} meters.", distance);
            announcedForCurrentEntry = true;
        }

        // Reset when leaving the donut in either direction, so the next entry arms again
        if (distance < innerDonut || distance > outerDonut) {
            announcedForCurrentEntry = false;
        }

        previousDistance = distance;
    }

    @Subscribe
    public void onTouchdownEvent(TouchdownEvent event) {
        previousDistance = -1;
        announcedForCurrentEntry = false;
        log.debug("State reset on touchdown.");
    }

    @Subscribe
    public void onDockSRV(DockSRVEvent event) {
        previousDistance = -1;
        announcedForCurrentEntry = false;
        log.debug("State reset on dock.");
    }

    @Subscribe
    public void onSrvLaunch(LaunchSRVEvent event) {
        previousDistance = -1;
        announcedForCurrentEntry = false;
        log.debug("State reset on srv launch.");
    }
}