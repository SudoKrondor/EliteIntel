package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.SignalName;
import elite.intel.gameapi.journal.ScanBodyClassifier;
import elite.intel.gameapi.journal.events.FSSBodySignalsEvent;
import elite.intel.gameapi.journal.events.dto.FssSignalDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static elite.intel.util.StringUtls.localizedEventPlural;

public class FSSBodySignalsSubscriber {

    private static final Logger log = LogManager.getLogger(FSSBodySignalsSubscriber.class);
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    /**
     * What a signal report said about a body, for the announcement.
     */
    record SignalCounts(int bio, int geo) {
    }

    @Subscribe
    public void onFssBodySignal(FSSBodySignalsEvent event) {
        Thread.ofVirtual().start(() -> {
            LocationDto primaryStarLocation = locationManager.findBySystemAddress(event.getSystemAddress());
            LocationDto location = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBodyID());
            location.setX(primaryStarLocation.getX());
            location.setY(primaryStarLocation.getY());
            location.setZ(primaryStarLocation.getZ());

            // Not the primary star's name: that record exists only for a system we jumped into, and a record
            // with no system name is not saved at all - so in a system entered by carrier every FSS signal
            // report used to be written to nothing.
            SignalCounts counts = file(location, event, locationManager.findStarName(event.getSystemAddress()));
            if (counts == null) return;
            locationManager.save(location);

            if (playerSession.isDiscoveryAnnouncementOn()) {
                if (counts.bio() > 0)
                    VegaRuntime.narrator().announce(localizedEventPlural(counts.bio(), "event.fss.body.bioSignals"), false);
                if (counts.geo() > 0)
                    VegaRuntime.narrator().announce(localizedEventPlural(counts.geo(), "event.fss.body.geoSignals"), false);
            }
        });
    }

    /**
     * Writes the report onto the body's record: name, kind, the signal list and the bio and geo counts.
     * Shared with the pre-scan so a replayed report and a live one file the same thing, and the one place
     * that decides whether a report can be filed at all.
     *
     * @return the counts the report carried, or null when there is nothing to save: no signals, no body name
     * or ID to file them under, or no system name (which {@link LocationManager#save} would refuse
     * silently - six moons' bio signals once went missing that way, so it is said here).
     */
    static SignalCounts file(LocationDto location, FSSBodySignalsEvent event, String starName) {
        if (event.getBodyName() == null || event.getBodyID() == null) return null;
        if (starName == null || starName.isBlank()) {
            log.debug("FSS body signals for {} not filed: nothing names system {}", event.getBodyName(), event.getSystemAddress());
            return null;
        }
        location.setStarName(starName);
        location.setPlanetName(event.getBodyName());
        location.setSystemAddress(event.getSystemAddress());
        location.setBodyId(event.getBodyID());
        // A body the FSS resolved on an earlier visit gets no Scan event this time round, so this signal
        // report may be the only thing ever written about it: read the kind off the designation rather than
        // leave the row unclassified, or the bio count it carries never reaches a moon tally.
        location.setLocationType(ScanBodyClassifier.resolve(location));

        List<FSSBodySignalsEvent.Signal> signals = event.getSignals();
        if (signals == null || signals.isEmpty()) return null;

        location.setFssSignals(signals);
        int bioSignals = 0;
        int geoSignals = 0;
        for (FSSBodySignalsEvent.Signal s : signals) {
            FssSignalDto signal = new FssSignalDto();
            signal.setSignalName(event.getEvent());
            signal.setSignalType(SignalName.display(s.getTypeLocalised(), s.getType()));
            if ("$SAA_SignalType_Biological;".equalsIgnoreCase(s.getType())) {
                bioSignals = bioSignals + s.getCount();
            }
            if ("$SAA_SignalType_Geological;".equalsIgnoreCase(s.getType())) {
                geoSignals = geoSignals + s.getCount();
            }
            signal.setSystemAddress(event.getSystemAddress());
            location.addDetectedSignal(signal);
        }

        location.setBioSignals(bioSignals);
        location.setGeoSignals(geoSignals);
        return new SignalCounts(bioSignals, geoSignals);
    }
}
