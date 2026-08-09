package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.hge.HighGradeEmissionsAdvisor;
import elite.intel.gameapi.journal.events.FSSSignalDiscoveredEvent;
import elite.intel.gameapi.journal.events.dto.FssSignalDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;

import static elite.intel.util.StringUtls.localizedEvent;
import static elite.intel.util.StringUtls.localizedEventPlural;

@SuppressWarnings("unused")
public class FSSSignalDiscoveredSubscriber {

    private static final String USS_TYPE_SALVAGE = "$USS_Type_Salvage";
    private static final String USS_TYPE_VALUABLE_SALVAGE = "$USS_Type_ValuableSalvage";
    private static final String USS_TYPE_VERY_VALUABLE_SALVAGE = "$USS_Type_VeryValuableSalvage";
    private static final String NOTABLE_STELLAR_PHENOMENON = "$Fixed_Event_Life_Cloud;";
    private static final int SECONDS_PER_MINUTE = 60;

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final HuntingGroundManager pirateMissionDataManager = HuntingGroundManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final SystemSession systemSession = SystemSession.getInstance();
    private final HighGradeEmissionsAdvisor hgeAdvisor = HighGradeEmissionsAdvisor.getInstance();

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
                // The "very valuable salvage" USS type is what the game calls a High Grade Emissions
                // source, and those are the ones that drop Very Rare manufactured materials.
                if (!event.isReplay()) {
                    hgeAdvisor.onHighGradeEmissions(event.getSystemAddress());
                }
            }
            if (event.getSignalName() != null && event.getSignalName().contains(NOTABLE_STELLAR_PHENOMENON)) {
                publishVoice(localizedEvent("event.fss.notable.stellar.phenomenon"));
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
        CompanionRuntime.narrator().announce(message, false);
    }


    private void announceSalvage(String qualityKey, FSSSignalDiscoveredEvent event) {
        StringBuilder msg = new StringBuilder(localizedEvent(qualityKey));

        if (event.getUssTypeLocalised() != null && !event.getUssTypeLocalised().isBlank()) {
            msg.append(" ").append(event.getUssTypeLocalised());
        }

        String timeRemaining = formatTimeRemaining(event.getTimeRemaining());
        if (!timeRemaining.isEmpty()) {
            msg.append(": ").append(timeRemaining);
        }

        if (event.getThreatLevel() > 0) {
            msg.append(localizedEvent("event.fss.signal.salvage.threatLevel", event.getThreatLevel()));
        }

        publishVoice(msg.toString());
    }

    /**
     * TimeRemaining is absent on most signal types, so a zero here means "no timer", not "expired".
     */
    private String formatTimeRemaining(double seconds) {
        int minutes = (int) (seconds / SECONDS_PER_MINUTE);
        if (minutes <= 0) {
            return "";
        }
        return localizedEvent("event.fss.signal.timeRemaining", localizedEventPlural(minutes, "event.time.minutes"));
    }

}
