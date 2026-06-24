package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.DiscoveryAnnouncementEvent;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.FSSSignalDiscoveredEvent;
import elite.intel.gameapi.journal.events.dto.FssSignalDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.util.TimeUtils;

import static elite.intel.util.StringUtls.localizedEvent;

@SuppressWarnings("unused")
public class FSSSignalDiscoveredSubscriber {

    private static final String USS_TYPE_SALVAGE = "$USS_Type_Salvage";
    private static final String USS_TYPE_VALUABLE_SALVAGE = "$USS_Type_ValuableSalvage";
    private static final String USS_TYPE_VERY_VALUABLE_SALVAGE = "$USS_Type_VeryValuableSalvage";

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final HuntingGroundManager pirateMissionDataManager = HuntingGroundManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final SystemSession systemSession = SystemSession.getInstance();

    @Subscribe
    public void onFSSSignalDiscovered(FSSSignalDiscoveredEvent event) {
        Thread.ofVirtual().start(() -> {
            locationManager.save(updateLocation(event));

            if ("ResourceExtraction".equals(event.getSignalType())) {
                pirateMissionDataManager.confirmTargetReconResourceSite(playerSession.getPrimaryStarName());
            }

            if (event.getUssTypeLocalised() != null && event.getUssTypeLocalised().equals("Nonhuman signal source")) {
                publishVoice(localizedEvent("event.fss.signal.nonhuman", event.getThreatLevel()));
            }
            if (event.getUssType() != null && event.getUssType().contains(USS_TYPE_SALVAGE)) {
                announceSalvage("event.fss.signal.salvage.low", event);
            }
            if (event.getUssType() != null && event.getUssType().contains(USS_TYPE_VALUABLE_SALVAGE)) {
                announceSalvage("event.fss.signal.salvage.valuable", event);
            }
            if (event.getUssType() != null && event.getUssType().contains(USS_TYPE_VERY_VALUABLE_SALVAGE)) {
                announceSalvage("event.fss.signal.salvage.veryValuable", event);
            }
        });
    }

    private LocationDto updateLocation(FSSSignalDiscoveredEvent event) {
        LocationDto location = locationManager.findBySystemAddress(event.getSystemAddress());
        FssSignalDto signal = new FssSignalDto();
        signal.setSignalName(event.getSignalName());
        signal.setSignalNameLocalised(event.getSignalNameLocalised());
        signal.setSignalType(event.getSignalType());
        signal.setSpawningFaction(event.getSpawningFactionLocalised());
        signal.setSpawningState(event.getSpawningStateLocalised());
        signal.setThreatLevel(event.getThreatLevel());
        signal.setTimeRemaining(event.getTimeRemaining());
        signal.setUssType(event.getUssType());
        signal.setUssTypeLocalised(event.getUssTypeLocalised());
        signal.setSystemAddress(event.getSystemAddress());
        location.addDetectedSignal(signal);
        return location;
    }

    private void publishVoice(String message) {
        GameEventBus.publish(new DiscoveryAnnouncementEvent(message));
    }


    private void announceSalvage(String qualityKey, FSSSignalDiscoveredEvent event) {
        StringBuilder msg = new StringBuilder()
                .append(localizedEvent(qualityKey)).append(" ")
                .append(event.getUssTypeLocalised()).append(": ")
                .append(TimeUtils.secondsToMinutesRemainingString(event.getTimeRemaining()));

        if (event.getThreatLevel() > 0) {
            msg.append(localizedEvent("event.fss.signal.salvage.threatLevel", event.getThreatLevel()));
        }

        publishVoice(msg.toString());
    }

}