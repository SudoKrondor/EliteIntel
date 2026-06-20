package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.TouchdownEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;

import static elite.intel.util.StringUtls.localizedEvent;

@SuppressWarnings("unused")
public class TouchdownEventSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();


    @Subscribe
    public void onTouchdownEvent(TouchdownEvent event) {
        Thread.ofVirtual().start(() -> {
            String body = event.getBody();
            Double latitude = event.getLatitude();
            Double longitude = event.getLongitude();
            String pointOfInterest = event.getNearestDestinationLocalised();
            String starSystem = event.getStarSystem();
            boolean isStation = event.isOnStation();
            boolean isOnPlanet = event.isOnPlanet();
            String locationType = isStation ? localizedEvent("event.touchdown.station") : isOnPlanet ? localizedEvent("event.touchdown.planet") : null;

            StringBuilder sb = new StringBuilder();
            sb.append(localizedEvent("event.touchdown.header")).append(": ");
            sb.append(" ");
            sb.append(event.isPlayerControlled() ? localizedEvent("event.touchdown.manual") : localizedEvent("event.touchdown.unmanned"));
            sb.append(" ");
            if (isStation) sb.append(" ").append(localizedEvent("event.touchdown.onStation")).append(" ");
            if (isStation)
                sb.append(" ").append(localizedEvent("event.touchdown.on")).append(" ").append(event.getBody()).append(". ").append(localizedEvent("event.touchdown.nearestDest")).append(": ").append(pointOfInterest).append(".");

            sb.append(" ");
            sb.append(locationType == null ? localizedEvent("event.touchdown.unknown") : locationType);
            sb.append(" ");
            sb.append(body);
            sb.append(" ");
            sb.append(localizedEvent("event.touchdown.latitude")).append(": ");
            sb.append(latitude);
            sb.append(" ");
            sb.append(localizedEvent("event.touchdown.longitude")).append(": ");
            sb.append(longitude);
            sb.append(" ");
            sb.append(localizedEvent("event.touchdown.pointOfInterest")).append(": ");
            sb.append(pointOfInterest);


            LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
            currentLocation.setLandingCoordinates(new double[]{event.getLatitude(), event.getLongitude()});
            locationManager.save(currentLocation);

            if (pointOfInterest != null && !pointOfInterest.isEmpty()) {
                GameEventBus.publish(new SensorDataEvent(sb.toString(), "Confirm touchdown. State the point of interest we have landed at."));
            } else {
                GameEventBus.publish(new MissionCriticalAnnouncementEvent(localizedEvent("event.touchdown")));
            }
            playerSession.setShipAutoDeparted(false);
        });
    }
}
