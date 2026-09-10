package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.FSSSignalDiscoveredEvent;
import elite.intel.gameapi.journal.events.dto.FssSignalDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.ResourceSiteProfile;
import elite.intel.gameapi.signals.ResourceSiteGrade;
import elite.intel.gameapi.signals.ResourceSiteSweep;
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
    private final HuntingGroundManager huntingGrounds = HuntingGroundManager.getInstance();
    private final MissionManager missionManager = MissionManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final SystemSession systemSession = SystemSession.getInstance();
    private final ResourceSiteSweep resourceSites = new ResourceSiteSweep();

    @Subscribe
    public void onFSSSignalDiscovered(FSSSignalDiscoveredEvent event) {
        Thread.ofVirtual().start(() -> {
            LocationDto location = updateLocation(event);
            locationManager.save(location);
            recordResourceSite(event, location);

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
            if (event.getSignalName() != null && event.getSignalName().contains(NOTABLE_STELLAR_PHENOMENON)) {
                publishVoice(localizedEvent("event.fss.notable.stellar.phenomenon"));
            }
        });
    }

    /**
     * Files a resource extraction site against the system that reported it.
     * <p>
     * WHY the location resolved from the event's own system address rather than the star we think we
     * are orbiting: signals do not always follow the arrival that explains them, and crediting one to
     * wherever the app last thought it was records resource sites in systems that have none.
     */
    private void recordResourceSite(FSSSignalDiscoveredEvent event, LocationDto location) {
        ResourceSiteGrade grade = ResourceSiteGrade.fromSymbol(event.getSignalName());
        if (grade == null) return;

        String starSystem = location.getStarName();
        if (starSystem == null || starSystem.isBlank()) return;

        ResourceSiteProfile sweep = resourceSites.add(event.getSystemAddress() + "@" + event.getTimestamp(), grade);
        huntingGrounds.recordResourceSites(
                starSystem,
                event.getSystemAddress(),
                new Coordinates(starSystem, location.getX(), location.getY(), location.getZ()),
                sweep,
                event.getTimestamp()
        );

        if (sweep.total() == 1) announceHuntingGround(starSystem);
    }

    /**
     * Speaks only when this system is where an open pirate massacre contract sends the commander.
     * <p>
     * WHY not on every hunting ground found: the game announces resource sites on arrival in most
     * populated systems, so a line here would fire a couple of hundred times in a month of ordinary
     * flying. The ledger underneath is meant to fill up quietly. It is worth hearing only when the
     * commander is standing in the system their contracts point at.
     */
    private void announceHuntingGround(String starSystem) {
        boolean targetOfOpenContract = missionManager
                .getMissions(missionManager.getPirateMissionTypes())
                .values().stream()
                .map(MissionDto::getDestinationSystem)
                .anyMatch(starSystem::equalsIgnoreCase);
        if (!targetOfOpenContract) return;

        publishVoice(localizedEvent("event.fss.huntingGroundConfirmed", starSystem));
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
        VegaRuntime.narrator().announce(message, false);
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
