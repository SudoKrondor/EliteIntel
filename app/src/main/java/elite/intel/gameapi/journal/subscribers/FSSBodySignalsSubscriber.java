package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.DiscoveryAnnouncementEvent;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.FSSBodySignalsEvent;
import elite.intel.gameapi.journal.events.dto.FssSignalDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;

import java.util.List;

import static elite.intel.util.StringUtls.localizedEventPlural;

public class FSSBodySignalsSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Subscribe
    public void onFssBodySignal(FSSBodySignalsEvent event) {
        Thread.ofVirtual().start(() -> {
            LocationDto primaryStarLocation = locationManager.findPrimaryStar(playerSession.getPrimaryStarName());
            LocationDto location = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBodyID());

            location.setPlanetName(event.getBodyName());
            location.setStarName(primaryStarLocation.getStarName());
            location.setX(primaryStarLocation.getX());
            location.setY(primaryStarLocation.getY());
            location.setZ(primaryStarLocation.getZ());
            List<FSSBodySignalsEvent.Signal> signals = event.getSignals();
            if (signals == null || signals.isEmpty()) return;

            location.setFssSignals(signals);
            int bioSignals = 0;
            int geoSignals = 0;
            for (FSSBodySignalsEvent.Signal s : signals) {
                FssSignalDto signal = new FssSignalDto();
                signal.setSignalName(event.getEvent());
                signal.setSignalType(s.getTypeLocalised());
                if ("$SAA_SignalType_Biological;".equalsIgnoreCase(s.getType())) {
                    bioSignals = bioSignals + s.getCount();
                }
                if ("$SAA_SignalType_Geological;".equalsIgnoreCase(s.getType())) {
                    geoSignals = geoSignals + s.getCount();
                }
                signal.setSystemAddress(event.getSystemAddress());
                location.addDetectedSignal(signal);
                locationManager.save(location);
            }

            location.setBioSignals(bioSignals);
            location.setGeoSignals(geoSignals);
            locationManager.save(location);

            if (playerSession.isDiscoveryAnnouncementOn()) {
                if (bioSignals > 0)
                    EventBusManager.publish(new DiscoveryAnnouncementEvent(localizedEventPlural(bioSignals, "event.fss.body.bioSignals")));
                if (geoSignals > 0)
                    EventBusManager.publish(new DiscoveryAnnouncementEvent(localizedEventPlural(geoSignals, "event.fss.body.geoSignals")));
            }
        });
    }
}
