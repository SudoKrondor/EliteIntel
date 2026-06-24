package elite.intel.gameapi.gamestate.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.gamestate.status_events.PlayerMovedEvent;
import elite.intel.gameapi.journal.BioSampleDistanceCalculator;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;

import java.util.ArrayList;
import java.util.List;

public class BioSampleTrackingSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final Status status = Status.getInstance();

    @Subscribe
    public void onPlayerMovedEvent(PlayerMovedEvent event) {
        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        List<BioSampleDto> bioSamples = currentLocation.getPartialBioSamples();

        // If no samples are being tracked, don't announce.
        if (bioSamples == null || bioSamples.isEmpty()) {
            return;
        }

        // Compute previous aggregate state before updating flags.
        boolean wasFarEnough = bioSamples.stream().allMatch(BioSampleDto::isPlayerFarEnough);

        List<BioSampleDto> temp = new ArrayList<>();
        boolean isFarEnough = true;

        for (BioSampleDto bioSample : bioSamples) {
            boolean canTakeSample = BioSampleDistanceCalculator.isFarEnoughFromSample(
                    bioSample.getGenus(),
                    bioSample.getSpecies(),
                    bioSample.getScanLatitude(),
                    bioSample.getScanLongitude(),
                    status.getStatus().getLatitude(),
                    status.getStatus().getLongitude(),
                    status.getStatus().getPlanetRadius()
            );

            bioSample.setPlayerFarEnough(canTakeSample);
            temp.add(bioSample);

            if (!canTakeSample) {
                isFarEnough = false;
            }
        }

        currentLocation.setPartialBioSamples(temp);
        locationManager.save(currentLocation);

        // Announce only on state transition
        if (!status.isInSupercruise()) {
            boolean onFoot = status.isOnFoot();
            boolean inSrv = status.isInSrv();
            String shipName = playerSession.getShipLoadout().getShipName();
            String vehicle = inSrv ? "Surface Recon Vehicle" : (onFoot ? "You Are" : shipName);

            if (wasFarEnough != isFarEnough) {
                if (isFarEnough) {
                    GameEventBus.publish(new SensorDataEvent(vehicle + " is now far enough to take the new sample.", "State that minimum separation distance from the previous bio colony has been reached and a new sample can now be taken."));
                } else {
                    GameEventBus.publish(new SensorDataEvent(vehicle + " is moved too close to previous colony to take new sample. insufficient bio diversity.", "Warn that we have moved within minimum separation distance of the previous bio colony and a new sample cannot be taken at this position."));
                }
            }
        }
    }
}
