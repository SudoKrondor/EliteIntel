package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.MissionRedirectedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

@SuppressWarnings("unused")
public class MissionRedirectedSubscriber {

    private final MissionManager missionManager = MissionManager.getInstance();

    @Subscribe
    public void onMissionRedirectedSubscriber(MissionRedirectedEvent event) {
        Thread.ofVirtual().start(() -> {
            MissionDto mission = missionManager.getMission(event.getMissionID());
            // A redirect can arrive for a mission we never saw accepted - accepted on another
            // machine, or before this DB existed. Nothing to update then, and nothing to announce.
            if (mission == null) return;

            String newDestinationStation = event.getNewDestinationStation();
            String newDestinationSystem = event.getNewDestinationSystem();

            if (!newDestinationStation.isEmpty()) {
                mission.setDestinationStation(newDestinationStation);
            }
            if (!newDestinationSystem.isEmpty()) {
                mission.setDestinationSystem(newDestinationSystem);
            }
            // The redirect IS the objectives-complete signal - MassacreProgress will not call a
            // kill mission finished without it, so this has to be persisted with the destination.
            mission.setRedirectedAt(event.getTimestamp());
            missionManager.save(mission);

            String instructions = """
                        Notify user of mission update.
                         - IF new destination system present, announce new destination star system.
                         - IF new station is present announce new destination station.
                         Example: Mission for <faction> is redirected to <New System> - <New Station>
                    """;
            CompanionRuntime.narrator().narrate(
                            new MissionRedirectData(mission.getFaction(), newDestinationSystem, newDestinationStation).toYaml(),
                            instructions
                    );
        });
    }

    record MissionRedirectData(String faction, String newDestination, String newStation) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

}
